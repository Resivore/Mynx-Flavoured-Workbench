package dev.resivore.xaerodiscovery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

final class LegacyCacheManifest {
    private static final int SCHEMA_VERSION = 1;
    private static final String MANIFEST_NAME = "legacy-xaero-cache-manifest.json";

    private final Path cacheRoot;
    private final Map<String, String> expectedHashes;
    private final Map<String, Verification> verificationCache = new HashMap<>();
    private final boolean available;

    private LegacyCacheManifest(Path cacheRoot, Map<String, String> expectedHashes, boolean available) {
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
        this.expectedHashes = Map.copyOf(expectedHashes);
        this.available = available;
    }

    static LegacyCacheManifest captureOrLoad(Path gameDirectory, Path stateDirectory, Logger logger) {
        Path cacheRoot = gameDirectory.resolve("xaero").resolve("world-map").toAbsolutePath().normalize();
        Path manifestPath = stateDirectory.resolve(MANIFEST_NAME);
        try {
            Files.createDirectories(stateDirectory);
            Map<String, String> hashes;
            if (Files.exists(manifestPath)) {
                hashes = read(manifestPath);
                logger.info("Loaded frozen Xaero legacy-cache manifest with {} files", hashes.size());
            } else {
                hashes = capture(cacheRoot);
                write(manifestPath, hashes);
                logger.info("Captured frozen Xaero legacy-cache manifest with {} files", hashes.size());
            }
            return new LegacyCacheManifest(cacheRoot, hashes, true);
        } catch (Exception exception) {
            logger.error(
                    "Could not establish the frozen Xaero legacy-cache manifest. "
                            + "Singleplayer direct-save reads will fail open to avoid hiding historical map terrain.",
                    exception
            );
            return new LegacyCacheManifest(cacheRoot, Map.of(), false);
        }
    }

    boolean isAvailable() {
        return available;
    }

    int entryCount() {
        return expectedHashes.size();
    }

    synchronized boolean permits(Path cacheFile) {
        if (!available || cacheFile == null) {
            return false;
        }
        Path normalized = cacheFile.toAbsolutePath().normalize();
        if (!normalized.startsWith(cacheRoot) || !Files.isRegularFile(normalized)) {
            return false;
        }
        String key = relativeKey(cacheRoot, normalized);
        String expectedHash = expectedHashes.get(key);
        if (expectedHash == null) {
            return false;
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(normalized, BasicFileAttributes.class);
            Verification cached = verificationCache.get(key);
            if (cached != null
                    && cached.size == attributes.size()
                    && cached.modifiedMillis == attributes.lastModifiedTime().toMillis()
                    && cached.fileKey.equals(String.valueOf(attributes.fileKey()))) {
                return cached.matches;
            }
            boolean matches = expectedHash.equals(sha256(normalized));
            verificationCache.put(key, new Verification(
                    attributes.size(),
                    attributes.lastModifiedTime().toMillis(),
                    String.valueOf(attributes.fileKey()),
                    matches
            ));
            return matches;
        } catch (IOException exception) {
            return false;
        }
    }

    private static Map<String, String> capture(Path cacheRoot) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        if (!Files.isDirectory(cacheRoot)) {
            return hashes;
        }
        try (var paths = Files.walk(cacheRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString();
                if (name.endsWith(".xwmc") || name.endsWith(".xwmc.outdated")) {
                    hashes.put(relativeKey(cacheRoot, path), sha256(path));
                }
            }
        }
        return hashes;
    }

    private static Map<String, String> read(Path manifestPath) throws IOException {
        JsonElement rootElement = JsonParser.parseString(Files.readString(manifestPath, StandardCharsets.UTF_8));
        if (!rootElement.isJsonObject()) {
            throw new IOException("Legacy cache manifest root is not an object");
        }
        JsonObject root = rootElement.getAsJsonObject();
        if (root.get("schemaVersion") == null || root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) {
            throw new IOException("Unsupported legacy cache manifest schema");
        }
        JsonArray files = root.getAsJsonArray("files");
        if (files == null) {
            throw new IOException("Legacy cache manifest has no files array");
        }
        Map<String, String> hashes = new HashMap<>();
        for (JsonElement entryElement : files) {
            JsonObject entry = entryElement.getAsJsonObject();
            String path = entry.get("path").getAsString();
            String sha256 = entry.get("sha256").getAsString();
            if (path.isBlank() || Path.of(path).isAbsolute() || !sha256.matches("[0-9A-F]{64}")) {
                throw new IOException("Invalid legacy cache manifest entry");
            }
            if (hashes.put(path, sha256) != null) {
                throw new IOException("Duplicate legacy cache manifest path " + path);
            }
        }
        return hashes;
    }

    private static void write(Path manifestPath, Map<String, String> hashes) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray files = new JsonArray();
        List<Map.Entry<String, String>> entries = new ArrayList<>(hashes.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, String> entry : entries) {
            JsonObject file = new JsonObject();
            file.addProperty("path", entry.getKey());
            file.addProperty("sha256", entry.getValue());
            files.add(file);
        }
        root.add("files", files);

        Path temporary = manifestPath.resolveSibling(manifestPath.getFileName() + ".tmp");
        Files.writeString(temporary, root + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, manifestPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, manifestPath);
        }
    }

    private static String relativeKey(Path root, Path path) {
        return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private record Verification(long size, long modifiedMillis, String fileKey, boolean matches) {
    }
}
