package com.donnie1337.spigotplus.bootstrap;

import com.donnie1337.spigotplus.core.ServerRuntime;

/** Entry point responsible only for creating and owning the server lifecycle. */
public final class SpigotPlusBootstrap {
    private SpigotPlusBootstrap() {
    }

    public static void main(String[] args) {
        ServerRuntime server = new ServerRuntime(args);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "SpigotPlus Shutdown"));
        server.start();

        server.awaitTermination();
    }
}
