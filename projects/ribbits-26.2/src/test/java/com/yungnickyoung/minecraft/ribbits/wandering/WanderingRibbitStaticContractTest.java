package com.yungnickyoung.minecraft.ribbits.wandering;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.AABB;

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
    void schedulerUsesTheHeightmapSurfaceAsSupportAndPlacesFeetAboveIt() throws Exception {
        String scheduler = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/world/spawn/WanderingRibbitScheduler.java");
        assertTrue(scheduler.contains("int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);"));
        assertTrue(scheduler.contains("BlockPos groundPos = new BlockPos(x, surfaceY, z);"));
        assertTrue(scheduler.contains("BlockPos feet = groundPos.above();"));
        assertTrue(scheduler.contains("groundPos.getY() < level.getMinY() || feet.above().getY() >= level.getMaxY()"));
        assertTrue(scheduler.contains("BlockState ground = chunk.getBlockState(groundPos);"));
        assertTrue(scheduler.contains("BlockState body = chunk.getBlockState(feet);"));
        assertTrue(scheduler.contains("BlockState head = chunk.getBlockState(feet.above());"));
        assertTrue(scheduler.contains("x + 0.5D, feet.getY(), z + 0.5D"));
        assertTrue(scheduler.contains("EntitySpawnReason.EVENT"));
        assertFalse(scheduler.contains("BlockPos feet = new BlockPos(x, y, z);"));
        assertFalse(scheduler.contains("BlockPos groundPos = feet.below();"));

        // The mapped AABB type confirms the corrected feet-height geometry: the 0.5 x 0.75
        // merchant box clears a full support block, while an actual body-space block intersects.
        AABB support = new AABB(30.0D, -61.0D, -22.0D, 31.0D, -60.0D, -21.0D);
        AABB correctedMerchant = new AABB(30.25D, -60.0D, -21.75D, 30.75D, -59.25D, -21.25D);
        AABB bodyObstruction = new AABB(30.0D, -60.0D, -22.0D, 31.0D, -59.0D, -21.0D);
        assertFalse(correctedMerchant.intersects(support));
        assertTrue(correctedMerchant.intersects(bodyObstruction));
    }

    @Test
    void nativeMenuUsesCanonicalMapModifierWithoutCuriosityRandomization() throws Exception {
        String map = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitMapOffer.java");
        assertTrue(map.contains("ribbit_village_explorer_result"));
        assertEquals(1, occurrences(map, "modifier.apply("));
        assertFalse(map.contains("findNearestMapStructure"));

        String nativeProvider = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/WanderingRibbitNativeTradeProvider.java");
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 20)"));
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 8)"));
        assertFalse(nativeProvider.contains("new ItemCost(Items.COMPASS, 1)"));
        assertTrue(nativeProvider.contains("new ItemCost(glowcap, 8)"));
        assertTrue(nativeProvider.contains("SCHEMA_VERSION = 4"));
        assertTrue(nativeProvider.contains("NATIVE_OFFER_COUNT = 3"));
        assertFalse(nativeProvider.contains("record Curiosity"));
        assertFalse(nativeProvider.contains("CURIOSITIES"));
        assertFalse(nativeProvider.contains("nextInt(pool"));
    }

    @Test
    void clientResourcesUseApprovedPrivateIdsAndSharedAnimations() throws Exception {
        String model = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/model/WanderingRibbitModel.java");
        assertTrue(model.contains("id(\"wandering_ribbit\")"));
        assertTrue(model.contains("id(\"textures/entity/wandering_ribbit.png\")"));
        assertTrue(model.contains("id(\"ribbit\")"));
    }

    @Test
    void optionalNaturalistLeashPatchNeverProbesNaturalistDuringMixinSelection() throws Exception {
        String normalConfig = read("common/src/main/resources/ribbits.mixins.json");
        String optionalConfig = read("common/src/main/resources/ribbits.naturalist.mixins.json");
        String metadata = read("fabric/src/main/resources/fabric.mod.json");
        String mixin = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/compat/NaturalistSnailLeashMixin.java");

        assertFalse(normalConfig.contains("\"plugin\""));
        assertFalse(normalConfig.contains("NaturalistSnailLeashMixin"));
        assertFalse(Files.exists(root.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/mixin/RibbitsMixinPlugin.java")));
        assertTrue(optionalConfig.contains("\"required\": false"));
        assertTrue(optionalConfig.contains("\"package\": \"com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.compat\""));
        assertTrue(optionalConfig.contains("\"NaturalistSnailLeashMixin\""));
        assertTrue(optionalConfig.contains("\"defaultRequire\": 0"));
        assertTrue(metadata.contains("\"config\": \"ribbits.naturalist.mixins.json\""));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertFalse(metadata.contains("\"naturalist\""));
        assertTrue(mixin.contains("@Pseudo"));
        assertTrue(mixin.contains("@Mixin(targets = \"com.crispytwig.naturalist.server.entity.mob.Snail\")"));
        assertFalse(mixin.contains("import com.crispytwig"));

        try (var sources = Files.walk(root.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/mixin"))) {
            String mixinStartupSource = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (java.io.IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    })
                    .reduce("", String::concat);
            assertFalse(mixinStartupSource.contains("Class.forName"));
            assertFalse(mixinStartupSource.contains(".loadClass("));
            assertFalse(mixinStartupSource.contains("getClassLoader()"));
        }

        assertTrue(mixin.contains("getLeashHolder() instanceof WanderingRibbitEntity"));
        assertFalse(mixin.contains("@Inject"));
        assertTrue(mixin.contains("public Vec3 getLeashOffset()"));
        assertTrue(mixin.contains("new Vec3(0.0D, snail.getBbHeight() * 0.48D, 0.0D)"));

        String wandering = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/WanderingRibbitEntity.java");
        assertFalse(wandering.contains("public Vec3 getLeashOffset()"));
        assertTrue(wandering.contains("public Vec3 getRopeHoldPosition(float partialTick)"));
        assertTrue(wandering.contains("this.getPosition(partialTick).add(0.0D, this.getBbHeight() * 0.82D, 0.0D)"));
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
