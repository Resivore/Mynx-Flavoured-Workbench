package dev.resivore.slabdecorations.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import dev.resivore.slabdecorations.PlantFamilyEligibility;
import dev.resivore.slabdecorations.SlabPlantRaycast;
import dev.resivore.slabdecorations.SubstrateBonemeal;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class FoliageSurfaceGameTests implements CustomTestMethodInvoker {
    private static final double EPSILON = 1.0E-7D;

    @GameTest(maxTicks = 40)
    public void structuralFamiliesAreDiscoveredWithoutARegistryPermissionList(GameTestHelper helper) {
        assertFamily(helper, Blocks.DANDELION.defaultBlockState(),
                PlantFamilyEligibility.Family.UPWARD_VEGETATION);
        assertFamily(helper, Blocks.RED_MUSHROOM.defaultBlockState(),
                PlantFamilyEligibility.Family.UPWARD_VEGETATION);
        assertFamily(helper, Blocks.SWEET_BERRY_BUSH.defaultBlockState(),
                PlantFamilyEligibility.Family.UPWARD_VEGETATION);
        assertFamily(helper, Blocks.ROSE_BUSH.defaultBlockState(),
                PlantFamilyEligibility.Family.DOUBLE_HEIGHT_VEGETATION);
        assertFamily(helper, Blocks.SMALL_DRIPLEAF.defaultBlockState(),
                PlantFamilyEligibility.Family.DOUBLE_HEIGHT_VEGETATION);
        assertFamily(helper, Blocks.BIG_DRIPLEAF.defaultBlockState(),
                PlantFamilyEligibility.Family.DRIPLEAF_COLUMN);
        assertFamily(helper, Blocks.BIG_DRIPLEAF_STEM.defaultBlockState(),
                PlantFamilyEligibility.Family.DRIPLEAF_COLUMN);
        assertFamily(helper, Blocks.MOSS_CARPET.defaultBlockState(),
                PlantFamilyEligibility.Family.SURFACE_FOLIAGE);
        assertFamily(helper, Blocks.PALE_MOSS_CARPET.defaultBlockState(),
                PlantFamilyEligibility.Family.SURFACE_FOLIAGE);

        for (Block excluded : List.of(
                Blocks.WHEAT,
                Blocks.PITCHER_CROP,
                Blocks.OAK_SAPLING,
                Blocks.CRIMSON_FUNGUS,
                Blocks.SEAGRASS,
                Blocks.LILY_PAD,
                Blocks.CARPET.white(),
                Blocks.RAIL,
                Blocks.REDSTONE_WIRE,
                Blocks.TORCH)) {
            helper.assertTrue(PlantFamilyEligibility.family(excluded.defaultBlockState()).isEmpty(),
                    "non-foliage or unsupported lifecycle family entered projection: " + excluded);
        }

        EnumSet<PlantFamilyEligibility.Family> discovered = EnumSet.noneOf(
                PlantFamilyEligibility.Family.class);
        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                PlantFamilyEligibility.family(state).ifPresent(discovered::add);
            }
        }
        helper.assertTrue(discovered.equals(EnumSet.allOf(PlantFamilyEligibility.Family.class)),
                "registry-driven structural discovery did not exercise every root/segment family: "
                        + discovered);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void redAndBrownMushroomsMatchPodzolAndRejectGlass(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = support.above();
        for (Block mushroom : List.of(Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM)) {
            assertCanonicalParity(helper, support, plant,
                    mushroom.defaultBlockState(), Blocks.PODZOL, true);
            assertCanonicalParity(helper, support, plant,
                    mushroom.defaultBlockState(), Blocks.GLASS, false);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void smallAndBigDripleafMatchMossAndRejectGlassForEverySegment(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();

        clearColumn(level, support, 5);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        List<BlockPos> smallSegments = placeDouble(level, support.above(),
                Blocks.SMALL_DRIPLEAF.defaultBlockState());
        assertSegmentSurvival(helper, smallSegments, true,
                "small dripleaf over canonical moss");
        level.setBlock(support, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        assertSegmentSurvival(helper, smallSegments, true,
                "small dripleaf over native moss bottom slab");
        level.setBlock(support, slab(Blocks.GLASS, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertSegmentSurvival(helper, smallSegments, false,
                "small dripleaf over canonical-rejecting glass slab");

        clearColumn(level, support, 5);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        List<BlockPos> bigSegments = placeBigDripleaf(level, support.above());
        assertSegmentSurvival(helper, bigSegments, true,
                "big dripleaf column over canonical moss");
        level.setBlock(support, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        assertSegmentSurvival(helper, bigSegments, true,
                "big dripleaf column over native moss bottom slab");
        for (BlockPos segment : bigSegments) {
            helper.assertTrue(NibaruHorizontalSurface.visibleOffset(
                            level.getBlockState(segment), level, segment)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET,
                    "big dripleaf segment did not resolve through its physical root: " + segment);
        }
        level.setBlock(support, slab(Blocks.GLASS, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertSegmentSurvival(helper, bigSegments, false,
                "big dripleaf column over canonical-rejecting glass slab");

        clearColumn(level, support, 5);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        List<BlockPos> leafSegments = placeBigDripleafLeafStack(level, support.above());
        assertSegmentSurvival(helper, leafSegments, true,
                "stacked big-dripleaf leaves over canonical moss");
        level.setBlock(support, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        assertSegmentSurvival(helper, leafSegments, true,
                "stacked big-dripleaf leaves over native moss bottom slab");
        for (BlockPos segment : leafSegments) {
            helper.assertTrue(NibaruHorizontalSurface.visibleOffset(
                            level.getBlockState(segment), level, segment)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET,
                    "stacked big-dripleaf leaf did not resolve the leaf-on-leaf root");
        }
        level.setBlock(support, slab(Blocks.GLASS, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertSegmentSurvival(helper, leafSegments, false,
                "stacked big-dripleaf leaves over canonical-rejecting glass slab");

        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void retainedFoliageFamiliesFollowTheirOwnCanonicalSurvival(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos plant = support.above();

        for (PlantSupportCase testCase : List.of(
                new PlantSupportCase(Blocks.DANDELION.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.POPPY.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.SHORT_GRASS.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.FERN.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.DEAD_BUSH.defaultBlockState(), Blocks.SAND, true),
                new PlantSupportCase(Blocks.DEAD_BUSH.defaultBlockState(), Blocks.GLASS, false),
                new PlantSupportCase(Blocks.NETHER_WART.defaultBlockState(), Blocks.SOUL_SAND, true),
                new PlantSupportCase(Blocks.NETHER_WART.defaultBlockState(), Blocks.SOUL_SOIL, false),
                new PlantSupportCase(Blocks.PINK_PETALS.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.WILDFLOWERS.defaultBlockState(), Blocks.GRASS_BLOCK, true),
                new PlantSupportCase(Blocks.AZALEA.defaultBlockState(), Blocks.MOSS_BLOCK, true),
                new PlantSupportCase(Blocks.FLOWERING_AZALEA.defaultBlockState(), Blocks.MOSS_BLOCK, true),
                new PlantSupportCase(Blocks.MOSS_CARPET.defaultBlockState(), Blocks.MOSS_BLOCK, true),
                new PlantSupportCase(Blocks.PALE_MOSS_CARPET.defaultBlockState(), Blocks.PALE_MOSS_BLOCK, true))) {
            assertCanonicalParity(helper, support, plant,
                    testCase.plant(), testCase.parent(), testCase.expected());
        }

        var level = helper.getLevel();
        clearColumn(level, support, 4);
        level.setBlock(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM), 2);
        List<BlockPos> roseBush = placeDouble(level, plant, Blocks.ROSE_BUSH.defaultBlockState());
        assertSegmentSurvival(helper, roseBush, true, "rose-bush halves over grass slab");
        for (BlockPos segment : roseBush) {
            helper.assertTrue(NibaruHorizontalSurface.visibleOffset(
                            level.getBlockState(segment), level, segment)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET,
                    "double-height flower segment did not share the root surface");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void sweetBerryAgeStatesRetainCanonicalParityAndGrowthLifecycle(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos bush = support.above();
        var level = helper.getLevel();

        for (int age = 0; age <= SweetBerryBushBlock.MAX_AGE; age++) {
            BlockState state = Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                    .setValue(SweetBerryBushBlock.AGE, age);
            assertCanonicalParity(helper, support, bush, state, Blocks.GRASS_BLOCK, true);
            BlockState retained = level.getBlockState(bush);
            helper.assertTrue(retained.equals(state)
                            && retained.getValue(SweetBerryBushBlock.AGE) == age
                            && retained.isRandomlyTicking() == (age < SweetBerryBushBlock.MAX_AGE)
                            && codecRoundTrip(retained).equals(retained),
                    "sweet-berry age state lost identity, ticking, or serialization at age " + age);
        }

        BlockState immature = Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                .setValue(SweetBerryBushBlock.AGE, 0);
        assertCanonicalParity(helper, support, bush, immature, Blocks.GLASS, false);

        level.setBlock(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM), 2);
        level.setBlock(bush, immature, 2);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertTrue(BoneMealItem.growCrop(boneMeal, level, bush),
                "sweet-berry bush lost ordinary bonemeal activation on projected grass");
        BlockState grown = level.getBlockState(bush);
        helper.assertTrue(boneMeal.getCount() == 1
                        && grown.is(Blocks.SWEET_BERRY_BUSH)
                        && grown.getValue(SweetBerryBushBlock.AGE) > 0
                        && grown.canSurvive(level, bush)
                        && level.getBlockState(support).equals(
                                slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM)),
                "sweet-berry growth changed support/identity or failed to advance its age");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bottomTopAndDoubleShareTheVisibleOutlineOffsetContract(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos plant = support.above();
        var level = helper.getLevel();
        BlockState flower = Blocks.DANDELION.defaultBlockState();

        level.setBlock(support, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(plant, flower, 3);
        AABB control = flower.getShape(level, plant).bounds();

        AABB bottom = shapeOver(level, support, plant, flower, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM));
        assertBoundsShifted(helper, bottom, control, NibaruHorizontalSurface.BOTTOM_OFFSET,
                "bottom-slab outline");
        NibaruHorizontalSurface.Surface bottomSurface = NibaruHorizontalSurface
                .supporting(flower, level, plant).orElseThrow();
        helper.assertTrue(bottomSurface.height() == 0.5D
                        && bottomSurface.offset() == NibaruHorizontalSurface.BOTTOM_OFFSET
                        && NibaruHorizontalSurface.visibleOffset(flower, level, plant)
                        == NibaruHorizontalSurface.BOTTOM_OFFSET,
                "bottom surface height, render offset, and outline offset diverged");

        AABB top = shapeOver(level, support, plant, flower, slab(Blocks.GRASS_BLOCK, SlabType.TOP));
        assertBoundsShifted(helper, top, control, 0.0D, "top-slab outline");
        NibaruHorizontalSurface.Surface topSurface = NibaruHorizontalSurface
                .supporting(flower, level, plant).orElseThrow();
        helper.assertTrue(topSurface.height() == 1.0D && topSurface.offset() == 0.0D
                        && NibaruHorizontalSurface.visibleOffset(flower, level, plant) == 0.0D,
                "top slab received an erroneous lowered-surface offset");

        AABB doubled = shapeOver(level, support, plant, flower, slab(Blocks.GRASS_BLOCK, SlabType.DOUBLE));
        assertBoundsShifted(helper, doubled, control, 0.0D, "double-slab outline");
        NibaruHorizontalSurface.Surface doubleSurface = NibaruHorizontalSurface
                .supporting(flower, level, plant).orElseThrow();
        helper.assertTrue(doubleSurface.height() == 1.0D && doubleSurface.offset() == 0.0D
                        && NibaruHorizontalSurface.visibleOffset(flower, level, plant) == 0.0D,
                "double slab stopped following full-block-equivalent surface behavior");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void everyRootedSegmentMovesOutlineAndBothCollisionOverloadsExactlyOnce(
            GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        Entity geometryProbe = helper.spawn(EntityTypes.PIG, new BlockPos(0, 1, 0));

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.GRASS_BLOCK,
                placeDouble(level, support.above(), Blocks.ROSE_BUSH.defaultBlockState()),
                geometryProbe, "double-height flower");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.MOSS_BLOCK,
                placeDouble(level, support.above(), Blocks.SMALL_DRIPLEAF.defaultBlockState()),
                geometryProbe, "small dripleaf");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.MOSS_BLOCK,
                placeBigDripleaf(level, support.above()), geometryProbe, "big dripleaf");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.MOSS_BLOCK,
                placeBigDripleafLeafStack(level, support.above()),
                geometryProbe, "stacked big-dripleaf leaves");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.PALE_MOSS_BLOCK.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.PALE_MOSS_BLOCK,
                placePaleMossCarpet(level, support.above()), geometryProbe,
                "pale-moss carpet column");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        level.setBlock(support.above(), Blocks.AZALEA.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.MOSS_BLOCK,
                List.of(support.above()), geometryProbe, "azalea default collision");

        clearColumn(level, support, 6);
        level.setBlock(support, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        level.setBlock(support.above(), Blocks.WITHER_ROSE.defaultBlockState(), 2);
        assertProjectedGeometry(helper, support, Blocks.GRASS_BLOCK,
                List.of(support.above()), geometryProbe, "wither-rose entity-inside volume");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void waterloggedExcludedGeometryAndFullBlocksRemainOutsideProjection(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = support.above();
        var level = helper.getLevel();
        BlockState flower = Blocks.DANDELION.defaultBlockState();

        level.setBlock(support, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        level.setBlock(plant, flower, 2);
        boolean fullSurvival = flower.canSurvive(level, plant);
        VoxelShape fullOutline = flower.getShape(level, plant);
        helper.assertTrue(fullSurvival
                        && CanonicalSurvivalProjection.evaluate(flower, level, plant).isEmpty()
                        && NibaruHorizontalSurface.candidate(flower, level, plant).isEmpty()
                        && NibaruHorizontalSurface.visibleOffset(flower, level, plant) == 0.0D
                        && sameShape(fullOutline, flower.getShape(level, plant)),
                "ordinary full-block behavior was intercepted or geometrically changed");

        BlockState mushroom = Blocks.RED_MUSHROOM.defaultBlockState();
        level.setBlock(support, Blocks.GLASS.defaultBlockState(), 2);
        level.setBlock(plant, mushroom, 2);
        helper.assertTrue(!mushroom.canSurvive(level, plant)
                        && CanonicalSurvivalProjection.evaluate(mushroom, level, plant).isEmpty()
                        && NibaruHorizontalSurface.candidate(mushroom, level, plant).isEmpty()
                        && NibaruHorizontalSurface.visibleOffset(mushroom, level, plant) == 0.0D,
                "canonical rejection on an ordinary full block was broadened by projection");

        BlockState waterlogged = slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(support, waterlogged, 2);
        level.setBlock(plant, flower, 2);
        helper.assertFalse(flower.canSurvive(level, plant),
                "terrestrial foliage survived over a waterlogged bottom slab");
        helper.assertTrue(NibaruHorizontalSurface.candidate(flower, level, plant).isPresent()
                        && NibaruHorizontalSurface.supporting(flower, level, plant).isEmpty()
                        && NibaruHorizontalSurface.visibleOffset(flower, level, plant) == 0.0D,
                "waterlogged candidate entered usable or shifted surface geometry");

        List<BlockState> excludedGeometry = new ArrayList<>();
        for (DerivedGeometrySupport.Geometry geometry : DerivedGeometrySupport.Geometry.values()) {
            Optional<Block> equivalent = DerivedMaterialTraits.equivalent(Blocks.GRASS_BLOCK, geometry);
            if (geometry == DerivedGeometrySupport.Geometry.VERTICAL_SLAB
                    || geometry == DerivedGeometrySupport.Geometry.STEP) {
                helper.assertTrue(equivalent.isPresent(),
                        "fixture is missing required canonical grass geometry " + geometry);
            }
            if (equivalent.isEmpty()) continue;
            Block derived = equivalent.orElseThrow();
            DerivedMaterialTraits.Entry trait = DerivedMaterialTraits.fromBlock(derived).orElseThrow();
            helper.assertTrue(trait.canonicalParent() == Blocks.GRASS_BLOCK && trait.geometry() == geometry,
                    "fixture did not obtain the canonical CNM grass geometry " + geometry);
            excludedGeometry.add(derived.defaultBlockState());
        }
        NibaruMaterialProfile grass = profile(Blocks.GRASS_BLOCK);
        excludedGeometry.add(grass.nativeStair().orElseThrow().defaultBlockState());
        excludedGeometry.add(grass.nativeWall().orElseThrow().defaultBlockState());
        excludedGeometry.add(Blocks.STONE_SLAB.defaultBlockState());

        for (BlockState geometry : excludedGeometry) {
            level.setBlock(support, geometry, 2);
            level.setBlock(plant, flower, 2);
            helper.assertTrue(CanonicalSurvivalProjection.evaluate(flower, level, plant).isEmpty()
                            && NibaruHorizontalSurface.candidate(flower, level, plant).isEmpty()
                            && NibaruHorizontalSurface.supporting(flower, level, plant).isEmpty()
                            && NibaruHorizontalSurface.visibleOffset(flower, level, plant) == 0.0D,
                    "excluded support geometry entered canonical horizontal projection: " + geometry);
            helper.assertTrue(SubstrateBonemeal.target(geometry).isEmpty(),
                    "excluded support geometry entered this project's substrate adapter: "
                            + geometry);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80, padding = 5)
    public void threeBottomSubstratesActivateConsumeAndKeepNativeOwnership(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();

        for (Block parent : List.of(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK)) {
            clearPatch(level, support, 4);
            BlockState expected = slab(parent, SlabType.BOTTOM);
            level.setBlock(support, expected, 2);
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 2);

            helper.assertTrue(BoneMealItem.growCrop(boneMeal, level, support),
                    parent + " bottom slab did not activate through canonical bonemeal");
            helper.assertTrue(boneMeal.getCount() == 1,
                    parent + " bottom slab did not consume exactly one bonemeal");
            BlockState retained = level.getBlockState(support);
            helper.assertTrue(retained.getBlock() == expected.getBlock()
                            && retained.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM,
                    parent + " activation did not restore exact native bottom-slab ownership");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 120, padding = 5)
    public void grassSlabMatchesCanonicalVegetationAndFlowerSideEffects(GameTestHelper helper) {
        PatchComparison comparison = compareCanonicalPatch(
                helper, Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, 0xC201A55L);
        helper.assertTrue(comparison.full().vegetationCount() > 0,
                "canonical grass fixture did not generate vegetation");
        helper.assertTrue(comparison.full().flowerLikeCount() > 0,
                "canonical grass fixture did not exercise its biome flower feature");
        helper.assertTrue(comparison.slabOffsetCount() > 0,
                "grass-slab vegetation did not enter the half-height surface system");
        helper.succeed();
    }

    @GameTest(maxTicks = 120, padding = 5)
    public void mossSlabMatchesCanonicalSpreadAndVegetationSideEffects(GameTestHelper helper) {
        PatchComparison comparison = compareCanonicalPatch(
                helper, Blocks.MOSS_BLOCK, Blocks.DIRT, 0xC202A55L);
        helper.assertTrue(comparison.full().canonicalGroundCount(Blocks.MOSS_BLOCK) > 1,
                "canonical moss fixture did not spread beyond its source");
        helper.assertTrue(comparison.full().vegetationCount() > 0,
                "canonical moss fixture did not generate representative vegetation");
        helper.assertTrue(comparison.slabOffsetCount() > 0,
                "moss-slab vegetation did not enter the half-height surface system");
        helper.succeed();
    }

    @GameTest(maxTicks = 120, padding = 5)
    public void paleMossSlabMatchesCanonicalSpreadAndGrowthSideEffects(GameTestHelper helper) {
        PatchComparison comparison = compareCanonicalPatch(
                helper, Blocks.PALE_MOSS_BLOCK, Blocks.DIRT, 0xC203A55L);
        helper.assertTrue(comparison.full().canonicalGroundCount(Blocks.PALE_MOSS_BLOCK) > 1,
                "canonical pale-moss fixture did not spread beyond its source");
        helper.assertTrue(comparison.full().vegetationCount() > 0,
                "canonical pale-moss fixture did not generate representative growth");
        helper.assertTrue(comparison.slabOffsetCount() > 0,
                "pale-moss slab growth did not enter the half-height surface system");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void unsupportedMaterialsAndNibaruNonSlabGeometryDoNotActivate(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        NibaruMaterialProfile grass = profile(Blocks.GRASS_BLOCK);
        List<BlockState> unsupportedSlabs = List.of(
                slab(Blocks.CALCITE, SlabType.BOTTOM),
                slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM)
                        .setValue(BlockStateProperties.WATERLOGGED, true)
        );

        for (BlockState state : unsupportedSlabs) {
            clearPatch(level, support, 2);
            level.setBlock(support, state, 2);
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 2);
            helper.assertTrue(SubstrateBonemeal.target(state).isEmpty(),
                    "unsupported state entered the Canary 2 substrate adapter: " + state);
            helper.assertFalse(BoneMealItem.growCrop(boneMeal, level, support),
                    "unsupported state activated substrate bonemeal: " + state);
            helper.assertTrue(boneMeal.getCount() == 2,
                    "unsupported state consumed bonemeal: " + state);
            helper.assertTrue(level.getBlockState(support).equals(state),
                    "unsupported state changed during rejected bonemeal: " + state);
        }
        for (BlockState geometry : List.of(
                grass.nativeStair().orElseThrow().defaultBlockState(),
                grass.nativeWall().orElseThrow().defaultBlockState())) {
            helper.assertTrue(SubstrateBonemeal.target(geometry).isEmpty(),
                    "Nibaru non-slab geometry entered the Canary 2 adapter: " + geometry);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void exactGeneratedOutputsUseTheirCanonicalHalfHeightSurfaces(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();

        assertGeneratedOutputSurface(helper, support, Blocks.GRASS_BLOCK, Blocks.PINK_PETALS.defaultBlockState());
        assertGeneratedOutputSurface(helper, support, Blocks.GRASS_BLOCK, Blocks.WILDFLOWERS.defaultBlockState());
        assertGeneratedOutputSurface(helper, support, Blocks.MOSS_BLOCK, Blocks.AZALEA.defaultBlockState());
        assertGeneratedOutputSurface(helper, support, Blocks.MOSS_BLOCK, Blocks.FLOWERING_AZALEA.defaultBlockState());
        assertGeneratedOutputSurface(helper, support, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET.defaultBlockState());

        clearPatch(level, support, 2);
        BlockPos azaleaPos = support.above();
        level.setBlock(support, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        level.setBlock(azaleaPos, Blocks.AZALEA.defaultBlockState(), 2);
        ItemStack azaleaBoneMeal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertFalse(BoneMealItem.growCrop(azaleaBoneMeal, level, azaleaPos),
                "lowered moss-patch azalea entered the deliberately deferred tree-growth path");
        helper.assertTrue(azaleaBoneMeal.getCount() == 2
                        && level.getBlockState(azaleaPos).is(Blocks.AZALEA),
                "rejected lowered-azalea growth consumed bonemeal or changed the decoration");

        clearPatch(level, support, 2);
        BlockPos lower = support.above();
        BlockPos upper = support.above(2);
        level.setBlock(support, slab(Blocks.PALE_MOSS_BLOCK, SlabType.BOTTOM), 2);
        level.setBlock(lower, Blocks.PALE_MOSS_CARPET.defaultBlockState(), 2);
        BlockState topper = Blocks.PALE_MOSS_CARPET.defaultBlockState()
                .setValue(MossyCarpetBlock.BASE, false)
                .setValue(MossyCarpetBlock.NORTH, WallSide.LOW);
        level.setBlock(upper, topper, 2);

        for (BlockPos pos : List.of(lower, upper)) {
            BlockState carpet = level.getBlockState(pos);
            helper.assertTrue(carpet.canSurvive(level, pos)
                            && NibaruHorizontalSurface.supporting(carpet, level, pos).isPresent()
                            && NibaruHorizontalSurface.visibleOffset(carpet, level, pos)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET,
                    "pale-moss carpet base/topper lost the shared half-height surface: " + carpet);
        }
        AABB lowerCollision = level.getBlockState(lower).getCollisionShape(level, lower).bounds();
        helper.assertTrue(lowerCollision.minY < 0.0D && close(lowerCollision.minY, -0.5D),
                "pale-moss carpet collision did not move with its visible base");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void supportRemovalCleansSingleDoubleDripleafAndCarpetRoots(GameTestHelper helper) {
        var level = helper.getLevel();
        List<RootedFixture> fixtures = new ArrayList<>();

        BlockPos flowerSupport = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(flowerSupport, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM), 2);
        level.setBlock(flowerSupport.above(), Blocks.DANDELION.defaultBlockState(), 2);
        fixtures.add(new RootedFixture(flowerSupport, List.of(flowerSupport.above())));

        BlockPos mushroomSupport = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlock(mushroomSupport, slab(Blocks.PODZOL, SlabType.BOTTOM), 2);
        level.setBlock(mushroomSupport.above(), Blocks.BROWN_MUSHROOM.defaultBlockState(), 2);
        fixtures.add(new RootedFixture(mushroomSupport, List.of(mushroomSupport.above())));

        BlockPos doubleSupport = helper.absolutePos(new BlockPos(1, 1, 4));
        level.setBlock(doubleSupport, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM), 2);
        fixtures.add(new RootedFixture(doubleSupport,
                placeDouble(level, doubleSupport.above(), Blocks.ROSE_BUSH.defaultBlockState())));

        BlockPos smallSupport = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(smallSupport, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        fixtures.add(new RootedFixture(smallSupport,
                placeDouble(level, smallSupport.above(), Blocks.SMALL_DRIPLEAF.defaultBlockState())));

        BlockPos bigSupport = helper.absolutePos(new BlockPos(7, 1, 1));
        level.setBlock(bigSupport, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        fixtures.add(new RootedFixture(bigSupport, placeBigDripleaf(level, bigSupport.above())));

        BlockPos carpetSupport = helper.absolutePos(new BlockPos(7, 1, 4));
        level.setBlock(carpetSupport, slab(Blocks.PALE_MOSS_BLOCK, SlabType.BOTTOM), 2);
        fixtures.add(new RootedFixture(carpetSupport,
                placePaleMossCarpet(level, carpetSupport.above())));

        for (RootedFixture fixture : fixtures) {
            assertSegmentSurvival(helper, fixture.segments(), true,
                    "pre-removal rooted fixture at " + fixture.support());
        }
        for (RootedFixture fixture : fixtures) {
            level.setBlockAndUpdate(fixture.support(), Blocks.AIR.defaultBlockState());
        }

        helper.succeedWhen(() -> {
            for (RootedFixture fixture : fixtures) {
                for (BlockPos segment : fixture.segments()) {
                    helper.assertTrue(level.getBlockState(segment).isAir(),
                            "support removal did not clean rooted segment at " + segment
                                    + ": " + level.getBlockState(segment));
                }
            }
        });
    }

    @GameTest(maxTicks = 40)
    public void shiftedRayReturnsTheVisiblePlantsLogicalBlock(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos plant = support.above();
        var level = helper.getLevel();
        level.setBlockAndUpdate(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM));
        level.setBlockAndUpdate(plant, Blocks.DANDELION.defaultBlockState());

        BlockState plantState = level.getBlockState(plant);
        AABB visibleShape = plantState.getShape(level, plant).bounds();
        double visibleY = plant.getY() + (visibleShape.minY + visibleShape.maxY) / 2.0D;
        double visibleZ = plant.getZ() + (visibleShape.minZ + visibleShape.maxZ) / 2.0D;
        Vec3 from = new Vec3(support.getX() + 0.05D, visibleY, visibleZ);
        Vec3 to = new Vec3(support.getX() + 0.95D, visibleY, visibleZ);
        BlockHitResult vanillaMiss = BlockHitResult.miss(to, Direction.EAST, BlockPos.containing(to));
        HitResult preferred = SlabPlantRaycast.preferShiftedPlant(level, from, to, vanillaMiss);

        helper.assertTrue(preferred instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK
                        && hit.getBlockPos().equals(plant),
                "shifted ray did not select the logical plant block at its visible half-height position"
                        + "; resultType=" + preferred.getType()
                        + "; resultLocation=" + preferred.getLocation()
                        + "; resultBlock=" + (preferred instanceof BlockHitResult hit ? hit.getBlockPos() : "n/a")
                        + "; plantState=" + plantState
                        + "; canSurvive=" + plantState.canSurvive(level, plant)
                        + "; visibleOffset=" + NibaruHorizontalSurface.visibleOffset(
                                plantState, level, plant)
                        + "; shape=" + visibleShape);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void repeatedProjectionIsGuardedAndNeverMutatesTheWorld(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos plant = support.above();
        var level = helper.getLevel();
        BlockState validSupport = slab(Blocks.PODZOL, SlabType.BOTTOM);
        BlockState invalidSupport = slab(Blocks.GLASS, SlabType.BOTTOM);
        BlockState mushroom = Blocks.RED_MUSHROOM.defaultBlockState();

        level.setBlock(support, validSupport, 2);
        level.setBlock(plant, mushroom, 2);
        for (int attempt = 0; attempt < 32; attempt++) {
            Optional<Boolean> result = CanonicalSurvivalProjection.evaluate(mushroom, level, plant);
            helper.assertTrue(result.equals(Optional.of(true)),
                    "valid repeated projection did not return canonical podzol survival");
            helper.assertFalse(CanonicalSurvivalProjection.isEvaluating(),
                    "projection recursion guard leaked after valid evaluation");
            helper.assertTrue(level.getBlockState(support).equals(validSupport)
                            && level.getBlockState(plant).equals(mushroom),
                    "valid projection mutated support or plant state");
        }

        // Preserve this deliberately invalid pair long enough to prove that evaluating it is
        // read-only; ordinary neighbour processing is covered independently by cleanup tests.
        level.setBlock(support, invalidSupport, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        Optional<Boolean> invalid = CanonicalSurvivalProjection.evaluate(mushroom, level, plant);
        helper.assertTrue(invalid.equals(Optional.of(false)),
                "canonical-rejecting projection did not return false");
        helper.assertFalse(CanonicalSurvivalProjection.isEvaluating(),
                "projection recursion guard leaked after rejected evaluation");
        helper.assertTrue(level.getBlockState(support).equals(invalidSupport)
                        && level.getBlockState(plant).equals(mushroom),
                "rejected projection mutated support or plant state");

        level.setBlock(support, validSupport, 2);
        helper.assertTrue(CanonicalSurvivalProjection.evaluate(mushroom, level, plant)
                        .equals(Optional.of(true)),
                "valid projection failed after a preceding rejection");
        helper.assertTrue(CanonicalSurvivalProjection.evaluate(
                        Blocks.WHEAT.defaultBlockState(), level, plant).isEmpty(),
                "excluded crop unexpectedly opened a projection");
        helper.assertFalse(CanonicalSurvivalProjection.isEvaluating(),
                "projection recursion guard leaked after a non-candidate evaluation");
        helper.assertTrue(level.getBlockState(support).equals(validSupport)
                        && level.getBlockState(plant).equals(mushroom),
                "sequential projection calls left any world mutation behind");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void blockStateCodecRoundTripPreservesRootedIdentityAndAlignment(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        level.setBlock(support, slab(Blocks.MOSS_BLOCK, SlabType.BOTTOM), 2);
        List<BlockPos> segments = placeBigDripleaf(level, support.above());
        List<BlockPos> persisted = new ArrayList<>();
        persisted.add(support);
        persisted.addAll(segments);

        List<BlockState> encodedStates = new ArrayList<>();
        List<VoxelShape> shiftedOutlines = new ArrayList<>();
        for (BlockPos pos : persisted) {
            BlockState original = level.getBlockState(pos);
            BlockState decoded = codecRoundTrip(original);
            helper.assertTrue(decoded.equals(original),
                    "block-state codec changed identity/properties for " + original);
            encodedStates.add(decoded);
            if (!pos.equals(support)) shiftedOutlines.add(original.getShape(level, pos));
        }

        for (int index = persisted.size() - 1; index >= 0; index--) {
            level.setBlock(persisted.get(index), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int index = 0; index < persisted.size(); index++) {
            level.setBlock(persisted.get(index), encodedStates.get(index), 2);
        }

        helper.assertTrue(level.getBlockState(support).equals(encodedStates.getFirst()),
                "serialized native slab did not restore exactly");
        for (int index = 0; index < segments.size(); index++) {
            BlockPos segment = segments.get(index);
            BlockState restored = level.getBlockState(segment);
            helper.assertTrue(restored.equals(encodedStates.get(index + 1))
                            && restored.canSurvive(level, segment)
                            && NibaruHorizontalSurface.visibleOffset(restored, level, segment)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET
                            && sameShape(shiftedOutlines.get(index), restored.getShape(level, segment)),
                    "serialized/reloaded dripleaf segment lost identity, survival, or alignment at "
                            + segment);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void grassAndFernBonemealKeepBothDoublePlantHalvesCoherent(GameTestHelper helper) {
        BlockPos firstSupport = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos secondSupport = helper.absolutePos(new BlockPos(4, 1, 1));
        var level = helper.getLevel();

        assertBonemealDoubleTransition(helper, firstSupport, Blocks.SHORT_GRASS, Blocks.TALL_GRASS);
        assertBonemealDoubleTransition(helper, secondSupport, Blocks.FERN, Blocks.LARGE_FERN);

        level.setBlockAndUpdate(firstSupport, slab(Blocks.CALCITE, SlabType.BOTTOM));
        level.setBlockAndUpdate(secondSupport, slab(Blocks.CALCITE, SlabType.BOTTOM));
        helper.succeedWhen(() -> {
            for (BlockPos support : List.of(firstSupport, secondSupport)) {
                helper.assertTrue(level.getBlockState(support.above()).isAir()
                                && level.getBlockState(support.above(2)).isAir(),
                        "double-height grass companion did not remove both halves after support invalidation");
            }
        });
    }

    @GameTest(maxTicks = 80)
    public void netherWartUsesVanillaAgeRandomTicksAndRejectsBonemeal(GameTestHelper helper) {
        BlockPos relativeSupport = new BlockPos(2, 1, 2);
        BlockPos support = helper.absolutePos(relativeSupport);
        BlockPos wart = support.above();
        var level = helper.getLevel();
        BlockState ageZero = Blocks.NETHER_WART.defaultBlockState()
                .setValue(NetherWartBlock.AGE, 0);
        level.setBlockAndUpdate(support, slab(Blocks.SOUL_SAND, SlabType.BOTTOM));
        level.setBlockAndUpdate(wart, ageZero);

        helper.assertTrue(level.getBlockState(wart).canSurvive(level, wart),
                "nether wart did not survive on canonical soul-sand slab support");
        helper.assertTrue(level.getBlockState(wart).isRandomlyTicking(),
                "age-zero nether wart lost vanilla random ticking");
        helper.assertFalse(Blocks.NETHER_WART instanceof BonemealableBlock,
                "nether wart unexpectedly acquired bonemeal behavior");

        RandomSource random = RandomSource.create(0x5A17B00BL);
        for (int attempt = 0; attempt < 1024
                && level.getBlockState(wart).getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
             attempt++) {
            level.getBlockState(wart).randomTick(level, wart, random);
        }
        BlockState mature = level.getBlockState(wart);
        helper.assertTrue(mature.is(Blocks.NETHER_WART)
                        && mature.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE
                        && !mature.isRandomlyTicking(),
                "nether wart did not preserve vanilla age progression and maximum state");

        for (int attempt = 0; attempt < 32; attempt++) {
            level.getBlockState(wart).randomTick(level, wart, random);
        }
        helper.assertTrue(level.getBlockState(wart).getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE,
                "mature nether wart progressed beyond the vanilla maximum age");

        level.destroyBlock(wart, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockState(wart).isAir(),
                    "mature nether wart was not replaceable through normal destruction");
            helper.assertTrue(!helper.getEntities(EntityTypes.ITEM).isEmpty(),
                    "mature nether wart did not produce its vanilla item drop");
            helper.assertItemEntityPresent(Items.NETHER_WART, relativeSupport.above(), 2.0D);
        });
    }

    private static void assertFamily(
            GameTestHelper helper,
            BlockState state,
            PlantFamilyEligibility.Family expected) {
        helper.assertTrue(PlantFamilyEligibility.family(state).orElse(null) == expected,
                "structural family mismatch for " + state + ": expected " + expected
                        + ", got " + PlantFamilyEligibility.family(state));
    }

    private static void assertCanonicalParity(
            GameTestHelper helper,
            BlockPos support,
            BlockPos plantPos,
            BlockState plantState,
            Block parent,
            boolean expected) {
        var level = helper.getLevel();
        clearColumn(level, support, 5);
        level.setBlock(support, parent.defaultBlockState(), 2);
        level.setBlock(plantPos, plantState, 2);
        boolean canonical = plantState.canSurvive(level, plantPos);
        helper.assertTrue(canonical == expected,
                "canonical fixture expectation drifted for " + plantState + " on " + parent
                        + ": got " + canonical);

        BlockState exactSlab = slab(parent, SlabType.BOTTOM);
        level.setBlock(support, exactSlab, 2);
        level.setBlock(plantPos, plantState, 2);
        boolean projected = plantState.canSurvive(level, plantPos);
        Optional<Boolean> directProjection = CanonicalSurvivalProjection.evaluate(
                plantState, level, plantPos);
        helper.assertTrue(projected == canonical
                        && directProjection.equals(Optional.of(canonical)),
                plantState + " slab survival did not delegate to canonical " + parent
                        + ": full=" + canonical + ", slab=" + projected
                        + ", direct=" + directProjection);
        helper.assertTrue(NibaruHorizontalSurface.candidate(plantState, level, plantPos).isPresent()
                        && NibaruHorizontalSurface.supporting(plantState, level, plantPos).isPresent() == canonical
                        && NibaruHorizontalSurface.visibleOffset(plantState, level, plantPos)
                        == (canonical ? NibaruHorizontalSurface.BOTTOM_OFFSET : 0.0D),
                plantState + " usable surface did not follow projected canonical result");
        helper.assertTrue(level.getBlockState(support).equals(exactSlab)
                        && level.getBlockState(plantPos).equals(plantState)
                        && !CanonicalSurvivalProjection.isEvaluating(),
                "canonical parity evaluation mutated state or leaked its recursion guard");
    }

    private static List<BlockPos> placeDouble(
            net.minecraft.server.level.ServerLevel level,
            BlockPos lower,
            BlockState state) {
        DoublePlantBlock.placeAt(level, state, lower, 2);
        return List.of(lower, lower.above());
    }

    private static List<BlockPos> placeBigDripleaf(
            net.minecraft.server.level.ServerLevel level,
            BlockPos root) {
        level.setBlock(root, Blocks.BIG_DRIPLEAF_STEM.defaultBlockState(), 2);
        level.setBlock(root.above(), Blocks.BIG_DRIPLEAF_STEM.defaultBlockState(), 2);
        level.setBlock(root.above(2), Blocks.BIG_DRIPLEAF.defaultBlockState(), 2);
        return List.of(root, root.above(), root.above(2));
    }

    private static List<BlockPos> placeBigDripleafLeafStack(
            net.minecraft.server.level.ServerLevel level,
            BlockPos root) {
        level.setBlock(root, Blocks.BIG_DRIPLEAF.defaultBlockState(), 2);
        level.setBlock(root.above(), Blocks.BIG_DRIPLEAF.defaultBlockState(), 2);
        return List.of(root, root.above());
    }

    private static List<BlockPos> placePaleMossCarpet(
            net.minecraft.server.level.ServerLevel level,
            BlockPos root) {
        BlockState base = Blocks.PALE_MOSS_CARPET.defaultBlockState()
                .setValue(MossyCarpetBlock.BASE, true);
        BlockState topper = Blocks.PALE_MOSS_CARPET.defaultBlockState()
                .setValue(MossyCarpetBlock.BASE, false)
                .setValue(MossyCarpetBlock.NORTH, WallSide.LOW);
        level.setBlock(root, base, 2);
        level.setBlock(root.above(), topper, 2);
        return List.of(root, root.above());
    }

    private static void assertSegmentSurvival(
            GameTestHelper helper,
            List<BlockPos> segments,
            boolean expected,
            String label) {
        var level = helper.getLevel();
        for (BlockPos segment : segments) {
            BlockState state = level.getBlockState(segment);
            helper.assertTrue(state.canSurvive(level, segment) == expected,
                    label + " had mismatched survival at " + segment + ": " + state);
        }
    }

    private static void assertProjectedGeometry(
            GameTestHelper helper,
            BlockPos support,
            Block canonicalParent,
            List<BlockPos> segments,
            Entity geometryProbe,
            String label) {
        var level = helper.getLevel();
        List<SegmentGeometry> control = new ArrayList<>();
        for (BlockPos segment : segments) {
            BlockState state = level.getBlockState(segment);
            helper.assertTrue(state.canSurvive(level, segment),
                    label + " canonical control cannot survive at " + segment + ": " + state);
            control.add(new SegmentGeometry(
                    state,
                    state.getShape(level, segment),
                    state.getCollisionShape(level, segment),
                    state.getCollisionShape(level, segment, CollisionContext.empty()),
                    state.getVisualShape(level, segment, CollisionContext.empty()),
                    state.getInteractionShape(level, segment),
                    state.getEntityInsideCollisionShape(level, segment, geometryProbe)));
        }

        level.setBlock(support, slab(canonicalParent, SlabType.BOTTOM), 2);
        for (int index = 0; index < segments.size(); index++) {
            BlockPos segment = segments.get(index);
            SegmentGeometry expected = control.get(index);
            BlockState state = level.getBlockState(segment);
            helper.assertTrue(state.equals(expected.state())
                            && state.canSurvive(level, segment)
                            && NibaruHorizontalSurface.visibleOffset(state, level, segment)
                            == NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " segment lost identity/survival/root offset at " + segment);
            assertShapeShifted(helper, state.getShape(level, segment), expected.outline(),
                    NibaruHorizontalSurface.BOTTOM_OFFSET, label + " outline at " + segment);
            assertShapeShifted(helper, state.getCollisionShape(level, segment), expected.cachedCollision(),
                    NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " cached collision at " + segment);
            assertShapeShifted(helper,
                    state.getCollisionShape(level, segment, CollisionContext.empty()),
                    expected.contextCollision(), NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " contextual collision at " + segment);
            assertShapeShifted(helper,
                    state.getVisualShape(level, segment, CollisionContext.empty()),
                    expected.visualShape(), NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " visual shape at " + segment);
            assertShapeShifted(helper, state.getInteractionShape(level, segment),
                    expected.interactionShape(), NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " interaction shape at " + segment);
            assertShapeShifted(helper,
                    state.getEntityInsideCollisionShape(level, segment, geometryProbe),
                    expected.entityInsideShape(), NibaruHorizontalSurface.BOTTOM_OFFSET,
                    label + " entity-inside collision at " + segment);
        }
    }

    private static void assertShapeShifted(
            GameTestHelper helper,
            VoxelShape actual,
            VoxelShape control,
            double yOffset,
            String label) {
        List<AABB> actualBoxes = actual.toAabbs();
        List<AABB> controlBoxes = control.toAabbs();
        helper.assertTrue(actualBoxes.size() == controlBoxes.size(),
                label + " changed shape box count");
        for (int index = 0; index < controlBoxes.size(); index++) {
            AABB expected = controlBoxes.get(index);
            AABB shifted = actualBoxes.get(index);
            helper.assertTrue(close(shifted.minX, expected.minX)
                            && close(shifted.maxX, expected.maxX)
                            && close(shifted.minY, expected.minY + yOffset)
                            && close(shifted.maxY, expected.maxY + yOffset)
                            && close(shifted.minZ, expected.minZ)
                            && close(shifted.maxZ, expected.maxZ),
                    label + " was not translated exactly once; expected=" + expected
                            + ", actual=" + shifted);
        }
    }

    private static boolean sameShape(VoxelShape first, VoxelShape second) {
        List<AABB> firstBoxes = first.toAabbs();
        List<AABB> secondBoxes = second.toAabbs();
        if (firstBoxes.size() != secondBoxes.size()) return false;
        for (int index = 0; index < firstBoxes.size(); index++) {
            AABB firstBox = firstBoxes.get(index);
            AABB secondBox = secondBoxes.get(index);
            if (!close(firstBox.minX, secondBox.minX)
                    || !close(firstBox.maxX, secondBox.maxX)
                    || !close(firstBox.minY, secondBox.minY)
                    || !close(firstBox.maxY, secondBox.maxY)
                    || !close(firstBox.minZ, secondBox.minZ)
                    || !close(firstBox.maxZ, secondBox.maxZ)) {
                return false;
            }
        }
        return true;
    }

    private static BlockState codecRoundTrip(BlockState state) {
        JsonElement encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow(IllegalStateException::new);
        return BlockState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(IllegalStateException::new);
    }

    private static void clearColumn(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support,
            int height) {
        for (int dy = height; dy >= 0; dy--) {
            level.setBlock(support.above(dy), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static AABB shapeOver(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support,
            BlockPos plant,
            BlockState plantState,
            BlockState supportState) {
        level.setBlock(support, supportState, 2);
        level.setBlock(plant, plantState, 2);
        if (!plantState.canSurvive(level, plant)) {
            throw new AssertionError("shape fixture plant cannot survive over " + supportState);
        }
        return plantState.getShape(level, plant).bounds();
    }

    private static void assertBoundsShifted(
            GameTestHelper helper,
            AABB actual,
            AABB control,
            double yOffset,
            String label) {
        helper.assertTrue(close(actual.minX, control.minX)
                        && close(actual.maxX, control.maxX)
                        && close(actual.minZ, control.minZ)
                        && close(actual.maxZ, control.maxZ)
                        && close(actual.minY, control.minY + yOffset)
                        && close(actual.maxY, control.maxY + yOffset),
                label + " did not match the exact shared visible/selection translation");
    }

    private static void assertBonemealDoubleTransition(
            GameTestHelper helper,
            BlockPos support,
            Block source,
            Block expectedDouble) {
        var level = helper.getLevel();
        BlockPos lower = support.above();
        BlockPos upper = support.above(2);
        level.setBlockAndUpdate(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM));
        level.setBlockAndUpdate(lower, source.defaultBlockState());
        BlockState sourceState = level.getBlockState(lower);
        BonemealableBlock bonemealable = (BonemealableBlock) source;

        helper.assertTrue(sourceState.canSurvive(level, lower)
                        && bonemealable.isValidBonemealTarget(level, lower, sourceState)
                        && bonemealable.isBonemealSuccess(level, RandomSource.create(17L), lower, sourceState),
                source + " lost vanilla bonemeal eligibility on a bottom grass slab");
        bonemealable.performBonemeal(level, RandomSource.create(17L), lower, sourceState);

        BlockState lowerState = level.getBlockState(lower);
        BlockState upperState = level.getBlockState(upper);
        helper.assertTrue(lowerState.is(expectedDouble)
                        && upperState.is(expectedDouble)
                        && lowerState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                        && upperState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                source + " bonemeal did not create the expected coherent vanilla double plant");
        helper.assertTrue(lowerState.canSurvive(level, lower) && upperState.canSurvive(level, upper),
                expectedDouble + " halves did not preserve vanilla survival behavior");
        helper.assertTrue(NibaruHorizontalSurface.visibleOffset(lowerState, level, lower)
                        == NibaruHorizontalSurface.BOTTOM_OFFSET
                        && NibaruHorizontalSurface.visibleOffset(upperState, level, upper)
                        == NibaruHorizontalSurface.BOTTOM_OFFSET,
                expectedDouble + " halves did not share the same visible/outline offset");
    }

    private static PatchComparison compareCanonicalPatch(
            GameTestHelper helper,
            Block source,
            Block surrounding,
            long seed) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();

        preparePatch(level, support, source, surrounding, false);
        level.getRandom().setSeed(seed);
        ItemStack fullBoneMeal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertTrue(BoneMealItem.growCrop(fullBoneMeal, level, support)
                        && fullBoneMeal.getCount() == 1,
                "canonical " + source + " fixture did not activate and consume exactly one item");
        PatchSnapshot full = snapshot(level, support);

        preparePatch(level, support, source, surrounding, true);
        level.getRandom().setSeed(seed);
        ItemStack slabBoneMeal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertTrue(BoneMealItem.growCrop(slabBoneMeal, level, support)
                        && slabBoneMeal.getCount() == 1,
                "native slab for " + source + " did not activate and consume exactly one item");
        PatchSnapshot slab = snapshot(level, support);

        helper.assertTrue(full.states().size() == slab.states().size(),
                "canonical/slab snapshots had different dimensions");
        for (int index = 0; index < full.states().size(); index++) {
            helper.assertTrue(full.states().get(index).equals(slab.states().get(index)),
                    "canonical/slab side effects diverged for " + source + " at snapshot index "
                            + index + ": full=" + full.states().get(index)
                            + ", slab=" + slab.states().get(index));
        }

        BlockState retained = level.getBlockState(support);
        helper.assertTrue(retained.getBlock() == profile(source).nativeSlab().orElseThrow()
                        && retained.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM,
                source + " source did not remain its exact native bottom slab");

        int offsetCount = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos pos = support.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!PlantFamilyEligibility.isEligible(state)) continue;
                    if (NibaruHorizontalSurface.supporting(state, level, pos).isEmpty()) continue;
                    helper.assertTrue(NibaruHorizontalSurface.visibleOffset(state, level, pos)
                                    == NibaruHorizontalSurface.BOTTOM_OFFSET,
                            "generated slab decoration did not receive the shared offset: " + state);
                    offsetCount++;
                }
            }
        }
        return new PatchComparison(full, slab, offsetCount);
    }

    private static void preparePatch(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support,
            Block source,
            Block surrounding,
            boolean slabs) {
        clearPatch(level, support, 4);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block parent = dx == 0 && dz == 0 ? source : surrounding;
                BlockState state = slabs
                        ? slab(parent, SlabType.BOTTOM)
                        : parent.defaultBlockState();
                level.setBlock(support.offset(dx, 0, dz), state, 2);
            }
        }
    }

    private static void clearPatch(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support,
            int radius) {
        for (int dy = 4; dy >= 0; dy--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    level.setBlock(support.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setBlock(support.offset(dx, -1, dz), Blocks.BEDROCK.defaultBlockState(), 2);
            }
        }
    }

    private static PatchSnapshot snapshot(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support) {
        List<BlockState> states = new java.util.ArrayList<>();
        int vegetation = 0;
        int flowerLike = 0;
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockState state = level.getBlockState(support.offset(dx, dy, dz));
                    states.add(normalizeNativeBottomSlab(state));
                    if (dy == 0 || state.isAir()) continue;
                    vegetation++;
                    if (!state.is(Blocks.SHORT_GRASS)
                            && !state.is(Blocks.TALL_GRASS)
                            && !state.is(Blocks.FERN)
                            && !state.is(Blocks.LARGE_FERN)) {
                        flowerLike++;
                    }
                }
            }
        }
        return new PatchSnapshot(List.copyOf(states), vegetation, flowerLike);
    }

    private static BlockState normalizeNativeBottomSlab(BlockState state) {
        if (!state.hasProperty(BlockStateProperties.SLAB_TYPE)
                || state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.BOTTOM) {
            return state;
        }
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(state.getBlock()).orElse(null);
        if (profile == null || profile.nativeSlab().orElse(null) != state.getBlock()) return state;
        return profile.canonicalParent().withPropertiesOf(state);
    }

    private static void assertGeneratedOutputSurface(
            GameTestHelper helper,
            BlockPos support,
            Block canonicalParent,
            BlockState output) {
        var level = helper.getLevel();
        clearPatch(level, support, 2);
        BlockPos plant = support.above();
        level.setBlock(support, slab(canonicalParent, SlabType.BOTTOM), 2);
        level.setBlock(plant, output, 2);
        BlockState actual = level.getBlockState(plant);
        helper.assertTrue(actual.canSurvive(level, plant)
                        && NibaruHorizontalSurface.supporting(actual, level, plant).isPresent()
                        && NibaruHorizontalSurface.visibleOffset(actual, level, plant)
                        == NibaruHorizontalSurface.BOTTOM_OFFSET,
                output.getBlock() + " did not use its exact generated-output substrate mapping");
        AABB shape = actual.getShape(level, plant).bounds();
        helper.assertTrue(shape.minY < 0.0D,
                output.getBlock() + " outline did not move down to the half-height surface");
    }

    private record PatchComparison(PatchSnapshot full, PatchSnapshot slab, int slabOffsetCount) {
    }

    private record PlantSupportCase(BlockState plant, Block parent, boolean expected) {
    }

    private record RootedFixture(BlockPos support, List<BlockPos> segments) {
    }

    private record SegmentGeometry(
            BlockState state,
            VoxelShape outline,
            VoxelShape cachedCollision,
            VoxelShape contextCollision,
            VoxelShape visualShape,
            VoxelShape interactionShape,
            VoxelShape entityInsideShape) {
    }

    private record PatchSnapshot(List<BlockState> states, int vegetationCount, int flowerLikeCount) {
        int canonicalGroundCount(Block parent) {
            int count = 0;
            // Snapshot ordering stores the complete dy=0 plane first (9 x 9 entries).
            for (int index = 0; index < 81; index++) {
                if (states.get(index).is(parent)) count++;
            }
            return count;
        }
    }

    private static BlockState slab(Block canonicalParent, SlabType type) {
        BlockState state = profile(canonicalParent).nativeSlab().orElseThrow().defaultBlockState();
        state = state.setValue(BlockStateProperties.SLAB_TYPE, type);
        if (state.hasProperty(BlockStateProperties.SNOWY)) {
            state = state.setValue(BlockStateProperties.SNOWY, false);
        }
        return state;
    }

    private static NibaruMaterialProfile profile(Block canonicalParent) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(canonicalParent).orElseThrow();
        if (profile.canonicalParent() != canonicalParent) {
            throw new AssertionError("profile did not retain canonical block identity for " + canonicalParent);
        }
        return profile;
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
