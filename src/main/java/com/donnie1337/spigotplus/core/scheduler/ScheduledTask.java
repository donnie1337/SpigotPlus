package com.donnie1337.spigotplus.core.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

/** A cancellable task handle owned by the SpigotPlus scheduler. */
public final class ScheduledTask {
    private final long id;
    private final Runnable action;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    ScheduledTask(long id, Runnable action) {
        this.id = id;
        this.action = action;
    }

    public long id() {
        return id;
    }

    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    public boolean cancelled() {
        return cancelled.get();
    }

    void run() {
        if (!cancelled()) {
            action.run();
        }
    }
}
