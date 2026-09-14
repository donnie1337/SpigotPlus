package com.donnie1337.spigotplus.core.world.chunk;

import com.donnie1337.spigotplus.core.config.ServerConfig;

/**
 * Server-side limits for active, generating and outgoing chunks.
 *
 * <p>The budget is deliberately independent from the client protocol so both
 * Java and Bedrock protocol adapters are subject to the same server limits.</p>
 */
public record ChunkBudget(
        int viewDistance,
        int simulationDistance,
        int maxActiveChunksPerPlayer,
        int maxGeneratingChunksPerPlayer,
        int maxSendingChunksPerPlayer,
        int globalMaxGeneratingChunks,
        int globalMaxSendingChunks
) {
    public ChunkBudget {
        if (viewDistance < 2) {
            throw new IllegalArgumentException("viewDistance must be at least 2");
        }
        if (simulationDistance < 1 || simulationDistance > viewDistance) {
            throw new IllegalArgumentException("simulationDistance must be between 1 and viewDistance");
        }
        if (maxActiveChunksPerPlayer < 1 || maxGeneratingChunksPerPlayer < 1 || maxSendingChunksPerPlayer < 1) {
            throw new IllegalArgumentException("Per-player chunk budgets must be positive");
        }
        if (globalMaxGeneratingChunks < 1 || globalMaxSendingChunks < 1) {
            throw new IllegalArgumentException("Global chunk budgets must be positive");
        }
    }

    public static ChunkBudget defaults() {
        return from(ServerConfig.defaults());
    }

    public static ChunkBudget from(ServerConfig config) {
        return new ChunkBudget(
                config.viewDistance(),
                config.simulationDistance(),
                config.maxActiveChunksPerPlayer(),
                config.maxGeneratingChunksPerPlayer(),
                config.maxSendingChunksPerPlayer(),
                config.globalMaxGeneratingChunks(),
                config.globalMaxSendingChunks()
        );
    }

    /** Returns the maximum square side represented by the active chunk budget. */
    public int activeChunkDiameter() {
        return (int) Math.floor(Math.sqrt(maxActiveChunksPerPlayer));
    }

    public boolean withinActiveBudget(int activeChunks) {
        return activeChunks <= maxActiveChunksPerPlayer;
    }

    public boolean withinGenerationBudget(int generatingChunks, int globalGeneratingChunks) {
        return generatingChunks < maxGeneratingChunksPerPlayer
                && globalGeneratingChunks < globalMaxGeneratingChunks;
    }

    public boolean withinSendBudget(int sendingChunks, int globalSendingChunks) {
        return sendingChunks < maxSendingChunksPerPlayer
                && globalSendingChunks < globalMaxSendingChunks;
    }
}
