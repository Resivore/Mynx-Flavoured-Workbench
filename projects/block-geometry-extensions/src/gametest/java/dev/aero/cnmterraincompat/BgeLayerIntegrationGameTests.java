package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Catalog, placement, family, and representative material-contract tests for BGE Layer. */
public final class BgeLayerIntegrationGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void exactPlacementAndStackingDecisionContract(GameTestHelper helper) {
        BgeLayerBlock stone = layer("minecraft:stone");

        helper.assertTrue(BgeLayerBlock.placementFacing(Direction.UP, 0.9, false, Direction.NORTH)
                        == Direction.UP,
                "Top-face placement did not anchor a floor Layer");
        helper.assertTrue(BgeLayerBlock.placementFacing(Direction.DOWN, 0.1, false, Direction.NORTH)
                        == Direction.DOWN,
                "Bottom-face placement did not anchor a ceiling Layer");
        for (Direction face : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(BgeLayerBlock.placementFacing(face, 0.5, false, Direction.NORTH)
                            == Direction.UP,
                    "Horizontal midpoint/lower placement did not choose floor for " + face);
            helper.assertTrue(BgeLayerBlock.placementFacing(face, 0.5001, false, Direction.NORTH)
                            == Direction.DOWN,
                    "Horizontal upper placement did not choose ceiling for " + face);
        }
        for (Direction nearest : Direction.values()) {
            helper.assertTrue(BgeLayerBlock.placementFacing(Direction.UP, 0.0, true, nearest)
                            == nearest.getOpposite(),
                    "Secondary placement did not oppose nearest look direction " + nearest);
        }

        BlockState east = stone.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.EAST)
                .setValue(BgeLayerBlock.LAYERS, 1)
                .setValue(BgeLayerBlock.WATERLOGGED, true);
        ItemStack matching = new ItemStack(stone);
        helper.assertTrue(BgeLayerBlock.canStack(east, Direction.EAST, matching),
                "Matching Layer was not stackable through its exposed face");
        helper.assertTrue(!BgeLayerBlock.canStack(east, Direction.WEST, matching),
                "Layer stacked through its anchored face");
        helper.assertTrue(!BgeLayerBlock.canStack(east, Direction.EAST,
                        new ItemStack(profile("minecraft:dirt").canonicalParent())),
                "Layer stacked a different held material");

        BlockState second = BgeLayerBlock.stackedState(east);
        helper.assertTrue(second.getValue(BgeLayerBlock.LAYERS) == 2
                        && second.getValue(BgeLayerBlock.FACING) == Direction.EAST
                        && !second.getValue(BgeLayerBlock.DOUBLE)
                        && second.getValue(BgeLayerBlock.WATERLOGGED),
                "Stacking changed orientation/water before the full state");
        BlockState fourth = BgeLayerBlock.stackedState(east.setValue(BgeLayerBlock.LAYERS, 3));
        helper.assertTrue(fourth.getValue(BgeLayerBlock.LAYERS) == 4
                        && fourth.getValue(BgeLayerBlock.FACING) == Direction.EAST
                        && !fourth.getValue(BgeLayerBlock.DOUBLE)
                        && !fourth.getValue(BgeLayerBlock.WATERLOGGED)
                        && fourth.getBlock() == stone,
                "Fourth placement did not remain a dry Layer state");
        helper.assertTrue(!BgeLayerBlock.canStack(fourth, Direction.EAST, matching),
                "Four-layer state remained stackable in-place");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void normalBlockItemPlacementFundsCompatibleLayerGrowthOnlyOnce(GameTestHelper helper) {
        BgeLayerBlock oak = layer("minecraft:oak_planks");
        BgeLayerBlock dirt = layer("minecraft:dirt");
        helper.assertTrue(oak instanceof BlockspaceFundedGeometry
                        && oak.asItem() instanceof BgeBlockItem,
                "Generated Oak Planks Layer does not use the shared blockspace-funded item contract");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos target = support.above();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(target, Blocks.AIR);
        ItemStack placementStack = new ItemStack(oak, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, placementStack);

        helper.placeAt(player, placementStack, support, Direction.UP);
        BlockState placed = helper.getBlockState(target);
        helper.assertTrue(placed.is(oak)
                        && placed.getValue(BgeLayerBlock.LAYERS) == 1
                        && !placed.getValue(BgeLayerBlock.DOUBLE)
                        && placementStack.getCount() == 1,
                "First Layer placement did not consume exactly one funding source item");

        for (int expectedLayers = 2; expectedLayers <= 4; expectedLayers++) {
            helper.placeAt(player, placementStack, support, Direction.UP);
            BlockState grown = helper.getBlockState(target);
            helper.assertTrue(grown.is(oak)
                            && grown.getValue(BgeLayerBlock.LAYERS) == expectedLayers
                            && !grown.getValue(BgeLayerBlock.DOUBLE)
                            && placementStack.getCount() == 1,
                    "Compatible Layer growth to " + expectedLayers
                            + " did not preserve the already-funded source stack");
        }

        BlockState full = helper.getBlockState(target);
        helper.setBlock(target.above(), Blocks.STONE);
        helper.placeAt(player, placementStack, support, Direction.UP);
        helper.assertTrue(helper.getBlockState(target).equals(full) && placementStack.getCount() == 1,
                "Failed fifth Layer growth changed state or consumed an item");

        BlockPos wrongFaceTarget = new BlockPos(4, 2, 1);
        BlockState partial = oak.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        helper.setBlock(wrongFaceTarget, partial);
        for (Direction direction : Direction.values()) {
            helper.setBlock(wrongFaceTarget.relative(direction), Blocks.STONE);
        }
        ItemStack wrongFaceStack = new ItemStack(oak);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrongFaceStack);
        helper.placeAt(player, wrongFaceStack,
                wrongFaceTarget.relative(Direction.NORTH), Direction.SOUTH);
        helper.assertTrue(helper.getBlockState(wrongFaceTarget).equals(partial)
                        && wrongFaceStack.getCount() == 1,
                "Wrong-face Layer growth changed state or consumed an item: state="
                        + helper.getBlockState(wrongFaceTarget) + ", expected=" + partial
                        + ", count=" + wrongFaceStack.getCount());

        BlockPos incompatibleTarget = new BlockPos(7, 2, 1);
        helper.setBlock(incompatibleTarget, partial);
        for (Direction direction : Direction.values()) {
            helper.setBlock(incompatibleTarget.relative(direction), Blocks.STONE);
        }
        ItemStack incompatibleStack = new ItemStack(dirt);
        player.setItemInHand(InteractionHand.MAIN_HAND, incompatibleStack);
        helper.placeAt(player, incompatibleStack, incompatibleTarget.below(), Direction.UP);
        helper.assertTrue(helper.getBlockState(incompatibleTarget).equals(partial)
                        && incompatibleStack.getCount() == 1,
                "Incompatible Layer growth changed state or consumed an item");

        var drops = Block.getDrops(helper.getBlockState(target), helper.getLevel(),
                helper.absolutePos(target), null, player, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1
                        && drops.getFirst().is(Blocks.OAK_PLANKS.asItem())
                        && drops.getFirst().getCount() == 1,
                "Loaded CNM drop path did not return one full Oak Planks source block: " + drops);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void automaticPopulationIsExactNonrecursiveAndOrdered(GameTestHelper helper) {
        var profiles = NibaruMaterialProfiles.all();
        helper.assertTrue(profiles.size() == 311, "Expected 311 canonical profiles, found " + profiles.size());
        Set<Block> layers = Collections.newSetFromMap(new IdentityHashMap<>());
        int ordered = 0;
        for (NibaruMaterialProfile profile : profiles) {
            Block layer = NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER)
                    .orElseThrow();
            Identifier expected = CnmTerrainCompat.layerId(profile);
            helper.assertTrue(layers.add(layer), "Duplicate Layer owner for " + profile.canonicalParentId());
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(layer).equals(expected)
                            && BuiltInRegistries.ITEM.getKey(layer.asItem()).equals(expected),
                    "Layer block/item identity mismatch for " + profile.canonicalParentId());
            var binding = NibaruProviderAdapter.runtimeBinding(layer).orElseThrow();
            helper.assertTrue(binding.profile() == profile
                            && binding.geometry() == DerivedGeometrySupport.Geometry.LAYER,
                    "Layer lost its exact canonical profile binding: " + expected);
            helper.assertTrue(CanonicalGeometryRegistry.contains(layer),
                    "Layer was not recursion-guarded: " + expected);
            helper.assertTrue(NibaruMaterialProfiles.fromBlock(layer).isEmpty(),
                    "BGE Layer became a new canonical material source: " + expected);

            var component = ShapeMap.getShapes(profile.canonicalParent().asItem());
            long occurrences = component.stream().filter(item -> item == layer.asItem()).count();
            helper.assertTrue(occurrences == 1,
                    "ShapeMap contains " + occurrences + " Layer members for " + expected);
            int layerIndex = component.indexOf(layer.asItem());
            int stepIndex = component.indexOf(NibaruProviderAdapter.derived(profile,
                    DerivedGeometrySupport.Geometry.STEP).orElseThrow().asItem());
            helper.assertTrue(stepIndex >= 0 && layerIndex > stepIndex,
                    "Layer is not deterministically ordered after Step for " + expected);
            ordered++;
        }

        var traits = DerivedMaterialTraits.entries().stream()
                .filter(entry -> entry.geometry() == DerivedGeometrySupport.Geometry.LAYER).toList();
        helper.assertTrue(layers.size() == 311 && ordered == 311 && traits.size() == 311,
                "Layer population mismatch: blocks=" + layers.size() + ", ordered=" + ordered
                        + ", traits=" + traits.size());
        helper.assertTrue(traits.stream().allMatch(entry -> entry.fuelDivisor() == 4
                        && layers.contains(entry.derived())),
                "Layer material traits do not use exact quarter-block ownership");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void representativeMaterialContractsRemainTyped(GameTestHelper helper) {
        BgeLayerBlock stone = layer("minecraft:stone");
        helper.assertTrue(stone.getStateDefinition().getProperties().size() == 4
                        && BgeLayerBlock.DOUBLE == VerticalSlabBlock.DOUBLE
                        && stone.defaultBlockState().hasProperty(VerticalSlabBlock.DOUBLE)
                        && !stone.defaultBlockState().getValue(BgeLayerBlock.DOUBLE),
                "Layer lost its inert C54 DOUBLE compatibility carrier");

        NibaruMaterialProfile grassProfile = profile("minecraft:grass_block");
        BgeLayerBlock grass = layer(grassProfile);
        helper.assertTrue(grassProfile.visualProfile() == VisualProfile.GRASS_OVERLAY
                        && grassProfile.tintProfile() == TintProfile.GRASS_BIOME
                        && grass.getClass().getName().contains("Spreadable"),
                "Tinted TOP/SIDE/BOTTOM grass Layer lost its typed contract");

        BgeLayerBlock log = layer("minecraft:oak_log");
        helper.assertTrue(log.defaultBlockState().hasProperty(MaterialAxisState.AXIS),
                "Directional pillar Layer omitted independent axis");
        for (Direction.Axis axis : Direction.Axis.values()) {
            BlockState state = log.defaultBlockState()
                    .setValue(MaterialAxisState.AXIS, axis)
                    .setValue(BgeLayerBlock.FACING, Direction.NORTH)
                    .setValue(BgeLayerBlock.LAYERS, 3);
            helper.assertTrue(state.getValue(MaterialAxisState.AXIS) == axis,
                    "Pillar Layer rejected material axis " + axis);
        }
        BlockState rotatedLog = log.rotate(log.defaultBlockState()
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(BgeLayerBlock.FACING, Direction.NORTH), Rotation.CLOCKWISE_90);
        helper.assertTrue(rotatedLog.getValue(MaterialAxisState.AXIS) == Direction.Axis.Z
                        && rotatedLog.getValue(BgeLayerBlock.FACING) == Direction.EAST,
                "Pillar Layer rotation did not transform material and geometry independently");

        BgeLayerBlock glazed = layer("minecraft:white_glazed_terracotta");
        helper.assertTrue(glazed.defaultBlockState().hasProperty(GlazedPatternState.PATTERN_FACING),
                "Glazed Layer omitted independent pattern direction");

        BgeLayerBlock leaves = layer("minecraft:oak_leaves");
        helper.assertTrue(leaves instanceof LeafDistanceCarrier
                        && leaves.defaultBlockState().hasProperty(BlockStateProperties.DISTANCE)
                        && leaves.defaultBlockState().hasProperty(BlockStateProperties.PERSISTENT),
                "Leaf Layer omitted lifecycle state/contract");

        BgeLayerBlock path = layer("minecraft:dirt_path");
        helper.assertTrue(path instanceof PathGeometry,
                "Dirt Path Layer omitted path-survival geometry contract");
        var pathState = path.defaultBlockState().setValue(BgeLayerBlock.FACING, Direction.SOUTH)
                .setValue(BgeLayerBlock.LAYERS, 2);
        double pathHeight = pathState.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                CollisionContext.empty()).bounds().maxY;
        helper.assertTrue(pathHeight == 15.0 / 16.0,
                "Dirt Path Layer did not retain lowered world-top surface: " + pathHeight);

        BgeLayerBlock copper = layer("minecraft:copper_block");
        BgeLayerBlock sand = layer("minecraft:sand");
        BgeLayerBlock redstone = layer("minecraft:redstone_block");
        helper.assertTrue(copper instanceof ChangeOverTimeBlock<?>,
                "Copper Layer omitted oxidation lifecycle interface");
        helper.assertTrue(sand instanceof Fallable, "Sand Layer omitted falling contract");
        helper.assertTrue(redstone.defaultBlockState().isSignalSource()
                        && redstone.defaultBlockState().getSignal(helper.getLevel(),
                                helper.absolutePos(new BlockPos(2, 1, 1)), Direction.UP) == 15,
                "Redstone Layer omitted power contract");

        long supported = NibaruMaterialProfiles.all().stream().filter(profile ->
                profile.supportFor(DerivedGeometrySupport.Geometry.LAYER,
                        NibaruProviderAdapter.ADAPTED_CAPABILITIES,
                        NibaruProviderAdapter.ADAPTED_VISUALS).supported()).count();
        helper.assertTrue(supported == 311,
                "Not every canonical family is eligible for Layer: " + supported + "/311");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void flatteningValidatesTheComposedLayerPathState(GameTestHelper helper) {
        BgeLayerBlock dirt = layer("minecraft:dirt");
        BgeLayerBlock path = layer("minecraft:dirt_path");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        BlockPos lowFloorPos = new BlockPos(1, 1, 1);
        BlockState lowFloor = dirt.defaultBlockState()
                .setValue(BgeLayerBlock.FACING, Direction.UP)
                .setValue(BgeLayerBlock.LAYERS, 1);
        helper.setBlock(lowFloorPos, lowFloor);
        helper.setBlock(lowFloorPos.above(), Blocks.STONE);
        ItemStack successfulShovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, successfulShovel);
        InteractionResult success = NibaruProviderAdapter.useComposedCapabilities(dirt, successfulShovel,
                lowFloor, helper.getLevel(), helper.absolutePos(lowFloorPos), player, InteractionHand.MAIN_HAND)
                .orElseThrow();
        BlockState flattened = helper.getBlockState(lowFloorPos);
        helper.assertTrue(success == InteractionResult.SUCCESS_SERVER
                        && flattened.is(path)
                        && flattened.getValue(BgeLayerBlock.FACING) == Direction.UP
                        && flattened.getValue(BgeLayerBlock.LAYERS) == 1
                        && successfulShovel.getDamageValue() == 1,
                "Exposed low floor Layer did not flatten through its geometry-specific survival exception");

        assertObstructedFlattenRejected(helper, player, dirt, new BlockPos(3, 1, 1),
                dirt.defaultBlockState().setValue(BgeLayerBlock.FACING, Direction.DOWN)
                        .setValue(BgeLayerBlock.LAYERS, 1),
                "ceiling Layer");
        assertObstructedFlattenRejected(helper, player, dirt, new BlockPos(5, 1, 1),
                dirt.defaultBlockState().setValue(BgeLayerBlock.FACING, Direction.UP)
                        .setValue(BgeLayerBlock.LAYERS, 4),
                "full Layer");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void partialTranslucentAdjacencyKeepsAnchoredBoundary(GameTestHelper helper) {
        BgeLayerBlock glass = layer("minecraft:glass");
        int checked = 0;
        for (Direction facing : Direction.values()) {
            for (int layers = 1; layers <= 4; layers++) {
                BlockState state = glass.defaultBlockState()
                        .setValue(BgeLayerBlock.FACING, facing)
                        .setValue(BgeLayerBlock.LAYERS, layers);
                for (Direction direction : Direction.values()) {
                    boolean expected = layers == 4 || direction != facing.getOpposite();
                    helper.assertTrue(BgeLayerBlock.equalStateFaceCanCull(state, direction) == expected,
                            "Wrong equal-Layer translucent cull for facing=" + facing
                                    + ",layers=" + layers + ",direction=" + direction);
                    checked++;
                }
            }
        }
        helper.assertTrue(checked == 144, "Incomplete Layer translucent cull truth table: " + checked);
        helper.succeed();
    }

    private static NibaruMaterialProfile profile(String id) {
        return NibaruMaterialProfiles.fromId(Identifier.parse(id)).orElseThrow();
    }

    private static BgeLayerBlock layer(String id) { return layer(profile(id)); }

    private static BgeLayerBlock layer(NibaruMaterialProfile profile) {
        return (BgeLayerBlock) NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER)
                .orElseThrow();
    }

    private static void assertObstructedFlattenRejected(GameTestHelper helper,
            net.minecraft.world.entity.player.Player player, BgeLayerBlock dirt, BlockPos pos,
            BlockState source, String description) {
        helper.setBlock(pos, source);
        helper.setBlock(pos.above(), Blocks.STONE);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        InteractionResult result = NibaruProviderAdapter.useComposedCapabilities(dirt, shovel, source,
                helper.getLevel(), helper.absolutePos(pos), player, InteractionHand.MAIN_HAND).orElseThrow();
        helper.assertTrue(result == InteractionResult.TRY_WITH_EMPTY_HAND
                        && helper.getBlockState(pos).equals(source)
                        && shovel.getDamageValue() == 0,
                "Obstructed " + description + " flattened before validating the composed path state");
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
