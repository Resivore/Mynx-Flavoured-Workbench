package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class DiscoveryConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesValidIntegralValuesIncludingLimits() {
        assertEquals(0, DiscoveryConfig.parse("{\"discoveryRadiusChunks\":0}").discoveryRadiusChunks());
        assertEquals(4, DiscoveryConfig.parse("{\"discoveryRadiusChunks\":4}").discoveryRadiusChunks());
        assertEquals(32, DiscoveryConfig.parse("{\"discoveryRadiusChunks\":32}").discoveryRadiusChunks());
    }

    @Test
    void fallsBackForMissingWrongTypeFractionalAndOutOfRangeValues() {
        String[] invalid = {
                "{}",
                "{\"discoveryRadiusChunks\":null}",
                "{\"discoveryRadiusChunks\":\"4\"}",
                "{\"discoveryRadiusChunks\":4.5}",
                "{\"discoveryRadiusChunks\":-1}",
                "{\"discoveryRadiusChunks\":33}"
        };
        for (String json : invalid) {
            assertEquals(DiscoveryConfig.DEFAULT_RADIUS, DiscoveryConfig.parse(json).discoveryRadiusChunks());
        }
    }

    @Test
    void createsDefaultConfigWhenMissingAndSurvivesMalformedJson() throws Exception {
        Path config = temporaryDirectory.resolve("config").resolve("xaero-discovery-radius.json");
        DiscoveryConfig first = DiscoveryConfig.load(config, LoggerFactory.getLogger(getClass()));
        assertEquals(DiscoveryConfig.DEFAULT_RADIUS, first.discoveryRadiusChunks());
        assertTrue(Files.readString(config, StandardCharsets.UTF_8).contains("\"discoveryRadiusChunks\": 2"));

        Files.writeString(config, "not json", StandardCharsets.UTF_8);
        DiscoveryConfig fallback = DiscoveryConfig.load(config, LoggerFactory.getLogger(getClass()));
        assertEquals(DiscoveryConfig.DEFAULT_RADIUS, fallback.discoveryRadiusChunks());
    }
}
