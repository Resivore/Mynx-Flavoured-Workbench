package dev.resivore.slabdecorations.gametest;

import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import dev.resivore.slabdecorations.PlantFamilyEligibility;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Systemic canonical-parent/root-resolution regressions for the post-C7 architecture. */
public final class SystemicSurfaceGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 80)
    public void acceptanceAlwaysSuppliesTheDirectionalNonzeroOffset(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));

        setColumn(level, support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM),
                List.of(Blocks.DANDELION.defaultBlockState()));
        assertAcceptedOffset(helper, support.above(), support,
                NibaruHorizontalSurface.BOTTOM_OFFSET, "upward flower");

        clearVertical(level, support, 4, 4);
        setColumn(level, support, slab(Blocks.CALCITE, SlabType.TOP), List.of());
        BlockPos hanging = support.below();
        level.setBlock(hanging, Blocks.HANGING_ROOTS.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertAcceptedOffset(helper, hanging, support,
                NibaruHorizontalSurface.CEILING_TOP_OFFSET, "ceiling roots");

        clearVertical(level, support, 4, 4);
        setColumn(level, support, slab(Blocks.SAND, SlabType.BOTTOM),
                List.of(Blocks.CACTUS.defaultBlockState()));
        assertAcceptedOffset(helper, support.above(), support,
                NibaruHorizontalSurface.BOTTOM_OFFSET, "plain-Block rooted cactus");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void sixSegmentCaveVinesUseOwningLevelBeyondBoundedRenderView(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 8, 3));
        clearVertical(level, support, 8, 1);
        level.setBlock(support, slab(Blocks.CALCITE, SlabType.TOP),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

        List<BlockPos> segments = new ArrayList<>();
        for (int depth = 1; depth <= 6; depth++) {
            BlockPos segment = support.below(depth);
            BlockState state = depth == 6
                    ? Blocks.CAVE_VINES.defaultBlockState()
                    : Blocks.CAVE_VINES_PLANT.defaultBlockState();
            level.setBlock(segment, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            segments.add(segment);
        }

        for (BlockPos segment : segments) {
            assertBoundedOffset(helper, segment, support,
                    NibaruHorizontalSurface.CEILING_TOP_OFFSET,
                    "six-segment cave-vines column");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void longUpwardGrowingSugarCaneAndCactusColumnsShareOneRoot(GameTestHelper helper) {
        var level = helper.getLevel();

        BlockPos twistingSupport = helper.absolutePos(new BlockPos(2, 1, 2));
        clearVertical(level, twistingSupport, 1, 8);
        level.setBlock(twistingSupport, slab(Blocks.NETHERRACK, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (int height = 1; height <= 6; height++) {
            BlockPos segment = twistingSupport.above(height);
            level.setBlock(segment, height == 6
                            ? Blocks.TWISTING_VINES.defaultBlockState()
                            : Blocks.TWISTING_VINES_PLANT.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        assertUpwardColumn(helper, twistingSupport, 6, "twisting vines");

        BlockPos caneSupport = helper.absolutePos(new BlockPos(6, 1, 2));
        clearVertical(level, caneSupport, 1, 8);
        level.setBlock(caneSupport, slab(Blocks.SAND, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(caneSupport.east(), Blocks.WATER.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (int height = 1; height <= 6; height++) {
            level.setBlock(caneSupport.above(height), Blocks.SUGAR_CANE.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        assertUpwardColumn(helper, caneSupport, 6, "sugar cane");

        BlockPos cactusSupport = helper.absolutePos(new BlockPos(10, 1, 2));
        clearVertical(level, cactusSupport, 1, 8);
        level.setBlock(cactusSupport, slab(Blocks.SAND, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (int height = 1; height <= 6; height++) {
            level.setBlock(cactusSupport.above(height), height == 6
                            ? Blocks.CACTUS_FLOWER.defaultBlockState()
                            : Blocks.CACTUS.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        assertUpwardColumn(helper, cactusSupport, 6, "cactus with terminal flower");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void bambooSaplingTransitionAndMixedMatureStatesShareOneRoot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos root = support.above();
        clearVertical(level, support, 1, 9);
        level.setBlock(support, slab(Blocks.DIRT, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bambooItem = new ItemStack(Items.BAMBOO);
        player.setItemInHand(InteractionHand.MAIN_HAND, bambooItem);
        InteractionResult placement = bambooItem.useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                        support, false)));
        helper.assertTrue(placement.consumesAction()
                        && level.getBlockState(root).is(Blocks.BAMBOO_SAPLING),
                "Bamboo item placement did not create its initial sapling state on a canonical-valid slab");
        player.discard();
        assertAcceptedOffset(helper, root, support, NibaruHorizontalSurface.BOTTOM_OFFSET,
                "bamboo sapling");

        BlockState sapling = level.getBlockState(root);
        ((BonemealableBlock) Blocks.BAMBOO_SAPLING).performBonemeal(
                level, RandomSource.create(0xB4AB00L), root, sapling);
        helper.assertTrue(level.getBlockState(root).is(Blocks.BAMBOO),
                "bamboo sapling did not retain its vanilla transition to a stalk");
        assertAcceptedOffset(helper, root, support, NibaruHorizontalSurface.BOTTOM_OFFSET,
                "transitioned bamboo root");

        clearVertical(level, root, 0, 8);
        for (int height = 0; height < 6; height++) {
            BambooLeaves leaves = height < 3 ? BambooLeaves.NONE
                    : height < 5 ? BambooLeaves.SMALL : BambooLeaves.LARGE;
            BlockState stalk = Blocks.BAMBOO.defaultBlockState()
                    .setValue(BambooStalkBlock.AGE, height % 2)
                    .setValue(BambooStalkBlock.STAGE, height == 5 ? 1 : 0)
                    .setValue(BambooStalkBlock.LEAVES, leaves);
            level.setBlock(root.above(height), stalk, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        for (int height = 0; height < 6; height++) {
            assertBoundedOffset(helper, root.above(height), support,
                    NibaruHorizontalSurface.BOTTOM_OFFSET, "mixed-state bamboo column");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void directSurvivalStateToleratesNeighborUpdatesAndCleansAfterSupportRemoval(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 6, 3));
        BlockPos plant = support.below();
        BlockPos neighbor = plant.east();
        clearVertical(level, support, 4, 2);
        level.setBlockAndUpdate(support, slab(Blocks.CALCITE, SlabType.TOP));
        level.setBlockAndUpdate(plant, Blocks.HANGING_ROOTS.defaultBlockState());
        assertAcceptedOffset(helper, plant, support,
                NibaruHorizontalSurface.CEILING_TOP_OFFSET, "neighbor-updated hanging roots");

        level.setBlockAndUpdate(neighbor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(neighbor, Blocks.AIR.defaultBlockState());
        helper.assertTrue(level.getBlockState(plant).is(Blocks.HANGING_ROOTS)
                        && level.getBlockState(plant).canSurvive(level, plant),
                "accepted direct-survival state removed itself during an unrelated neighbor update");
        assertAcceptedOffset(helper, plant, support,
                NibaruHorizontalSurface.CEILING_TOP_OFFSET, "post-neighbor-update hanging roots");

        level.setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
        helper.succeedWhen(() -> helper.assertTrue(level.getBlockState(plant).isAir(),
                "hanging roots survived removal of their resolved slab support"));
    }

    @GameTest(maxTicks = 60)
    public void hypotheticalSubmergedSporeBlossomRetainsRealFluidRejection(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 6, 3));
        BlockPos plant = support.below();
        BlockState hypothetical = Blocks.SPORE_BLOSSOM.defaultBlockState();

        level.setBlock(support, slab(Blocks.CALCITE, SlabType.TOP),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(plant, Blocks.WATER.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

        helper.assertTrue(level.getBlockState(plant).is(Blocks.WATER)
                        && level.getFluidState(plant).is(Fluids.WATER)
                        && level.getFluidState(plant).isSource(),
                "hypothetical placement fixture did not retain its replaced source water");
        helper.assertTrue(PlantFamilyEligibility.isEligible(hypothetical)
                        && NibaruHorizontalSurface.candidate(hypothetical, level, plant).isPresent(),
                "canonical-valid Calcite top slab did not produce a Spore Blossom candidate");
        helper.assertTrue(CanonicalSurvivalProjection.evaluate(hypothetical, level, plant)
                        .equals(java.util.Optional.of(false))
                        && !hypothetical.canSurvive(level, plant)
                        && NibaruHorizontalSurface.supporting(hypothetical, level, plant).isEmpty()
                        && NibaruHorizontalSurface.visibleOffset(hypothetical, level, plant) == 0.0D,
                "candidate-state projection hid the real water replaced by a hypothetical"
                        + " terrestrial Spore Blossom placement");
        helper.assertTrue(level.getBlockState(plant).is(Blocks.WATER),
                "read-only hypothetical projection mutated the real water cell");
        helper.succeed();
    }

    @GameTest(maxTicks = 160)
    public void ordinaryCropItemsPlaceOnCanonicalBgeFarmlandSlabs(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos crop = support.above();
        helper.assertTrue(NibaruMaterialProfiles.fromBlock(CnmTerrainCompat.FARMLAND_SLAB).isEmpty(),
                "BGE Farmland Slab unexpectedly became a material-profile source");

        for (SlabType type : SlabType.values()) {
            for (CropPlacement cropItem : ordinaryFarmlandCropItems()) {
                clearVertical(level, support, 1, 4);
                level.setBlock(support, farmlandSlab(type), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack held = new ItemStack(cropItem.item(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                InteractionResult result = held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                                support, false)));
                BlockState placed = level.getBlockState(crop);
                double expectedOffset = type == SlabType.BOTTOM
                        ? NibaruHorizontalSurface.BOTTOM_OFFSET : 0.0D;
                NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                        .supporting(placed, level, crop).orElse(null);
                helper.assertTrue(result.consumesAction()
                                && placed.is(cropItem.crop())
                                && placed.canSurvive(level, crop)
                                && surface != null
                                && surface.canonicalParentState().is(Blocks.FARMLAND)
                                && surface.offset() == expectedOffset
                                && NibaruHorizontalSurface.visibleOffset(placed, level, crop)
                                == expectedOffset
                                && held.getCount() == 1,
                        cropItem.item() + " did not naturally place its normal " + cropItem.crop()
                                + " state on BGE Farmland Slab " + type + ": " + placed);
                player.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void farmlandCropProjectionUsesActualBgeSurfaceHeightsAndExcludesStems(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos crop = support.above();

        for (BlockState cropState : List.of(
                Blocks.WHEAT.defaultBlockState(),
                Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE))) {
            clearVertical(level, support, 1, 4);
            level.setBlock(support, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            level.setBlock(crop, cropState, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            VoxelShape canonicalOutline = cropState.getShape(level, crop);
            VoxelShape canonicalInteraction = cropState.getInteractionShape(level, crop);

            for (SlabType type : SlabType.values()) {
                level.setBlock(support, farmlandSlab(type), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                level.setBlock(crop, cropState, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                BlockState actual = level.getBlockState(crop);
                double expectedOffset = type == SlabType.BOTTOM
                        ? NibaruHorizontalSurface.BOTTOM_OFFSET : 0.0D;
                NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                        .supporting(actual, level, crop).orElse(null);
                helper.assertTrue(actual.canSurvive(level, crop)
                                && surface != null
                                && surface.canonicalParentState().is(Blocks.FARMLAND)
                                && surface.offset() == expectedOffset,
                        "BGE Farmland Slab " + type + " did not preserve wheat survival/projection");
                assertShapeOffset(helper, actual.getShape(level, crop), canonicalOutline, expectedOffset,
                        "wheat outline " + type);
                assertShapeOffset(helper, actual.getInteractionShape(level, crop), canonicalInteraction,
                        expectedOffset, "wheat interaction shape " + type);
            }
        }

        level.setBlock(support, farmlandSlab(SlabType.BOTTOM), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (Block stem : List.of(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM,
                Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM)) {
            BlockState state = stem.defaultBlockState();
            level.setBlock(crop, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            helper.assertTrue(!PlantFamilyEligibility.isEligible(state)
                            && NibaruHorizontalSurface.candidate(state, level, crop).isEmpty()
                            && NibaruHorizontalSurface.supporting(state, level, crop).isEmpty(),
                    "BGE Farmland Slab re-enabled excluded stem state " + state);
        }
        for (Item seed : List.of(Items.PUMPKIN_SEEDS, Items.MELON_SEEDS)) {
            clearVertical(level, support, 1, 4);
            level.setBlock(support, farmlandSlab(SlabType.BOTTOM), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack held = new ItemStack(seed, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            InteractionResult result = held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                            support, false)));
            helper.assertTrue(!result.consumesAction() && level.getBlockState(crop).isAir()
                            && held.getCount() == 2,
                    seed + " unexpectedly gained BGE Farmland Slab placement support");
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void exactBgeFarmlandSlabsMatchVanillaThreeByThreeCropFertility(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos crop = helper.absolutePos(new BlockPos(7, 2, 7));
        BlockPos center = crop.below();

        for (int moisture : List.of(0, 7)) {
            float expected = moisture == 0 ? 4.0F : 10.0F;
            for (CropPlacement cropCase : ordinaryFarmlandCropItems()) {
                Block cropBlock = cropCase.crop();
                fillCropFertilityNeighborhood(level, center,
                        Blocks.FARMLAND.defaultBlockState()
                                .setValue(BlockStateProperties.MOISTURE, moisture));
                level.setBlock(crop, cropBlock.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                float vanilla = cropGrowthSpeed(cropBlock, level, crop);
                helper.assertTrue(vanilla == expected,
                        "Minecraft 26.2 vanilla 3x3 farmland fertility drifted for " + cropBlock
                                + " at moisture " + moisture + ": " + vanilla);

                for (SlabType type : SlabType.values()) {
                    fillCropFertilityNeighborhood(level, center, farmlandSlab(type)
                            .setValue(BlockStateProperties.MOISTURE, moisture));
                    level.setBlock(crop, cropBlock.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                    float slab = cropGrowthSpeed(cropBlock, level, crop);
                    helper.assertTrue(slab == vanilla,
                            "BGE Farmland Slab " + type + " changed complete 3x3 crop fertility for "
                                    + cropBlock + " at moisture " + moisture + ": slab=" + slab
                                    + ", vanilla=" + vanilla);

                    fillMixedCropFertilityNeighborhood(level, center, type, moisture);
                    level.setBlock(crop, cropBlock.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                    float mixed = cropGrowthSpeed(cropBlock, level, crop);
                    helper.assertTrue(mixed == vanilla,
                            "mixed vanilla/BGE Farmland Slab neighborhood changed crop fertility for "
                                    + cropBlock + " " + type + " at moisture " + moisture
                                    + ": mixed=" + mixed + ", vanilla=" + vanilla);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void wheatKeepsVanillaGrowthBonemealHarvestAndFarmlandLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos crop = support.above();
        clearVertical(level, support, 1, 4);
        level.setBlock(support, farmlandSlab(SlabType.BOTTOM), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

        BlockState planted = level.getBlockState(crop);
        helper.assertTrue(planted.canSurvive(level, crop)
                        && planted.getValue(CropBlock.AGE) == 0
                        && NibaruHorizontalSurface.visibleOffset(planted, level, crop)
                        == NibaruHorizontalSurface.BOTTOM_OFFSET,
                "young wheat did not retain normal state/survival on BGE Farmland Slab");
        helper.randomTick(support);
        helper.assertTrue(level.getBlockState(support).is(CnmTerrainCompat.FARMLAND_SLAB),
                "an ordinary crop stopped BGE's MAINTAINS_FARMLAND lifecycle from retaining farmland");

        RandomSource random = RandomSource.create(0xC0FFEE);
        int ageBeforeGrowth = planted.getValue(CropBlock.AGE);
        for (int attempt = 0; attempt < 256
                && level.getBlockState(crop).getValue(CropBlock.AGE) == ageBeforeGrowth; attempt++) {
            level.getBlockState(crop).randomTick(level, crop, random);
        }
        BlockState grown = level.getBlockState(crop);
        helper.assertTrue(grown.is(Blocks.WHEAT) && grown.getValue(CropBlock.AGE) > ageBeforeGrowth,
                "ordinary wheat random-tick growth no longer advanced on BGE Farmland Slab");
        ((BonemealableBlock) Blocks.WHEAT).performBonemeal(level, RandomSource.create(0xB0BE),
                crop, grown);
        BlockState bonemealed = level.getBlockState(crop);
        helper.assertTrue(bonemealed.is(Blocks.WHEAT)
                        && bonemealed.getValue(CropBlock.AGE) >= grown.getValue(CropBlock.AGE),
                "ordinary wheat bonemeal did not retain vanilla age progression");

        level.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.destroyBlock(crop, true);
        helper.assertItemEntityCountIs(Items.WHEAT, new BlockPos(3, 2, 3), 2.0D, 1);

        level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlockAndUpdate(support, slab(Blocks.DIRT, SlabType.BOTTOM));
        helper.succeedWhen(() -> helper.assertTrue(level.getBlockState(crop).isAir(),
                "wheat did not retain normal cleanup after Farmland Slab became a Dirt slab"));
    }

    @GameTest(maxTicks = 80)
    public void exactOptionalToadstoolStemContinuesOnlySuccessfulBottomTransactions(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos source = support.above();

        for (SlabType type : SlabType.values()) {
            clearVertical(level, support, 1, 4);
            BlockState original = slab(Blocks.MYCELIUM, type);
            level.setBlock(support, original, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            level.setBlock(source, Blocks.RED_MUSHROOM.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            boolean grown = StructureGrowthTransaction.run(level, source,
                    Blocks.RED_MUSHROOM.defaultBlockState(), () -> {
                        helper.assertTrue(level.getBlockState(support).is(Blocks.MYCELIUM),
                                "huge toadstool feature did not receive canonical Mycelium for " + type);
                        level.setBlock(source, SystemicFixtureInitializer.TOADSTOOL_STEM.defaultBlockState(),
                                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        return true;
                    });
            helper.assertTrue(grown && (type == SlabType.BOTTOM
                            ? level.getBlockState(support).is(SystemicFixtureInitializer.TOADSTOOL_STEM)
                            : level.getBlockState(support).equals(original)),
                    "exact Ribbits toadstool stem reconciliation drifted for " + type);
        }

        clearVertical(level, support, 1, 4);
        BlockState original = slab(Blocks.MYCELIUM, SlabType.BOTTOM);
        BlockState sourceState = Blocks.RED_MUSHROOM.defaultBlockState();
        level.setBlock(support, original, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(source, sourceState, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        boolean grown = StructureGrowthTransaction.run(level, source, sourceState, () -> {
            level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            // Mirrors Ribbits' own failed-feature recovery before its normal spread fallback.
            level.setBlock(source, sourceState, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            return false;
        });
        helper.assertTrue(!grown && level.getBlockState(support).equals(original)
                        && level.getBlockState(source).equals(sourceState),
                "failed optional huge-toadstool transaction did not retain Ribbits' recovered source/slab");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void snowLayersKeepNativeStackingAndGeometryOnEveryEligibleSlabType(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos snowPos = support.above();

        for (SlabType type : SlabType.values()) {
            clearVertical(level, support, 1, 3);
            level.setBlock(support, slab(Blocks.STONE, type), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack snow = new ItemStack(Items.SNOW, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, snow);

            InteractionResult first = snow.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                            support, false)));
            InteractionResult second = snow.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(snowPos), net.minecraft.core.Direction.UP,
                            snowPos, false)));
            BlockState stacked = level.getBlockState(snowPos);
            double expectedOffset = type == SlabType.BOTTOM
                    ? NibaruHorizontalSurface.BOTTOM_OFFSET : 0.0D;
            BlockState fullLayers = stacked.setValue(SnowLayerBlock.LAYERS, 8);
            level.setBlock(support, Blocks.STONE.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            VoxelShape twoLayerCanonical = Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, 2).getShape(level, snowPos);
            VoxelShape canonicalCollision = fullLayers.getCollisionShape(level, snowPos);
            level.setBlock(support, slab(Blocks.STONE, type), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

            helper.assertTrue(first.consumesAction() && second.consumesAction()
                            && stacked.is(Blocks.SNOW)
                            && stacked.getValue(SnowLayerBlock.LAYERS) == 2
                            && stacked.canSurvive(level, snowPos)
                            && snow.getCount() == 0
                            && NibaruHorizontalSurface.visibleOffset(stacked, level, snowPos)
                            == expectedOffset,
                    "Snow Layers did not retain native two-layer stacking on BGE " + type);
            assertShapeOffset(helper, stacked.getShape(level, snowPos), twoLayerCanonical, expectedOffset,
                    "snow layer outline " + type);

            level.setBlock(snowPos, fullLayers, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            assertShapeOffset(helper, level.getBlockState(snowPos).getCollisionShape(level, snowPos),
                    canonicalCollision, expectedOffset, "snow layer collision " + type);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void generatedPillarTransactionsConsumeOnlyMatchingDirectionalHalfSlabs(
            GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos floorSupport = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos upwardPrecursor = floorSupport.above();
        BlockPos ceilingSupport = helper.absolutePos(new BlockPos(8, 6, 3));
        BlockPos downwardPrecursor = ceilingSupport.below();
        BlockState exactStem = SystemicFixtureInitializer.ENDERSCAPE_PILLAR.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);

        for (SlabType type : SlabType.values()) {
            clearVertical(level, floorSupport, 2, 3);
            BlockState floorSlab = slab(Blocks.DIRT, type);
            level.setBlock(floorSupport, floorSlab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            level.setBlock(upwardPrecursor, Blocks.OAK_SAPLING.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            boolean upward = StructureGrowthTransaction.runEnderscapeGrowth(
                    level, upwardPrecursor, Blocks.OAK_SAPLING.defaultBlockState(), () -> {
                        level.setBlock(upwardPrecursor, exactStem, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        return true;
                    });
            helper.assertTrue(upward && level.getBlockState(floorSupport)
                            .equals(type == SlabType.BOTTOM ? exactStem : floorSlab),
                    "upward generated pillar consumed a non-BOTTOM or lost its exact axis on " + type);

            clearVertical(level, ceilingSupport, 3, 2);
            BlockState ceilingSlab = slab(Blocks.STONE, type);
            level.setBlock(ceilingSupport, ceilingSlab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            level.setBlock(downwardPrecursor, Blocks.HANGING_ROOTS.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            boolean downward = StructureGrowthTransaction.runEnderscapeGrowth(
                    level, downwardPrecursor, Blocks.HANGING_ROOTS.defaultBlockState(), () -> {
                        level.setBlock(downwardPrecursor, exactStem, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        return true;
                    });
            helper.assertTrue(downward && level.getBlockState(ceilingSupport)
                            .equals(type == SlabType.TOP ? exactStem : ceilingSlab),
                    "downward generated pillar consumed a non-TOP or lost its exact axis on " + type);
        }

        BlockState original = slab(Blocks.DIRT, SlabType.BOTTOM);
        level.setBlock(floorSupport, original, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(upwardPrecursor, Blocks.OAK_SAPLING.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        helper.assertTrue(!StructureGrowthTransaction.runEnderscapeGrowth(
                        level, upwardPrecursor, Blocks.OAK_SAPLING.defaultBlockState(), () -> false)
                        && level.getBlockState(floorSupport).equals(original)
                        && level.getBlockState(upwardPrecursor).is(Blocks.OAK_SAPLING),
                "failed Enderscape-style generation did not restore the exact slab and precursor");

        BlockState originalCeiling = slab(Blocks.STONE, SlabType.TOP);
        level.setBlock(ceilingSupport, originalCeiling, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(downwardPrecursor, Blocks.HANGING_ROOTS.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        helper.assertTrue(!StructureGrowthTransaction.runEnderscapeGrowth(
                        level, downwardPrecursor, Blocks.HANGING_ROOTS.defaultBlockState(), () -> false)
                        && level.getBlockState(ceilingSupport).equals(originalCeiling)
                        && level.getBlockState(downwardPrecursor).is(Blocks.HANGING_ROOTS),
                "failed downward Enderscape-style generation did not restore the exact top slab and precursor");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void fungiPlacementAndTransformationUsesCanonicalParent(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos fungus = support.above();

        for (FungusCase testCase : List.of(
                new FungusCase(Blocks.CRIMSON_FUNGUS, Blocks.CRIMSON_NYLIUM),
                new FungusCase(Blocks.WARPED_FUNGUS, Blocks.WARPED_NYLIUM))) {
            var level = helper.getLevel();
            clearVertical(level, support, 1, 3);
            level.setBlock(support, slab(testCase.parent(), SlabType.BOTTOM),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            level.setBlock(fungus, testCase.fungus().defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            assertAcceptedOffset(helper, fungus, support, NibaruHorizontalSurface.BOTTOM_OFFSET,
                    testCase.fungus() + " small decoration");
            BonemealableBlock bonemealable = (BonemealableBlock) testCase.fungus();
            helper.assertTrue(bonemealable.isValidBonemealTarget(
                            level, fungus, level.getBlockState(fungus)),
                    testCase.fungus() + " did not expose vanilla huge-fungus growth on canonical nylium");

            level.setBlock(support, slab(Blocks.GLASS, SlabType.BOTTOM),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            BlockState state = level.getBlockState(fungus);
            helper.assertTrue(NibaruHorizontalSurface.supporting(state, level, fungus).isEmpty()
                            && NibaruHorizontalSurface.visibleOffset(state, level, fungus) == 0.0D,
                    testCase.fungus() + " ignored canonical invalid support");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 120)
    public void aquaticFloorPlantsUseRealWaterAndWaterloggedCanonicalSlabs(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockState wetStoneSlab = slab(Blocks.STONE, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, true);

        clearVertical(level, support, 1, 9);
        level.setBlock(support, wetStoneSlab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (int height = 1; height <= 6; height++) {
            level.setBlock(support.above(height), height == 6
                            ? Blocks.KELP.defaultBlockState()
                            : Blocks.KELP_PLANT.defaultBlockState(),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        for (int height = 1; height <= 6; height++) {
            BlockPos segment = support.above(height);
            helper.assertTrue(level.getFluidState(segment).is(Fluids.WATER),
                    "kelp fixture lost real water occupancy at " + segment);
            assertBoundedOffset(helper, segment, support,
                    NibaruHorizontalSurface.BOTTOM_OFFSET, "kelp head/body column");
        }

        clearVertical(level, support, 1, 9);
        level.setBlock(support, wetStoneSlab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        BlockPos plant = support.above();
        level.setBlock(plant, Blocks.SEAGRASS.defaultBlockState(),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertWetAccepted(helper, plant, support, "seagrass");

        clearVertical(level, plant, 0, 3);
        BlockState tallLower = Blocks.TALL_SEAGRASS.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState tallUpper = tallLower.setValue(
                BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlock(plant, tallLower, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(plant.above(), tallUpper, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        assertWetAccepted(helper, plant, support, "tall seagrass lower");
        assertWetAccepted(helper, plant.above(), support, "tall seagrass upper");

        for (AquaticCase testCase : List.of(
                new AquaticCase(Blocks.SEA_PICKLE.defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), "sea pickle"),
                new AquaticCase(Blocks.TUBE_CORAL.defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), "floor coral plant"),
                new AquaticCase(Blocks.TUBE_CORAL_FAN.defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true), "floor coral fan"))) {
            clearVertical(level, plant, 0, 3);
            level.setBlock(plant, testCase.state(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            assertWetAccepted(helper, plant, support, testCase.label());
        }

        // Real item placement distinguishes fluid parity from merely forcing an aquatic state.
        clearVertical(level, plant, 0, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drySeagrass = new ItemStack(Items.SEAGRASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, drySeagrass);
        InteractionResult dryResult = drySeagrass.useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                        support, false)));
        helper.assertTrue(!dryResult.consumesAction() && level.getBlockState(plant).isAir(),
                "seagrass item placement ignored a missing water cell");

        level.setBlock(plant, Blocks.WATER.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        ItemStack wetSeagrass = new ItemStack(Items.SEAGRASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, wetSeagrass);
        InteractionResult wetResult = wetSeagrass.useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support), net.minecraft.core.Direction.UP,
                        support, false)));
        helper.assertTrue(wetResult.consumesAction() && level.getBlockState(plant).is(Blocks.SEAGRASS),
                "seagrass item placement rejected the real submerged canonical slab context");
        assertWetAccepted(helper, plant, support, "item-placed seagrass");
        player.discard();

        clearVertical(level, support, 1, 3);
        BlockState wetMagmaSlab = slab(Blocks.MAGMA_BLOCK, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(support, wetMagmaSlab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(plant, Blocks.KELP.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        BlockState rejectedKelp = level.getBlockState(plant);
        helper.assertTrue(NibaruHorizontalSurface.supporting(rejectedKelp, level, plant).isEmpty()
                        && NibaruHorizontalSurface.visibleOffset(rejectedKelp, level, plant) == 0.0D,
                "kelp ignored its canonical magma-substrate rejection");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void lanternSignsAndFloorTorchesShareTheSurfaceResolver(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos floor = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos ceiling = helper.absolutePos(new BlockPos(7, 4, 3));
        level.setBlock(floor, slab(Blocks.STONE, SlabType.BOTTOM), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(ceiling, slab(Blocks.STONE, SlabType.TOP), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

        for (BlockState state : List.of(Blocks.LANTERN.defaultBlockState(),
                Blocks.SOUL_LANTERN.defaultBlockState(), Blocks.TORCH.defaultBlockState(),
                Blocks.SOUL_TORCH.defaultBlockState(), Blocks.OAK_SIGN.defaultBlockState())) {
            BlockPos pos = floor.above();
            level.setBlock(pos, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            assertAcceptedOffset(helper, pos, floor, NibaruHorizontalSurface.BOTTOM_OFFSET,
                    "floor decoration " + state.getBlock());
        }

        for (BlockState state : List.of(
                Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true),
                Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true),
                Blocks.OAK_HANGING_SIGN.defaultBlockState())) {
            BlockPos pos = ceiling.below();
            level.setBlock(pos, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            assertAcceptedOffset(helper, pos, ceiling, NibaruHorizontalSurface.CEILING_TOP_OFFSET,
                    "ceiling decoration " + state.getBlock());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void explicitBlacklistNeverProducesAHorizontalAttachment(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos plant = support.above();
        level.setBlock(support, slab(Blocks.GRASS_BLOCK, SlabType.BOTTOM),
                Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

        List<Block> representatives = List.of(
                Blocks.PUMPKIN_STEM,
                Blocks.ATTACHED_PUMPKIN_STEM,
                Blocks.MELON_STEM,
                Blocks.ATTACHED_MELON_STEM,
                Blocks.CHORUS_FLOWER,
                Blocks.CHORUS_PLANT,
                Blocks.LILY_PAD,
                Blocks.VINE,
                Blocks.GLOW_LICHEN,
                Blocks.COCOA,
                Blocks.TUBE_CORAL_WALL_FAN);
        for (Block block : representatives) {
            BlockState state = block.defaultBlockState();
            level.setBlock(plant, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            helper.assertTrue(!PlantFamilyEligibility.isEligible(state)
                            && NibaruHorizontalSurface.candidate(state, level, plant).isEmpty()
                            && NibaruHorizontalSurface.supporting(state, level, plant).isEmpty()
                            && NibaruHorizontalSurface.visibleOffset(state, level, plant) == 0.0D,
                    "explicitly blacklisted state entered a horizontal surface: " + state);
        }

        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof StemBlock)
                    && !(block instanceof AttachedStemBlock)) {
                continue;
            }
            helper.assertFalse(PlantFamilyEligibility.isEligible(block.defaultBlockState()),
                    "structural tree/stem blacklist missed " + block);
        }
        helper.succeed();
    }

    private static void assertFullParentAndSlabParity(
            GameTestHelper helper,
            BlockPos support,
            BlockPos plant,
            BlockState state,
            BlockState canonicalParent,
            BlockState slab,
            double expectedOffset,
            String label) {
        var level = helper.getLevel();
        clearVertical(level, support, 1, 4);
        level.setBlock(support, canonicalParent, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(plant, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        helper.assertTrue(state.canSurvive(level, plant),
                label + " canonical full-parent fixture is not valid");

        level.setBlock(support, slab, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        level.setBlock(plant, state, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        helper.assertTrue(state.canSurvive(level, plant)
                        && CanonicalSurvivalProjection.evaluate(state, level, plant).equals(java.util.Optional.of(true)),
                label + " did not preserve canonical-parent survival");
        assertAcceptedOffset(helper, plant, support, expectedOffset, label);
    }

    private static List<CropPlacement> ordinaryFarmlandCropItems() {
        // These are the current vanilla planting-item contracts, not a production permission
        // table. Production delegates acceptance to each crop's ordinary projected survival.
        return List.of(
                new CropPlacement(Items.WHEAT_SEEDS, Blocks.WHEAT),
                new CropPlacement(Items.CARROT, Blocks.CARROTS),
                new CropPlacement(Items.POTATO, Blocks.POTATOES),
                new CropPlacement(Items.BEETROOT_SEEDS, Blocks.BEETROOTS),
                new CropPlacement(Items.TORCHFLOWER_SEEDS, Blocks.TORCHFLOWER_CROP),
                new CropPlacement(Items.PITCHER_POD, Blocks.PITCHER_CROP));
    }

    private static BlockState farmlandSlab(SlabType type) {
        return CnmTerrainCompat.FARMLAND_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, type);
    }

    private static void fillCropFertilityNeighborhood(
            net.minecraft.server.level.ServerLevel level,
            BlockPos center,
            BlockState support) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), support, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        }
    }

    private static void fillMixedCropFertilityNeighborhood(
            net.minecraft.server.level.ServerLevel level,
            BlockPos center,
            SlabType type,
            int moisture) {
        BlockState vanilla = Blocks.FARMLAND.defaultBlockState()
                .setValue(BlockStateProperties.MOISTURE, moisture);
        BlockState slab = farmlandSlab(type).setValue(BlockStateProperties.MOISTURE, moisture);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), (x + z & 1) == 0 ? slab : vanilla,
                        Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        }
    }

    private static float cropGrowthSpeed(Block crop, BlockGetter level, BlockPos pos) {
        try {
            Method method = CropBlock.class.getDeclaredMethod("getGrowthSpeed",
                    Block.class, BlockGetter.class, BlockPos.class);
            method.setAccessible(true);
            return (float) method.invoke(null, crop, level, pos);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not invoke Minecraft 26.2 crop fertility helper", exception);
        }
    }

    private static void assertShapeOffset(
            GameTestHelper helper,
            VoxelShape actual,
            VoxelShape canonical,
            double offset,
            String label) {
        List<AABB> actualBoxes = actual.toAabbs();
        List<AABB> canonicalBoxes = canonical.toAabbs();
        helper.assertTrue(actualBoxes.size() == canonicalBoxes.size(),
                label + " changed the canonical shape box count");
        for (int index = 0; index < canonicalBoxes.size(); index++) {
            AABB observed = actualBoxes.get(index);
            AABB expected = canonicalBoxes.get(index);
            helper.assertTrue(close(observed.minX, expected.minX)
                            && close(observed.maxX, expected.maxX)
                            && close(observed.minY, expected.minY + offset)
                            && close(observed.maxY, expected.maxY + offset)
                            && close(observed.minZ, expected.minZ)
                            && close(observed.maxZ, expected.maxZ),
                    label + " did not apply the shared crop translation exactly once");
        }
    }

    private static void assertUpwardColumn(
            GameTestHelper helper,
            BlockPos support,
            int height,
            String label) {
        for (int dy = 1; dy <= height; dy++) {
            assertBoundedOffset(helper, support.above(dy), support,
                    NibaruHorizontalSurface.BOTTOM_OFFSET, label);
        }
    }

    private static void assertWetAccepted(
            GameTestHelper helper,
            BlockPos plant,
            BlockPos support,
            String label) {
        helper.assertTrue(helper.getLevel().getFluidState(plant).is(Fluids.WATER),
                label + " fixture is not actually water-occupied");
        assertAcceptedOffset(helper, plant, support, NibaruHorizontalSurface.BOTTOM_OFFSET, label);
    }

    private static void assertAcceptedOffset(
            GameTestHelper helper,
            BlockPos plant,
            BlockPos expectedSupport,
            double expectedOffset,
            String label) {
        var level = helper.getLevel();
        BlockState state = level.getBlockState(plant);
        NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                .supporting(state, level, plant).orElse(null);
        double visibleOffset = NibaruHorizontalSurface.visibleOffset(state, level, plant);
        helper.assertTrue(surface != null
                        && surface.supportPos().equals(expectedSupport)
                        && surface.offset() == expectedOffset
                        && visibleOffset == expectedOffset
                        && visibleOffset != 0.0D,
                label + " acceptance and directional translation diverged: state=" + state
                        + ", surface=" + surface + ", visibleOffset=" + visibleOffset);
    }

    private static void assertBoundedOffset(
            GameTestHelper helper,
            BlockPos plant,
            BlockPos expectedSupport,
            double expectedOffset,
            String label) {
        var level = helper.getLevel();
        BlockState state = level.getBlockState(plant);
        BlockGetter bounded = new BoundedBlockGetter(level, plant, 0);
        NibaruHorizontalSurface.Surface surface = NibaruHorizontalSurface
                .supporting(state, bounded, level, plant).orElse(null);
        double visibleOffset = NibaruHorizontalSurface.visibleOffset(state, bounded, level, plant);
        helper.assertTrue(surface != null
                        && surface.supportPos().equals(expectedSupport)
                        && surface.offset() == expectedOffset
                        && visibleOffset == expectedOffset
                        && visibleOffset != 0.0D,
                label + " lost its nonlocal root through a bounded render view at " + plant
                        + ": state=" + state + ", surface=" + surface
                        + ", visibleOffset=" + visibleOffset);
    }

    private static void setColumn(
            net.minecraft.server.level.ServerLevel level,
            BlockPos support,
            BlockState supportState,
            List<BlockState> states) {
        clearVertical(level, support, 2, Math.max(4, states.size() + 1));
        level.setBlock(support, supportState, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        for (int index = 0; index < states.size(); index++) {
            level.setBlock(support.above(index + 1), states.get(index),
                    Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
    }

    private static void clearVertical(
            net.minecraft.server.level.ServerLevel level,
            BlockPos origin,
            int below,
            int above) {
        for (int dy = -below; dy <= above; dy++) {
            BlockPos pos = origin.above(dy);
            if (!level.isOutsideBuildHeight(pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        }
    }

    private static BlockState slab(Block canonicalParent, SlabType type) {
        NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(canonicalParent)
                .orElseThrow(() -> new AssertionError(
                        "missing BGE fixture profile for " + canonicalParent));
        Block exact = profile.nativeSlab()
                .orElseGet(() -> profile.effectiveSlabSource().orElseThrow());
        if (!(exact instanceof SlabBlock)) {
            throw new AssertionError("profile does not expose a horizontal slab: " + profile);
        }
        return exact.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, type);
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) <= 1.0E-6D;
    }

    private record BoundedBlockGetter(
            BlockGetter delegate,
            BlockPos center,
            int radius) implements BlockGetter {
        private boolean contains(BlockPos pos) {
            return Math.abs(pos.getX() - center.getX()) <= radius
                    && Math.abs(pos.getY() - center.getY()) <= radius
                    && Math.abs(pos.getZ() - center.getZ()) <= radius;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return contains(pos) ? delegate.getBlockEntity(pos) : null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return contains(pos) ? delegate.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return contains(pos) ? delegate.getFluidState(pos) : Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public int getMinY() {
            return delegate.getMinY();
        }
    }

    private record FungusCase(Block fungus, Block parent) {
    }

    private record AquaticCase(BlockState state, String label) {
    }

    private record CropPlacement(Item item, Block crop) {
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
