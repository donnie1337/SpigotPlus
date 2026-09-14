package com.donnie1337.spigotplus.core.plugin;

import com.donnie1337.spigotplus.api.plugin.Plugin;
import com.donnie1337.spigotplus.api.plugin.PluginDescription;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Core plugin lifecycle manager. Plugin-specific behavior stays outside the server core. */
public final class PluginManager {
    private final Map<String, RegisteredPlugin> plugins = new LinkedHashMap<>();

    public synchronized void register(Plugin plugin, PluginDescription description) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(description, "description");
        String key = description.name().toLowerCase(java.util.Locale.ROOT);
        if (plugins.containsKey(key)) throw new IllegalArgumentException("Plugin already registered: " + description.name());
        plugins.put(key, new RegisteredPlugin(plugin, description));
    }

    public synchronized void enableAll() {
        for (RegisteredPlugin registered : plugins.values()) registered.plugin().onLoad();
        for (RegisteredPlugin registered : plugins.values()) registered.plugin().onEnable();
    }

    public synchronized void disableAll() {
        var values = plugins.values().stream().toList();
        for (int i = values.size() - 1; i >= 0; i--) values.get(i).plugin().onDisable();
    }

    public synchronized Plugin getPlugin(String name) {
        RegisteredPlugin registered = plugins.get(name.toLowerCase(java.util.Locale.ROOT));
        return registered == null ? null : registered.plugin();
    }

    public synchronized Collection<PluginDescription> descriptions() {
        return plugins.values().stream().map(RegisteredPlugin::description).toList();
    }

    private record RegisteredPlugin(Plugin plugin, PluginDescription description) {}
}
