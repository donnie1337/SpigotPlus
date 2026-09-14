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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

public final class ServerDistributionLauncher {
    private static final String CONFIG_FILE = "spigotplus.properties";
    private static final String SPIGOT_VERSION = "26.2";
    private static final String JAVA_VERSION = "26";
    private static final String DEFAULT_GEYSER = "2.11.2";
    private static final String DEFAULT_VIA = "5.11.0";
    private final Path root;
    private final Properties config = new Properties();

    public ServerDistributionLauncher(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public int run(String[] args) throws Exception {
        Files.createDirectories(root);
        loadConfig();

        if (has(args, "status") || has(args, "--status")) {
            status();
            return 0;
        }

        validateJava();
        Path spigot = root.resolve("spigot.jar");
        if (!validJar(spigot)) {
            System.err.println("[SpigotPlus] spigot.jar não foi encontrado ou é inválido.");
            System.err.println("[SpigotPlus] Coloque o Spigot " + getSpigotVersion() + " já compilado como 'spigot.jar'.");
            System.err.println("[SpigotPlus] O SpigotPlus não executa o BuildTools durante a inicialização.");
            return 1;
        }

        if (isEnabled("updates.auto-update", true)) {
            ensureRuntimePlugins();
        } else {
            System.out.println("[SpigotPlus] Atualizações automáticas desativadas.");
        }

        ensureServerProperties();
        ensureEula();

        List<String> command = new ArrayList<>(List.of(
                javaExecutable(),
                "-Xms" + get("memory.min", "2048M"),
                "-Xmx" + get("memory.max", "4096M"),
                "-jar",
                spigot.toString()
        ));
        if (isEnabled("startup.nogui", true)) command.add("nogui");

        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .inheritIO()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(process), "SpigotPlus Shutdown"));
        return process.waitFor();
    }

    private void loadConfig() throws IOException {
        Path file = root.resolve(CONFIG_FILE);
        if (!Files.exists(file)) {
            try (InputStream in = ServerDistributionLauncher.class.getResourceAsStream("/spigotplus.properties")) {
                if (in != null) {
                    Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.writeString(file, defaultConfig());
                }
            }
            System.out.println("[SpigotPlus] Configuração criada: " + CONFIG_FILE);
        }
        try (InputStream in = Files.newInputStream(file)) {
            config.load(in);
        }
    }

    private void validateJava() {
        String actual = String.valueOf(Runtime.version().feature());
        String expected = getJavaVersion();
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Java " + expected + " é necessária; detectada Java " + actual + ".");
        }
    }

    private void ensureRuntimePlugins() throws IOException, InterruptedException {
        Path plugins = root.resolve("plugins");
        Path backup = root.resolve(get("backup.directory", "plugins/.backup"));
        Files.createDirectories(plugins);
        if (isEnabled("backup.enabled", true)) Files.createDirectories(backup);

        String geyser = getCompatibilityVersion("compatibility.geyser-version", "geyser.version", DEFAULT_GEYSER);
        String via = getCompatibilityVersion("compatibility.viaversion-version", "viaversion.version", DEFAULT_VIA);
        String backwards = getCompatibilityVersion("compatibility.viabackwards-version", "viabackwards.version", DEFAULT_VIA);

        List<RuntimePlugin> list = List.of(
                new RuntimePlugin("Geyser-Spigot.jar", geyser,
                        "https://download.geysermc.org/v2/projects/geyser/versions/" + geyser + "/builds/latest/downloads/spigot"),
                new RuntimePlugin("ViaVersion.jar", via,
                        "https://github.com/ViaVersion/ViaVersion/releases/download/" + via + "/ViaVersion-" + via + ".jar"),
                new RuntimePlugin("ViaBackwards.jar", backwards,
                        "https://github.com/ViaVersion/ViaBackwards/releases/download/" + backwards + "/ViaBackwards-" + backwards + ".jar")
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        int retries = Math.max(1, getInt("updates.retries", 3));
        long retryDelay = Math.max(0L, getLong("updates.retry-delay-ms", 1500L));

        for (RuntimePlugin p : list) {
            Path target = plugins.resolve(p.fileName());
            String local = version(target);
            if (validJar(target) && p.matches(local)) {
                System.out.println("[SpigotPlus] " + p.fileName() + " " + local + " ✓");
                continue;
            }

            Path tmp = plugins.resolve(p.fileName() + ".download");
            Files.deleteIfExists(tmp);
            Exception error = null;

            for (int attempt = 1; attempt <= retries; attempt++) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(p.url()))
                            .timeout(Duration.ofMinutes(5))
                            .header("User-Agent", "SpigotPlus/1.0")
                            .GET()
                            .build();
                    HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() < 200 || res.statusCode() >= 300) {
                        res.body().close();
                        throw new IOException("HTTP " + res.statusCode());
                    }
                    try (InputStream in = res.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }

                    String downloaded = version(tmp);
                    if (!validJar(tmp) || !p.matches(downloaded)) {
                        throw new IOException("JAR inválido ou versão inesperada: " + downloaded);
                    }

                    String expectedSha = get("sha256." + p.fileName(), "");
                    if (!expectedSha.isBlank() && !expectedSha.equalsIgnoreCase(sha256(tmp))) {
                        throw new IOException("SHA-256 inválido");
                    }

                    if (isEnabled("backup.enabled", true) && validJar(target)) {
                        Files.copy(target,
                                backup.resolve(p.fileName() + "." + (local == null ? "unknown" : local) + ".jar"),
                                StandardCopyOption.REPLACE_EXISTING);
                    }

                    replace(tmp, target);
                    System.out.println("[SpigotPlus] " + p.fileName() + " " + downloaded + " ✓");
                    error = null;
                    break;
                } catch (Exception e) {
                    error = e;
                    Files.deleteIfExists(tmp);
                    if (attempt < retries) Thread.sleep(retryDelay * attempt);
                }
            }

            if (error != null && !(validJar(target) && p.matches(version(target)))) {
                throw new IOException("Falha ao instalar " + p.fileName(), error);
            }
            if (error != null) {
                System.err.println("[SpigotPlus] Falha na atualização; mantendo " + p.fileName() + " atual.");
            }
        }
    }

    private void ensureEula() throws IOException {
        Path eula = root.resolve("eula.txt");
        if (!Files.exists(eula)) {
            Files.writeString(eula, "eula=false\n");
            throw new IllegalStateException("EULA criada. Aceite-a em eula.txt.");
        }
        if (!Files.readString(eula).matches("(?s).*\\beula\\s*=\\s*true\\b.*")) {
            throw new IllegalStateException("EULA ainda não foi aceita.");
        }
    }

    private void ensureServerProperties() throws IOException {
        Path file = root.resolve("server.properties");
        if (!Files.exists(file)) {
            Files.writeString(file,
                    "server-port=25565\n" +
                    "server-ip=\n" +
                    "view-distance=10\n" +
                    "simulation-distance=6\n" +
                    "max-players=100\n" +
                    "online-mode=true\n" +
                    "enforce-secure-profile=false\n" +
                    "motd=SpigotPlus Server\n" +
                    "level-name=world\n" +
                    "allow-flight=true\n");
        }
    }

    private void status() {
        System.out.println("[SpigotPlus] Status");
        System.out.println("Spigot " + getSpigotVersion() + ": " + (validJar(root.resolve("spigot.jar")) ? "✓" : "✗"));
        System.out.println("Java " + Runtime.version().feature() + ": " +
                (String.valueOf(Runtime.version().feature()).equals(getJavaVersion()) ? "✓" : "✗"));
        System.out.println("Auto-update: " + (isEnabled("updates.auto-update", true) ? "ON" : "OFF"));
        System.out.println("Backup: " + (isEnabled("backup.enabled", true) ? "ON" : "OFF"));
        System.out.println("Diagnostics: " + (isEnabled("diagnostics.enabled", true) ? "ON" : "OFF"));
        System.out.println("Memória: " + get("memory.min", "2048M") + " → " + get("memory.max", "4096M"));
        for (String name : List.of("Geyser-Spigot.jar", "ViaVersion.jar", "ViaBackwards.jar", "EssentialsPlus.jar", "CargoPlus.jar", "UtilidadesPlus.jar", "ChatPlus.jar", "LoginPlus.jar", "ClanPlus.jar")) {
            Path file = root.resolve("plugins").resolve(name);
            System.out.println(name + ": " + (validJar(file) ? "✓" : "✗"));
        }
    }

    private String getSpigotVersion() { return getCompatibilityVersion("server.spigot-version", "spigot.version", SPIGOT_VERSION); }
    private String getJavaVersion() { return getCompatibilityVersion("server.java-version", "java.version", JAVA_VERSION); }

    private String getCompatibilityVersion(String modernKey, String legacyKey, String fallback) {
        String modern = config.getProperty(modernKey);
        if (modern != null && !modern.isBlank()) return modern.trim();
        return get(legacyKey, fallback);
    }

    private int getInt(String key, int fallback) {
        try { return Integer.parseInt(get(key, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private long getLong(String key, long fallback) {
        try { return Long.parseLong(get(key, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private boolean isEnabled(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, String.valueOf(fallback)));
    }

    private static boolean validJar(Path p) {
        if (!Files.isRegularFile(p)) return false;
        try (JarFile ignored = new JarFile(p.toFile())) { return true; }
        catch (IOException | RuntimeException e) { return false; }
    }

    private static String version(Path p) {
        if (!validJar(p)) return null;
        try (JarFile jar = new JarFile(p.toFile())) {
            var m = jar.getManifest();
            if (m == null) return null;
            String v = m.getMainAttributes().getValue("Implementation-Version");
            if (v == null || v.isBlank()) v = m.getMainAttributes().getValue("Specification-Version");
            return v == null || v.isBlank() ? null : v.trim();
        } catch (IOException | RuntimeException e) { return null; }
    }

    private static String sha256(Path p) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) != -1) d.update(b, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte b : d.digest()) s.append(String.format("%02x", b));
        return s.toString();
    }

    private static void replace(Path src, Path dst) throws IOException {
        try { Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING); }
    }

    private String get(String key, String fallback) { return config.getProperty(key, fallback).trim(); }

    private static String defaultConfig() {
        return "server.spigot-version=26.2\nserver.java-version=26\nmemory.min=2048M\nmemory.max=4096M\n" +
                "compatibility.geyser-version=2.11.2\ncompatibility.viaversion-version=5.11.0\n" +
                "compatibility.viabackwards-version=5.11.0\nupdates.auto-update=true\nupdates.retries=3\n" +
                "updates.retry-delay-ms=1500\nbackup.enabled=true\nbackup.directory=plugins/.backup\n" +
                "diagnostics.enabled=true\nstartup.nogui=true\n";
    }

    private static boolean has(String[] args, String value) {
        if (args == null) return false;
        for (String a : args) if (value.equalsIgnoreCase(a)) return true;
        return false;
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    private static void stop(Process p) {
        if (!p.isAlive()) return;
        p.destroy();
        try {
            if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
    }

    private record RuntimePlugin(String fileName, String version, String url) {
        boolean matches(String v) { return v != null && (v.equals(version) || v.startsWith(version + "-")); }
    }
}
