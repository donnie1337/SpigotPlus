package com.donnie1337.spigotplus.core.scheduler;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicBoolean;

/** Minimal authoritative 20 TPS loop. It will later dispatch the Core tick phases. */
public final class TickEngine {
    private static final long TICK_NANOS = 50_000_000L;

    private final Runnable tickTask;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Thread thread;

    public TickEngine(Runnable tickTask) {
        this.tickTask = tickTask;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Tick engine is already running");
        }

        thread = new Thread(this::runLoop, "SpigotPlus Main");
        thread.setDaemon(false);
        thread.start();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        Thread current = thread;
        if (current != null) {
            LockSupport.unpark(current);
            if (current != Thread.currentThread()) {
                try {
                    current.join(5_000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void runLoop() {
        long nextTick = System.nanoTime();
        try {
            while (running.get()) {
                nextTick += TICK_NANOS;
                long start = System.nanoTime();
                tickTask.run();

                long remaining = nextTick - System.nanoTime();
                if (remaining > 0) {
                    LockSupport.parkNanos(remaining);
                } else if (System.nanoTime() - start > TICK_NANOS) {
                    // Never busy-spin when a tick overruns. The next iteration immediately
                    // measures the new schedule and naturally records the pressure.
                    nextTick = System.nanoTime();
                }
            }
        } finally {
            thread = null;
        }
    }
}
