package com.donnie1337.spigotplus.api.plugin;

/** Minimal stable lifecycle contract exposed to SpigotPlus plugins. */
public interface Plugin {
    default void onLoad() {}
    default void onEnable() {}
    default void onDisable() {}
}
