package com.donnie1337.spigotplus.core.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/** Owns the worlds visible to every protocol adapter. */
public final class WorldManager {
    private final Path worldsDirectory;
    private final Map<String, World> worlds = new LinkedHashMap<>();

    public WorldManager(Path worldsDirectory) {
        this.worldsDirectory = Objects.requireNonNull(worldsDirectory, "worldsDirectory");
    }

    public synchronized World loadOrCreate(String name, int minY, int maxY) throws IOException {
        Objects.requireNonNull(name, "name");
        World existing = worlds.get(name);
        if (existing != null) return existing;
        Files.createDirectories(worldsDirectory.resolve(name));
        Path seedFile = worldsDirectory.resolve(name).resolve("seed.dat");
        long seed;
        if (Files.exists(seedFile)) seed = Long.parseLong(Files.readString(seedFile).trim());
        else {
            seed = ThreadLocalRandom.current().nextLong();
            Files.writeString(seedFile, Long.toString(seed));
        }
        World world = new World(name, minY, maxY, seed);
        worlds.put(name, world);
        return world;
    }

    public synchronized World get(String name) { return worlds.get(name); }
    public synchronized Collection<World> worlds() { return java.util.List.copyOf(worlds.values()); }
    public Path worldsDirectory() { return worldsDirectory; }
}
