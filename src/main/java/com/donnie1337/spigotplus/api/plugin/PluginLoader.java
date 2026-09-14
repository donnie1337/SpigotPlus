package com.donnie1337.spigotplus.api.plugin;

import java.nio.file.Path;
import java.util.List;

/** Loads plugins without coupling the core to any particular plugin repository. */
public interface PluginLoader {
    List<LoadedPlugin> load(Path pluginsDirectory) throws Exception;

    record LoadedPlugin(Plugin plugin, PluginDescription description, Path source) {}
}
