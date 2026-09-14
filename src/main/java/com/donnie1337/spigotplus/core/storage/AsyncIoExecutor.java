package com.donnie1337.spigotplus.core.storage;

import java.util.concurrent.*;

/** Bounded IO executor. Core never creates an unbounded thread per operation. */
public final class AsyncIoExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    public AsyncIoExecutor(int threads, int queueCapacity) {
        if (threads < 1 || queueCapacity < 1) throw new IllegalArgumentException();
        this.executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor.allowCoreThreadTimeOut(true);
    }

    public Future<?> submit(Runnable task) { return executor.submit(task); }
    public <T> Future<T> submit(Callable<T> task) { return executor.submit(task); }
    public int queuedTasks() { return executor.getQueue().size(); }
    public int activeTasks() { return executor.getActiveCount(); }
    @Override public void close() { executor.shutdown(); }
}
