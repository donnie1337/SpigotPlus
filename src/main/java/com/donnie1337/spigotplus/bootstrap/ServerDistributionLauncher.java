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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Provisions a real Spigot runtime for SpigotPlus. */
public final class ServerDistributionLauncher {
    private static final String SPIGOT_VERSION = "26.2";
    private static final String BUILD_TOOLS_URL =
            "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";
    private static final String GEYSER_URL =
            "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot";
    private static final String VIA_VERSION_URL =
            "https://github.com/ViaVersion/ViaVersion/releases/download/5.11.0/ViaVersion-5.11.0.jar";
    private static final String VIA_BACKWARDS_URL =
            "https://github.com/ViaVersion/ViaBackwards/releases/download/5.11.0/ViaBackwards-5.11.0.jar";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final Path root;
    private final Path plugins;
    private final Path buildDirectory;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ServerDistributionLauncher(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.plugins = this.root.resolve("plugins");
        // Keep BuildTools completely outside the server directory. This avoids OneDrive,
        // cloud-sync and path/line-ending interference with Git patch application.
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            this.buildDirectory = Path.of(localAppData, "SpigotPlus", "BuildTools", SPIGOT_VERSION);
        } else {
            this.buildDirectory = Path.of(System.getProperty("java.io.tmpdir"), "SpigotPlus", "BuildTools", SPIGOT_VERSION);
        }
    }

    public int run(String[] args) throws Exception {
        Files.createDirectories(root);
        Files.createDirectories(plugins);
        ensureServerProperties();
        ensureSpigot();
        ensurePlugin("Geyser-Spigot.jar", GEYSER_URL);
        ensurePlugin("ViaVersion.jar", VIA_VERSION_URL);
        ensurePlugin("ViaBackwards.jar", VIA_BACKWARDS_URL);

        Path eula = root.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula, "# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.\neula=false\n");
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
        command.add(root.resolve("spigot.jar").toString());
        if (args == null || args.length == 0) command.add("nogui");
        else command.addAll(List.of(args));

        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(process), "SpigotPlus Spigot Shutdown"));
        return process.waitFor();
    }

    private void ensureSpigot() throws IOException, InterruptedException {
        Path target = root.resolve("spigot.jar");
        if (Files.exists(target) && Files.size(target) > 1024 * 1024) return;

        Files.createDirectories(buildDirectory);
        Path buildTools = buildDirectory.resolve("BuildTools.jar");
        if (!Files.exists(buildTools) || Files.size(buildTools) < 64 * 1024) {
            System.out.println("[SpigotPlus] Baixando Spigot BuildTools...");
            download(BUILD_TOOLS_URL, buildTools);
        }

        cleanBuildWorkspace();
        System.out.println("[SpigotPlus] Construindo Spigot " + SPIGOT_VERSION + " com o BuildTools oficial...");
        Process process = new ProcessBuilder(javaExecutable(), "-jar", buildTools.toString(), "--rev", SPIGOT_VERSION)
                .directory(buildDirectory.toFile()).inheritIO().start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new IOException("Spigot BuildTools terminou com código " + exitCode + ". Verifique o log acima.");

        Path builtJar = buildDirectory.resolve("spigot-" + SPIGOT_VERSION + ".jar");
        if (!Files.exists(builtJar) || Files.size(builtJar) <= 1024 * 1024) {
            try (Stream<Path> files = Files.list(buildDirectory)) {
                builtJar = files.filter(path -> path.getFileName().toString().startsWith("spigot-") && path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> {
                            try { return Files.size(path) > 1024 * 1024; } catch (IOException ignored) { return false; }
                        }).findFirst().orElse(null);
            }
        }
        if (builtJar == null || !Files.exists(builtJar)) throw new IOException("O BuildTools terminou sem gerar o spigot-" + SPIGOT_VERSION + ".jar.");
        Files.copy(builtJar, target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[SpigotPlus] Spigot " + SPIGOT_VERSION + " pronto em " + target.getFileName() + ".");
    }

    private void cleanBuildWorkspace() throws IOException {
        if (!Files.exists(buildDirectory)) return;
        try (Stream<Path> entries = Files.list(buildDirectory)) {
            entries.filter(path -> !path.equals(buildDirectory.resolve("BuildTools.jar")))
                    .sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { deleteRecursively(path); } catch (IOException e) { throw new BuildCleanupException(e); }
                    });
        } catch (BuildCleanupException e) { throw e.getCause(); }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> children = Files.list(path)) {
                children.forEach(child -> { try { deleteRecursively(child); } catch (IOException e) { throw new BuildCleanupException(e); } });
            } catch (BuildCleanupException e) { throw e.getCause(); }
        }
        Files.deleteIfExists(path);
    }

    private static final class BuildCleanupException extends RuntimeException {
        private BuildCleanupException(IOException cause) { super(cause); }
        @Override public synchronized IOException getCause() { return (IOException) super.getCause(); }
    }

    private void ensurePlugin(String fileName, String url) throws IOException, InterruptedException {
        Path target = plugins.resolve(fileName);
        if (Files.exists(target) && Files.size(target) > 64 * 1024) return;
        System.out.println("[SpigotPlus] Baixando " + fileName + "...");
        download(url, target);
    }

    private void download(String url, Path target) throws IOException, InterruptedException {
        Path temp = target.resolveSibling(target.getFileName() + ".download");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10))
                .header("User-Agent", "SpigotPlus/1.0 (+https://github.com/donnie1337/SpigotPlus)").GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + url);
        }
        try (InputStream input = response.body()) { Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING); }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void ensureServerProperties() throws IOException {
        Path file = root.resolve("server.properties");
        if (Files.exists(file)) return;
        Files.writeString(file, "server-port=25565\nbind-address=\nserver-ip=\nview-distance=10\nsimulation-distance=6\nmax-players=100\nonline-mode=true\nenforce-secure-profile=false\nmotd=SpigotPlus Server\nlevel-name=world\nallow-flight=true\n");
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        return Path.of(home, "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    private static void stop(Process process) {
        if (!process.isAlive()) return;
        process.destroy();
        try { if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); process.destroyForcibly(); }
    }
}
