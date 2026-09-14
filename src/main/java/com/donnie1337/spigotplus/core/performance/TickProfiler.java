package com.donnie1337.spigotplus.core.performance;

import java.util.concurrent.atomic.AtomicLong;

/** Low-overhead tick profiler. Detailed samples are opt-in by recording callers. */
public final class TickProfiler {
    private final AtomicLong ticks = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();
    private volatile boolean enabled;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean enabled() { return enabled; }

    public void record(long nanos) {
        if (!enabled) return;
        ticks.incrementAndGet();
        totalNanos.addAndGet(nanos);
    }

    public long ticks() { return ticks.get(); }
    public double averageMspt() {
        long count = ticks.get();
        return count == 0 ? 0.0 : totalNanos.get() / 1_000_000.0 / count;
    }
}
