package dev.resivore.slabdecorations.gametest;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;

public final class FoliageSurfaceGameTests implements CustomTestMethodInvoker {
    private static final double EPSILON = 1.0E-7D;
    private static final List<Block> ORDINARY_FOLIAGE_PARENTS = List.of(
            Blocks.GRASS_BLOCK,
            Blocks.PODZOL,
            Blocks.MYCELIUM,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.MUD,
            Blocks.MUDDY_MANGROVE_ROOTS,
            Blocks.MOSS_BLOCK,
            Blocks.PALE_MOSS_BLOCK
    );

    @GameTest(maxTicks = 40)
    public void canonicalMaterialInventoryAndSubstrateAdapterRemainExact(GameTestHelper helper) {
        long vegetationProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.nativeSlab().isPresent())
                .filter(profile -> profile.canonicalParent().defaultBlockState().is(BlockTags.SUPPORTS_VEGETATION))
                .count();
        long dryProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.nativeSlab().isPresent())
                .filter(profile -> profile.canonicalParent().defaultBlockState()
                        .is(BlockTags.SUPPORTS_DRY_VEGETATION))
                .count();
        long wartProfiles = NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.nativeSlab().isPresent())
                .filter(profile -> profile.canonicalParent().defaultBlockState()
                        .is(BlockTags.SUPPORTS_NETHER_WART))
                .count();

        helper.assertTrue(vegetationProfiles == 10,
                "accepted Nibaru inventory no longer has exactly ten native vegetation slab families");
        helper.assertTrue(dryProfiles == 29,
                "accepted Nibaru inventory no longer has exactly twenty-nine native dry-vegetation slab families");
        helper.assertTrue(wartProfiles == 1,
                "accepted Nibaru inventory no longer has exactly one native nether-wart slab family");

        for (Block parent : ORDINARY_FOLIAGE_PARENTS) {
            NibaruMaterialProfile profile = profile(parent);
            Block slab = profile.nativeSlab().orElseThrow();
            helper.assertTrue(profile.canonicalParent() == parent,
                    "canonical parent identity changed for " + parent);
            helper.assertTrue(NibaruMaterialProfiles.fromBlock(slab).orElseThrow() == profile,
                    "native slab did not resolve to its exact canonical profile for " + parent);
            helper.assertTrue(PlantFamilyEligibility.acceptsCanonicalParent(
                            Blocks.DANDELION, profile.canonicalParent().defaultBlockState()),
                    "ordinary foliage adapter rejected canonical parent " + parent);
        }

        helper.assertTrue(PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.DEAD_BUSH, Blocks.SAND.defaultBlockState())
                        && PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.DEAD_BUSH, Blocks.RED_SAND.defaultBlockState())
                        && PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.DEAD_BUSH, Blocks.TERRACOTTA.defaultBlockState()),
                "dead-bush adapter stopped following the exact dry-vegetation tag");
        helper.assertFalse(PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.DANDELION, Blocks.SAND.defaultBlockState()),
                "ordinary flowers acquired sand support from the dead-bush family");
        helper.assertTrue(PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.NETHER_WART, Blocks.SOUL_SAND.defaultBlockState()),
                "nether wart adapter rejected soul sand");
        helper.assertFalse(PlantFamilyEligibility.acceptsCanonicalParent(
                        Blocks.NETHER_WART, Blocks.SOUL_SOIL.defaultBlockState()),
                "nether wart adapter broadened from soul sand to soul soil");
        for (Block invalid : List.of(Blocks.STONE, Blocks.GLASS)) {
            helper.assertFalse(PlantFamilyEligibility.acceptsCanonicalParent(
                            Blocks.DANDELION, invalid.defaultBlockState()),
                    "ordinary flower accepted inappropriate canonical material " + invalid);
            helper.assertFalse(PlantFamilyEligibility.acceptsCanonicalParent(
                            Blocks.DEAD_BUSH, invalid.defaultBlockState()),
                    "dead bush accepted inappropriate canonical material " + invalid);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void actualPlacementSurvivalUsesCanonicalParentSemantics(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = support.above();
        var level = helper.getLevel();

        for (Block parent : ORDINARY_FOLIAGE_PARENTS) {
            BlockState state = slab(parent, SlabType.BOTTOM);
            BlockState flower = Blocks.DANDELION.defaultBlockState();
            level.setBlock(support, state, 2);
            level.setBlock(plant, flower, 2);
            helper.assertTrue(flower.canSurvive(level, plant),
                    "dandelion failed actual survival on canonical native slab " + parent);
            NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                    .supporting(flower, level, plant).orElseThrow();
            helper.assertTrue(surface.profile() == profile(parent)
                            && surface.supportState().getBlock() == profile(parent).nativeSlab().orElseThrow(),
                    "surface resolution stopped using exact profile/native-slab identity for " + parent);
        }

        assertSurvival(helper, support, plant, Blocks.DEAD_BUSH, Blocks.SAND, true);
        assertSurvival(helper, support, plant, Blocks.DANDELION, Blocks.SAND, false);
        assertSurvival(helper, support, plant, Blocks.NETHER_WART, Blocks.SOUL_SAND, true);
        assertSurvival(helper, support, plant, Blocks.NETHER_WART, Blocks.SOUL_SOIL, false);
        assertSurvival(helper, support, plant, Blocks.DANDELION, Blocks.CALCITE, false);

        BlockState flower = Blocks.DANDELION.defaultBlockState();
        level.setBlock(support, Blocks.STONE_SLAB.defaultBlockState(), 2);
        level.setBlock(plant, flower, 2);
        helper.assertFalse(flower.canSurvive(level, plant),
                "ordinary flower acquired support from a vanilla stone slab");
        helper.assertTrue(NibaruHorizontalSurface.candidate(flower, level, plant).isEmpty(),
                "vanilla stone slab was mistaken for a canonical Nibaru native slab");

        for (SlabType type : List.of(SlabType.BOTTOM, SlabType.TOP)) {
            BlockState waterloggedGrass = slab(Blocks.GRASS_BLOCK, type)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            level.setBlock(support, waterloggedGrass, 2);
            level.setBlock(plant, flower, 2);
            helper.assertFalse(flower.canSurvive(level, plant),
                    "Canary 1 admitted terrestrial foliage over waterlogged " + type + " slab support");
            helper.assertTrue(NibaruHorizontalSurface.supporting(flower, level, plant).isEmpty(),
                    "waterlogged " + type + " slab entered the horizontal support path");
        }
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

    @GameTest(maxTicks = 40)
    public void cnmVerticalSlabAndStepNeverEnterHorizontalSurfacePath(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = support.above();
        var level = helper.getLevel();

        for (DerivedGeometrySupport.Geometry geometry : List.of(
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                DerivedGeometrySupport.Geometry.STEP)) {
            Block derived = DerivedMaterialTraits.equivalent(Blocks.GRASS_BLOCK, geometry).orElseThrow();
            DerivedMaterialTraits.Entry trait = DerivedMaterialTraits.fromBlock(derived).orElseThrow();
            helper.assertTrue(trait.canonicalParent() == Blocks.GRASS_BLOCK && trait.geometry() == geometry,
                    "fixture did not obtain the canonical CNM grass geometry " + geometry);
            level.setBlock(support, derived.defaultBlockState(), 2);
            level.setBlock(plant, Blocks.DANDELION.defaultBlockState(), 2);
            helper.assertTrue(NibaruHorizontalSurface.supporting(
                            level.getBlockState(plant), level, plant).isEmpty(),
                    "CNM " + geometry + " entered the native horizontal-slab resolver");
            helper.assertTrue(NibaruHorizontalSurface.visibleOffset(
                            level.getBlockState(plant), level, plant) == 0.0D,
                    "CNM " + geometry + " received a horizontal foliage offset");
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 2);
            helper.assertFalse(BoneMealItem.growCrop(boneMeal, level, support),
                    "CNM " + geometry + " entered substrate bonemeal activation");
            helper.assertTrue(boneMeal.getCount() == 2,
                    "CNM " + geometry + " consumed bonemeal despite exclusion");
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
    public void invalidatingSupportTriggersVanillaNeighbourBreakage(GameTestHelper helper) {
        BlockPos relativeSupport = new BlockPos(1, 1, 1);
        BlockPos support = helper.absolutePos(relativeSupport);
        BlockPos plant = support.above();
        var level = helper.getLevel();

        level.setBlockAndUpdate(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM));
        level.setBlockAndUpdate(plant, Blocks.DANDELION.defaultBlockState());
        helper.assertTrue(level.getBlockState(plant).is(Blocks.DANDELION)
                        && level.getBlockState(plant).canSurvive(level, plant),
                "valid flower fixture did not establish on a bottom grass slab");

        level.setBlockAndUpdate(support, slab(Blocks.CALCITE, SlabType.BOTTOM));
        helper.succeedWhen(() -> helper.assertTrue(level.getBlockState(plant).isAir(),
                "flower did not break through vanilla neighbour updates after support became invalid"));
    }

    @GameTest(maxTicks = 40)
    public void shiftedRayReturnsTheVisiblePlantsLogicalBlock(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos plant = support.above();
        var level = helper.getLevel();
        level.setBlockAndUpdate(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM));
        level.setBlockAndUpdate(plant, Blocks.DANDELION.defaultBlockState());

        Vec3 from = new Vec3(support.getX() + 0.05D, support.getY() + 0.75D, support.getZ() + 0.5D);
        Vec3 to = new Vec3(support.getX() + 0.95D, support.getY() + 0.75D, support.getZ() + 0.5D);
        BlockHitResult vanillaMiss = BlockHitResult.miss(to, Direction.EAST, BlockPos.containing(to));
        HitResult preferred = SlabPlantRaycast.preferShiftedPlant(level, from, to, vanillaMiss);

        helper.assertTrue(preferred instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK
                        && hit.getBlockPos().equals(plant),
                "shifted ray did not select the logical plant block at its visible half-height position");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everyEligibleStateKeepsShiftedOutlineInsideItsLogicalColumn(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos lower = support.above();
        BlockPos upper = support.above(2);
        var level = helper.getLevel();
        int checkedStates = 0;

        for (Block plant : BuiltInRegistries.BLOCK) {
            if (!PlantFamilyEligibility.isEligible(plant)) continue;
            NibaruMaterialProfile supportProfile = stateSweepSupport(plant);

            for (BlockState variant : plant.getStateDefinition().getPossibleStates()) {
                // Clear the old double plant top-first. Replacing one paired block directly with a
                // different DoublePlantBlock lets the removed old half clean up the newly placed half.
                level.setBlock(upper, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(lower, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(support, supportProfile.nativeSlab().orElseThrow().defaultBlockState()
                        .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), 2);
                if (PlantFamilyEligibility.isDoubleGrassCompanion(plant)) {
                    DoublePlantBlock.placeAt(level, plant.defaultBlockState(), lower, 2);
                } else if (PlantFamilyEligibility.isPaleMossCarpetCompanion(plant)
                        && !variant.getValue(MossyCarpetBlock.BASE)) {
                    level.setBlock(lower, plant.defaultBlockState(), 2);
                    level.setBlock(upper, variant, 2);
                } else {
                    level.setBlock(lower, variant, 2);
                    level.setBlock(upper, Blocks.AIR.defaultBlockState(), 2);
                }

                BlockPos checkedPos = PlantFamilyEligibility.isDoubleGrassCompanion(plant)
                        && variant.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                        || PlantFamilyEligibility.isPaleMossCarpetCompanion(plant)
                        && !variant.getValue(MossyCarpetBlock.BASE)
                        ? upper : lower;
                BlockState checked = level.getBlockState(checkedPos);
                boolean survives = checked.canSurvive(level, checkedPos);
                boolean candidate = NibaruHorizontalSurface.candidate(checked, level, checkedPos).isPresent();
                boolean supporting = NibaruHorizontalSurface.supporting(checked, level, checkedPos).isPresent();
                double visibleOffset = NibaruHorizontalSurface.visibleOffset(checked, level, checkedPos);
                helper.assertTrue(survives && candidate && supporting
                                && visibleOffset == NibaruHorizontalSurface.BOTTOM_OFFSET,
                        "eligible state did not establish the expected bottom-slab offset: " + checked
                                + "; survives=" + survives
                                + ", candidate=" + candidate
                                + ", supporting=" + supporting
                                + ", visibleOffset=" + visibleOffset
                                + ", lower=" + level.getBlockState(lower)
                                + ", upper=" + level.getBlockState(upper)
                                + ", support=" + level.getBlockState(support));
                AABB bounds = checked.getShape(level, checkedPos).bounds();
                helper.assertTrue(bounds.minX >= -EPSILON && bounds.maxX <= 1.0D + EPSILON
                                && bounds.minZ >= -EPSILON && bounds.maxZ <= 1.0D + EPSILON,
                        "shifted outline escaped the logical X/Z column required by the ray DDA: " + checked);
                helper.assertTrue(bounds.minY >= NibaruHorizontalSurface.BOTTOM_OFFSET - EPSILON,
                        "eligible outline was translated downward more than once: " + checked);
                checkedStates++;
            }
        }

        helper.assertTrue(checkedStates >= 27,
                "eligible-state outline sweep did not cover the complete Canary 1 plant inventory");
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

    private static void assertSurvival(
            GameTestHelper helper,
            BlockPos support,
            BlockPos plantPos,
            Block plant,
            Block parent,
            boolean expected) {
        var level = helper.getLevel();
        BlockState plantState = plant.defaultBlockState();
        level.setBlock(support, slab(parent, SlabType.BOTTOM), 2);
        level.setBlock(plantPos, plantState, 2);
        helper.assertTrue(plantState.canSurvive(level, plantPos) == expected,
                plant + " survival over canonical " + parent + " did not match vanilla source semantics");
        helper.assertTrue(NibaruHorizontalSurface.supporting(plantState, level, plantPos).isPresent() == expected,
                plant + " surface eligibility over canonical " + parent + " diverged from substrate semantics");
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
                    if (!PlantFamilyEligibility.isEligible(state.getBlock())) continue;
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

    private static NibaruMaterialProfile stateSweepSupport(Block plant) {
        Block canonicalParent = plant == Blocks.NETHER_WART ? Blocks.SOUL_SAND
                : plant == Blocks.DEAD_BUSH ? Blocks.SAND
                : plant == Blocks.PINK_PETALS || plant == Blocks.WILDFLOWERS ? Blocks.GRASS_BLOCK
                : plant == Blocks.AZALEA || plant == Blocks.FLOWERING_AZALEA
                        || plant == Blocks.MOSS_CARPET ? Blocks.MOSS_BLOCK
                : plant == Blocks.PALE_MOSS_CARPET ? Blocks.PALE_MOSS_BLOCK
                : Blocks.GRASS_BLOCK;
        NibaruMaterialProfile profile = profile(canonicalParent);
        if (!PlantFamilyEligibility.acceptsCanonicalParent(
                plant, profile.canonicalParent().defaultBlockState())) {
            throw new AssertionError("explicit state-sweep support does not admit " + plant);
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
