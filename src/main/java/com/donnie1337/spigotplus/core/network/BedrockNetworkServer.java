package com.donnie1337.spigotplus.core.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/** Bedrock UDP/RakNet entry point for IPv4 and IPv6. */
public final class BedrockNetworkServer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");
    private static final byte[] RAKNET_MAGIC = {
            0x00, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
            (byte) 0xfd, 0x12, 0x34, 0x56, 0x78
    };

    private final int ipv4Port;
    private final int ipv6Port;
    private final NioEventLoopGroup workers = new NioEventLoopGroup(1);
    private final AtomicBoolean running = new AtomicBoolean();
    private final long serverGuid = System.nanoTime();
    private volatile Channel ipv4Channel;
    private volatile Channel ipv6Channel;

    public BedrockNetworkServer(int ipv4Port, int ipv6Port) {
        this.ipv4Port = ipv4Port;
        this.ipv6Port = ipv6Port;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) throw new IllegalStateException("Bedrock network server is already running");
        try {
            ipv4Channel = bind(new InetSocketAddress("0.0.0.0", ipv4Port));
            ipv6Channel = bind(new InetSocketAddress("::", ipv6Port));
            LOGGER.info("Bedrock network listening on 0.0.0.0:" + ipv4Port + " (IPv4/UDP)");
            LOGGER.info("Bedrock network listening on [::]:" + ipv6Port + " (IPv6/UDP)");
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    private Channel bind(InetSocketAddress address) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workers).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override protected void initChannel(NioDatagramChannel channel) {
                        channel.pipeline().addLast(new BedrockDatagramHandler());
                    }
                });
        return bootstrap.bind(address).syncUninterruptibly().channel();
    }

    public boolean isRunning() {
        return running.get() && ipv4Channel != null && ipv4Channel.isOpen()
                && ipv6Channel != null && ipv6Channel.isOpen();
    }

    @Override public void close() {
        if (!running.compareAndSet(true, false)) return;
        Channel ipv4 = ipv4Channel;
        Channel ipv6 = ipv6Channel;
        ipv4Channel = null;
        ipv6Channel = null;
        if (ipv4 != null) ipv4.close().syncUninterruptibly();
        if (ipv6 != null) ipv6.close().syncUninterruptibly();
        workers.shutdownGracefully().syncUninterruptibly();
        LOGGER.info("Bedrock network stopped");
    }

    private final class BedrockDatagramHandler extends io.netty.channel.SimpleChannelInboundHandler<DatagramPacket> {
        @Override protected void channelRead0(io.netty.channel.ChannelHandlerContext context, DatagramPacket packet) {
            ByteBuf in = packet.content();
            int index = in.readerIndex();
            if (in.readableBytes() < 25 || in.getUnsignedByte(index) != 0x01) return;
            int magicIndex = index + 1 + 8;
            for (int i = 0; i < RAKNET_MAGIC.length; i++) {
                if (in.getByte(magicIndex + i) != RAKNET_MAGIC[i]) return;
            }
            long timestamp = in.getLong(index + 1);
            ByteBuf out = Unpooled.buffer(256);
            out.writeByte(0x1c).writeLong(timestamp).writeLong(serverGuid).writeBytes(RAKNET_MAGIC);
            byte[] motd = ("MCPE;SpigotPlus;0;26.2;0;100;" + serverGuid + ";SpigotPlus;Survival;1;19132;19133;").getBytes(StandardCharsets.UTF_8);
            out.writeShort(motd.length).writeBytes(motd);
            context.writeAndFlush(new DatagramPacket(out, packet.sender()));
        }
    }
}
