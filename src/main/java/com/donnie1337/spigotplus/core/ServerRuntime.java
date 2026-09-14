package com.donnie1337.spigotplus.core;

import com.donnie1337.spigotplus.core.scheduler.TickEngine;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the Core lifecycle. Version-specific code must not be placed here. */
public final class ServerRuntime {
    private static final Logger LOGGER = Logger.getLogger("SpigotPlus");

    private final String[] arguments;
    private final AtomicReference<ServerState> state = new AtomicReference<>(ServerState.NEW);
    private final CountDownLatch termination = new CountDownLatch(1);
    private final TickEngine tickEngine;

    public ServerRuntime(String[] arguments) {
        this.arguments = arguments == null ? new String[0] : arguments.clone();
        this.tickEngine = new TickEngine(this::tick);
    }

    public void start() {
        if (!state.compareAndSet(ServerState.NEW, ServerState.STARTING)) {
            throw new IllegalStateException("Server can only be started from NEW state");
        }

        try {
            LOGGER.info("Starting SpigotPlus Core");
            if (arguments.length > 0) {
                LOGGER.info("Bootstrap arguments: " + Arrays.toString(arguments));
            }

            tickEngine.start();
            state.set(ServerState.RUNNING);
            LOGGER.info("SpigotPlus Core is running at 20 TPS");
        } catch (Throwable throwable) {
            state.set(ServerState.FAILED);
            LOGGER.log(Level.SEVERE, "Failed to start SpigotPlus Core", throwable);
            termination.countDown();
            throw throwable;
        }
    }

    private void tick() {
        // World, player, plugin and network systems will be attached here in later phases.
        // The Core intentionally owns the single authoritative tick thread.
    }

    public void stop() {
        ServerState current = state.get();
        if (current == ServerState.STOPPED || current == ServerState.STOPPING) {
            return;
        }

        if (!state.compareAndSet(current, ServerState.STOPPING)) {
            return;
        }

        try {
            LOGGER.info("Stopping SpigotPlus Core");
            tickEngine.stop();
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
        try {
            termination.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            stop();
        }
    }

    public ServerState state() {
        return state.get();
    }
}
