package com.donnie1337.spigotplus.core.performance;

/** Cheap load governor based on recent MSPT. */
public final class AdaptiveLoadController {
    private volatile double mspt = 0.0;
    private volatile LoadLevel level = LoadLevel.NORMAL;

    public void recordTickNanos(long nanos) {
        double current = nanos / 1_000_000.0;
        mspt = mspt == 0.0 ? current : (mspt * 0.90) + (current * 0.10);
        level = mspt >= 100 ? LoadLevel.CRITICAL : mspt >= 50 ? LoadLevel.HEAVY : mspt >= 40 ? LoadLevel.ELEVATED : LoadLevel.NORMAL;
    }

    public double mspt() { return mspt; }
    public LoadLevel level() { return level; }
    public boolean deferNonEssentialWork() { return level != LoadLevel.NORMAL; }
    public enum LoadLevel { NORMAL, ELEVATED, HEAVY, CRITICAL }
}
