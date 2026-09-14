package com.donnie1337.spigotplus.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Launches the prebuilt Spigot runtime distributed with SpigotPlus. */
public final class ServerDistributionLauncher {
    private final Path root;

    public ServerDistributionLauncher(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Starts the already-built Spigot server.
     *
     * SpigotPlus is intentionally not a BuildTools runner at server startup.
     * The distribution must contain a valid spigot.jar produced during the
     * release/build process. This keeps production startup fast and prevents
     * BuildTools, CraftBukkit and temporary build artifacts from being created
     * in the server environment.
     */
    public int run(String[] args) throws Exception {
        Files.createDirectories(root);

        Path spigotJar = root.resolve("spigot.jar");
        if (!Files.exists(spigotJar) || Files.size(spigotJar) <= 1024 * 1024) {
            System.err.println("[SpigotPlus] spigot.jar não foi encontrado ou é inválido.");
            System.err.println("[SpigotPlus] Coloque o Spigot 26.2 já compilado como 'spigot.jar' no diretório do servidor.");
            System.err.println("[SpigotPlus] O SpigotPlus não executa o BuildTools durante a inicialização.");
            return 1;
        }

        ensureServerProperties();

        Path eula = root.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula,
                    "# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.\n"
                            + "eula=false\n");
            System.out.println("[SpigotPlus] eula.txt foi criado. Altere eula=false para eula=true antes de iniciar o servidor.");
            return 1;
        }

        String eulaText = Files.readString(eula);
        if (!eulaText.matches("(?s).*\\beula\\s*=\\s*true\\b.*")) {
            System.out.println("[SpigotPlus] EULA ainda não foi aceita. Altere eula=false para eula=true em eula.txt.");
            return 1;
        }

        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-Xms2048M");
        command.add("-Xmx4096M");
        command.add("-jar");
        command.add(spigotJar.toString());
        if (args == null || args.length == 0) {
            command.add("nogui");
        } else {
            command.addAll(List.of(args));
        }

        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .inheritIO()
                .start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> stop(process), "SpigotPlus Spigot Shutdown"));

        return process.waitFor();
    }

    private void ensureServerProperties() throws IOException {
        Path file = root.resolve("server.properties");
        if (Files.exists(file)) return;

        Files.writeString(file,
                "server-port=25565\n"
                        + "bind-address=\n"
                        + "server-ip=\n"
                        + "view-distance=10\n"
                        + "simulation-distance=6\n"
                        + "max-players=100\n"
                        + "online-mode=true\n"
                        + "enforce-secure-profile=false\n"
                        + "motd=SpigotPlus Server\n"
                        + "level-name=world\n"
                        + "allow-flight=true\n");
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        return Path.of(home, "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void stop(Process process) {
        if (!process.isAlive()) return;

        process.destroy();
        try {
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
