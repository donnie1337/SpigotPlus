package com.donnie1337.spigotplus.core.config;

import java.nio.file.Path;

/** Immutable bootstrap configuration. Parsing will be added without coupling it to the Core. */
public record ServerConfig(
        Path serverDirectory,
        int targetTps,
        String minimumClientVersion,
        String maximumClientVersion
) {
    public static ServerConfig defaults() {
        return new ServerConfig(Path.of("server"), 20, "1.19.x", "26.2.x");
    }
}
