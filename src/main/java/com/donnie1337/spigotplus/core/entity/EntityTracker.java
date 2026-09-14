package com.donnie1337.spigotplus.core.entity;

import java.util.*;

/** Spatial entity tracker using fixed-size buckets to avoid full-world scans. */
public final class EntityTracker {
    private final int bucketSize;
    private final Map<Long, Set<UUID>> buckets = new HashMap<>();
    private final Map<UUID, Long> locations = new HashMap<>();

    public EntityTracker(int bucketSize) {
        if (bucketSize <= 0) throw new IllegalArgumentException("bucketSize");
        this.bucketSize = bucketSize;
    }

    public synchronized void update(UUID entityId, int blockX, int blockZ) {
        long next = key(blockX >> 4, blockZ >> 4);
        Long previous = locations.put(entityId, next);
        if (Objects.equals(previous, next)) return;
        if (previous != null) removeFrom(previous, entityId);
        buckets.computeIfAbsent(next, ignored -> new HashSet<>()).add(entityId);
    }

    public synchronized void remove(UUID entityId) {
        Long bucket = locations.remove(entityId);
        if (bucket != null) removeFrom(bucket, entityId);
    }

    public synchronized Set<UUID> nearby(int chunkX, int chunkZ, int radius) {
        Set<UUID> result = new HashSet<>();
        int minX = chunkX - radius, maxX = chunkX + radius;
        int minZ = chunkZ - radius, maxZ = chunkZ + radius;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            Set<UUID> bucket = buckets.get(key(x, z));
            if (bucket != null) result.addAll(bucket);
        }
        return result;
    }

    private void removeFrom(long key, UUID id) {
        Set<UUID> set = buckets.get(key);
        if (set != null && set.remove(id) && set.isEmpty()) buckets.remove(key);
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
}
