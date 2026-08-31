package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MatchaColdWaterGuardTest {
    private static final Pattern ARMOR_GUARD = Pattern.compile(
            "equipment:\\{(head|chest|legs|feet):.*?\"main:freezing_protection\":(\\d+)");

    @Test
    void packagedGuardRequiresExactLevelThreeOnExactlyTheFourArmorSlots()
            throws IOException {
        assertEquals(
                Map.of("head", 3, "chest", 3, "legs", 3, "feet", 3),
                parsedRequirements());
    }

    @Test
    void actualPackagedGuardCoversEverySlotAndLevelMatrix() throws IOException {
        Map<String, Integer> requirements = parsedRequirements();
        for (String slot : Set.of("head", "chest", "legs", "feet")) {
            for (int level = 0; level <= 4; level++) {
                assertEquals(
                        level == 3,
                        bypassesPenalty(Map.of(slot, level), requirements),
                        slot + " level " + level);
            }
        }
    }

    @Test
    void actualPackagedGuardIsAnyMatchIdempotentAndReevaluatedAfterRemoval()
            throws IOException {
        Map<String, Integer> requirements = parsedRequirements();
        assertFalse(bypassesPenalty(Map.of(), requirements));
        assertTrue(bypassesPenalty(Map.of("head", 3, "feet", 3), requirements));
        assertFalse(bypassesPenalty(Map.of("head", 0, "feet", 0), requirements));
        assertFalse(bypassesPenalty(Map.of("mainhand", 3), requirements));
    }

    private static Map<String, Integer> parsedRequirements() throws IOException {
        String command = Files.readString(guardPath()).trim();
        var matcher = ARMOR_GUARD.matcher(command);
        Map<String, Integer> requirements = new LinkedHashMap<>();
        while (matcher.find()) {
            String slot = matcher.group(1);
            int level = Integer.parseInt(matcher.group(2));
            assertNull(requirements.put(slot, level), () -> "Duplicate armor guard: " + slot);
        }
        return Map.copyOf(requirements);
    }

    private static boolean bypassesPenalty(
            Map<String, Integer> equippedLevels,
            Map<String, Integer> requiredLevels) {
        return requiredLevels.entrySet().stream().anyMatch(requirement ->
                equippedLevels.getOrDefault(requirement.getKey(), 0).intValue()
                        == requirement.getValue().intValue());
    }

    private static Path guardPath() {
        return Path.of(System.getProperty("projectRoot"))
                .toAbsolutePath()
                .normalize()
                .resolve("src/main/resources/data/matcha_frost_protection/function/environmental")
                .resolve("check_freezing_water_conditions.mcfunction");
    }
}
