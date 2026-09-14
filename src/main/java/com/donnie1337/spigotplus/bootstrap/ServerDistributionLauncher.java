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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provisions the proven Bukkit-compatible 26.2 runtime used by SpigotPlus.
 * SpigotPlus remains the distribution/bootstrap layer; Paper provides the
 * complete Minecraft/Bukkit/Spigot implementation instead of maintaining a
 * second incomplete protocol implementation in parallel.
 */
public final class ServerDistributionLauncher {
    private static final String PAPER_VERSION = "26.2";
    private static final int PAPER_BUILD = 123;
    private static final String PAPER_URL =
            "https://api.papermc.io/v2/projects/paper/versions/26.2/builds/123/downloads/paper-26.2-123.jar";
    private static final String GEYSER_URL =
            "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot";
    private static final String VIA_VERSION_URL =
            "https://github.com/ViaVersion/ViaVersion/releases/download/5.11.0/ViaVersion-5.11.0.jar";
    private static final String VIA_BACKWARDS_URL =
            "https://github.com/ViaVersion/ViaBackwards/releases/download/5.11.0/ViaBackwards-5.11.0.jar";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final Path root;
    private final Path plugins;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public ServerDistributionLauncher(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.plugins = this.root.resolve("plugins");
    }

    public int run(String[] args) throws Exception {
        Files.createDirectories(root);
        Files.createDirectories(plugins);
        ensureServerProperties();
        ensurePaper();
        ensurePlugin("Geyser-Spigot.jar", GEYSER_URL);
        ensurePlugin("ViaVersion.jar", VIA_VERSION_URL);
        ensurePlugin("ViaBackwards.jar", VIA_BACKWARDS_URL);

        Path eula = root.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula, "# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.\neula=false\n");
            System.out.println("[SpigotPlus] eula.txt foi criado. Leia a EULA e altere eula=false para eula=true antes de iniciar o servidor.");
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
        command.add(root.resolve("paper.jar").toString());
        if (args == null || args.length == 0) command.add("nogui");
        else command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .inheritIO()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(process), "SpigotPlus Paper Shutdown"));
        return process.waitFor();
    }

    private void ensurePaper() throws IOException, InterruptedException {
        Path target = root.resolve("paper.jar");
        if (Files.exists(target) && Files.size(target) > 1024 * 1024) return;
        System.out.println("[SpigotPlus] Baixando Paper " + PAPER_VERSION + " build #" + PAPER_BUILD + "...");
        download(PAPER_URL, target);
    }

    private void ensurePlugin(String fileName, String url) throws IOException, InterruptedException {
        Path target = plugins.resolve(fileName);
        if (Files.exists(target) && Files.size(target) > 64 * 1024) return;
        System.out.println("[SpigotPlus] Baixando " + fileName + "...");
        download(url, target);
    }

    private void download(String url, Path target) throws IOException, InterruptedException {
        Path temp = target.resolveSibling(target.getFileName() + ".download");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "SpigotPlus/1.0 (+https://github.com/donnie1337/SpigotPlus)")
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + url);
        }
        try (InputStream input = response.body()) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void ensureServerProperties() throws IOException {
        Path file = root.resolve("server.properties");
        if (Files.exists(file)) return;
        Files.writeString(file, "server-port=25565\nbind-address=\nserver-ip=\nview-distance=10\nsimulation-distance=6\nmax-players=100\nonline-mode=true\nenforce-secure-profile=false\nmotd=SpigotPlus Server\nlevel-name=world\nallow-flight=true\n");
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        Path bin = Path.of(home, "bin", isWindows() ? "java.exe" : "java");
        return bin.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void stop(Process process) {
        if (!process.isAlive()) return;
        process.destroy();
        try {
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
