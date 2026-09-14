package com.donnie1337.spigotplus.core.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Lightweight packet abuse guard with bounded per-key token windows. */
public final class PacketSecurity {
    private final int maxEvents;
    private final long windowNanos;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public PacketSecurity(int maxEvents, long windowMillis) {
        if (maxEvents < 1 || windowMillis < 1) throw new IllegalArgumentException();
        this.maxEvents = maxEvents;
        this.windowNanos = windowMillis * 1_000_000L;
    }

    public boolean allow(String key) {
        long now = System.nanoTime();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.started >= windowNanos) { window.started = now; window.events.set(0); }
            if (window.events.incrementAndGet() > maxEvents) return false;
            return true;
        }
    }

    public int trackedKeys() { return windows.size(); }

    private static final class Window {
        private long started;
        private final AtomicLong events = new AtomicLong();
        private Window(long started) { this.started = started; }
    }
}
