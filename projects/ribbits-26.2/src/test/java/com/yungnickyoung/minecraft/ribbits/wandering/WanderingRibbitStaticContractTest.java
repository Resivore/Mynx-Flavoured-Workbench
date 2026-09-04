package com.yungnickyoung.minecraft.ribbits.wandering;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitStaticContractTest {
    private final Path root = Path.of(System.getProperty("projectRoot"));

    @Test
    void entityIsDedicatedAndDoesNotRestockOrAwardXp() throws Exception {
        String entity = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/WanderingRibbitEntity.java");
        assertTrue(entity.contains("extends AbstractVillager implements GeoEntity"));
        assertFalse(entity.contains("extends RibbitEntity"));
        assertFalse(entity.contains("extends WanderingTrader"));
        assertTrue(entity.contains("public boolean canRestock()"));
        assertTrue(entity.contains("return false;"));
        assertTrue(entity.contains("protected void rewardTradeXp(MerchantOffer offer)"));
        assertTrue(entity.contains("SchedulerManaged"));
        assertTrue(entity.contains("reason.shouldDestroy()"));
    }

    @Test
    void schedulerIsIndependentBoundedAndLoadedChunkOnly() throws Exception {
        String scheduler = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/world/spawn/WanderingRibbitScheduler.java");
        assertFalse(scheduler.contains("WanderingTraderSpawner"));
        assertFalse(scheduler.contains("WANDERING_TRADER"));
        assertTrue(scheduler.contains("getChunkNow"));
        assertTrue(scheduler.contains("areEntitiesLoaded"));
        assertTrue(scheduler.contains("level.getEntity(entity.getUUID()) != entity"));
        assertTrue(scheduler.contains("distanceSquared < (long) MIN_RADIUS * MIN_RADIUS"));
        assertTrue(scheduler.contains("distanceSquared > (long) MAX_RADIUS * MAX_RADIUS"));
        assertTrue(scheduler.contains("EntitySpawnReason.EVENT"));
        assertTrue(scheduler.contains("CANDIDATES_PER_PLAYER = 16"));
        assertTrue(scheduler.contains("MAX_CANDIDATES = 64"));
        assertTrue(scheduler.contains("VISIT_LIFETIME_TICKS = 48_000"));
        assertTrue(scheduler.contains("player.gameMode()"));
        assertTrue(scheduler.contains("GameType.ADVENTURE"));
    }

    @Test
    void nativeMenuUsesCanonicalMapModifierOnceAndThreeUniqueCuriosities() throws Exception {
        String map = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitMapOffer.java");
        assertTrue(map.contains("ribbit_village_explorer_result"));
        assertEquals(1, occurrences(map, "modifier.apply("));
        assertFalse(map.contains("findNearestMapStructure"));

        String nativeProvider = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitNativeTradeProvider.java");
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 20)"));
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 8)"));
        assertTrue(nativeProvider.contains("new ItemCost(Items.COMPASS, 1)"));
        assertTrue(nativeProvider.contains("for (int i = 0; i < 3; i++)"));
        assertTrue(nativeProvider.contains("pool.set(i, pool.get(selected))"));
    }

    @Test
    void clientResourcesUseApprovedPrivateIdsAndSharedAnimations() throws Exception {
        String model = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/model/WanderingRibbitModel.java");
        assertTrue(model.contains("id(\"wandering_ribbit\")"));
        assertTrue(model.contains("id(\"textures/entity/wandering_ribbit.png\")"));
        assertTrue(model.contains("id(\"ribbit\")"));
    }

    @Test
    void biomeTagsAreConcreteAndDenyOceans() throws Exception {
        String allow = read("common/src/main/resources/data/ribbits/tags/worldgen/biome/allows_wandering_ribbit_spawns.json");
        String deny = read("common/src/main/resources/data/ribbits/tags/worldgen/biome/without_wandering_ribbit_spawns.json");
        assertTrue(allow.contains("#minecraft:is_forest"));
        assertTrue(allow.contains("#minecraft:has_structure/swamp_hut"));
        assertTrue(deny.contains("#minecraft:is_ocean"));
    }

    private String read(String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int at = 0;
        while ((at = source.indexOf(needle, at)) >= 0) {
            count++;
            at += needle.length();
        }
        return count;
    }
}
