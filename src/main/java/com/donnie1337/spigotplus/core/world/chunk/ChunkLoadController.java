package com.donnie1337.spigotplus.core.world.chunk;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight admission controller for chunk generation and network delivery.
 * Actual loading/generation is performed by the future chunk subsystem.
 */
public final class ChunkLoadController {
    private final ChunkBudget budget;
    private final AtomicInteger globalGenerating = new AtomicInteger();
    private final AtomicInteger globalSending = new AtomicInteger();

    public ChunkLoadController(ChunkBudget budget) {
        this.budget = budget;
    }

    public ChunkBudget budget() {
        return budget;
    }

    public boolean tryAcquireGeneration(int playerGenerating) {
        if (playerGenerating >= budget.maxGeneratingPerPlayer()) {
            return false;
        }
        return tryAcquire(globalGenerating, budget.globalMaxGenerating());
    }

    public void releaseGeneration() {
        decrement(globalGenerating);
    }

    public boolean tryAcquireSending(int playerSending) {
        if (playerSending >= budget.maxSendingPerPlayer()) {
            return false;
        }
        return tryAcquire(globalSending, budget.globalMaxSending());
    }

    public void releaseSending() {
        decrement(globalSending);
    }

    public boolean allowsActiveChunks(int activeChunks) {
        return budget.withinActiveBudget(activeChunks);
    }

    public int globalGenerating() {
        return globalGenerating.get();
    }

    public int globalSending() {
        return globalSending.get();
    }

    private static boolean tryAcquire(AtomicInteger counter, int limit) {
        while (true) {
            int current = counter.get();
            if (current >= limit) {
                return false;
            }
            if (counter.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private static void decrement(AtomicInteger counter) {
        counter.updateAndGet(value -> Math.max(0, value - 1));
    }
}
