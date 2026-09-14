package com.donnie1337.spigotplus.protocol;

import java.util.NavigableMap;
import java.util.TreeMap;

/** Keeps protocol selection isolated from Core. */
public final class ProtocolRegistry {
    private final NavigableMap<ProtocolVersion, ProtocolAdapter> adapters = new TreeMap<>();

    public synchronized void register(ProtocolAdapter adapter) {
        adapters.put(adapter.version(), adapter);
    }

    public synchronized ProtocolAdapter find(ProtocolVersion requested) {
        var exact = adapters.get(requested);
        if (exact != null) return exact;
        var floor = adapters.floorEntry(requested);
        return floor == null ? null : floor.getValue();
    }

    public synchronized int size() { return adapters.size(); }
}
