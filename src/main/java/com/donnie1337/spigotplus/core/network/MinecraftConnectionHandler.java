package com.donnie1337.spigotplus.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

/** Minecraft protocol front-end. The wire version is kept separate from the server world model. */
public final class MinecraftConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");
    private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
    private static final int MIN_PROTOCOL = 759; // Java 1.19
    private static final int MAX_PROTOCOL = 776; // Java 26.2
    private static final UUID SERVER_SESSION_ID = UUID.randomUUID();

    private ByteBuf pending;
    private ConnectionState state = ConnectionState.HANDSHAKE;
    private int protocolVersion = -1;
    private PlayerSession playerSession;

    @Override public void channelActive(ChannelHandlerContext ctx) { pending = Unpooled.buffer(256); }

    @Override public void channelRead(ChannelHandlerContext ctx, Object message) {
        ByteBuf incoming = (ByteBuf) message;
        try { pending.writeBytes(incoming); decode(ctx); }
        finally { incoming.release(); }
    }

    private void decode(ChannelHandlerContext ctx) {
        while (ctx.channel().isOpen()) {
            int reader = pending.readerIndex();
            int length = readVarInt(pending, true);
            if (length == -1) { pending.readerIndex(reader); return; }
            if (length < 0 || length > MAX_PACKET_SIZE) { ctx.close(); return; }
            if (pending.readableBytes() < length) { pending.readerIndex(reader); return; }
            ByteBuf packet = pending.readSlice(length);
            handlePacket(ctx, packet);
        }
    }

    private void handlePacket(ChannelHandlerContext ctx, ByteBuf packet) {
        int packetId = readVarInt(packet, false);
        if (packetId < 0) { ctx.close(); return; }
        switch (state) {
            case HANDSHAKE -> handleHandshake(ctx, packetId, packet);
            case STATUS -> handleStatus(ctx, packetId, packet);
            case LOGIN -> handleLogin(ctx, packetId, packet);
            case LOGIN_ACKNOWLEDGEMENT -> handleLoginAcknowledgement(ctx, packetId, packet);
            case CONFIGURATION -> handleConfiguration(ctx, packetId, packet);
            case PLAY -> handlePlay(ctx, packetId, packet);
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 0) { ctx.close(); return; }
        int protocol = readVarInt(packet, false);
        if (protocol < 0 || readString(packet, 255) == null || packet.readableBytes() < 2) { ctx.close(); return; }
        packet.skipBytes(2);
        int nextState = readVarInt(packet, false);
        protocolVersion = protocol;
        if (nextState == 1) state = ConnectionState.STATUS;
        else if (nextState == 2) state = ConnectionState.LOGIN;
        else ctx.close();
    }

    private void handleStatus(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId == 0) {
            String json = "{\"version\":{\"name\":\"SpigotPlus 26.2\",\"protocol\":776},\"players\":{\"max\":100,\"online\":0},\"description\":{\"text\":\"SpigotPlus Server\"}}";
            ByteBuf body = Unpooled.buffer(256);
            writeVarInt(body, 0); writeString(body, json); writeFramed(ctx, body); body.release();
        } else if (packetId == 1 && packet.readableBytes() == 8) {
            ByteBuf body = Unpooled.buffer(16); writeVarInt(body, 1); body.writeLong(packet.readLong()); writeFramed(ctx, body); body.release();
        } else ctx.close();
    }

    private void handleLogin(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 0 || protocolVersion < MIN_PROTOCOL || protocolVersion > MAX_PROTOCOL) {
            disconnect(ctx, "SpigotPlus: versão Java não suportada. Use 1.19 até 26.2."); return;
        }
        String username = readString(packet, 16);
        if (username == null || username.isBlank()) { disconnect(ctx, "SpigotPlus: nome de jogador inválido."); return; }
        playerSession = new PlayerSession(username, protocolVersion, SERVER_SESSION_ID);
        sendLoginSuccess(ctx, playerSession);
        state = ConnectionState.LOGIN_ACKNOWLEDGEMENT;
        LOGGER.info("Login accepted: " + username + " protocol=" + protocolVersion);
    }

    private void sendLoginSuccess(ChannelHandlerContext ctx, PlayerSession session) {
        ByteBuf body = Unpooled.buffer(96);
        writeVarInt(body, 2); writeUuid(body, session.uniqueId()); writeString(body, session.username());
        writeVarInt(body, 0); writeUuid(body, session.sessionId());
        writeFramed(ctx, body); body.release();
    }

    private void handleLoginAcknowledgement(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        if (packetId != 3 || packet.isReadable()) { ctx.close(); return; }
        state = ConnectionState.CONFIGURATION;
        // 26.2 configuration must be negotiated before Play; this keeps the connection open.
        sendFeatureFlags(ctx);
        LOGGER.info("Configuration started: " + playerSession.username());
    }

    private void sendFeatureFlags(ChannelHandlerContext ctx) {
        // Feature Flags packet: empty set is valid for a minimal vanilla-compatible feature set.
        ByteBuf body = Unpooled.buffer(8); writeVarInt(body, 0x0C); writeVarInt(body, 0); writeFramed(ctx, body); body.release();
    }

    private void handleConfiguration(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        // Configuration packet IDs vary across protocol generations. For 26.2 the client must
        // complete the negotiation; until registry data/known-packs are attached, do not claim Play.
        if (packetId == 0x03 && !packet.isReadable()) {
            state = ConnectionState.PLAY;
            sendPlayLogin(ctx);
            return;
        }
        // Known-packs/settings/finish are intentionally tolerated while the registry bridge is built.
        if (packetId == 0x02 || packetId == 0x04 || packetId == 0x05) return;
        ctx.close();
    }

    private void sendPlayLogin(ChannelHandlerContext ctx) {
        // Protocol 776 Play Login packet ID is 0x31. Full registry-backed dimension data is the next
        // layer; this packet is emitted only after configuration completion is received.
        ByteBuf body = Unpooled.buffer(512);
        writeVarInt(body, 0x31);
        writeInt(body, 1);                 // entity id
        body.writeBoolean(false);          // hardcore
        writeVarInt(body, 1);              // dimension names count
        writeString(body, "minecraft:overworld");
        writeVarInt(body, 20);             // max players
        writeVarInt(body, 10);             // view distance
        writeVarInt(body, 6);              // simulation distance
        body.writeBoolean(false);          // reduced debug
        body.writeBoolean(true);           // respawn screen
        body.writeBoolean(true);            // limited crafting
        writeString(body, "minecraft:overworld"); // dimension type key placeholder
        writeString(body, "minecraft:overworld"); // dimension name
        body.writeLong(0L);                // hashed seed
        body.writeByte(1);                 // gamemode survival
        body.writeByte(-1);                // previous gamemode
        body.writeBoolean(false);          // debug
        body.writeBoolean(false);          // flat
        body.writeBoolean(false);          // no death location
        writeVarInt(body, 0);              // portal cooldown
        writeVarInt(body, 63);             // sea level
        body.writeBoolean(false);          // secure chat
        body.writeBoolean(false);          // online mode
        writeFramed(ctx, body); body.release();
    }

    private void handlePlay(ChannelHandlerContext ctx, int packetId, ByteBuf packet) {
        // Keep the connection alive for bootstrap packets; gameplay decoding follows the world/chunk layer.
        if (packetId == 0x15 && packet.readableBytes() >= 8) { // ping/keep-alive family guard
            return;
        }
    }

    private void disconnect(ChannelHandlerContext ctx, String reason) {
        ByteBuf body = Unpooled.buffer(128); writeVarInt(body, 0); writeString(body, "{\"text\":\"" + escapeJson(reason) + "\"}"); writeFramed(ctx, body); body.release(); ctx.close();
    }

    private static String escapeJson(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static void writeFramed(ChannelHandlerContext ctx, ByteBuf body) { ByteBuf frame = Unpooled.buffer(body.readableBytes() + 5); writeVarInt(frame, body.readableBytes()); frame.writeBytes(body, body.readerIndex(), body.readableBytes()); ctx.writeAndFlush(frame); }
    private static int readVarInt(ByteBuf b, boolean incomplete) { int value=0, shift=0; while(b.isReadable() && shift<35){byte c=b.readByte(); value|=(c&0x7F)<<shift; if((c&0x80)==0)return value; shift+=7;} return incomplete&&shift<35?-1:-2; }
    private static void writeVarInt(ByteBuf b, int value) { while((value&0xFFFFFF80)!=0){b.writeByte((value&0x7F)|0x80);value>>>=7;} b.writeByte(value&0x7F); }
    private static String readString(ByteBuf b,int maxChars){int n=readVarInt(b,false);if(n<0||n>maxChars*4||n>b.readableBytes())return null;return b.readCharSequence(n,StandardCharsets.UTF_8).toString();}
    private static void writeString(ByteBuf b,String s){byte[] x=s.getBytes(StandardCharsets.UTF_8);writeVarInt(b,x.length);b.writeBytes(x);}
    private static void writeUuid(ByteBuf b,UUID u){b.writeLong(u.getMostSignificantBits());b.writeLong(u.getLeastSignificantBits());}
    private static void writeInt(ByteBuf b,int v){b.writeInt(v);}
    @Override public void channelInactive(ChannelHandlerContext ctx){if(pending!=null){pending.release();pending=null;}}
    @Override public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){LOGGER.fine("Minecraft connection closed: "+cause.getMessage());ctx.close();}
    private enum ConnectionState { HANDSHAKE, STATUS, LOGIN, LOGIN_ACKNOWLEDGEMENT, CONFIGURATION, PLAY }
}
