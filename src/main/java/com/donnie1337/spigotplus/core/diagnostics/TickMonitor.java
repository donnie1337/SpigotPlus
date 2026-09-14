package com.donnie1337.spigotplus.core.diagnostics;

import java.util.concurrent.atomic.AtomicLong;

/** Lightweight tick diagnostics. All values are nanosecond based and lock-free. */
public final class TickMonitor {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private final AtomicLong tickCount = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();
    private final AtomicLong worstNanos = new AtomicLong();
    private volatile long windowStart = System.nanoTime();
    private volatile double tps = 20.0D;

    public void record(long durationNanos) {
        tickCount.incrementAndGet();
        totalNanos.addAndGet(durationNanos);
        worstNanos.accumulateAndGet(durationNanos, Math::max);
        long now = System.nanoTime();
        long elapsed = now - windowStart;
        if (elapsed >= NANOS_PER_SECOND) {
            long count = tickCount.getAndSet(0);
            tps = Math.min(20.0D, count * (NANOS_PER_SECOND / (double) elapsed));
            totalNanos.set(0);
            worstNanos.set(0);
            windowStart = now;
        }
    }

    public double tps() {
        return tps;
    }

    public double mspt() {
        long count = tickCount.get();
        if (count == 0) {
            return 0.0D;
        }
        return totalNanos.get() / 1_000_000.0D / count;
    }

    public double worstMspt() {
        return worstNanos.get() / 1_000_000.0D;
    }
}
