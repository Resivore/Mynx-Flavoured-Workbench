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
    private static final int SURFACE_LAYER = Integer.MAX_VALUE;

    @TempDir
    Path temporaryDirectory;

    @Test
    void unpreparedDimensionsFailOpenWithoutCreatingState() {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey key = key("world-a", "minecraft:overworld");
        assertTrue(store.allows(key, 500, -500, SURFACE_LAYER));
        assertFalse(Files.exists(store.historyPath(key)));
    }

    @Test
    void circleEligibilityPersistsWithInclusiveBoundaryAndNegativeCoordinates() {
        WorldDimensionKey key = key("world-a", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        assertFalse(first.allows(key, -10, 12, SURFACE_LAYER));
        first.recordCircle(key, -10, 12, 1, SURFACE_LAYER);

        assertTrue(first.allows(key, -11, 12, SURFACE_LAYER));
        assertTrue(first.allows(key, -10, 13, SURFACE_LAYER));
        assertFalse(first.allows(key, -11, 11, SURFACE_LAYER));

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.allows(key, -11, 12, SURFACE_LAYER));
        assertTrue(reloaded.allows(key, -10, 13, SURFACE_LAYER));
        assertFalse(reloaded.allows(key, -11, 11, SURFACE_LAYER));
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
        store.recordPacked(overworld, List.of(ChunkRadius.pack(7, -9)), SURFACE_LAYER);

        assertTrue(store.allows(overworld, 7, -9, SURFACE_LAYER));
        assertFalse(store.allows(nether, 7, -9, SURFACE_LAYER));
        assertFalse(store.allows(otherWorld, 7, -9, SURFACE_LAYER));
    }

    @Test
    void malformedOrTruncatedRecordsFailOpen() throws Exception {
        WorldDimensionKey malformedKey = key("world-malformed", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(malformedKey);
        Files.writeString(
                first.layerHistoryPath(malformedKey),
                "LAYER cave 0000000000000000\n",
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND
        );

        DiscoveryHistoryStore malformed = store();
        malformed.prepare(malformedKey);
        assertTrue(malformed.isFailOpen(malformedKey));
        assertTrue(malformed.allows(malformedKey, 12345, 54321, -50));
    }

    @Test
    void explicitFailOpenMarkerSurvivesReload() {
        WorldDimensionKey key = key("world-failure", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        first.markFailOpen(key, "test failure");
        assertTrue(first.allows(key, 999, 999, SURFACE_LAYER));

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.isFailOpen(key));
        assertTrue(reloaded.allows(key, 999, 999, -10));
    }

    @Test
    void interruptedAppendMarkerMakesEvenAValidJournalPrefixFailOpen() throws Exception {
        WorldDimensionKey key = key("world-interrupted", "minecraft:overworld");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        first.recordCircle(key, 0, 0, 0, SURFACE_LAYER);
        Path history = first.layerHistoryPath(key);
        Path pending = history.resolveSibling(history.getFileName() + ".pending");
        Files.writeString(pending, "simulated interrupted append", StandardCharsets.UTF_8);

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.isFailOpen(key));
        assertTrue(reloaded.allows(key, 100_000, -100_000, -20));
    }

    @Test
    void successfulForcedAppendClearsItsTransactionMarker() {
        WorldDimensionKey key = key("world-committed", "minecraft:overworld");
        DiscoveryHistoryStore store = store();
        store.prepare(key);
        store.recordCircle(key, 4, 4, 1, SURFACE_LAYER);
        Path history = store.layerHistoryPath(key);
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
            executor.submit(() -> store.recordCircle(key, chunk, -chunk, 1, SURFACE_LAYER));
            executor.submit(() -> store.recordPacked(key, List.of(ChunkRadius.pack(-chunk, chunk)), -10));
            executor.submit(() -> store.allows(key, chunk, -chunk, SURFACE_LAYER));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        for (int i = 0; i < 40; i++) {
            assertTrue(store.allows(key, i, -i, SURFACE_LAYER));
            assertTrue(store.allows(key, -i, i, -10));
        }
    }

    private DiscoveryHistoryStore store() {
        return new DiscoveryHistoryStore(LoggerFactory.getLogger(getClass()));
    }

    private WorldDimensionKey key(String relativeDirectory, String dimension) {
        return new WorldDimensionKey(temporaryDirectory.resolve(relativeDirectory), dimension);
    }
}
