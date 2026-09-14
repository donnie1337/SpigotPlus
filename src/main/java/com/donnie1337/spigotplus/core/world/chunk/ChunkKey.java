package com.donnie1337.spigotplus.core.world.chunk;

public record ChunkKey(String world, int x, int z) {
    public ChunkKey {
        if (world == null || world.isBlank()) throw new IllegalArgumentException("world");
    }

    public long packed() {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
