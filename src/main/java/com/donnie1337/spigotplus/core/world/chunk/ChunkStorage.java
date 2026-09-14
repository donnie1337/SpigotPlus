package com.donnie1337.spigotplus.core.world.chunk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal crash-safe chunk container. Protocol encoders can read the same internal data for Java/Bedrock. */
public final class ChunkStorage {
    private static final int MAGIC = 0x53504348; // SPCH
    private final Path directory;

    public ChunkStorage(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    public synchronized void save(StoredChunk chunk) throws IOException {
        Path target = file(chunk.key());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
            out.writeInt(MAGIC);
            out.writeInt(chunk.key().x());
            out.writeInt(chunk.key().z());
            out.writeInt(chunk.version());
            out.writeLong(chunk.seed());
        }
        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    public synchronized StoredChunk load(ChunkKey key) throws IOException {
        Path target = file(key);
        if (!Files.exists(target)) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(target)))) {
            if (in.readInt() != MAGIC) throw new IOException("Invalid SpigotPlus chunk file: " + target);
            int x = in.readInt();
            int z = in.readInt();
            int version = in.readInt();
            long seed = in.readLong();
            if (x != key.x() || z != key.z()) throw new IOException("Chunk coordinate mismatch: " + target);
            return new StoredChunk(key, version, seed);
        }
    }

    public Path file(ChunkKey key) {
        return directory.resolve(key.x() + "." + key.z() + ".spch");
    }

    public record StoredChunk(ChunkKey key, int version, long seed) {}
}
