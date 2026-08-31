package dev.aero.shulkertrowel.gametest;

import dev.aero.cnmterraincompat.BgeColumnBlock;
import dev.aero.cnmterraincompat.BgeLayerBlock;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ShulkerTrowelGameTests implements CustomTestMethodInvoker {
    private static final CnmNibaruGeometryResolver RESOLVER = new CnmNibaruGeometryResolver();
    private static final PaletteCandidateCollector COLLECTOR = new PaletteCandidateCollector(RESOLVER);
    private static final TargetGeometry VERTICAL_SLAB = bge("vertical_slab");
    private static final TargetGeometry STEP = bge("step");
    private static final TargetGeometry CORNER = bge("corner");
    private static final TargetGeometry QUARTER_COLUMN = bge("quarter_column");
    private static final TargetGeometry LAYER = bge("layer");

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

        for (TargetGeometry geometry : TargetGeometry.ordered()) {
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
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, VERTICAL_SLAB).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, STEP).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, CORNER).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, QUARTER_COLUMN).isPresent()
                        && RESOLVER.resolveGeometry(Blocks.OAK_PLANKS, LAYER).isPresent(),
                "Accepted typed Oak Planks roles were incomplete");
        var oakProfile = NibaruProviderAdapter.profile(Blocks.OAK_PLANKS).orElseThrow();
        for (TargetGeometry geometry : TargetGeometry.ordered().stream()
                .filter(mode -> mode.bgeDescriptor().isPresent()).toList()) {
            Block catalogBlock = geometry.bgeDescriptor().orElseThrow()
                    .resolveBlock(oakProfile).orElseThrow();
            BlockItem resolved = resolve(Blocks.OAK_PLANKS, geometry);
            helper.assertTrue(resolved.getBlock() == catalogBlock
                            && geometry.bgeDescriptor().orElseThrow().resolveItem(oakProfile)
                                    .orElseThrow() == resolved,
                    geometry + " did not resolve the catalog-owned exact block/item");
        }

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
                CORNER
        );
        helper.assertTrue(sparseCandidates.size() == 1
                        && sparseCandidates.getFirst().sourceItem() == Blocks.SPRUCE_PLANKS.asItem()
                        && sparseCandidates.getFirst().placementItem()
                                == resolve(Blocks.SPRUCE_PLANKS, CORNER)
                        && sparseCandidates.getFirst().weight() == 3,
                "Unavailable BGE material collapsed identity or contributed quantity weight");
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
        assertFittingMerge(helper, player, VERTICAL_SLAB, new BlockPos(4, 1, 2));
        assertFittingMerge(helper, player, STEP, new BlockPos(6, 1, 2));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void layerModeDelegatesCanonicalGrowthEconomyFailuresAndDrop(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        equip(player, shulker(new ItemStack(Blocks.OAK_PLANKS, 2)), LAYER);

        BlockPos support = new BlockPos(2, 1, 2);
        BlockPos target = support.above();
        BgeLayerBlock oakLayer = (BgeLayerBlock) resolve(
                Blocks.OAK_PLANKS,
                LAYER
        ).getBlock();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(target, Blocks.AIR);

        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);
        BlockState placed = helper.getBlockState(target);
        helper.assertTrue(placed.is(oakLayer)
                        && placed.getValue(BgeLayerBlock.LAYERS) == 1
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Actual trowel Layer placement did not spend exactly one Oak Planks source");

        for (int expectedLayers = 2; expectedLayers <= 4; expectedLayers++) {
            helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);
            BlockState grown = helper.getBlockState(target);
            helper.assertTrue(grown.is(oakLayer)
                            && grown.getValue(BgeLayerBlock.LAYERS) == expectedLayers
                            && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                    "Delegated Layer growth to " + expectedLayers
                            + " debited the already-funded Oak Planks source");
        }

        BlockState full = helper.getBlockState(target);
        helper.setBlock(target.above(), Blocks.STONE);
        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);
        helper.assertTrue(helper.getBlockState(target).equals(full)
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Failed fifth trowel Layer placement changed state or source count");

        BlockPos wrongFaceTarget = new BlockPos(5, 2, 2);
        BlockState partial = oakLayer.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        helper.setBlock(wrongFaceTarget, partial);
        for (Direction direction : Direction.values()) {
            helper.setBlock(wrongFaceTarget.relative(direction), Blocks.STONE);
        }
        helper.placeAt(
                player,
                player.getMainHandItem(),
                wrongFaceTarget.relative(Direction.NORTH),
                Direction.SOUTH
        );
        helper.assertTrue(helper.getBlockState(wrongFaceTarget).equals(partial)
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Wrong-face trowel Layer placement changed state or source count");

        BlockPos incompatibleSupport = new BlockPos(8, 1, 2);
        BlockPos incompatibleTarget = incompatibleSupport.above();
        BgeLayerBlock spruceLayer = (BgeLayerBlock) resolve(
                Blocks.SPRUCE_PLANKS,
                LAYER
        ).getBlock();
        BlockState incompatible = spruceLayer.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        helper.setBlock(incompatibleSupport, Blocks.STONE);
        helper.setBlock(incompatibleTarget, incompatible);
        for (Direction direction : Direction.values()) {
            helper.setBlock(incompatibleTarget.relative(direction), Blocks.STONE);
        }
        helper.placeAt(player, player.getMainHandItem(), incompatibleSupport, Direction.UP);
        helper.assertTrue(helper.getBlockState(incompatibleTarget).equals(incompatible)
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Incompatible trowel Layer placement changed state or source count");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        boolean destroyed = player.gameMode.destroyBlock(helper.absolutePos(target));
        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM, target, 2.0);
        helper.assertTrue(destroyed && helper.getBlockState(target).isAir(),
                "Survival break did not remove the four-part Oak Planks Layer");
        helper.assertTrue(drops.size() == 1
                        && drops.getFirst().getItem().is(Blocks.OAK_PLANKS.asItem())
                        && drops.getFirst().getItem().getCount() == 1,
                "Normal loaded-stack break did not drop one full Oak Planks source item: " + drops);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cornerAndQuarterColumnUseCatalogBlockItemsAndBgeEconomy(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);

        BlockPos cornerTarget = new BlockPos(2, 2, 6);
        blockAdjacentPositions(helper, cornerTarget);
        helper.setBlock(cornerTarget, Blocks.AIR);
        Block corner = resolve(Blocks.OAK_PLANKS, CORNER).getBlock();
        equip(player, shulker(new ItemStack(Blocks.OAK_PLANKS)), CORNER);
        placeInto(helper, player, cornerTarget, Direction.UP);
        helper.assertTrue(helper.getBlockState(cornerTarget).is(corner)
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).isEmpty(),
                "Catalog Corner did not place through its exact BlockItem and consume one source");

        BlockPos columnTarget = new BlockPos(6, 2, 6);
        blockAdjacentPositions(helper, columnTarget);
        helper.setBlock(columnTarget, Blocks.AIR);
        BgeColumnBlock column = (BgeColumnBlock) resolve(
                Blocks.OAK_PLANKS, QUARTER_COLUMN).getBlock();
        equip(player, shulker(new ItemStack(Blocks.OAK_PLANKS, 2)), QUARTER_COLUMN);
        placeInto(helper, player, columnTarget, Direction.UP);
        BlockState singleton = helper.getBlockState(columnTarget);
        helper.assertTrue(singleton.is(column)
                        && singleton.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.SE
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Catalog Quarter Column did not fund its first blockspace with one source");

        placeInto(helper, player, columnTarget, Direction.NORTH);
        BlockState expanded = helper.getBlockState(columnTarget);
        helper.assertTrue(expanded.is(column)
                        && expanded.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NW_SE
                        && ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 1,
                "Normal BGE Quarter Column expansion was not free through the Trowel path");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void noEligibleCandidateProducesNoPlacementOrConsumption(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        equip(player, shulker(new ItemStack(Blocks.CRAFTING_TABLE, 3)), CORNER);

        BlockPos support = new BlockPos(2, 1, 2);
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(support.above(), Blocks.AIR);
        helper.placeAt(player, player.getMainHandItem(), support, Direction.UP);

        helper.assertTrue(helper.getBlockState(support.above()).isAir(),
                "No-role palette unexpectedly placed a block");
        helper.assertTrue(ShulkerPaletteContents.read(player.getOffhandItem()).get(0).getCount() == 3,
                "No-role palette unexpectedly consumed an item");
        helper.assertTrue(TrowelGeometryState.get(player.getMainHandItem()) == CORNER,
                "Unavailable material handling collapsed the selected BGE mode identity");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void modeStatePersistsAndOnlyValidMainHandRequestsApply(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack trowel = new ItemStack(ModItems.TROWEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, trowel);

        helper.assertTrue(TrowelGeometryState.get(trowel) == TargetGeometry.FULL,
                "Fresh trowel did not default to Full");
        helper.assertTrue(TrowelGeometryAuthority.apply(player, CORNER.networkId())
                        && CORNER.networkId() == 7
                        && TrowelGeometryState.get(trowel) == CORNER
                        && TrowelGeometryState.get(trowel.copy()) == CORNER,
                "Stable Corner request did not persist and synchronize stack state");
        helper.assertTrue(TrowelGeometryAuthority.apply(player, QUARTER_COLUMN.networkId())
                        && QUARTER_COLUMN.networkId() == 8
                        && TrowelGeometryState.get(trowel) == QUARTER_COLUMN,
                "Stable Quarter Column request did not persist and synchronize stack state");
        helper.assertTrue(TrowelGeometryAuthority.apply(player, LAYER.networkId())
                        && LAYER.networkId() == 6
                        && TrowelGeometryState.get(trowel) == LAYER
                        && TrowelGeometryState.get(trowel.copy()) == LAYER,
                "Legacy Layer ID changed while its selector position moved");
        helper.assertTrue(!TrowelGeometryAuthority.apply(player, 999)
                        && TrowelGeometryState.get(trowel) == LAYER,
                "Invalid mode ID changed server state");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        helper.assertTrue(!TrowelGeometryAuthority.apply(player, TargetGeometry.SLAB.networkId())
                        && TrowelGeometryState.get(trowel) == LAYER,
                "A request without a main-hand trowel changed state");
        helper.succeed();
    }

    private static Map<TargetGeometry, BlockItem> allModes(GameTestHelper helper, Block source) {
        Map<TargetGeometry, BlockItem> resolved = new LinkedHashMap<>();
        for (TargetGeometry geometry : TargetGeometry.ordered()) {
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
        if (geometry == VERTICAL_SLAB) {
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

    private static TargetGeometry bge(String path) {
        return TargetGeometry.byKey(Identifier.fromNamespaceAndPath(
                "cnm_terrain_slabs_compat", path)).orElseThrow();
    }

    private static void blockAdjacentPositions(GameTestHelper helper, BlockPos target) {
        for (Direction direction : Direction.values()) {
            helper.setBlock(target.relative(direction), Blocks.STONE);
        }
    }

    private static void placeInto(GameTestHelper helper, ServerPlayer player,
            BlockPos target, Direction face) {
        helper.placeAt(player, player.getMainHandItem(), target.relative(face.getOpposite()), face);
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
