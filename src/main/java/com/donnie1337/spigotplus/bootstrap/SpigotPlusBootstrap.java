package com.donnie1337.spigotplus.bootstrap;

import java.nio.file.Path;

/** Entry point for the SpigotPlus distribution runtime. */
public final class SpigotPlusBootstrap {
    private SpigotPlusBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new ServerDistributionLauncher(Path.of("."))
                .run(args);
        if (exitCode != 0) System.exit(exitCode);
    }
}
