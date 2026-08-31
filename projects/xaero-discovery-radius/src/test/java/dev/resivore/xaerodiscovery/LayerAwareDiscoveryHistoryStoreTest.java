package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class LayerAwareDiscoveryHistoryStoreTest {
    private static final int SURFACE_LAYER = Integer.MAX_VALUE;

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingV1CreatesNoWildcardWhileSurfaceAndNegativeCaveLayersStayIndependent() {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey key = key("world-layers", "minecraft:overworld");
        long chunk = ChunkRadius.pack(-7, 11);
        store.prepare(key);

        assertFalse(Files.exists(store.historyPath(key)));
        assertFalse(store.allows(key, -7, 11, SURFACE_LAYER));
        assertFalse(store.allows(key, -7, 11, -32));

        store.recordPacked(key, List.of(chunk), SURFACE_LAYER);
        assertTrue(store.allows(key, -7, 11, SURFACE_LAYER));
        assertFalse(store.allows(key, -7, 11, -32));

        store.recordPacked(key, List.of(chunk), -32);
        assertTrue(store.allows(key, -7, 11, -32));
        assertFalse(store.allows(key, -7, 11, -31));
    }

    @Test
    void validV1ChunksAreReadOnlyWildcardsAcrossEveryLayer() throws Exception {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey key = key("world-legacy", "minecraft:overworld");
        long legacyChunk = ChunkRadius.pack(-19, 23);
        writeLegacyV1(store, key, legacyChunk);
        byte[] originalV1 = Files.readAllBytes(store.historyPath(key));

        store.prepare(key);
        store.recordPacked(key, List.of(ChunkRadius.pack(4, 5)), -8);

        assertTrue(store.allows(key, -19, 23, SURFACE_LAYER));
        assertTrue(store.allows(key, -19, 23, 0));
        assertTrue(store.allows(key, -19, 23, -128));
        assertFalse(store.allows(key, -18, 23, SURFACE_LAYER));
        assertArrayEquals(originalV1, Files.readAllBytes(store.historyPath(key)));
    }

    @Test
    void explicitTypedLayerRecordsPersistAcrossReload() throws Exception {
        WorldDimensionKey key = key("world-persisted", "minecraft:the_nether");
        DiscoveryHistoryStore first = store();
        first.prepare(key);
        first.recordPacked(key, List.of(ChunkRadius.pack(3, -5)), SURFACE_LAYER);
        first.recordPacked(key, List.of(ChunkRadius.pack(3, -5), ChunkRadius.pack(-9, -10)), -37);
        first.recordPacked(key, List.of(ChunkRadius.pack(12, 13)), Integer.MIN_VALUE);

        List<String> lines = Files.readAllLines(first.layerHistoryPath(key), StandardCharsets.UTF_8);
        assertEquals("xaero-discovery-radius-layer-history-v2", lines.get(0));
        assertEquals("dimension=" + encode(key.dimensionId()), lines.get(1));
        assertEquals("legacyCacheImport=frozen-manifest-v1-layer-aware-v2", lines.get(2));
        assertTrue(lines.contains("LAYER 2147483647 00000003FFFFFFFB"));
        assertTrue(lines.contains("LAYER -37 FFFFFFF7FFFFFFF6"));
        assertTrue(lines.contains("LAYER -2147483648 0000000C0000000D"));

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.allows(key, 3, -5, SURFACE_LAYER));
        assertFalse(reloaded.allows(key, -9, -10, SURFACE_LAYER));
        assertTrue(reloaded.allows(key, 3, -5, -37));
        assertTrue(reloaded.allows(key, -9, -10, -37));
        assertFalse(reloaded.allows(key, 3, -5, -36));
        assertTrue(reloaded.allows(key, 12, 13, Integer.MIN_VALUE));
        assertFalse(reloaded.allows(key, 12, 13, Integer.MIN_VALUE + 1));
    }

    @Test
    void radiusTwoCircleContainsExactlyThirteenChunksAtNegativeCoordinates() {
        WorldDimensionKey key = key("world-circle", "minecraft:overworld");
        DiscoveryHistoryStore store = store();
        store.prepare(key);
        store.recordCircle(key, -10, -20, 2, -16);

        int allowed = 0;
        for (int deltaX = -2; deltaX <= 2; deltaX++) {
            for (int deltaZ = -2; deltaZ <= 2; deltaZ++) {
                boolean expected = deltaX * deltaX + deltaZ * deltaZ <= 4;
                assertEquals(expected, store.allows(key, -10 + deltaX, -20 + deltaZ, -16));
                if (expected) {
                    allowed++;
                }
            }
        }
        assertEquals(13, allowed);
        assertFalse(store.allows(key, -10, -20, SURFACE_LAYER));
        assertFalse(store.allows(key, -13, -20, -16));
    }

    @Test
    void eitherV1FailureMarkerMakesAllLayerVisibilityFailOpen() throws Exception {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey pendingKey = key("world-v1-pending", "minecraft:overworld");
        WorldDimensionKey failedKey = key("world-v1-failed", "minecraft:overworld");
        writeLegacyV1(store, pendingKey, ChunkRadius.pack(0, 0));
        writeLegacyV1(store, failedKey, ChunkRadius.pack(0, 0));
        writeMarker(store.historyPath(pendingKey), ".pending");
        writeMarker(store.historyPath(failedKey), ".fail-open");

        store.prepare(pendingKey);
        store.prepare(failedKey);

        assertTrue(store.isFailOpen(pendingKey));
        assertTrue(store.allows(pendingKey, 91, 92, -7));
        assertTrue(store.isFailOpen(failedKey));
        assertTrue(store.allows(failedKey, -91, -92, SURFACE_LAYER));
    }

    @Test
    void malformedV1FailsOpenWithoutBeingRewritten() throws Exception {
        DiscoveryHistoryStore store = store();
        WorldDimensionKey key = key("world-v1-corrupt", "minecraft:overworld");
        long legacyChunk = ChunkRadius.pack(5, 6);
        writeLegacyV1(store, key, legacyChunk);
        Files.writeString(store.historyPath(key), "not-a-chunk\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        byte[] corruptV1 = Files.readAllBytes(store.historyPath(key));

        store.prepare(key);

        assertTrue(store.isFailOpen(key));
        assertTrue(store.allows(key, 77, 88, -44));
        assertArrayEquals(corruptV1, Files.readAllBytes(store.historyPath(key)));

        Path layerMarker = store.layerHistoryPath(key).resolveSibling(
                store.layerHistoryPath(key).getFileName() + ".fail-open"
        );
        assertTrue(Files.exists(layerMarker));
        writeLegacyV1(store, key, legacyChunk);

        DiscoveryHistoryStore reloaded = store();
        reloaded.prepare(key);
        assertTrue(reloaded.isFailOpen(key));
        assertTrue(reloaded.allows(key, -77, -88, SURFACE_LAYER));
    }

    private void writeLegacyV1(DiscoveryHistoryStore store, WorldDimensionKey key, long... chunks) throws Exception {
        Path history = store.historyPath(key);
        Files.createDirectories(history.getParent());
        StringBuilder contents = new StringBuilder()
                .append("xaero-discovery-radius-history-v1\n")
                .append("dimension=").append(encode(key.dimensionId())).append('\n')
                .append("legacyCacheImport=frozen-manifest-v1\n");
        for (long chunk : chunks) {
            contents.append(String.format("%016x%n", chunk));
        }
        Files.writeString(history, contents, StandardCharsets.UTF_8);
    }

    private static void writeMarker(Path history, String suffix) throws Exception {
        Path marker = history.resolveSibling(history.getFileName() + suffix);
        Files.writeString(marker, "simulated interrupted state", StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private DiscoveryHistoryStore store() {
        return new DiscoveryHistoryStore(LoggerFactory.getLogger(getClass()));
    }

    private WorldDimensionKey key(String relativeDirectory, String dimension) {
        return new WorldDimensionKey(temporaryDirectory.resolve(relativeDirectory), dimension);
    }
}
