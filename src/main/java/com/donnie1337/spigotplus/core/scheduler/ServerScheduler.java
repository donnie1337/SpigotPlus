package com.donnie1337.spigotplus.core.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/** Lifecycle-aware scheduler with bounded async and IO pools. */
public final class ServerScheduler {
    private final AtomicLong ids = new AtomicLong();
    private final ExecutorService async;
    private final ExecutorService io;
    private volatile boolean running;

    public ServerScheduler() {
        this.async = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                namedFactory("SpigotPlus Async-"));
        this.io = Executors.newFixedThreadPool(Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2)),
                namedFactory("SpigotPlus IO-"));
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("Scheduler is already running");
        }
        running = true;
    }

    public ScheduledTask task(Runnable action) {
        requireRunning();
        return new ScheduledTask(ids.incrementAndGet(), action);
    }

    public CompletableFuture<Void> runAsync(Runnable action) {
        requireRunning();
        return CompletableFuture.runAsync(action, async);
    }

    public CompletableFuture<Void> runIo(Runnable action) {
        requireRunning();
        return CompletableFuture.runAsync(action, io);
    }

    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        async.shutdown();
        io.shutdown();
    }

    private void requireRunning() {
        if (!running) {
            throw new IllegalStateException("Scheduler is not running");
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }

    private static final AtomicLong THREAD_COUNTER = new AtomicLong();
}
