package com.donnie1337.spigotplus.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

/** Minecraft protocol front-end: framing, handshake, status and Java 26.2 login/configuration phases. */
public final class MinecraftConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");
    private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
    private static final int PROTOCOL_26_2 = 776;
    private static final UUID SERVER_SESSION_ID = UUID.randomUUID();

    private ByteBuf pending;
    private ConnectionState state = ConnectionState.HANDSHAKE;
    private int protocolVersion = -1;
    private PlayerSession playerSession;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        pending = Unpooled.buffer(256);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        ByteBuf incoming = (ByteBuf) message;
        try {
            pending.writeBytes(incoming);
            decode(ctx);
        } finally {
            incoming.release();
        }
    }

    private void decode(ChannelHandlerContext ctx) {
        while (true) {
            int reader = pending.readerIndex();
            int length = readVarInt(pending, true);
            if (length < 0) {
                pending.readerIndex(reader);
                return;
            }
            if (length > MAX_PACKET_SIZE) {
                ctx.close();
                return;
            }
            if (pending.readableBytes() < length) {
                pending.readerIndex(reader);
                return;
            }

            ByteBuf packet = pending.readSlice(length);
            handlePacket(ctx, packet);
            if (!ctx.channel().isOpen()) return;
        }
    }

    private void handlePacket(ChannelHandlerContext ctx, ByteBuf packet) {
        int packetId = readVarInt(packet, false);
        if (packetId < 0) {
            ctx.close();
            return;
        }

        switch (state) {
            case HANDSHAKE -> handleHandshake(ctx, packetId, packet);
            case STATUS -> handleStatus(ctx, packetId, packet);
            case LOGIN -> handleLogin(ctx, packetId, packet);
            case CONFIGURATION -> handleConfiguration(ctx, packetId, packet);
            case PLAY -> handlePlay(ctx, packetId, packet);
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 0) {
            ctx.close();
            return;
        }

        int protocol = readVarInt(packet, false);
        if (protocol < 0) {
            ctx.close();
            return;
        }
        protocolVersion = protocol;
        readString(packet, 255);
        if (packet.readableBytes() < 2) {
            ctx.close();
            return;
        }
        packet.skipBytes(2);
        int nextState = readVarInt(packet, false);
        if (nextState == 1) {
            state = ConnectionState.STATUS;
            LOGGER.fine("Status handshake from protocol " + protocol);
        } else if (nextState == 2) {
            state = ConnectionState.LOGIN;
            LOGGER.fine("Login handshake from protocol " + protocol);
        } else {
            ctx.close();
        }
    }

    private void handleStatus(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId == 0) {
            String json = "{\"version\":{\"name\":\"SpigotPlus 26.2\",\"protocol\":776},\"players\":{\"max\":100,\"online\":0},\"description\":{\"text\":\"SpigotPlus Server\"}}";
            writePacket(ctx, 0, json.getBytes(StandardCharsets.UTF_8));
        } else if (packetId == 1 && packet.readableBytes() >= 8) {
            long payload = packet.readLong();
            ByteBuf body = Unpooled.buffer(16);
            writeVarInt(body, 1);
            body.writeLong(payload);
            writeFramed(ctx, body);
            body.release();
            ctx.close();
        } else {
            ctx.close();
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 0 || protocolVersion != PROTOCOL_26_2) {
            disconnect(ctx, "SpigotPlus: only Minecraft Java 26.2 is enabled for the initial login implementation.");
            return;
        }

        String username = readString(packet, 16);
        if (username == null || username.isBlank() || username.length() > 16) {
            disconnect(ctx, "SpigotPlus: invalid player name.");
            return;
        }

        playerSession = new PlayerSession(username, protocolVersion, SERVER_SESSION_ID);
        sendLoginSuccess(ctx, playerSession);
        state = ConnectionState.LOGIN_ACKNOWLEDGEMENT;
        LOGGER.info("Login accepted for " + username + " using protocol " + protocolVersion);
    }

    private void sendLoginSuccess(ChannelHandlerContext ctx, PlayerSession session) {
        ByteBuf body = Unpooled.buffer(96);
        writeVarInt(body, 2);
        writeUuid(body, session.uniqueId());
        writeString(body, session.username());
        writeVarInt(body, 0); // profile properties
        writeUuid(body, session.sessionId()); // Java 26.2 session UUID
        writeFramed(ctx, body);
        body.release();
    }

    private void handleConfiguration(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId == 3 && state == ConnectionState.CONFIGURATION) {
            state = ConnectionState.PLAY;
            disconnect(ctx, "SpigotPlus: play bootstrap is the next server phase.");
            return;
        }
        ctx.close();
    }

    private void handlePlay(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        // Gameplay packets are deliberately not decoded until the Play protocol registry
        // and world/player bootstrap are attached. Never silently accept unknown packets.
        ctx.close();
    }

    private void handleLoginAcknowledgement(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 3 || packet.isReadable()) {
            ctx.close();
            return;
        }
        state = ConnectionState.CONFIGURATION;
        writeEmptyPacket(ctx, 3); // Clientbound Finish Configuration; registry/bootstrap follows next.
        LOGGER.info("Configuration phase started for " + playerSession.username());
    }

    private void disconnect(ChannelHandlerContext ctx, String reason) {
        ByteBuf body = Unpooled.buffer(128);
        writeVarInt(body, 0);
        writeString(body, "{\"text\":\"" + escapeJson(reason) + "\"}");
        writeFramed(ctx, body);
        body.release();
        ctx.close();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeEmptyPacket(ChannelHandlerContext ctx, int packetId) {
        ByteBuf body = Unpooled.buffer(5);
        writeVarInt(body, packetId);
        writeFramed(ctx, body);
        body.release();
    }

    private static void writePacket(ChannelHandlerContext ctx, int packetId, byte[] payload) {
        ByteBuf body = Unpooled.buffer(payload.length + 8);
        writeVarInt(body, packetId);
        writeString(body, new String(payload, StandardCharsets.UTF_8));
        writeFramed(ctx, body);
        body.release();
    }

    private static void writeFramed(ChannelHandlerContext ctx, ByteBuf body) {
        ByteBuf frame = Unpooled.buffer(body.readableBytes() + 5);
        writeVarInt(frame, body.readableBytes());
        frame.writeBytes(body, body.readerIndex(), body.readableBytes());
        ctx.writeAndFlush(frame);
    }

    private static int readVarInt(ByteBuf buffer, boolean incompleteAllowed) {
        int value = 0;
        int shift = 0;
        while (buffer.isReadable() && shift < 35) {
            byte current = buffer.readByte();
            value |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) return value;
            shift += 7;
        }
        if (incompleteAllowed && shift < 35) return -1;
        return -2;
    }

    private static void writeVarInt(ByteBuf buffer, int value) {
        while ((value & 0xFFFFFF80) != 0) {
            buffer.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer.writeByte(value & 0x7F);
    }

    private static String readString(ByteBuf buffer, int maxChars) {
        int length = readVarInt(buffer, false);
        if (length < 0 || length > maxChars * 4 || length > buffer.readableBytes()) return null;
        return buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();
    }

    private static void writeString(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    private static void writeUuid(ByteBuf buffer, UUID uuid) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (pending != null) {
            pending.release();
            pending = null;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.fine("Minecraft connection closed: " + cause.getMessage());
        ctx.close();
    }

    private enum ConnectionState {
        HANDSHAKE,
        STATUS,
        LOGIN,
        LOGIN_ACKNOWLEDGEMENT,
        CONFIGURATION,
        PLAY
    }
}
