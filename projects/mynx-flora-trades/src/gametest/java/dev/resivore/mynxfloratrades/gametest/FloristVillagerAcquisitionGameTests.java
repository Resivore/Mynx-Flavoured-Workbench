package dev.resivore.mynxfloratrades.gametest;

import dev.resivore.mynxfloratrades.FloristRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Exercises Minecraft 26.2's real POI acquisition, profession assignment, and release behaviors. */
public final class FloristVillagerAcquisitionGameTests {
    private static final BlockPos POT = new BlockPos(3, 2, 1);
    private static final BlockPos VILLAGER = new BlockPos(2, 2, 1);

    @GameTest(maxTicks = 130)
    public void emptyFlowerPotAcquiresFloristThroughVanillaBehaviors(GameTestHelper helper) {
        acquireAssignAndRelease(helper, Blocks.FLOWER_POT);
    }

    @GameTest(maxTicks = 130)
    public void pottedVanillaFlowerAcquiresFloristThroughVanillaBehaviors(GameTestHelper helper) {
        acquireAssignAndRelease(helper, Blocks.POTTED_POPPY);
    }

    private static void acquireAssignAndRelease(GameTestHelper helper, Block workstation) {
        Holder<PoiType> florist = FloristRegistry.floristHolder();
        Holder<PoiType> vanillaFarmer = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.FARMER);
        helper.assertTrue(florist.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                "Florist is absent from Minecraft's acquirable_job_site tag");
        helper.assertTrue(vanillaFarmer.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                "Flora's tag contribution replaced vanilla acquirable job sites");

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 3; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        helper.setBlock(POT, workstation);
        // Suppress the scheduler so this harness alone drives the exact production controls under test.
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER);
        Holder<VillagerProfession> unemployed = villager.getVillagerData().profession();
        helper.assertTrue(unemployed.is(VillagerProfession.NONE), "fixture villager is not unemployed");

        var acquire = AcquirePoi.create(unemployed.value().acquirableJobSite(),
                MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty(),
                (level, pos) -> true);
        var assign = AssignProfessionFromJobSite.create();
        var validate = ValidateNearbyPoi.create(holder -> holder.is(FloristRegistry.FLORIST_POI),
                MemoryModuleType.JOB_SITE);
        BlockPos absolutePot = helper.absolutePos(POT);

        // Tick the exact production control through its jittered retry window, stopping once it has claimed a site.
        for (int tick = 2; tick < 80; tick++) {
            helper.runAtTickTime(tick, () -> {
                if (villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty()) {
                    tick(acquire, helper, villager);
                }
            });
        }
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(helper.getLevel().getPoiManager().getType(absolutePot)
                            .map(type -> type.is(FloristRegistry.FLORIST_POI)).orElse(false),
                    "PoiManager did not index the FlowerPotBlock workstation as Florist");
            GlobalPos potential = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).orElseThrow(
                    () -> helper.assertionException("AcquirePoi did not establish POTENTIAL_JOB_SITE"));
            helper.assertTrue(potential.pos().equals(absolutePot) && potential.dimension().equals(helper.getLevel().dimension()),
                    "AcquirePoi selected the wrong job site: " + potential);
            helper.assertTrue(helper.getLevel().getPoiManager().findClosest(
                            holder -> holder.is(FloristRegistry.FLORIST_POI), absolutePot, 1,
                            PoiManager.Occupancy.HAS_SPACE).isEmpty(),
                    "Florist POI did not retain its normal single-workstation reservation");
            helper.assertTrue(BuiltInRegistries.VILLAGER_PROFESSION.listElements()
                            .anyMatch(profession -> profession.value().heldJobSite().test(florist)),
                    "the vanilla profession registry cannot resolve Florist from its claimed POI");
            // AcquirePoi already proved a normal reachable path. Complete that path deterministically before
            // invoking vanilla's proximity-gated profession assignment control.
            villager.setPos(Vec3.atCenterOf(absolutePot));
            tick(assign, helper, villager);
            helper.assertTrue(villager.getVillagerData().profession().is(FloristRegistry.FLORIST),
                    "vanilla profession assignment did not select mynx Florist");
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                            .map(site -> site.pos().equals(absolutePot)).orElse(false),
                    "JOB_SITE was not established by the production profession-assignment behavior");
        });
        helper.runAtTickTime(90, () -> helper.destroyBlock(POT));
        helper.runAtTickTime(93, () -> {
            helper.assertTrue(helper.getLevel().getPoiManager().getType(absolutePot).isEmpty(),
                    "breaking the workstation did not invalidate the Florist POI");
            tick(validate, helper, villager);
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                    "broken Florist workstation retained JOB_SITE memory");
            helper.succeed();
        });
    }

    private static <E extends net.minecraft.world.entity.LivingEntity> void tick(BehaviorControl<E> behavior,
            GameTestHelper helper, E entity) {
        behavior.tryStart(helper.getLevel(), entity, helper.getLevel().getGameTime());
        behavior.tickOrStop(helper.getLevel(), entity, helper.getLevel().getGameTime());
    }

}
