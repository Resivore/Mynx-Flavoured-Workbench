package dev.resivore.xaerodiscovery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;

public record DiscoveryConfig(int discoveryRadiusChunks) {
    public static final int DEFAULT_RADIUS = 2;
    public static final int MAX_RADIUS = 32;

    public static DiscoveryConfig load(Path path, Logger logger) {
        if (!Files.exists(path)) {
            DiscoveryConfig defaults = defaults();
            try {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(path, defaults.toJson(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                logger.warn("Could not create Xaero Discovery Radius config {}; using radius {}", path, DEFAULT_RADIUS, exception);
            }
            return defaults;
        }

        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            logger.warn("Could not parse Xaero Discovery Radius config {}; using radius {}", path, DEFAULT_RADIUS, exception);
            return defaults();
        }
    }

    static DiscoveryConfig parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return defaults();
        }
        JsonObject object = root.getAsJsonObject();
        JsonElement radiusElement = object.get("discoveryRadiusChunks");
        if (radiusElement == null || !radiusElement.isJsonPrimitive() || !radiusElement.getAsJsonPrimitive().isNumber()) {
            return defaults();
        }
        int radius;
        try {
            radius = radiusElement.getAsBigDecimal().intValueExact();
        } catch (RuntimeException exception) {
            return defaults();
        }
        if (radius < 0 || radius > MAX_RADIUS) {
            return defaults();
        }
        return new DiscoveryConfig(radius);
    }

    static DiscoveryConfig defaults() {
        return new DiscoveryConfig(DEFAULT_RADIUS);
    }

    private String toJson() {
        return "{\n  \"discoveryRadiusChunks\": " + discoveryRadiusChunks + "\n}\n";
    }
}
