package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class LegacyCacheManifestTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void freezesExactPrePolicyCachesAndRejectsNewOrChangedFiles() throws Exception {
        Path game = temporaryDirectory.resolve("game");
        Path state = temporaryDirectory.resolve("state");
        Path cache = game.resolve("xaero/world-map/save/dim/cache_1/0_0.xwmc");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, "legacy-a", StandardCharsets.UTF_8);

        LegacyCacheManifest manifest = LegacyCacheManifest.captureOrLoad(game, state, LoggerFactory.getLogger(getClass()));
        assertTrue(manifest.isAvailable());
        assertEquals(1, manifest.entryCount());
        assertTrue(manifest.permits(cache));

        Path newCache = cache.resolveSibling("1_0.xwmc");
        Files.writeString(newCache, "new-cache", StandardCharsets.UTF_8);
        assertFalse(manifest.permits(newCache));

        long oldModified = Files.getLastModifiedTime(cache).toMillis();
        Files.writeString(cache, "changed!", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(cache, FileTime.fromMillis(oldModified + 2_000));
        assertFalse(manifest.permits(cache));

        LegacyCacheManifest reloaded = LegacyCacheManifest.captureOrLoad(game, state, LoggerFactory.getLogger(getClass()));
        assertEquals(1, reloaded.entryCount());
        assertFalse(reloaded.permits(cache));
    }

    @Test
    void rejectsFilesOutsideXaerosCacheRoot() throws Exception {
        Path game = temporaryDirectory.resolve("game-external");
        Path state = temporaryDirectory.resolve("state-external");
        LegacyCacheManifest manifest = LegacyCacheManifest.captureOrLoad(game, state, LoggerFactory.getLogger(getClass()));
        Path external = temporaryDirectory.resolve("external.xwmc");
        Files.writeString(external, "external", StandardCharsets.UTF_8);
        assertFalse(manifest.permits(external));
    }

    @Test
    void malformedFrozenManifestDisablesMigration() throws Exception {
        Path game = temporaryDirectory.resolve("game-bad");
        Path state = temporaryDirectory.resolve("state-bad");
        Files.createDirectories(state);
        Files.writeString(state.resolve("legacy-xaero-cache-manifest.json"), "{}", StandardCharsets.UTF_8);
        LegacyCacheManifest manifest = LegacyCacheManifest.captureOrLoad(game, state, LoggerFactory.getLogger(getClass()));
        assertFalse(manifest.isAvailable());
    }
}
