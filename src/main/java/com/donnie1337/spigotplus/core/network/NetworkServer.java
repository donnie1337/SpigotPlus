package com.donnie1337.spigotplus.core.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/** Real TCP entry point for the Minecraft protocol pipeline. */
public final class NetworkServer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");

    private final String host;
    private final int port;
    private final NioEventLoopGroup boss = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workers = new NioEventLoopGroup(0);
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Channel channel;

    public NetworkServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Network server is already running");
        }
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, workers)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socket) {
                            socket.pipeline().addLast("minecraft", new MinecraftConnectionHandler());
                        }
                    });

            channel = bootstrap.bind(host, port).syncUninterruptibly().channel();
            LOGGER.info("Minecraft network listening on " + host + ":" + port);
        } catch (RuntimeException exception) {
            running.set(false);
            boss.shutdownGracefully();
            workers.shutdownGracefully();
            throw exception;
        }
    }

    public boolean isRunning() {
        return running.get() && channel != null && channel.isOpen();
    }

    public Channel channel() {
        return channel;
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) return;
        Channel current = channel;
        channel = null;
        if (current != null) current.close().syncUninterruptibly();
        boss.shutdownGracefully().syncUninterruptibly();
        workers.shutdownGracefully().syncUninterruptibly();
        LOGGER.info("Minecraft network stopped");
    }
}
