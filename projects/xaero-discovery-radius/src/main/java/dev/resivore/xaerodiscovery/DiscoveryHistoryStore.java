package dev.resivore.xaerodiscovery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

final class DiscoveryHistoryStore {
    private static final String LEGACY_MAGIC = "xaero-discovery-radius-history-v1";
    private static final String LEGACY_CACHE_IMPORT = "legacyCacheImport=frozen-manifest-v1";
    private static final String LAYER_MAGIC = "xaero-discovery-radius-layer-history-v2";
    private static final String LAYER_CACHE_IMPORT = "legacyCacheImport=frozen-manifest-v1-layer-aware-v2";
    private static final String LAYER_RECORD = "LAYER";
    private static final String FAIL_OPEN_SUFFIX = ".fail-open";
    private static final String PENDING_SUFFIX = ".pending";

    private final Logger logger;
    private final Map<WorldDimensionKey, Entry> entries = new HashMap<>();

    DiscoveryHistoryStore(Logger logger) {
        this.logger = logger;
    }

    synchronized void prepare(WorldDimensionKey key) {
        entries.computeIfAbsent(key, this::loadOrCreate);
    }

    synchronized void markFailOpen(WorldDimensionKey key, String reason) {
        Entry entry = entries.get(key);
        if (entry == null || entry.failOpen) {
            return;
        }
        entry.failOpen = true;
        Path marker = failOpenPath(entry.layerPath);
        try {
            writeForced(marker, reason + System.lineSeparator(), false);
        } catch (IOException exception) {
            logger.error("Could not persist fail-open marker {}", marker, exception);
        }
        logger.error("Xaero discovery history {} entered fail-open mode: {}", entry.layerPath, reason);
    }

    synchronized boolean allows(WorldDimensionKey key, int chunkX, int chunkZ, int layer) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return true;
        }
        long chunk = ChunkRadius.pack(chunkX, chunkZ);
        return entry.failOpen
                || entry.legacyWildcardChunks.contains(chunk)
                || entry.layerChunks.contains(new LayerChunk(layer, chunk));
    }

    synchronized void recordPacked(WorldDimensionKey key, List<Long> chunks, int layer) {
        Entry entry = entries.get(key);
        if (entry != null) {
            List<LayerChunk> additions = new ArrayList<>(chunks.size());
            for (long chunk : chunks) {
                additions.add(new LayerChunk(layer, chunk));
            }
            appendNew(entry, additions);
        }
    }

    synchronized void recordSquare(
            WorldDimensionKey key,
            int centerChunkX,
            int centerChunkZ,
            int radius,
            int layer
    ) {
        Entry entry = entries.get(key);
        if (entry == null || entry.failOpen || radius < 0) {
            return;
        }
        List<LayerChunk> additions = new ArrayList<>();
        for (int deltaX = -radius; deltaX <= radius; deltaX++) {
            for (int deltaZ = -radius; deltaZ <= radius; deltaZ++) {
                additions.add(new LayerChunk(
                        layer,
                        ChunkRadius.pack(centerChunkX + deltaX, centerChunkZ + deltaZ)
                ));
            }
        }
        appendNew(entry, additions);
    }

    synchronized boolean isFailOpen(WorldDimensionKey key) {
        Entry entry = entries.get(key);
        return entry == null || entry.failOpen;
    }

    synchronized void resetSession() {
        entries.clear();
    }

    Path historyPath(WorldDimensionKey key) {
        return key.dimensionDirectory().resolve("data").resolve("xaero-discovery-radius.history");
    }

    Path layerHistoryPath(WorldDimensionKey key) {
        return key.dimensionDirectory().resolve("data").resolve("xaero-discovery-radius.layers-v2.history");
    }

    private Entry loadOrCreate(WorldDimensionKey key) {
        Path legacyPath = historyPath(key);
        Path layerPath = layerHistoryPath(key);
        try {
            Files.createDirectories(layerPath.getParent());
            Set<Long> legacyWildcardChunks = pathExists(legacyPath)
                    ? loadLegacy(key, legacyPath)
                    : new HashSet<>();
            Set<LayerChunk> layerChunks;
            if (pathExists(layerPath)) {
                layerChunks = loadLayers(key, layerPath);
            } else {
                writeInitialLayers(key, layerPath);
                layerChunks = new HashSet<>();
                logger.info(
                        "Initialized layer-aware Xaero discovery history for {}; existing v1 chunks remain read-only wildcard eligibility",
                        key.dimensionId()
                );
            }
            boolean failOpen = hasFailureMarker(legacyPath) || hasFailureMarker(layerPath);
            return new Entry(layerPath, legacyWildcardChunks, layerChunks, failOpen);
        } catch (Exception exception) {
            Path marker = failOpenPath(layerPath);
            try {
                Files.createDirectories(marker.getParent());
                writeForced(marker, "layer history initialization failed" + System.lineSeparator(), false);
            } catch (Exception markerException) {
                exception.addSuppressed(markerException);
            }
            logger.error(
                    "Could not initialize layer-aware Xaero discovery history for {}. Singleplayer disk reads will remain ungated to preserve existing map visibility.",
                    key.dimensionId(),
                    exception
            );
            return new Entry(layerPath, new HashSet<>(), new HashSet<>(), true);
        }
    }

    private Set<Long> loadLegacy(WorldDimensionKey key, Path legacyPath) throws IOException {
        Set<Long> chunks = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(legacyPath, StandardCharsets.UTF_8)) {
            requireLine(LEGACY_MAGIC, reader.readLine(), legacyPath);
            requireLine("dimension=" + encode(key.dimensionId()), reader.readLine(), legacyPath);
            requireLine(LEGACY_CACHE_IMPORT, reader.readLine(), legacyPath);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    chunks.add(parseChunk(line, legacyPath));
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("Malformed legacy discovery history " + legacyPath, exception);
        }
        return chunks;
    }

    private Set<LayerChunk> loadLayers(WorldDimensionKey key, Path layerPath) throws IOException {
        Set<LayerChunk> chunks = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(layerPath, StandardCharsets.UTF_8)) {
            requireLine(LAYER_MAGIC, reader.readLine(), layerPath);
            requireLine("dimension=" + encode(key.dimensionId()), reader.readLine(), layerPath);
            requireLine(LAYER_CACHE_IMPORT, reader.readLine(), layerPath);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    chunks.add(parseLayerChunk(line, layerPath));
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("Malformed layer-aware discovery history " + layerPath, exception);
        }
        return chunks;
    }

    private void writeInitialLayers(WorldDimensionKey key, Path layerPath) throws IOException {
        Path temporary = layerPath.resolveSibling(layerPath.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            writer.write(LAYER_MAGIC);
            writer.newLine();
            writer.write("dimension=" + encode(key.dimensionId()));
            writer.newLine();
            writer.write(LAYER_CACHE_IMPORT);
            writer.newLine();
        }
        try {
            Files.move(temporary, layerPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, layerPath);
        }
    }

    private void appendNew(Entry entry, List<LayerChunk> candidates) {
        if (entry.failOpen) {
            return;
        }
        StringBuilder appended = new StringBuilder();
        for (LayerChunk candidate : candidates) {
            if (entry.layerChunks.add(candidate)) {
                appended.append(String.format(
                        Locale.ROOT,
                        "%s %d %016X%n",
                        LAYER_RECORD,
                        candidate.layer(),
                        candidate.chunk()
                ));
            }
        }
        if (appended.isEmpty()) {
            return;
        }
        Path pending = pendingPath(entry.layerPath);
        try {
            writeForced(pending, "layer history append pending" + System.lineSeparator(), false);
            writeForced(entry.layerPath, appended.toString(), true);
            Files.delete(pending);
        } catch (IOException exception) {
            entry.failOpen = true;
            Path marker = failOpenPath(entry.layerPath);
            try {
                writeForced(marker, "layer history append failed" + System.lineSeparator(), false);
            } catch (IOException markerException) {
                exception.addSuppressed(markerException);
            }
            logger.error(
                    "Could not persist layer-aware Xaero discovery history {}. Singleplayer disk reads will remain ungated for this session.",
                    entry.layerPath,
                    exception
            );
        }
    }

    private static LayerChunk parseLayerChunk(String line, Path path) throws IOException {
        String[] fields = line.split(" ", -1);
        if (fields.length != 3 || !LAYER_RECORD.equals(fields[0])) {
            throw new IOException("Malformed layer record in " + path);
        }
        try {
            return new LayerChunk(Integer.parseInt(fields[1]), parseChunk(fields[2], path));
        } catch (NumberFormatException exception) {
            throw new IOException("Malformed layer record in " + path, exception);
        }
    }

    private static long parseChunk(String value, Path path) throws IOException {
        if (value.length() != 16 || !value.matches("[0-9A-Fa-f]{16}")) {
            throw new IOException("Malformed chunk record in " + path);
        }
        return Long.parseUnsignedLong(value, 16);
    }

    private static Path failOpenPath(Path historyPath) {
        return historyPath.resolveSibling(historyPath.getFileName() + FAIL_OPEN_SUFFIX);
    }

    private static Path pendingPath(Path historyPath) {
        return historyPath.resolveSibling(historyPath.getFileName() + PENDING_SUFFIX);
    }

    private static boolean hasFailureMarker(Path historyPath) {
        return presentOrUnavailable(failOpenPath(historyPath)) || presentOrUnavailable(pendingPath(historyPath));
    }

    private static boolean presentOrUnavailable(Path path) {
        return Files.exists(path) || !Files.notExists(path);
    }

    private static boolean pathExists(Path path) throws IOException {
        if (Files.exists(path)) {
            return true;
        }
        if (Files.notExists(path)) {
            return false;
        }
        throw new IOException("Could not determine whether discovery history exists: " + path);
    }

    private static void writeForced(Path path, String value, boolean append) throws IOException {
        StandardOpenOption[] options = append
                ? new StandardOpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                };
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(value);
        try (FileChannel channel = FileChannel.open(path, options)) {
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        }
    }

    private static void requireLine(String expected, String actual, Path path) throws IOException {
        if (!expected.equals(actual)) {
            throw new IOException("Unexpected header in " + path);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record LayerChunk(int layer, long chunk) {
    }

    private static final class Entry {
        private final Path layerPath;
        private final Set<Long> legacyWildcardChunks;
        private final Set<LayerChunk> layerChunks;
        private boolean failOpen;

        private Entry(
                Path layerPath,
                Set<Long> legacyWildcardChunks,
                Set<LayerChunk> layerChunks,
                boolean failOpen
        ) {
            this.layerPath = layerPath;
            this.legacyWildcardChunks = legacyWildcardChunks;
            this.layerChunks = layerChunks;
            this.failOpen = failOpen;
        }
    }
}
