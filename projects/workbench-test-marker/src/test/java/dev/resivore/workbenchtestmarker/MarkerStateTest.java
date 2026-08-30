package dev.resivore.workbenchtestmarker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MarkerStateTest {
    @TempDir
    Path gameDirectory;

    @Test
    void readsCanonicalStackAndOccupiedSlotLines() throws IOException {
        writeProjection(List.of(
                "Baseline: Stack v1",
                "Slot A: Matcha Death Rebalance - Canary 8",
                "Slot B: Mossy Stone - Canary 3"));

        MarkerState state = MarkerState.load(gameDirectory);

        assertEquals(MarkerState.Status.ACTIVE, state.status());
        assertEquals("Baseline: Stack v1", state.lines().get(0));
        assertEquals("Slot A: Matcha Death Rebalance - Canary 8", state.lines().get(1));
        assertEquals("Slot B: Mossy Stone - Canary 3", state.lines().get(2));
    }

    @Test
    void emptySlotsAreExplicitAndNeverUnknown() throws IOException {
        writeProjection(List.of("Baseline: Stack v1", "Slot A: Empty", "Slot B: Empty"));

        MarkerState state = MarkerState.load(gameDirectory);

        assertEquals(MarkerState.Status.ACTIVE, state.status());
        assertFalse(state.lines().stream().anyMatch(line -> line.contains("UNKNOWN")));
    }

    @Test
    void atomicCanaryReplacementIsReadImmediately() throws IOException {
        writeProjection(List.of(
                "Baseline: Stack v1",
                "Slot A: Matcha Death Rebalance - Canary 8",
                "Slot B: Mossy Stone - Canary 3"));
        assertEquals("Slot A: Matcha Death Rebalance - Canary 8", MarkerState.load(gameDirectory).lines().get(1));

        Path replacement = gameDirectory.resolve("replacement.json");
        Files.writeString(replacement, projection(List.of(
                "Baseline: Stack v1",
                "Slot A: Matcha Death Rebalance - Canary 9",
                "Slot B: Mossy Stone - Canary 4")), StandardCharsets.UTF_8);
        Files.move(
                replacement,
                gameDirectory.resolve(MarkerState.PROJECTION_NAME),
                StandardCopyOption.REPLACE_EXISTING);

        assertEquals("Slot A: Matcha Death Rebalance - Canary 9", MarkerState.load(gameDirectory).lines().get(1));
        assertEquals("Slot B: Mossy Stone - Canary 4", MarkerState.load(gameDirectory).lines().get(2));
    }

    @Test
    void retiredV1MetadataCannotSupplyOrOverrideLines() throws IOException {
        Files.writeString(
                gameDirectory.resolve(".workbench-instance-manager.v1-retired.json"),
                "{\"testSet\":\"Stale Project — Canary 99\"}\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                gameDirectory.resolve(".workbench-instance-manager.json"),
                "{\"testSet\":\"Active But Stale — Canary 98\"}\n",
                StandardCharsets.UTF_8);
        MarkerState missing = MarkerState.load(gameDirectory);
        assertEquals(MarkerState.Status.INVALID, missing.status());

        writeProjection(List.of("Baseline: Stack v1", "Slot A: Empty", "Slot B: Empty"));
        MarkerState current = MarkerState.load(gameDirectory);
        assertEquals(MarkerState.Status.ACTIVE, current.status());
        assertEquals(List.of("Baseline: Stack v1", "Slot A: Empty", "Slot B: Empty"), current.lines());
    }

    private void writeProjection(List<String> lines) throws IOException {
        Files.writeString(
                gameDirectory.resolve(MarkerState.PROJECTION_NAME),
                projection(lines),
                StandardCharsets.UTF_8);
    }

    private static String projection(List<String> lines) {
        JsonObject root = new JsonObject();
        root.addProperty("$schema", "mynx-runtime-title-state-v1");
        root.addProperty("schema_version", 1);
        root.addProperty("state_revision", 12);
        root.addProperty("state_digest", "a".repeat(64));
        JsonArray array = new JsonArray();
        lines.forEach(array::add);
        root.add("lines", array);
        return root + "\n";
    }
}
