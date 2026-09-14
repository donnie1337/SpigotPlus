package com.donnie1337.spigotplus.core.world.chunk;

import java.util.*;

/** Chunk admission/scheduling foundation: bounded active area, prioritized work and shared viewer accounting. */
public final class ChunkManager {
    private final ChunkLoadController controller;
    private final ChunkBudget budget;
    private final Map<UUID, PlayerView> players = new HashMap<>();
    private final Map<ChunkKey, Integer> viewers = new HashMap<>();
    private final Map<UUID, Integer> generationInFlight = new HashMap<>();
    private final Map<UUID, Integer> sendingInFlight = new HashMap<>();
    private final PriorityQueue<ChunkTicket> generationQueue = new PriorityQueue<>();
    private final PriorityQueue<ChunkTicket> sendQueue = new PriorityQueue<>();
    private long sequence;

    public ChunkManager(ChunkLoadController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.budget = controller.budget();
    }

    public synchronized void updatePlayer(UUID playerId, String world, int chunkX, int chunkZ) {
        PlayerView view = players.computeIfAbsent(playerId, ignored -> new PlayerView());
        view.world = Objects.requireNonNull(world, "world");
        view.chunkX = chunkX;
        view.chunkZ = chunkZ;
        rebuildTickets(playerId, view);
    }

    public synchronized void removePlayer(UUID playerId) {
        PlayerView view = players.remove(playerId);
        if (view == null) return;
        for (ChunkKey key : view.active) decrementViewer(key);
        generationInFlight.remove(playerId);
        sendingInFlight.remove(playerId);
    }

    /** Admits only a small amount of work each tick; actual generation/IO/send is supplied by adapters. */
    public synchronized int pump(int generationLimit, int sendLimit) {
        int admitted = 0;
        int generated = 0;
        while (generated < generationLimit && !generationQueue.isEmpty()) {
            ChunkTicket ticket = generationQueue.poll();
            int playerCount = generationInFlight.getOrDefault(ticket.playerId(), 0);
            if (controller.tryAcquireGeneration(playerCount)) {
                generationInFlight.merge(ticket.playerId(), 1, Integer::sum);
                generationInFlight.computeIfPresent(ticket.playerId(), (id, count) -> count <= 1 ? null : count - 1);
                controller.releaseGeneration();
                generated++;
                admitted++;
            }
        }
        int sent = 0;
        while (sent < sendLimit && !sendQueue.isEmpty()) {
            ChunkTicket ticket = sendQueue.poll();
            int playerCount = sendingInFlight.getOrDefault(ticket.playerId(), 0);
            if (controller.tryAcquireSending(playerCount)) {
                sendingInFlight.merge(ticket.playerId(), 1, Integer::sum);
                sendingInFlight.computeIfPresent(ticket.playerId(), (id, count) -> count <= 1 ? null : count - 1);
                controller.releaseSending();
                sent++;
                admitted++;
            }
        }
        return admitted;
    }

    public synchronized int activeChunks(UUID playerId) {
        PlayerView view = players.get(playerId);
        return view == null ? 0 : view.active.size();
    }

    public synchronized int sharedViewers(ChunkKey key) { return viewers.getOrDefault(key, 0); }
    public ChunkBudget budget() { return budget; }

    private void rebuildTickets(UUID playerId, PlayerView view) {
        int radius = budget.viewDistance();
        Set<ChunkKey> next = new HashSet<>();
        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (next.size() >= budget.maxActiveChunksPerPlayer()) break outer;
                next.add(new ChunkKey(view.world, view.chunkX + dx, view.chunkZ + dz));
            }
        }
        for (ChunkKey key : view.active) if (!next.contains(key)) decrementViewer(key);
        for (ChunkKey key : next) if (view.active.add(key)) {
            viewers.merge(key, 1, Integer::sum);
            int distance = square(key.x() - view.chunkX) + square(key.z() - view.chunkZ);
            generationQueue.offer(new ChunkTicket(playerId, key, distance, sequence++));
            sendQueue.offer(new ChunkTicket(playerId, key, distance, sequence++));
        }
        view.active.retainAll(next);
    }

    private void decrementViewer(ChunkKey key) {
        viewers.computeIfPresent(key, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    private static int square(int value) { return value * value; }

    private static final class PlayerView {
        private String world = "world";
        private int chunkX;
        private int chunkZ;
        private final Set<ChunkKey> active = new HashSet<>();
    }
}
