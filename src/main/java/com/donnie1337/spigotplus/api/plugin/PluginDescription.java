package com.donnie1337.spigotplus.api.plugin;

import java.util.List;

public record PluginDescription(
        String name,
        String version,
        String mainClass,
        List<String> authors
) {
    public PluginDescription {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Plugin name is required");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Plugin version is required");
        if (mainClass == null || mainClass.isBlank()) throw new IllegalArgumentException("Plugin main class is required");
        authors = authors == null ? List.of() : List.copyOf(authors);
    }
}
