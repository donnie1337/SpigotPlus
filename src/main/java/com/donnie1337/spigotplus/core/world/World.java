package com.donnie1337.spigotplus.core.world;

import com.donnie1337.spigotplus.core.world.chunk.ChunkKey;

import java.util.Objects;

/** Server world descriptor shared by Java and Bedrock connections. */
public final class World {
    private final String name;
    private final int minY;
    private final int maxY;
    private final long seed;

    public World(String name, int minY, int maxY, long seed) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("World name is required");
        if (maxY <= minY) throw new IllegalArgumentException("Invalid world height");
        this.name = name;
        this.minY = minY;
        this.maxY = maxY;
        this.seed = seed;
    }

    public String name() { return name; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    public long seed() { return seed; }

    public ChunkKey chunk(int x, int z) {
        return new ChunkKey(name, x, z);
    }

    @Override public boolean equals(Object o) {
        return o instanceof World other && name.equals(other.name);
    }
    @Override public int hashCode() { return Objects.hash(name); }
    @Override public String toString() { return "World{" + name + "}"; }
}
