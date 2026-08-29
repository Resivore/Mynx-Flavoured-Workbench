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
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

final class DiscoveryHistoryStore {
    private static final String MAGIC = "xaero-discovery-radius-history-v1";
    private static final String CACHE_IMPORT = "legacyCacheImport=frozen-manifest-v1";
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
        Path marker = failOpenPath(entry.path);
        try {
            writeForced(marker, reason + System.lineSeparator(), false);
        } catch (IOException exception) {
            logger.error("Could not persist fail-open marker {}", marker, exception);
        }
        logger.error("Xaero discovery history {} entered fail-open mode: {}", entry.path, reason);
    }

    synchronized boolean allows(WorldDimensionKey key, int chunkX, int chunkZ) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return true;
        }
        return entry.failOpen || entry.allowedChunks.contains(ChunkRadius.pack(chunkX, chunkZ));
    }

    synchronized void recordPacked(WorldDimensionKey key, List<Long> chunks) {
        Entry entry = entries.get(key);
        if (entry != null) {
            appendNew(entry, chunks);
        }
    }

    synchronized void recordSquare(WorldDimensionKey key, int centerChunkX, int centerChunkZ, int radius) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return;
        }
        if (entry.failOpen) {
            return;
        }
        List<Long> additions = new ArrayList<>();
        for (int deltaX = -radius; deltaX <= radius; deltaX++) {
            for (int deltaZ = -radius; deltaZ <= radius; deltaZ++) {
                additions.add(ChunkRadius.pack(centerChunkX + deltaX, centerChunkZ + deltaZ));
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

    private Entry loadOrCreate(WorldDimensionKey key) {
        Path historyPath = historyPath(key);
        try {
            Files.createDirectories(historyPath.getParent());
            if (Files.exists(historyPath)) {
                return load(key, historyPath);
            }
            writeInitial(key, historyPath);
            logger.info("Initialized Xaero discovery history for {}; persisted Xaero cache data will be imported as it loads", key.dimensionId());
            return new Entry(historyPath, new HashSet<>(), hasFailureMarker(historyPath));
        } catch (Exception exception) {
            logger.error("Could not initialize Xaero discovery history for {}. Singleplayer disk reads will remain ungated to preserve existing map visibility.", key.dimensionId(), exception);
            return new Entry(historyPath, new HashSet<>(), true);
        }
    }

    private Entry load(WorldDimensionKey key, Path historyPath) throws IOException {
        Set<Long> chunks = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(historyPath, StandardCharsets.UTF_8)) {
            requireLine(MAGIC, reader.readLine(), historyPath);
            requireLine("dimension=" + encode(key.dimensionId()), reader.readLine(), historyPath);
            requireLine(CACHE_IMPORT, reader.readLine(), historyPath);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    if (line.length() != 16 || !line.matches("[0-9A-F]{16}")) {
                        throw new IOException("Malformed chunk record in " + historyPath);
                    }
                    chunks.add(Long.parseUnsignedLong(line, 16));
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("Malformed discovery history " + historyPath, exception);
        }
        return new Entry(historyPath, chunks, hasFailureMarker(historyPath));
    }

    private void writeInitial(WorldDimensionKey key, Path historyPath) throws IOException {
        Path temporary = historyPath.resolveSibling(historyPath.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            writer.write(MAGIC);
            writer.newLine();
            writer.write("dimension=" + encode(key.dimensionId()));
            writer.newLine();
            writer.write(CACHE_IMPORT);
            writer.newLine();
        }
        try {
            Files.move(temporary, historyPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, historyPath);
        }
    }

    private void appendNew(Entry entry, List<Long> candidates) {
        if (entry.failOpen) {
            return;
        }
        StringBuilder appended = new StringBuilder();
        for (long chunk : candidates) {
            if (entry.allowedChunks.add(chunk)) {
                appended.append(String.format("%016X", chunk)).append('\n');
            }
        }
        if (appended.isEmpty()) {
            return;
        }
        Path pending = pendingPath(entry.path);
        try {
            writeForced(pending, "history append pending" + System.lineSeparator(), false);
            writeForced(entry.path, appended.toString(), true);
            Files.delete(pending);
        } catch (IOException exception) {
            entry.failOpen = true;
            Path marker = failOpenPath(entry.path);
            try {
                writeForced(marker, "history append failed" + System.lineSeparator(), false);
            } catch (IOException markerException) {
                exception.addSuppressed(markerException);
            }
            logger.error("Could not persist Xaero discovery history {}. Singleplayer disk reads will remain ungated for this session.", entry.path, exception);
        }
    }

    private static Path failOpenPath(Path historyPath) {
        return historyPath.resolveSibling(historyPath.getFileName() + FAIL_OPEN_SUFFIX);
    }

    private static Path pendingPath(Path historyPath) {
        return historyPath.resolveSibling(historyPath.getFileName() + PENDING_SUFFIX);
    }

    private static boolean hasFailureMarker(Path historyPath) {
        return Files.exists(failOpenPath(historyPath)) || Files.exists(pendingPath(historyPath));
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

    private static final class Entry {
        private final Path path;
        private final Set<Long> allowedChunks;
        private boolean failOpen;

        private Entry(Path path, Set<Long> allowedChunks, boolean failOpen) {
            this.path = path;
            this.allowedChunks = allowedChunks;
            this.failOpen = failOpen;
        }
    }
}
