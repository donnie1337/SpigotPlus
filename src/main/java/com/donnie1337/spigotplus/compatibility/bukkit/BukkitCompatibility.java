package com.donnie1337.spigotplus.compatibility.bukkit;

/** Stable compatibility boundary for Bukkit-facing services. */
public interface BukkitCompatibility {
    String apiName();
    boolean supportsEvents();
    boolean supportsCommands();
    boolean supportsPlayers();
}
