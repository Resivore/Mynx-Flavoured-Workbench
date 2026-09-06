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
        assertTrue(scheduler.contains("Holder<Biome> biome = level.getBiome(feet)"));
        assertTrue(scheduler.contains(".get(ALLOW_BIOMES)"));
        assertTrue(scheduler.contains(".isPresent()"));
        assertTrue(scheduler.contains("[WanderingRibbitDiagnostic]"));
        assertTrue(scheduler.contains("scheduler initialized gameTime={} initialDelayTicks={} firstAttemptTime={}"));
        assertTrue(scheduler.contains("attempt gameTime={} scheduledAttemptTime={} eligiblePlayers={} candidatesExamined={}"));
        assertTrue(scheduler.contains("chunkUnavailable={} entitiesNotLoaded={} invalidHeight={} groundNotSturdy={}"));
        assertTrue(scheduler.contains("hazardousGroundBodyHead={} fluidGroundBodyHead={} biomeRejected={}"));
        assertTrue(scheduler.contains("worldBorderRejected={} collisionRejected={} minimumPlayerDistanceRejected={}"));
        assertTrue(scheduler.contains("acceptedSpawnSites={}"));
        assertTrue(scheduler.contains("spawn stage=entity_spawn_returned_null"));
        assertTrue(scheduler.contains("spawn stage=wandering_ribbit_entity_created"));
        assertTrue(scheduler.contains("spawn stage=scheduler_lease_initialized"));
        assertTrue(scheduler.contains("spawn stage=offers_materialized"));
        assertTrue(scheduler.contains("spawn stage=naturalist_companions_initialized"));
        assertTrue(scheduler.contains("spawn stage=entity_validity_check_passed"));
        assertTrue(scheduler.contains("spawn stage=scheduler_lease_committed"));
        assertTrue(scheduler.contains("if (!diagnostics.collisionDetailLogged)"));
        assertTrue(scheduler.contains("diagnostics.collisionDetailLogged = true"));
        assertTrue(scheduler.contains("logCollisionRejection(level, feet, selectedPlayer, spawnBox"));
        assertTrue(scheduler.contains("level.noBlockCollision(null, spawnBox)"));
        assertTrue(scheduler.contains("level.noEntityCollision(null, spawnBox)"));
        assertTrue(scheduler.contains("level.noBorderCollision(null, spawnBox)"));
        assertTrue(scheduler.contains("spawnBox.inflate(ENTITY_COLLISION_QUERY_EPSILON)"));
        assertTrue(scheduler.contains("EntitySelector.CAN_BE_COLLIDED_WITH"));
        assertTrue(scheduler.contains("noCollisionSources={} blocksClear={} entitiesClear={}"));
        assertTrue(scheduler.contains("ground={} body={} head={}"));
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
        assertFalse(nativeProvider.contains("new ItemCost(Items.COMPASS, 1)"));
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 8)"));
        assertTrue(nativeProvider.contains("SCHEMA_VERSION = 3"));
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
    void biomeTagsCarryTheAuditedBaselineAndDenyFamilies() throws Exception {
        String allow = read("common/src/main/resources/data/ribbits/tags/worldgen/biome/allows_wandering_ribbit_spawns.json");
        String deny = read("common/src/main/resources/data/ribbits/tags/worldgen/biome/without_wandering_ribbit_spawns.json");
        String[] baseline = {
                "swamp", "mangrove_swamp",
                "forest", "flower_forest", "birch_forest", "old_growth_birch_forest",
                "dark_forest", "pale_garden",
                "plains", "sunflower_plains", "meadow", "cherry_grove",
                "jungle", "sparse_jungle", "bamboo_jungle",
                "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga",
                "mushroom_fields", "river"
        };
        for (String biome : baseline) {
            assertEquals(1, occurrences(allow, "\"minecraft:" + biome + "\""), biome);
        }
        assertTrue(allow.contains("#minecraft:is_forest"));
        assertTrue(allow.contains("#minecraft:is_jungle"));
        assertTrue(allow.contains("#minecraft:is_taiga"));
        assertTrue(allow.contains("#minecraft:is_river"));
        assertTrue(allow.contains("minecraft:mangrove_swamp"));
        assertTrue(allow.contains("minecraft:meadow"));
        assertTrue(allow.contains("minecraft:cherry_grove"));
        assertTrue(allow.contains("minecraft:mushroom_fields"));
        assertTrue(deny.contains("#minecraft:is_ocean"));
        assertTrue(deny.contains("#minecraft:is_badlands"));
        assertTrue(deny.contains("#minecraft:is_savanna"));
        assertTrue(deny.contains("#minecraft:is_hill"));
        assertTrue(deny.contains("#minecraft:spawns_snow_foxes"));
        assertTrue(deny.contains("minecraft:stony_peaks"));
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
