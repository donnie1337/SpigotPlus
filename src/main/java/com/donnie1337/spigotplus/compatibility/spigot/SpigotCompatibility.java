package com.donnie1337.spigotplus.compatibility.spigot;

/** Spigot-facing compatibility boundary kept outside the Core implementation. */
public interface SpigotCompatibility {
    String apiName();
    boolean supportsLegacyPluginContracts();
}
