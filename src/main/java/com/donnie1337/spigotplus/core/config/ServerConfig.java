package com.donnie1337.spigotplus.core.config;

import java.nio.file.Path;

/** Immutable bootstrap configuration and server-side chunk budgets. */
public record ServerConfig(
        Path serverDirectory,
        int targetTps,
        String minimumClientVersion,
        String maximumClientVersion,
        int viewDistance,
        int simulationDistance,
        int maxActiveChunksPerPlayer,
        int maxGeneratingChunksPerPlayer,
        int maxSendingChunksPerPlayer,
        int globalMaxGeneratingChunks,
        int globalMaxSendingChunks
) {
    public static ServerConfig defaults() {
        return new ServerConfig(
                Path.of("server"),
                20,
                "1.19.x",
                "26.2.x",
                10,
                6,
                225,
                3,
                4,
                8,
                32
        );
    }
}
