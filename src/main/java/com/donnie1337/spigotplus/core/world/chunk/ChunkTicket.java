package com.donnie1337.spigotplus.core.world.chunk;

import java.util.UUID;

public record ChunkTicket(UUID playerId, ChunkKey key, int distanceSquared, long sequence) implements Comparable<ChunkTicket> {
    @Override
    public int compareTo(ChunkTicket other) {
        int distance = Integer.compare(distanceSquared, other.distanceSquared);
        return distance != 0 ? distance : Long.compare(sequence, other.sequence);
    }
}
