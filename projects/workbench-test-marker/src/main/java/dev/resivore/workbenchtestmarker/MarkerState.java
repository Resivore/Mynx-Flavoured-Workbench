package dev.resivore.workbenchtestmarker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public record MarkerState(List<String> lines, Status status) {
    static final String PROJECTION_NAME = ".mynx-runtime-v2-title.json";
    private static final String PROJECTION_SCHEMA = "mynx-runtime-title-state-v1";
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern BASELINE = Pattern.compile("Baseline: Stack v[1-9][0-9]*");
    private static final Pattern SLOT_A = Pattern.compile("Slot A: (?:Empty|.+ - Canary [0-9]+)");
    private static final Pattern SLOT_B = Pattern.compile("Slot B: (?:Empty|.+ - Canary [0-9]+)");

    public MarkerState {
        lines = List.copyOf(lines);
    }

    public static MarkerState current() {
        return load(FabricLoader.getInstance().getGameDir());
    }

    static MarkerState load(Path gameDirectory) {
        Path normalizedGameDirectory = gameDirectory.toAbsolutePath().normalize();
        Path projection = normalizedGameDirectory.resolve(PROJECTION_NAME).normalize();
        if (!normalizedGameDirectory.equals(projection.getParent())) {
            return invalid("WORKBENCH TEST STATE: INVALID PROJECTION PATH");
        }
        if (!Files.isRegularFile(projection)) {
            return invalid("WORKBENCH TEST STATE: DISPLAY PROJECTION MISSING");
        }

        try {
            JsonElement root = JsonParser.parseString(Files.readString(projection, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return invalid("WORKBENCH TEST STATE: INVALID DISPLAY PROJECTION");
            }
            JsonObject object = root.getAsJsonObject();
            if (!stringValue(object, "$schema", PROJECTION_SCHEMA)
                    || !integerAtLeastZero(object, "schema_version", 1, true)
                    || !integerAtLeastZero(object, "state_revision", 0, false)
                    || !sha256Value(object, "state_digest")) {
                return invalid("WORKBENCH TEST STATE: INVALID DISPLAY PROJECTION");
            }

            JsonElement rawLines = object.get("lines");
            if (rawLines == null || !rawLines.isJsonArray()) {
                return invalid("WORKBENCH TEST STATE: DISPLAY LINES MISSING");
            }
            JsonArray array = rawLines.getAsJsonArray();
            if (array.size() != 3) {
                return invalid("WORKBENCH TEST STATE: DISPLAY LINES INVALID");
            }
            List<String> lines = List.of(
                    requiredString(array.get(0)),
                    requiredString(array.get(1)),
                    requiredString(array.get(2)));
            if (!BASELINE.matcher(lines.get(0)).matches()
                    || !SLOT_A.matcher(lines.get(1)).matches()
                    || !SLOT_B.matcher(lines.get(2)).matches()
                    || lines.stream().anyMatch(line -> line.toUpperCase().contains("UNKNOWN"))) {
                return invalid("WORKBENCH TEST STATE: DISPLAY LINES INVALID");
            }
            return new MarkerState(lines, Status.ACTIVE);
        } catch (IOException | RuntimeException exception) {
            return invalid("WORKBENCH TEST STATE: DISPLAY PROJECTION READ FAILED");
        }
    }

    private static boolean stringValue(JsonObject object, String key, String expected) {
        JsonElement element = object.get(key);
        return element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && expected.equals(element.getAsString());
    }

    private static boolean integerAtLeastZero(JsonObject object, String key, int expected, boolean exact) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        int value = element.getAsInt();
        return exact ? value == expected : value >= expected;
    }

    private static boolean sha256Value(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null
                && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && SHA_256.matcher(element.getAsString()).matches();
    }

    private static String requiredString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("display line is not a string");
        }
        String value = element.getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException("display line is blank");
        }
        return value;
    }

    private static MarkerState invalid(String text) {
        return new MarkerState(List.of(text), Status.INVALID);
    }

    public enum Status {
        ACTIVE,
        INVALID
    }
}
