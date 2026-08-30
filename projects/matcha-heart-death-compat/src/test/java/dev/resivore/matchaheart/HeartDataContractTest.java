package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class HeartDataContractTest {
    @Test void authoritativeDataMapsOwnTheExactConsolidatedReloadSurface() {
        assertEquals("matcha_heart_death_compat:reinforced_crystal_heart",
                HeartDataContract.REINFORCED_RECIPE_ID);
        assertEquals("matcha_heart_death_compat:resonant_favour",
                HeartDataContract.RESONANT_FAVOUR_RECIPE_ID);
        assertEquals(Map.of(
                "minecraft:gameplay/fishing/deep_dark",
                "data/minecraft/loot_table/gameplay/fishing/deep_dark.json",
                "minecraft:blocks/sculk_sensor",
                "data/minecraft/loot_table/blocks/sculk_sensor.json",
                "minecraft:blocks/calibrated_sculk_sensor",
                "data/minecraft/loot_table/blocks/calibrated_sculk_sensor.json",
                "minecraft:blocks/sculk_shrieker",
                "data/minecraft/loot_table/blocks/sculk_shrieker.json"),
                HeartDataContract.LOOT_TABLE_RESOURCES);
        assertEquals(Map.of(
                HeartDataContract.RECIPE_ID, "data/crafting/recipe/crystal_heart.json",
                "crafting:sculk_sensor", "data/crafting/recipe/sculk_sensor.json",
                "crafting:sculk_shrieker", "data/crafting/recipe/sculk_shrieker.json"),
                HeartDataContract.RECIPE_RESOURCES);
        assertEquals(Map.of(
                HeartDataContract.ADVANCEMENT_ID,
                "data/main/advancement/mechanics/heart_container_obtained.json",
                "main:recipe_unlocks/echo_shard",
                "data/main/advancement/recipe_unlocks/echo_shard.json"),
                HeartDataContract.ADVANCEMENT_RESOURCES);
    }

    @Test void bothSourceOrdersProduceIdenticalAuthoritativeResult() {
        Map<String, String> matchaThenCompat = new LinkedHashMap<>();
        matchaThenCompat.put(HeartDataContract.RECIPE_ID, "turtle_scute");
        matchaThenCompat.put(HeartDataContract.RECIPE_ID, "echo_shard");
        Map<String, String> compatThenMatcha = new LinkedHashMap<>();
        compatThenMatcha.put(HeartDataContract.RECIPE_ID, "echo_shard");
        compatThenMatcha.put(HeartDataContract.RECIPE_ID, "turtle_scute");

        assertEquals(
                HeartDataContract.replaceExact(matchaThenCompat, HeartDataContract.RECIPE_ID, () -> "echo_shard"),
                HeartDataContract.replaceExact(compatThenMatcha, HeartDataContract.RECIPE_ID, () -> "echo_shard"));
    }

    @Test void exactIdReplacementIsScopedAndIdempotent() {
        Map<String, String> input = Map.of("other:recipe", "untouched", HeartDataContract.RECIPE_ID, "upstream");
        Map<String, String> once = HeartDataContract.replaceExact(
                input, HeartDataContract.RECIPE_ID, () -> "authoritative");
        Map<String, String> twice = HeartDataContract.replaceExact(
                once, HeartDataContract.RECIPE_ID, () -> "authoritative");
        assertEquals("untouched", twice.get("other:recipe"));
        assertEquals("authoritative", twice.get(HeartDataContract.RECIPE_ID));
        assertEquals(once, twice);
    }

    @Test void advancementReplacementRejectsUpstreamAndAcceptsNeutral() {
        Map<String, String> upstream = Map.of(HeartDataContract.ADVANCEMENT_ID, "inventory_changed->process");
        Map<String, String> enforced = HeartDataContract.replaceExact(
                upstream, HeartDataContract.ADVANCEMENT_ID, () -> "impossible");
        assertEquals("impossible", enforced.get(HeartDataContract.ADVANCEMENT_ID));
        assertFalse(enforced.containsValue("inventory_changed->process"));
    }

    @Test void unexpectedMissingAuthoritativeValueFailsClosed() {
        assertThrows(IllegalStateException.class, () -> HeartDataContract.replaceExact(
                Map.of(HeartDataContract.RECIPE_ID, "upstream"), HeartDataContract.RECIPE_ID, () -> null));
    }

    @Test void onlyTwoExactFunctionsAreBlocked() {
        assertTrue(HeartDataContract.blocksFunction(HeartDataContract.PROCESS_FUNCTION_ID));
        assertTrue(HeartDataContract.blocksFunction(HeartDataContract.DEATH_FUNCTION_ID));
        assertFalse(HeartDataContract.blocksFunction("main:unrelated"));
    }
}
