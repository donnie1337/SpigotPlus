package com.donnie1337.spigotplus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Launches the prebuilt Spigot runtime distributed with SpigotPlus. */
public final class ServerDistributionLauncher {
    private static final String GEYSER_VERSION = "2.11.2";
    private static final String GEYSER_BUILD = "1234";
    private static final String VIAVERSION_VERSION = "5.11.0";
    private static final String VIABACKWARDS_VERSION = "5.11.0";

    private static final List<RuntimePlugin> RUNTIME_PLUGINS = List.of(
            new RuntimePlugin(
                    "Geyser-Spigot.jar",
                    GEYSER_VERSION,
                    "https://download.geysermc.org/v2/projects/geyser/versions/" + GEYSER_VERSION
                            + "/builds/" + GEYSER_BUILD + "/downloads/spigot"),
            new RuntimePlugin(
                    "ViaVersion.jar",
                    VIAVERSION_VERSION,
                    "https://github.com/ViaVersion/ViaVersion/releases/download/" + VIAVERSION_VERSION
                            + "/ViaVersion-" + VIAVERSION_VERSION + ".jar"),
            new RuntimePlugin(
                    "ViaBackwards.jar",
                    VIABACKWARDS_VERSION,
                    "https://github.com/ViaVersion/ViaBackwards/releases/download/" + VIABACKWARDS_VERSION
                            + "/ViaBackwards-" + VIABACKWARDS_VERSION + ".jar")
    );

    private final Path root;

    public ServerDistributionLauncher(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Starts the already-built Spigot server.
     *
     * SpigotPlus does not run BuildTools at startup. Runtime compatibility
     * plugins are downloaded only when they are missing or when their local
     * implementation version does not match the version pinned above.
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

        ensureRuntimePlugins();
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

    private void ensureRuntimePlugins() throws IOException, InterruptedException {
        Path pluginsDir = root.resolve("plugins");
        Files.createDirectories(pluginsDir);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        for (RuntimePlugin plugin : RUNTIME_PLUGINS) {
            Path target = pluginsDir.resolve(plugin.fileName());
            String localVersion = readPluginVersion(target);

            if (Files.exists(target) && plugin.matches(localVersion)) {
                System.out.println("[SpigotPlus] " + plugin.fileName() + " " + localVersion + " já está atualizado.");
                continue;
            }

            if (!Files.exists(target)) {
                System.out.println("[SpigotPlus] " + plugin.fileName() + " não encontrado. Baixando " + plugin.version() + "...");
            } else {
                System.out.println("[SpigotPlus] " + plugin.fileName() + " está na versão "
                        + (localVersion == null ? "desconhecida" : localVersion)
                        + ". Atualizando para " + plugin.version() + "...");
            }

            Path temporary = pluginsDir.resolve(target.getFileName() + ".download");
            Files.deleteIfExists(temporary);

            HttpRequest request = HttpRequest.newBuilder(URI.create(plugin.url()))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "SpigotPlus/1.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                Files.deleteIfExists(temporary);
                throw new IOException("Não foi possível baixar " + plugin.fileName()
                        + ": HTTP " + response.statusCode());
            }

            try (InputStream input = response.body()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            String downloadedVersion = readPluginVersion(temporary);
            if (!plugin.matches(downloadedVersion)) {
                Files.deleteIfExists(temporary);
                throw new IOException("A versão baixada de " + plugin.fileName()
                        + " não corresponde à versão esperada " + plugin.version()
                        + " (recebida: " + downloadedVersion + ").");
            }

            Files.move(temporary, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            System.out.println("[SpigotPlus] " + plugin.fileName() + " " + downloadedVersion + " instalado.");
        }
    }

    private static String readPluginVersion(Path jar) {
        if (!Files.exists(jar) || !Files.isRegularFile(jar)) return null;

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var manifest = jarFile.getManifest();
            if (manifest == null) return null;

            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version == null || version.isBlank()) {
                version = manifest.getMainAttributes().getValue("Specification-Version");
            }
            return version == null || version.isBlank() ? null : version.trim();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
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

    private record RuntimePlugin(String fileName, String version, String url) {
        private boolean matches(String localVersion) {
            if (localVersion == null) return false;
            return localVersion.equals(version) || localVersion.startsWith(version + "-");
        }
    }
}
