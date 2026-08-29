package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class DiscoveryHistoryStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void unpreparedDimensionsFailOpenWithoutCreatingState() {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey key = key("world-a", "minecraft:overworld");
        assertTrue(store.allows(key, 500, -500));
        assertFalse(Files.exists(store.historyPath(key)));
    }

    @Test
    void squareEligibilityPersistsWithInclusiveBoundaryAndNegativeCoordinates() {
        WorldDimensionKey key = key("world-a", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        assertFalse(first.allows(key, -10, 12));
        first.recordSquare(key, -10, 12, 1);

        assertTrue(first.allows(key, -11, 11));
        assertTrue(first.allows(key, -9, 13));
        assertFalse(first.allows(key, -12, 12));

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.allows(key, -11, 11));
        assertTrue(reloaded.allows(key, -9, 13));
        assertFalse(reloaded.allows(key, -12, 12));
    }

    @Test
    void importedCacheChunksPersistAndWorldDimensionsStaySeparate() {
        WorldDimensionKey overworld = key("world-a", "minecraft:overworld");
        WorldDimensionKey nether = key("world-a/DIM-1", "minecraft:the_nether");
        WorldDimensionKey otherWorld = key("world-b", "minecraft:overworld");
        DiscoveryHistoryStore store = store();
        store.prepare(overworld);
        store.prepare(nether);
        store.prepare(otherWorld);
        store.recordPacked(overworld, List.of(ChunkRadius.pack(7, -9)));

        assertTrue(store.allows(overworld, 7, -9));
        assertFalse(store.allows(nether, 7, -9));
        assertFalse(store.allows(otherWorld, 7, -9));
    }

    @Test
    void malformedOrTruncatedRecordsFailOpen() throws Exception {
        WorldDimensionKey malformedKey = key("world-malformed", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(malformedKey);
        Files.writeString(first.historyPath(malformedKey), "0000000000000", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);

        DiscoveryHistoryStore malformed = store();
        malformed.prepare(malformedKey);
        assertTrue(malformed.isFailOpen(malformedKey));
        assertTrue(malformed.allows(malformedKey, 12345, 54321));
    }

    @Test
    void explicitFailOpenMarkerSurvivesReload() {
        WorldDimensionKey key = key("world-failure", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        first.markFailOpen(key, "test failure");
        assertTrue(first.allows(key, 999, 999));

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.isFailOpen(key));
        assertTrue(reloaded.allows(key, 999, 999));
    }

    @Test
    void interruptedAppendMarkerMakesEvenAValidJournalPrefixFailOpen() throws Exception {
        WorldDimensionKey key = key("world-interrupted", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        first.recordSquare(key, 0, 0, 0);
        Path history = first.historyPath(key);
        Path pending = history.resolveSibling(history.getFileName() + ".pending");
        Files.writeString(pending, "simulated interrupted append", StandardCharsets.UTF_8);

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.isFailOpen(key));
        assertTrue(reloaded.allows(key, 100_000, -100_000));
    }

    @Test
    void successfulForcedAppendClearsItsTransactionMarker() {
        WorldDimensionKey key = key("world-committed", "minecraft:overworld");
        DiscoveryHistoryStore store = store();
        store.prepare(key);
        store.recordSquare(key, 4, 4, 1);
        Path history = store.historyPath(key);
        assertFalse(Files.exists(history.resolveSibling(history.getFileName() + ".pending")));
    }

    @Test
    void synchronizedQueriesImportsAndMovementDoNotLoseEligibility() throws Exception {
        WorldDimensionKey key = key("world-concurrent", "minecraft:overworld");
        DiscoveryHistoryStore store = store();
        store.prepare(key);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 40; i++) {
            int chunk = i;
            executor.submit(() -> store.recordSquare(key, chunk, -chunk, 1));
            executor.submit(() -> store.recordPacked(key, List.of(ChunkRadius.pack(-chunk, chunk))));
            executor.submit(() -> store.allows(key, chunk, -chunk));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        for (int i = 0; i < 40; i++) {
            assertTrue(store.allows(key, i, -i));
            assertTrue(store.allows(key, -i, i));
        }
    }

    private DiscoveryHistoryStore store() {
        return new DiscoveryHistoryStore(LoggerFactory.getLogger(getClass()));
    }

    private WorldDimensionKey key(String relativeDirectory, String dimension) {
        return new WorldDimensionKey(temporaryDirectory.resolve(relativeDirectory), dimension);
    }
}
