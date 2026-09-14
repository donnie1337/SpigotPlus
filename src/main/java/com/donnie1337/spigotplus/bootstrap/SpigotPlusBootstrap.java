package com.donnie1337.spigotplus.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Single-JAR entry point. The release build embeds the Spigot server classes
 * into this same JAR, so no external spigot.jar is required at runtime.
 */
public final class SpigotPlusBootstrap {
    private SpigotPlusBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        // Spigot exposes this development-oriented property to bypass its
        // intentionally annoying outdated-build startup delay. SpigotPlus
        // owns the release build, so the check is not useful to the runtime.
        System.setProperty("IReallyKnowWhatIAmDoingISwear", "true");

        Class<?> spigotMain = Class.forName("org.bukkit.craftbukkit.Main");
        Method main = spigotMain.getMethod("main", String[].class);
        try {
            main.invoke(null, (Object) (args == null ? new String[0] : args));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw e;
        }
    }
}
