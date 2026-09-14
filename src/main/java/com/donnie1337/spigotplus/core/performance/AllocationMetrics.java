package com.donnie1337.spigotplus.core.performance;

import java.util.concurrent.atomic.AtomicLong;

/** Allocation-friendly counters kept off hot paths unless diagnostics are enabled. */
public final class AllocationMetrics {
    private final AtomicLong createdObjects = new AtomicLong();
    private final AtomicLong rejectedAllocations = new AtomicLong();
    private volatile boolean enabled;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void recordObject() { if (enabled) createdObjects.incrementAndGet(); }
    public void recordRejected() { if (enabled) rejectedAllocations.incrementAndGet(); }
    public long createdObjects() { return createdObjects.get(); }
    public long rejectedAllocations() { return rejectedAllocations.get(); }
}
