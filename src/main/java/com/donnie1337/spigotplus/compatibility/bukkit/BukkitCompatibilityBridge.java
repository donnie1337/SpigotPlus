package com.donnie1337.spigotplus.compatibility.bukkit;

/** Initial Bukkit compatibility contract; implementations can bridge the real Bukkit API later. */
public final class BukkitCompatibilityBridge implements BukkitCompatibility {
    @Override public String apiName() { return "Bukkit"; }
    @Override public boolean supportsEvents() { return true; }
    @Override public boolean supportsCommands() { return true; }
    @Override public boolean supportsPlayers() { return true; }
}
