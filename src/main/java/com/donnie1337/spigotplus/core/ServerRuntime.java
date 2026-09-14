package com.donnie1337.spigotplus.core;

import com.donnie1337.spigotplus.compatibility.bukkit.BukkitCompatibility;
import com.donnie1337.spigotplus.compatibility.bukkit.BukkitCompatibilityBridge;
import com.donnie1337.spigotplus.core.config.ServerConfig;
import com.donnie1337.spigotplus.core.entity.EntityTracker;
import com.donnie1337.spigotplus.core.network.NetworkBackpressure;
import com.donnie1337.spigotplus.core.network.PacketSecurity;
import com.donnie1337.spigotplus.core.performance.AdaptiveLoadController;
import com.donnie1337.spigotplus.core.performance.AllocationMetrics;
import com.donnie1337.spigotplus.core.performance.TickProfiler;
import com.donnie1337.spigotplus.core.scheduler.TickEngine;
import com.donnie1337.spigotplus.core.storage.AsyncIoExecutor;
import com.donnie1337.spigotplus.core.world.chunk.ChunkBudget;
import com.donnie1337.spigotplus.core.world.chunk.ChunkLoadController;
import com.donnie1337.spigotplus.core.world.chunk.ChunkManager;
import com.donnie1337.spigotplus.protocol.ProtocolRegistry;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the Core lifecycle. Version-specific code stays behind adapters. */
public final class ServerRuntime {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");
    private final String[] arguments;
    private final AtomicReference<ServerState> state = new AtomicReference<>(ServerState.NEW);
    private final CountDownLatch termination = new CountDownLatch(1);
    private final TickEngine tickEngine;
    private final ServerConfig config;
    private final ChunkLoadController chunkLoadController;
    private final ChunkManager chunkManager;
    private final EntityTracker entityTracker = new EntityTracker(16);
    private final AdaptiveLoadController adaptiveLoad = new AdaptiveLoadController();
    private final TickProfiler profiler = new TickProfiler();
    private final AllocationMetrics allocationMetrics = new AllocationMetrics();
    private final PacketSecurity packetSecurity = new PacketSecurity(200, 1000);
    private final AsyncIoExecutor ioExecutor = new AsyncIoExecutor(2, 256);
    private final ProtocolRegistry protocols = new ProtocolRegistry();
    private final BukkitCompatibility bukkit = new BukkitCompatibilityBridge();
    private final NetworkBackpressure<Object> networkBackpressure = new NetworkBackpressure<>(1024);

    public ServerRuntime(String[] arguments) {
        this.arguments = arguments == null ? new String[0] : arguments.clone();
        this.config = ServerConfig.defaults();
        this.chunkLoadController = new ChunkLoadController(ChunkBudget.from(config));
        this.chunkManager = new ChunkManager(chunkLoadController);
        this.tickEngine = new TickEngine(this::tick);
    }

    public void start() {
        if (!state.compareAndSet(ServerState.NEW, ServerState.STARTING)) {
            throw new IllegalStateException("Server can only be started from NEW state");
        }
        try {
            LOGGER.info("Starting SpigotPlus Core");
            if (arguments.length > 0) LOGGER.info("Bootstrap arguments: " + Arrays.toString(arguments));
            LOGGER.info("Chunk budget: " + config.maxActiveChunksPerPlayer() + " active/player, view "
                    + config.viewDistance() + ", simulation " + config.simulationDistance());
            LOGGER.info("Core subsystems: chunks, network, entities, async IO, adaptive load, security, profiler, Bukkit compatibility and protocol registry");
            tickEngine.start();
            state.set(ServerState.RUNNING);
            LOGGER.info("SpigotPlus Core is running at 20 TPS");
        } catch (Throwable throwable) {
            state.set(ServerState.FAILED);
            LOGGER.log(Level.SEVERE, "Failed to start SpigotPlus Core", throwable);
            termination.countDown();
            throw new IllegalStateException("Unable to start SpigotPlus Core", throwable);
        }
    }

    private void tick() {
        long start = System.nanoTime();
        try {
            if (!adaptiveLoad.deferNonEssentialWork()) chunkManager.pump(8, 32);
            else chunkManager.pump(2, 8);
        } finally {
            long elapsed = System.nanoTime() - start;
            adaptiveLoad.recordTickNanos(elapsed);
            profiler.record(elapsed);
        }
    }

    public ServerConfig config() { return config; }
    public ChunkLoadController chunkLoadController() { return chunkLoadController; }
    public ChunkManager chunkManager() { return chunkManager; }
    public EntityTracker entityTracker() { return entityTracker; }
    public AdaptiveLoadController adaptiveLoad() { return adaptiveLoad; }
    public TickProfiler profiler() { return profiler; }
    public AllocationMetrics allocationMetrics() { return allocationMetrics; }
    public PacketSecurity packetSecurity() { return packetSecurity; }
    public AsyncIoExecutor ioExecutor() { return ioExecutor; }
    public ProtocolRegistry protocols() { return protocols; }
    public BukkitCompatibility bukkit() { return bukkit; }
    public NetworkBackpressure<Object> networkBackpressure() { return networkBackpressure; }

    public void stop() {
        ServerState current = state.get();
        if (current == ServerState.STOPPED || current == ServerState.STOPPING) return;
        if (!state.compareAndSet(current, ServerState.STOPPING)) return;
        try {
            LOGGER.info("Stopping SpigotPlus Core");
            tickEngine.stop();
            ioExecutor.close();
            state.set(ServerState.STOPPED);
            LOGGER.info("SpigotPlus Core stopped cleanly");
        } catch (Throwable throwable) {
            state.set(ServerState.FAILED);
            LOGGER.log(Level.SEVERE, "Error while stopping SpigotPlus Core", throwable);
        } finally {
            termination.countDown();
        }
    }

    public void awaitTermination() {
        try { termination.await(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); stop(); }
    }

    public ServerState state() { return state.get(); }
}
