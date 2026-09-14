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

    public ServerDistributionLauncher(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public int run(String[] args) throws Exception {
        Files.createDirectories(root);
        loadConfig();
        if (has(args, "status") || has(args, "--status")) { status(); return 0; }
        validateJava();
        Path spigot = root.resolve("spigot.jar");
        if (!validJar(spigot)) {
            System.err.println("[SpigotPlus] spigot.jar não foi encontrado ou é inválido.");
            System.err.println("[SpigotPlus] Coloque o Spigot " + get("spigot.version", SPIGOT_VERSION) + " já compilado como 'spigot.jar'.");
            System.err.println("[SpigotPlus] O SpigotPlus não executa o BuildTools durante a inicialização.");
            return 1;
        }
        ensureRuntimePlugins();
        ensureServerProperties();
        ensureEula();
        List<String> command = new ArrayList<>(List.of(javaExecutable(), "-Xms" + get("memory.min", "2048M"), "-Xmx" + get("memory.max", "4096M"), "-jar", spigot.toString(), "nogui"));
        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(process), "SpigotPlus Shutdown"));
        return process.waitFor();
    }

    private void loadConfig() throws IOException {
        Path file = root.resolve(CONFIG_FILE);
        if (!Files.exists(file)) Files.writeString(file, "spigot.version=26.2\njava.version=26\nmemory.min=2048M\nmemory.max=4096M\ngeyser.version=2.11.2\nviaversion.version=5.11.0\nviabackwards.version=5.11.0\n");
        try (InputStream in = Files.newInputStream(file)) { config.load(in); }
    }

    private void validateJava() {
        String actual = String.valueOf(Runtime.version().feature());
        String expected = get("java.version", JAVA_VERSION);
        if (!expected.equals(actual)) throw new IllegalStateException("Java " + expected + " é necessária; detectada Java " + actual + ".");
    }

    private void ensureRuntimePlugins() throws IOException, InterruptedException {
        Path plugins = root.resolve("plugins"); Path backup = plugins.resolve(".backup");
        Files.createDirectories(plugins); Files.createDirectories(backup);
        String geyser = get("geyser.version", DEFAULT_GEYSER), via = get("viaversion.version", DEFAULT_VIA), backwards = get("viabackwards.version", DEFAULT_VIA);
        List<RuntimePlugin> list = List.of(
                new RuntimePlugin("Geyser-Spigot.jar", geyser, "https://download.geysermc.org/v2/projects/geyser/versions/" + geyser + "/builds/latest/downloads/spigot"),
                new RuntimePlugin("ViaVersion.jar", via, "https://github.com/ViaVersion/ViaVersion/releases/download/" + via + "/ViaVersion-" + via + ".jar"),
                new RuntimePlugin("ViaBackwards.jar", backwards, "https://github.com/ViaVersion/ViaBackwards/releases/download/" + backwards + "/ViaBackwards-" + backwards + ".jar"));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
        for (RuntimePlugin p : list) {
            Path target = plugins.resolve(p.fileName()); String local = version(target);
            if (validJar(target) && p.matches(local)) { System.out.println("[SpigotPlus] " + p.fileName() + " " + local + " ✓"); continue; }
            Path tmp = plugins.resolve(p.fileName() + ".download"); Files.deleteIfExists(tmp); Exception error = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(p.url())).timeout(Duration.ofMinutes(5)).header("User-Agent", "SpigotPlus/1.0").GET().build();
                    HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() < 200 || res.statusCode() >= 300) { res.body().close(); throw new IOException("HTTP " + res.statusCode()); }
                    try (InputStream in = res.body()) { Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING); }
                    String downloaded = version(tmp);
                    if (!validJar(tmp) || !p.matches(downloaded)) throw new IOException("JAR inválido ou versão inesperada: " + downloaded);
                    String expectedSha = get("sha256." + p.fileName(), "");
                    if (!expectedSha.isBlank() && !expectedSha.equalsIgnoreCase(sha256(tmp))) throw new IOException("SHA-256 inválido");
                    if (validJar(target)) Files.copy(target, backup.resolve(p.fileName() + "." + (local == null ? "unknown" : local) + ".jar"), StandardCopyOption.REPLACE_EXISTING);
                    replace(tmp, target); System.out.println("[SpigotPlus] " + p.fileName() + " " + downloaded + " ✓"); error = null; break;
                } catch (Exception e) { error = e; Files.deleteIfExists(tmp); if (attempt < 3) Thread.sleep(1500L * attempt); }
            }
            if (error != null && !(validJar(target) && p.matches(version(target)))) throw new IOException("Falha ao instalar " + p.fileName(), error);
            if (error != null) System.err.println("[SpigotPlus] Falha na atualização; mantendo " + p.fileName() + " atual.");
        }
    }

    private void ensureEula() throws IOException {
        Path eula = root.resolve("eula.txt");
        if (!Files.exists(eula)) { Files.writeString(eula, "eula=false\n"); throw new IllegalStateException("EULA criada. Aceite-a em eula.txt."); }
        if (!Files.readString(eula).matches("(?s).*\\beula\\s*=\\s*true\\b.*")) throw new IllegalStateException("EULA ainda não foi aceita.");
    }

    private void ensureServerProperties() throws IOException {
        Path file = root.resolve("server.properties");
        if (!Files.exists(file)) Files.writeString(file, "server-port=25565\nserver-ip=\nview-distance=10\nsimulation-distance=6\nmax-players=100\nonline-mode=true\nenforce-secure-profile=false\nmotd=SpigotPlus Server\nlevel-name=world\nallow-flight=true\n");
    }

    private void status() {
        System.out.println("[SpigotPlus] Status");
        System.out.println("Spigot " + get("spigot.version", SPIGOT_VERSION) + ": " + (validJar(root.resolve("spigot.jar")) ? "✓" : "✗"));
        System.out.println("Java " + Runtime.version().feature() + ": " + (String.valueOf(Runtime.version().feature()).equals(get("java.version", JAVA_VERSION)) ? "✓" : "✗"));
        for (String name : List.of("Geyser-Spigot.jar", "ViaVersion.jar", "ViaBackwards.jar", "EssentialsPlus.jar", "CargoPlus.jar", "UtilidadesPlus.jar", "ChatPlus.jar", "LoginPlus.jar", "ClanPlus.jar")) {
            Path file = root.resolve("plugins").resolve(name); System.out.println(name + ": " + (validJar(file) ? "✓" : "✗"));
        }
    }

    private static boolean validJar(Path p) { if (!Files.isRegularFile(p)) return false; try (JarFile ignored = new JarFile(p.toFile())) { return true; } catch (IOException | RuntimeException e) { return false; } }
    private static String version(Path p) { if (!validJar(p)) return null; try (JarFile jar = new JarFile(p.toFile())) { var m = jar.getManifest(); if (m == null) return null; String v = m.getMainAttributes().getValue("Implementation-Version"); if (v == null || v.isBlank()) v = m.getMainAttributes().getValue("Specification-Version"); return v == null || v.isBlank() ? null : v.trim(); } catch (IOException | RuntimeException e) { return null; } }
    private static String sha256(Path p) throws Exception { MessageDigest d = MessageDigest.getInstance("SHA-256"); try (InputStream in = Files.newInputStream(p)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) d.update(b, 0, n); } StringBuilder s = new StringBuilder(); for (byte b : d.digest()) s.append(String.format("%02x", b)); return s.toString(); }
    private static void replace(Path src, Path dst) throws IOException { try { Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING); } }
    private String get(String key, String fallback) { return config.getProperty(key, fallback).trim(); }
    private static boolean has(String[] args, String value) { if (args == null) return false; for (String a : args) if (value.equalsIgnoreCase(a)) return true; return false; }
    private static String javaExecutable() { return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString(); }
    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
    private static void stop(Process p) { if (!p.isAlive()) return; p.destroy(); try { if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); p.destroyForcibly(); } }
    private record RuntimePlugin(String fileName, String version, String url) { boolean matches(String v) { return v != null && (v.equals(version) || v.startsWith(version + "-")); } }
}
