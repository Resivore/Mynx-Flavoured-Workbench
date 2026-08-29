package dev.aero.shulkertrowel.gametest;

import dev.aero.shulkertrowel.geometry.CnmNibaruGeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import dev.aero.shulkertrowel.item.ModItems;
import dev.aero.shulkertrowel.network.TrowelGeometryAuthority;
import dev.aero.shulkertrowel.palette.PaletteCandidate;
import dev.aero.shulkertrowel.palette.PaletteCandidateCollector;
import dev.aero.shulkertrowel.palette.ShulkerPaletteContents;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShulkerTrowelGameTests implements CustomTestMethodInvoker {
    private static final CnmNibaruGeometryResolver RESOLVER = new CnmNibaruGeometryResolver();
    private static final PaletteCandidateCollector COLLECTOR = new PaletteCandidateCollector(RESOLVER);

    @GameTest(maxTicks = 40)
    public void exactProfilesPreserveMaterialVariantsAcrossEveryMode(GameTestHelper helper) {
        Map<TargetGeometry, BlockItem> oakLog = allModes(helper, Blocks.OAK_LOG);
        Map<TargetGeometry, BlockItem> oakWood = allModes(helper, Blocks.OAK_WOOD);
        Map<TargetGeometry, BlockItem> strippedLog = allModes(helper, Blocks.STRIPPED_OAK_LOG);
        Map<TargetGeometry, BlockItem> exposedCopper = allModes(
                helper,
                Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.EXPOSED)
        );
        Map<TargetGeometry, BlockItem> weatheredCopper = allModes(
                helper,
                Blocks.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.WEATHERED)
        );
        Map<TargetGeometry, BlockItem> waxedWeatheredCopper =
                allModes(
                        helper,
                        Blocks.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.WEATHERED)
                );

        for (TargetGeometry geometry : TargetGeometry.values()) {
            helper.assertTrue(oakLog.get(geometry) != oakWood.get(geometry),
                    "Oak Log collapsed into Oak Wood for " + geometry);
            helper.assertTrue(oakLog.get(geometry) != strippedLog.get(geometry),
                    "Oak Log collapsed into Stripped Oak Log for " + geometry);
            helper.assertTrue(exposedCopper.get(geometry) != weatheredCopper.get(geometry),
                    "Exposed Copper collapsed into Weathered Copper for " + geometry);
            helper.assertTrue(weatheredCopper.get(geometry) != waxedWeatheredCopper.get(geometry),
                    "Weathered Copper collapsed into Waxed Weathered Copper for " + geometry);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sparseProfilesUseTypedEffectiveRolesAndNongeometryFamiliesFailClosed(GameTestHelper helper) {
        helper.assertTrue(resolve(Blocks.OAK_PLANKS, TargetGeometry.FULL).getBlock() == Blocks.OAK_PLANKS,
                "Full mode did not preserve the exact Canary 2 source block");
        helper.assertTrue(resolve(Blocks.OAK_PLANKS, TargetGeometry.SLAB).getBlock() == Blocks.OAK_SLAB,
                "Sparse Oak Planks profile did not use its effective vanilla slab");
        helper.assertTrue(resolve(Blocks.OAK_PLANKS, TargetGeometry.STAIR).getBlock() == Blocks.OAK_STAIRS,
                "Sparse Oak Planks profile did not use its effective vanilla stair");
        helper.assertTrue(RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, TargetGeometry.WALL).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, TargetGeometry.VERTICAL_SLAB).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, TargetGeometry.STEP).isPresent(),
                "Accepted typed Oak Planks roles were incomplete");

        for (Block nongeometry : List.of(
                Blocks.OAK_DOOR,
                Blocks.OAK_TRAPDOOR,
                Blocks.OAK_FENCE_GATE,
                Blocks.CRAFTING_TABLE
        )) {
            helper.assertTrue(RESOLVER.resolveGeometry(nongeometry, TargetGeometry.SLAB).isEmpty(),
                    "Non-geometry family admitted as a slab source: " + nongeometry);
            helper.assertTrue(resolve(nongeometry, TargetGeometry.FULL).getBlock() == nongeometry,
                    "Full compatibility path changed exact non-profile placement: " + nongeometry);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void eligibilityFilteringPrecedesQuantityWeighting(GameTestHelper helper) {
        NonNullList<ItemStack> contents = palette(
                new ItemStack(Blocks.CRAFTING_TABLE, 64),
                new ItemStack(Blocks.OAK_PLANKS, 2)
        );
        List<PaletteCandidate> candidates = COLLECTOR.collect(contents, TargetGeometry.SLAB);

        helper.assertTrue(candidates.size() == 1, "Ineligible high-count source entered the draw");
        PaletteCandidate candidate = candidates.getFirst();
        helper.assertTrue(candidate.slot() == 1
                        && candidate.weight() == 2
                        && candidate.sourceItem() == Blocks.OAK_PLANKS.asItem()
                        && candidate.placementItem() == Blocks.OAK_SLAB.asItem(),
                "Eligible candidate lost its original slot/source/quantity or typed target");
        helper.assertTrue(COLLECTOR.collect(
                        palette(new ItemStack(Blocks.CRAFTING_TABLE, 64)),
                        TargetGeometry.SLAB
                ).isEmpty(),
                "Controlled no-role palette produced a candidate");

        PaletteCandidateCollector controlledSparse = new PaletteCandidateCollector(
                (source, geometry) -> source == Blocks.OAK_PLANKS
                        ? Optional.empty()
                        : RESOLVER.resolveGeometry(source, geometry)
        );
        List<PaletteCandidate> sparseCandidates = controlledSparse.collect(
                palette(
                        new ItemStack(Blocks.OAK_PLANKS, 64),
                        new ItemStack(Blocks.SPRUCE_PLANKS, 3)
                ),
                TargetGeometry.SLAB
        );
        helper.assertTrue(sparseCandidates.size() == 1
                        && sparseCandidates.getFirst().sourceItem() == Blocks.SPRUCE_PLANKS.asItem()
                        && sparseCandidates.getFirst().weight() == 3,
                "Controlled sparse missing-role source contributed quantity weight");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void fullModePreservesCanaryTwoPlacementAndConsumesOriginalSlot(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack shulker = shulker(new ItemStack(Blocks.CRAFTING_TABLE, 3));
        equip(player, shulker, TargetGeometry.FULL);

        BlockPos support = new BlockPos(2, 1, 2);
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(support.above(), Blocks.AIR);
        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);

        helper.assertTrue(helper.getBlockState(support.above()).is(Blocks.CRAFTING_TABLE),
                "Full mode did not place the exact non-profile BlockItem");
        helper.assertTrue(ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 2,
                "Successful Full placement did not consume exactly one original slot item");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stairModeUsesNormalPlacementWaterloggingAndConsumesSourceIdentity(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack shulker = shulker(
                new ItemStack(Blocks.CRAFTING_TABLE, 64),
                new ItemStack(Blocks.OAK_PLANKS, 2)
        );
        equip(player, shulker, TargetGeometry.STAIR);

        BlockPos support = new BlockPos(2, 1, 2);
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(support.above(), Blocks.WATER);
        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);

        helper.assertTrue(helper.getBlockState(support.above()).is(Blocks.OAK_STAIRS),
                "Typed stair target was not placed through normal BlockItem placement");
        helper.assertTrue(helper.getBlockState(support.above()).getValue(BlockStateProperties.WATERLOGGED),
                "Normal stair waterlogging behavior was bypassed");
        NonNullList<ItemStack> remaining = ShulkerPaletteContents.read(player.getOffhandItem());
        helper.assertTrue(remaining.get(0).getCount() == 64 && remaining.get(1).getCount() == 1,
                "Placement did not consume the original eligible source slot exactly once");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void fittingSameBlockGeometryUsesOnlyOneSourceBlock(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);

        assertFittingMerge(helper, player, TargetGeometry.SLAB, new BlockPos(2, 1, 2));
        assertFittingMerge(helper, player, TargetGeometry.VERTICAL_SLAB, new BlockPos(4, 1, 2));
        assertFittingMerge(helper, player, TargetGeometry.STEP, new BlockPos(6, 1, 2));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void noEligibleCandidateProducesNoPlacementOrConsumption(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        equip(player, shulker(new ItemStack(Blocks.CRAFTING_TABLE, 3)), TargetGeometry.SLAB);

        BlockPos support = new BlockPos(2, 1, 2);
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(support.above(), Blocks.AIR);
        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);

        helper.assertTrue(helper.getBlockState(support.above()).isAir(),
                "No-role palette unexpectedly placed a block");
        helper.assertTrue(ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 3,
                "No-role palette unexpectedly consumed an item");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void modeStatePersistsAndOnlyValidMainHandRequestsApply(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack trowel = new ItemStack(ModItems.TROWEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, trowel);

        helper.assertTrue(TrowelGeometryState.get(trowel) == TargetGeometry.FULL,
                "Fresh trowel did not default to Full");
        helper.assertTrue(TrowelGeometryAuthority.apply(player, TargetGeometry.STEP.networkId())
                        && TrowelGeometryState.get(trowel) == TargetGeometry.STEP
                        && TrowelGeometryState.get(trowel.copy()) == TargetGeometry.STEP,
                "Valid server request did not persist and synchronize stack state");
        helper.assertTrue(!TrowelGeometryAuthority.apply(player, 999)
                        && TrowelGeometryState.get(trowel) == TargetGeometry.STEP,
                "Invalid mode ID changed server state");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        helper.assertTrue(!TrowelGeometryAuthority.apply(player, TargetGeometry.SLAB.networkId())
                        && TrowelGeometryState.get(trowel) == TargetGeometry.STEP,
                "A request without a main-hand trowel changed state");
        helper.succeed();
    }

    private static Map<TargetGeometry, BlockItem> allModes(GameTestHelper helper, Block source) {
        Map<TargetGeometry, BlockItem> resolved = new EnumMap<>(TargetGeometry.class);
        for (TargetGeometry geometry : TargetGeometry.values()) {
            BlockItem item = RESOLVER.resolveGeometry(source, geometry).orElse(null);
            helper.assertTrue(item != null, source + " did not resolve " + geometry);
            resolved.put(geometry, item);
        }
        return resolved;
    }

    private static BlockItem resolve(Block source, TargetGeometry geometry) {
        return RESOLVER.resolveGeometry(source, geometry).orElseThrow();
    }

    private static void assertFittingMerge(
            GameTestHelper helper,
            ServerPlayer player,
            TargetGeometry geometry,
            BlockPos support
    ) {
        equip(player, shulker(new ItemStack(Blocks.OAK_PLANKS, 2)), geometry);
        Block target = resolve(Blocks.OAK_PLANKS, geometry).getBlock();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(support.above(), Blocks.AIR);

        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);
        helper.assertTrue(helper.getBlockState(support.above()).is(target),
                geometry + " did not place its first partial geometry");
        helper.assertTrue(ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                geometry + " first partial geometry did not consume exactly one source block");

        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);
        var merged = helper.getBlockState(support.above());
        helper.assertTrue(merged.is(target), geometry + " fitting placement changed block identity");
        helper.assertTrue(ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                geometry + " fitting merge consumed a second source block");
        if (geometry == TargetGeometry.VERTICAL_SLAB) {
            helper.assertTrue(merged.getValue(VerticalSlabBlock.DOUBLE),
                    "Vertical slab fitting placement did not create the canonical double state");
        }
        else {
            helper.assertTrue(merged.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE,
                    geometry + " fitting placement did not create the canonical double state");
        }
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void equip(ServerPlayer player, ItemStack shulker, TargetGeometry geometry) {
        ItemStack trowel = new ItemStack(ModItems.TROWEL);
        TrowelGeometryState.set(trowel, geometry);
        player.setItemInHand(InteractionHand.MAIN_HAND, trowel);
        player.setItemInHand(InteractionHand.OFF_HAND, shulker);
    }

    private static ItemStack shulker(ItemStack... stacks) {
        ItemStack shulker = new ItemStack(Blocks.SHULKER_BOX);
        ShulkerPaletteContents.write(shulker, palette(stacks));
        return shulker;
    }

    private static NonNullList<ItemStack> palette(ItemStack... stacks) {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int index = 0; index < stacks.length; index++) contents.set(index, stacks[index]);
        return contents;
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
