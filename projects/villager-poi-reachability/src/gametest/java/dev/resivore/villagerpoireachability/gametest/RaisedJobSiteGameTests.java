package dev.resivore.villagerpoireachability.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

/** Runs the production AcquirePoi -> reservation -> profession path against raised real POIs. */
public final class RaisedJobSiteGameTests {
    private static final BlockPos VILLAGER = new BlockPos(2, 2, 1);
    private static final BlockPos SUPPORT = new BlockPos(3, 2, 1);
    private static final BlockPos RAISED_POI = SUPPORT.above();

    @GameTest(maxTicks = 130)
    public void raisedBrewingStandAcquiresClericAtRealPoi(GameTestHelper helper) {
        acquireAndAssign(helper, Blocks.BREWING_STAND, PoiTypes.CLERIC, VillagerProfession.CLERIC);
    }

    @GameTest(maxTicks = 130)
    public void raisedComposterAcquiresFarmerAtRealPoi(GameTestHelper helper) {
        acquireAndAssign(helper, Blocks.COMPOSTER, PoiTypes.FARMER, VillagerProfession.FARMER);
    }

    @GameTest(maxTicks = 130)
    public void normalHeightBrewingStandStillAcquiresCleric(GameTestHelper helper) {
        BlockPos normalPoi = new BlockPos(3, 2, 1);
        prepareFloor(helper);
        helper.setBlock(normalPoi, Blocks.BREWING_STAND);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        acquire(helper, villager, normalPoi, PoiTypes.CLERIC, VillagerProfession.CLERIC);
    }

    @GameTest(maxTicks = 130)
    public void raisedFloristFixtureKeepsRealPotPoi(GameTestHelper helper) {
        ResourceKey<PoiType> florist = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                Identifier.fromNamespaceAndPath("mynx_flora_trades", "florist"));
        acquireAndAssign(helper, Blocks.FLOWER_POT, florist, null);
    }

    @GameTest(maxTicks = 130)
    public void twoBlockRaisedWorkstationIsNotClaimedFromPlainFloor(GameTestHelper helper) {
        prepareFloor(helper);
        BlockPos upperSupport = SUPPORT.above();
        BlockPos twoHighPoi = upperSupport.above();
        helper.setBlock(SUPPORT, Blocks.STONE);
        helper.setBlock(upperSupport, Blocks.STONE);
        helper.setBlock(twoHighPoi, Blocks.BREWING_STAND);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        assertNotAcquired(helper, villager, PoiTypes.CLERIC, "two-block raised workstation was claimed");
    }

    @GameTest(maxTicks = 130)
    public void sealedRaisedWorkstationIsNotClaimed(GameTestHelper helper) {
        prepareFloor(helper);
        helper.setBlock(SUPPORT, Blocks.STONE);
        helper.setBlock(RAISED_POI, Blocks.BREWING_STAND);
        for (BlockPos side : new BlockPos[] { SUPPORT.north(), SUPPORT.south(), SUPPORT.east(), SUPPORT.west() }) {
            helper.setBlock(side, Blocks.STONE);
            helper.setBlock(side.above(), Blocks.STONE);
        }
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        assertNotAcquired(helper, villager, PoiTypes.CLERIC, "sealed raised workstation was claimed");
    }

    @GameTest(maxTicks = 130)
    public void raisedPoiRemainsSingleCapacityAndReleasesWhenBroken(GameTestHelper helper) {
        prepareRaised(helper, Blocks.BREWING_STAND);
        Villager first = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        Villager second = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        var acquire = AcquirePoi.create(holder -> holder.is(PoiTypes.CLERIC), MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty(), (level, pos) -> true);
        for (int tick = 2; tick < 80; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (first.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty()) tick(acquire, helper, first);
                if (second.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty()) tick(acquire, helper, second);
            });
        }
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(first.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent(),
                    "first villager did not reserve the raised POI");
            helper.assertTrue(second.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty(),
                    "occupied raised POI was claimed by a second villager");
            helper.destroyBlock(RAISED_POI);
        });
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(helper.getLevel().getPoiManager().getType(helper.absolutePos(RAISED_POI)).isEmpty(),
                    "breaking the raised station did not remove its real POI");
            helper.succeed();
        });
    }

    private static void acquireAndAssign(GameTestHelper helper, Block block, ResourceKey<PoiType> poi,
            ResourceKey<VillagerProfession> profession) {
        prepareRaised(helper, block);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        acquire(helper, villager, RAISED_POI, poi, profession);
    }

    private static void acquire(GameTestHelper helper, Villager villager, BlockPos poiPos, ResourceKey<PoiType> poi,
            ResourceKey<VillagerProfession> profession) {
        Holder<PoiType> poiHolder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(poi);
        helper.assertTrue(poiHolder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE), "fixture is not an acquirable job site");
        var acquire = AcquirePoi.create(holder -> holder.is(poi), MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty(), (level, pos) -> true);
        for (int tick = 2; tick < 80; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty()) tick(acquire, helper, villager);
            });
        }
        helper.runAtTickTime(80, () -> {
            BlockPos absolutePoi = helper.absolutePos(poiPos);
            helper.assertTrue(helper.getLevel().getPoiManager().getType(absolutePoi)
                            .map(type -> type.is(poi)).orElse(false),
                    "real workstation POI was moved, absent, or typed incorrectly");
            GlobalPos potential = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                    .orElseThrow(() -> helper.assertionException("raised job site was not reserved"));
            helper.assertTrue(potential.pos().equals(absolutePoi), "POTENTIAL_JOB_SITE is not the real workstation position");
            helper.assertTrue(helper.getLevel().getPoiManager().findClosest(holder -> holder.is(poi), absolutePoi, 1,
                            PoiManager.Occupancy.HAS_SPACE).isEmpty(), "raised job site did not become reserved");
            if (profession != null) {
                tick(AssignProfessionFromJobSite.create(), helper, villager);
                helper.assertTrue(villager.getVillagerData().profession().is(profession),
                        "normal profession assignment did not follow reservation");
            }
            helper.succeed();
        });
    }

    private static void assertNotAcquired(GameTestHelper helper, Villager villager, ResourceKey<PoiType> poi,
            String failure) {
        var acquire = AcquirePoi.create(holder -> holder.is(poi), MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty(), (level, pos) -> true);
        for (int tick = 2; tick < 80; tick++) helper.runAtTickTime(tick, () -> tick(acquire, helper, villager));
        helper.runAtTickTime(90, () -> {
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty(), failure);
            helper.succeed();
        });
    }

    private static void prepareRaised(GameTestHelper helper, Block block) {
        prepareFloor(helper);
        helper.setBlock(SUPPORT, Blocks.STONE);
        helper.setBlock(RAISED_POI, block);
    }

    private static void prepareFloor(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 3; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
    }

    private static <E extends net.minecraft.world.entity.LivingEntity> void tick(BehaviorControl<E> behavior,
            GameTestHelper helper, E entity) {
        behavior.tryStart(helper.getLevel(), entity, helper.getLevel().getGameTime());
        behavior.tickOrStop(helper.getLevel(), entity, helper.getLevel().getGameTime());
    }
}
