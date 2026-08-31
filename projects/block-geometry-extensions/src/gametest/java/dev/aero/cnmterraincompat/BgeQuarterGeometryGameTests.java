package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Focused catalog, physical, economy, and material-contract coverage for C55 quarter geometry. */
public final class BgeQuarterGeometryGameTests implements CustomTestMethodInvoker {
    private static final BlockPos SHAPE_POS = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 40)
    public void catalogBindingsAndShapeMapOrderAreExact(GameTestHelper helper) {
        List<NibaruMaterialProfile> profiles = NibaruMaterialProfiles.all();
        helper.assertTrue(profiles.size() == 311,
                "Expected 311 canonical material profiles, found " + profiles.size());

        Set<Block> corners = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Block> columns = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Block> allQuarterGeometry = Collections.newSetFromMap(new IdentityHashMap<>());
        for (NibaruMaterialProfile profile : profiles) {
            Block corner = NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER).orElseThrow();
            Block column = NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN).orElseThrow();
            Identifier cornerId = CnmTerrainCompat.cornerId(profile);
            Identifier columnId = CnmTerrainCompat.quarterColumnId(profile);

            helper.assertTrue(corner instanceof BgeCornerBlock && column instanceof BgeColumnBlock,
                    "Quarter geometry has the wrong runtime type for " + profile.canonicalParentId());
            helper.assertTrue(corners.add(corner) && columns.add(column)
                            && allQuarterGeometry.add(corner) && allQuarterGeometry.add(column),
                    "Quarter geometry identity was reused for " + profile.canonicalParentId());
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(corner).equals(cornerId)
                            && BuiltInRegistries.ITEM.getKey(corner.asItem()).equals(cornerId)
                            && BuiltInRegistries.BLOCK.getKey(column).equals(columnId)
                            && BuiltInRegistries.ITEM.getKey(column.asItem()).equals(columnId),
                    "Quarter geometry block/item identity mismatch for " + profile.canonicalParentId());
            helper.assertTrue(corner.asItem() instanceof BlockItem
                            && !(corner.asItem() instanceof BgeBlockItem)
                            && column.asItem() instanceof BgeBlockItem,
                    "Compound item economy ownership is wrong for " + profile.canonicalParentId());

            var cornerBinding = NibaruProviderAdapter.runtimeBinding(corner).orElseThrow();
            var columnBinding = NibaruProviderAdapter.runtimeBinding(column).orElseThrow();
            helper.assertTrue(cornerBinding.profile() == profile
                            && cornerBinding.role() == BgeGeometryRole.CORNER
                            && columnBinding.profile() == profile
                            && columnBinding.role() == BgeGeometryRole.QUARTER_COLUMN,
                    "Quarter geometry lost its exact canonical binding for " + profile.canonicalParentId());
            helper.assertTrue(CanonicalGeometryRegistry.contains(corner)
                            && CanonicalGeometryRegistry.contains(column)
                            && NibaruMaterialProfiles.fromBlock(corner).isEmpty()
                            && NibaruMaterialProfiles.fromBlock(column).isEmpty(),
                    "Quarter geometry became a recursive canonical material source for "
                            + profile.canonicalParentId());

            List<Item> component = ShapeMap.getShapes(profile.canonicalParent().asItem());
            helper.assertTrue(Collections.frequency(component, corner.asItem()) == 1
                            && Collections.frequency(component, column.asItem()) == 1,
                    "ShapeMap does not contain each quarter geometry exactly once for "
                            + profile.canonicalParentId());
            List<Item> actualSegment = component.stream()
                    .filter(item -> providerProfile(item).orElse(null) == profile).toList();
            List<Item> expectedSegment = expectedShapeMapSegment(profile);
            helper.assertTrue(actualSegment.equals(expectedSegment),
                    "Provider ShapeMap segment is not parent/native/Vertical/Step/Layer/Corner/Column for "
                            + profile.canonicalParentId() + ": expected=" + ids(expectedSegment)
                            + ", actual=" + ids(actualSegment));
        }

        long cornerTraits = NibaruProviderAdapter.localMaterialTraits().stream()
                .filter(trait -> trait.role() == BgeGeometryRole.CORNER
                        && trait.fuelDivisor() == 4 && corners.contains(trait.derived())).count();
        long columnTraits = NibaruProviderAdapter.localMaterialTraits().stream()
                .filter(trait -> trait.role() == BgeGeometryRole.QUARTER_COLUMN
                        && trait.fuelDivisor() == 4 && columns.contains(trait.derived())).count();
        helper.assertTrue(corners.size() == 311 && columns.size() == 311
                        && allQuarterGeometry.size() == 622
                        && cornerTraits == 311 && columnTraits == 311,
                "Quarter geometry population/trait count mismatch: corners=" + corners.size()
                        + ", columns=" + columns.size() + ", total=" + allQuarterGeometry.size()
                        + ", traits=" + cornerTraits + "/" + columnTraits);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cornerHasExactlyEightPhysicalStatesAndExactTransforms(GameTestHelper helper) {
        BgeCornerBlock corner = corner("minecraft:stone");
        helper.assertTrue(corner.getStateDefinition().getProperties().size() == 3
                        && corner.getStateDefinition().getPossibleStates().size() == 16,
                "Ordinary Corner state space is not facing x half x waterlogged");

        BlockPos absolute = helper.absolutePos(SHAPE_POS);
        int checked = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Half half : Half.values()) {
                BlockState state = corner.defaultBlockState()
                        .setValue(BgeCornerBlock.FACING, facing)
                        .setValue(BgeCornerBlock.HALF, half)
                        .setValue(BgeCornerBlock.WATERLOGGED, true);
                assertShape(helper, state, absolute, List.of(expectedCorner(facing, half)),
                        "Corner " + facing + "/" + half);
                for (Rotation rotation : Rotation.values()) {
                    BlockState transformed = corner.rotate(state, rotation);
                    helper.assertTrue(transformed.getValue(BgeCornerBlock.FACING) == rotation.rotate(facing)
                                    && transformed.getValue(BgeCornerBlock.HALF) == half
                                    && transformed.getValue(BgeCornerBlock.WATERLOGGED),
                            "Corner rotation changed independent state for " + facing + "/" + half
                                    + "/" + rotation + ": " + transformed);
                }
                for (Mirror mirror : Mirror.values()) {
                    BlockState transformed = corner.mirror(state, mirror);
                    helper.assertTrue(transformed.getValue(BgeCornerBlock.FACING) == mirror.mirror(facing)
                                    && transformed.getValue(BgeCornerBlock.HALF) == half
                                    && transformed.getValue(BgeCornerBlock.WATERLOGGED),
                            "Corner mirror changed independent state for " + facing + "/" + half
                                    + "/" + mirror + ": " + transformed);
                }
                checked++;
            }
        }
        helper.assertTrue(checked == 8, "Expected exactly eight Corner physical states, checked " + checked);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void columnHasExactlySixPhysicalStatesGrowthMatrixAndTransforms(GameTestHelper helper) {
        BgeColumnBlock column = column("minecraft:stone");
        helper.assertTrue(column.getStateDefinition().getProperties().size() == 2
                        && column.getStateDefinition().getPossibleStates().size() == 12,
                "Ordinary Quarter Column state space is not six occupancies x waterlogged");

        BlockPos absolute = helper.absolutePos(SHAPE_POS);
        int states = 0;
        int growthEdges = 0;
        for (BgeColumnBlock.Occupancy occupancy : BgeColumnBlock.Occupancy.values()) {
            BlockState state = column.defaultBlockState()
                    .setValue(BgeColumnBlock.OCCUPANCY, occupancy)
                    .setValue(BgeColumnBlock.WATERLOGGED, true);
            assertShape(helper, state, absolute, expectedColumn(occupancy),
                    "Quarter Column " + occupancy);
            for (Direction face : Direction.values()) {
                BgeColumnBlock.Occupancy expected = expectedGrowth(occupancy, face);
                BgeColumnBlock.Occupancy actual = BgeColumnBlock.expandedOccupancy(occupancy, face);
                helper.assertTrue(actual == expected,
                        "Quarter Column growth matrix changed for " + occupancy + "/" + face
                                + ": expected=" + expected + ", actual=" + actual);
                if (actual != null) growthEdges++;
            }
            for (Rotation rotation : Rotation.values()) {
                BlockState transformed = column.rotate(state, rotation);
                helper.assertTrue(transformed.getValue(BgeColumnBlock.OCCUPANCY)
                                    == expectedRotation(occupancy, rotation)
                                && transformed.getValue(BgeColumnBlock.WATERLOGGED),
                        "Quarter Column rotation changed occupancy/water for " + occupancy
                                + "/" + rotation + ": " + transformed);
            }
            for (Mirror mirror : Mirror.values()) {
                BlockState transformed = column.mirror(state, mirror);
                helper.assertTrue(transformed.getValue(BgeColumnBlock.OCCUPANCY)
                                    == expectedMirror(occupancy, mirror)
                                && transformed.getValue(BgeColumnBlock.WATERLOGGED),
                        "Quarter Column mirror changed occupancy/water for " + occupancy
                                + "/" + mirror + ": " + transformed);
            }
            states++;
        }
        helper.assertTrue(states == 6 && growthEdges == 8,
                "Expected six Column states and eight compatible growth edges, found "
                        + states + "/" + growthEdges);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void columnEconomyFailuresDropsAndWaterPreserveOneFundedBlockspace(GameTestHelper helper) {
        BgeColumnBlock oak = column("minecraft:oak_planks");
        BgeColumnBlock dirt = column("minecraft:dirt");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        BlockPos funded = new BlockPos(1, 2, 1);
        blockAdjacentPositions(helper, funded);
        helper.setBlock(funded, Blocks.AIR);
        ItemStack fundedStack = new ItemStack(oak, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, fundedStack);
        placeInto(helper, player, fundedStack, funded, Direction.UP);
        BlockState singleton = helper.getBlockState(funded);
        helper.assertTrue(singleton.is(oak)
                        && singleton.getValue(BgeColumnBlock.OCCUPANCY) == BgeColumnBlock.Occupancy.SE
                        && fundedStack.getCount() == 1,
                "First Column placement did not consume exactly one blockspace funding item: state="
                        + singleton + ", count=" + fundedStack.getCount());

        placeInto(helper, player, fundedStack, funded, Direction.NORTH);
        BlockState expanded = helper.getBlockState(funded);
        helper.assertTrue(expanded.is(oak)
                        && expanded.getValue(BgeColumnBlock.OCCUPANCY) == BgeColumnBlock.Occupancy.NW_SE
                        && fundedStack.getCount() == 1,
                "Compatible in-place Column expansion consumed another source item: state="
                        + expanded + ", count=" + fundedStack.getCount());

        placeInto(helper, player, fundedStack, funded, Direction.WEST);
        helper.assertTrue(helper.getBlockState(funded).equals(expanded) && fundedStack.getCount() == 1,
                "Full Column attempt changed state or consumed an item");

        BlockPos wrongFace = new BlockPos(4, 2, 1);
        blockAdjacentPositions(helper, wrongFace);
        BlockState wrongFaceState = oak.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.SE);
        helper.setBlock(wrongFace, wrongFaceState);
        ItemStack wrongFaceStack = new ItemStack(oak);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrongFaceStack);
        placeInto(helper, player, wrongFaceStack, wrongFace, Direction.UP);
        helper.assertTrue(helper.getBlockState(wrongFace).equals(wrongFaceState)
                        && wrongFaceStack.getCount() == 1,
                "Wrong-face Column attempt changed state or consumed an item");

        BlockPos incompatible = new BlockPos(7, 2, 1);
        blockAdjacentPositions(helper, incompatible);
        BlockState incompatibleState = oak.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.SE);
        helper.setBlock(incompatible, incompatibleState);
        ItemStack incompatibleStack = new ItemStack(dirt);
        player.setItemInHand(InteractionHand.MAIN_HAND, incompatibleStack);
        placeInto(helper, player, incompatibleStack, incompatible, Direction.NORTH);
        helper.assertTrue(helper.getBlockState(incompatible).equals(incompatibleState)
                        && incompatibleStack.getCount() == 1,
                "Incompatible Column attempt changed state or consumed an item");

        assertCanonicalDrop(helper, player, corner("minecraft:oak_planks").defaultBlockState(),
                new BlockPos(1, 5, 1), Blocks.OAK_PLANKS, "Corner");
        assertCanonicalDrop(helper, player, expanded, funded, Blocks.OAK_PLANKS, "double Column");

        BgeCornerBlock stoneCorner = corner("minecraft:stone");
        BlockState wetCorner = stoneCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING, Direction.WEST)
                .setValue(BgeCornerBlock.HALF, Half.TOP)
                .setValue(BgeCornerBlock.WATERLOGGED, true);
        BlockState wetColumn = oak.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NE_SW)
                .setValue(BgeColumnBlock.WATERLOGGED, true);
        BlockPos wetCornerPos = new BlockPos(4, 5, 1);
        BlockPos wetColumnPos = new BlockPos(7, 5, 1);
        helper.setBlock(wetCornerPos, wetCorner);
        helper.setBlock(wetColumnPos, wetColumn);
        helper.assertTrue(wetCorner.getFluidState().getType() == Fluids.WATER
                        && wetColumn.getFluidState().getType() == Fluids.WATER
                        && helper.getBlockState(wetCornerPos).equals(wetCorner)
                        && helper.getBlockState(wetCornerPos).getFluidState().getType() == Fluids.WATER
                        && helper.getBlockState(wetColumnPos).equals(wetColumn)
                        && helper.getBlockState(wetColumnPos).getFluidState().getType() == Fluids.WATER
                        && stoneCorner.rotate(wetCorner, Rotation.CLOCKWISE_90)
                                .getValue(BgeCornerBlock.WATERLOGGED)
                        && oak.mirror(wetColumn, Mirror.FRONT_BACK)
                                .getValue(BgeColumnBlock.WATERLOGGED),
                "Quarter geometry did not persist source water across state transforms");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void obstructedPathColumnGrowthRevertsInPlaceWithoutAnotherDebit(GameTestHelper helper) {
        BgeColumnBlock path = column("minecraft:dirt_path");
        BgeColumnBlock dirt = column("minecraft:dirt");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        BlockPos target = new BlockPos(1, 2, 1);
        blockAdjacentPositions(helper, target);
        BlockState singleton = path.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.SE);
        helper.setBlock(target, singleton);
        ItemStack stack = new ItemStack(path);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        placeInto(helper, player, stack, target, Direction.NORTH);
        BlockState result = helper.getBlockState(target);
        helper.assertTrue(result.is(dirt)
                        && result.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NW_SE
                        && stack.getCount() == 1,
                "Obstructed Path Column growth did not preserve occupancy, revert to Dirt, "
                        + "and remain a free in-place expansion: state=" + result
                        + ", count=" + stack.getCount());
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void materialAxisPatternAndSpecializedProfilesRemainIndependent(GameTestHelper helper) {
        BgeCornerBlock logCorner = corner("minecraft:oak_log");
        BgeColumnBlock logColumn = column("minecraft:oak_log");
        helper.assertTrue(logCorner.defaultBlockState().hasProperty(MaterialAxisState.AXIS)
                        && logColumn.defaultBlockState().hasProperty(MaterialAxisState.AXIS),
                "Pillar quarter geometry omitted the independent material axis");
        BlockState cornerAxis = logCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING, Direction.NORTH)
                .setValue(BgeCornerBlock.HALF, Half.TOP)
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(BgeCornerBlock.WATERLOGGED, true);
        BlockState columnAxis = logColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW)
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(BgeColumnBlock.WATERLOGGED, true);
        BlockState rotatedCornerAxis = logCorner.rotate(cornerAxis, Rotation.CLOCKWISE_90);
        BlockState rotatedColumnAxis = logColumn.rotate(columnAxis, Rotation.CLOCKWISE_90);
        helper.assertTrue(rotatedCornerAxis.getValue(BgeCornerBlock.FACING) == Direction.EAST
                        && rotatedCornerAxis.getValue(BgeCornerBlock.HALF) == Half.TOP
                        && rotatedCornerAxis.getValue(MaterialAxisState.AXIS) == Direction.Axis.Z
                        && rotatedCornerAxis.getValue(BgeCornerBlock.WATERLOGGED)
                        && rotatedColumnAxis.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NE
                        && rotatedColumnAxis.getValue(MaterialAxisState.AXIS) == Direction.Axis.Z
                        && rotatedColumnAxis.getValue(BgeColumnBlock.WATERLOGGED),
                "Pillar material axis and quarter geometry did not transform independently");

        BgeCornerBlock glazedCorner = corner("minecraft:white_glazed_terracotta");
        BgeColumnBlock glazedColumn = column("minecraft:white_glazed_terracotta");
        helper.assertTrue(glazedCorner.defaultBlockState().hasProperty(GlazedPatternState.PATTERN_FACING)
                        && glazedColumn.defaultBlockState().hasProperty(GlazedPatternState.PATTERN_FACING),
                "Glazed quarter geometry omitted the independent pattern direction");
        BlockState cornerPattern = glazedCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING, Direction.EAST)
                .setValue(BgeCornerBlock.HALF, Half.BOTTOM)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.SOUTH);
        BlockState columnPattern = glazedColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW_SE)
                .setValue(GlazedPatternState.PATTERN_FACING, Direction.EAST);
        BlockState rotatedCornerPattern = glazedCorner.rotate(cornerPattern, Rotation.CLOCKWISE_90);
        BlockState rotatedColumnPattern = glazedColumn.rotate(columnPattern, Rotation.CLOCKWISE_90);
        helper.assertTrue(rotatedCornerPattern.getValue(BgeCornerBlock.FACING) == Direction.SOUTH
                        && rotatedCornerPattern.getValue(GlazedPatternState.PATTERN_FACING) == Direction.WEST
                        && rotatedColumnPattern.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NE_SW
                        && rotatedColumnPattern.getValue(GlazedPatternState.PATTERN_FACING) == Direction.SOUTH,
                "Glazed pattern frame was coupled to quarter geometry occupancy");

        NibaruMaterialProfile grassProfile = profile("minecraft:grass_block");
        helper.assertTrue(grassProfile.visualProfile() == VisualProfile.GRASS_OVERLAY
                        && corner(grassProfile) instanceof SpreadableGeometry
                        && column(grassProfile) instanceof SpreadableGeometry,
                "Grass TOP/SIDE/BOTTOM profile or spreadable quarter geometry contract changed");
        for (String id : List.of("minecraft:oak_leaves", "minecraft:dirt_path",
                "minecraft:copper_block", "minecraft:sand", "minecraft:redstone_block")) {
            BgeCornerBlock specializedCorner = corner(id);
            BgeColumnBlock specializedColumn = column(id);
            switch (id) {
                case "minecraft:oak_leaves" -> helper.assertTrue(
                        specializedCorner instanceof LeafDistanceCarrier
                                && specializedColumn instanceof LeafDistanceCarrier
                                && specializedCorner.defaultBlockState().hasProperty(BlockStateProperties.DISTANCE)
                                && specializedCorner.defaultBlockState().hasProperty(BlockStateProperties.PERSISTENT)
                                && specializedColumn.defaultBlockState().hasProperty(BlockStateProperties.DISTANCE)
                                && specializedColumn.defaultBlockState().hasProperty(BlockStateProperties.PERSISTENT),
                        "Leaf quarter geometry omitted lifecycle state/contracts");
                case "minecraft:dirt_path" -> {
                    helper.assertTrue(specializedCorner instanceof PathGeometry
                                    && specializedColumn instanceof PathGeometry,
                            "Path quarter geometry omitted survival contracts");
                    double cornerTop = specializedCorner.defaultBlockState()
                            .setValue(BgeCornerBlock.HALF, Half.TOP)
                            .getShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)),
                                    CollisionContext.empty()).bounds().maxY;
                    double columnTop = specializedColumn.defaultBlockState()
                            .getShape(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 2)),
                                    CollisionContext.empty()).bounds().maxY;
                    helper.assertTrue(cornerTop == 15.0 / 16.0 && columnTop == 15.0 / 16.0,
                            "Path quarter geometry lost its lowered world-top surface: "
                                    + cornerTop + "/" + columnTop);
                }
                case "minecraft:copper_block" -> helper.assertTrue(
                        specializedCorner instanceof ChangeOverTimeBlock<?>
                                && specializedColumn instanceof ChangeOverTimeBlock<?>,
                        "Copper quarter geometry omitted oxidation lifecycle contracts");
                case "minecraft:sand" -> helper.assertTrue(
                        specializedCorner instanceof Fallable && specializedColumn instanceof Fallable,
                        "Falling quarter geometry omitted the Fallable contract");
                case "minecraft:redstone_block" -> helper.assertTrue(
                        specializedCorner.defaultBlockState().isSignalSource()
                                && specializedColumn.defaultBlockState().isSignalSource()
                                && specializedCorner.defaultBlockState().getSignal(helper.getLevel(),
                                        helper.absolutePos(new BlockPos(4, 1, 2)), Direction.UP) == 15
                                && specializedColumn.defaultBlockState().getSignal(helper.getLevel(),
                                        helper.absolutePos(new BlockPos(5, 1, 2)), Direction.UP) == 15,
                        "Redstone quarter geometry omitted its power contract");
                default -> throw new IllegalStateException("Unhandled representative profile " + id);
            }
        }
        helper.succeed();
    }

    private static List<Item> expectedShapeMapSegment(NibaruMaterialProfile profile) {
        ArrayList<Item> expected = new ArrayList<>();
        addUnique(expected, profile.canonicalParent().asItem());
        profile.effectiveSlabSource().map(Block::asItem).ifPresent(item -> addUnique(expected, item));
        profile.effectiveStairSource().map(Block::asItem).ifPresent(item -> addUnique(expected, item));
        profile.nativeWall().map(Block::asItem).ifPresent(item -> addUnique(expected, item));
        addUnique(expected, NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.VERTICAL_SLAB)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.STEP)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN)
                .orElseThrow().asItem());
        return List.copyOf(expected);
    }

    private static java.util.Optional<NibaruMaterialProfile> providerProfile(Item item) {
        Block block = Block.byItem(item);
        var nativeProfile = NibaruMaterialProfiles.fromBlock(block);
        return nativeProfile.isPresent() ? nativeProfile
                : NibaruProviderAdapter.runtimeBinding(block).map(NibaruProviderAdapter.RuntimeBinding::profile);
    }

    private static void addUnique(List<Item> items, Item item) {
        if (!items.contains(item)) items.add(item);
    }

    private static List<Identifier> ids(List<Item> items) {
        return items.stream().map(BuiltInRegistries.ITEM::getKey).toList();
    }

    private static AABB expectedCorner(Direction facing, Half half) {
        double minY = half == Half.TOP ? 0.5 : 0.0;
        double maxY = minY + 0.5;
        return switch (facing) {
            case NORTH -> new AABB(0, minY, 0, 1, maxY, 0.5);
            case SOUTH -> new AABB(0, minY, 0.5, 1, maxY, 1);
            case EAST -> new AABB(0.5, minY, 0, 1, maxY, 1);
            case WEST -> new AABB(0, minY, 0, 0.5, maxY, 1);
            default -> throw new IllegalArgumentException("Corner facing must be horizontal");
        };
    }

    private static List<AABB> expectedColumn(BgeColumnBlock.Occupancy occupancy) {
        AABB nw = new AABB(0, 0, 0, 0.5, 1, 0.5);
        AABB ne = new AABB(0.5, 0, 0, 1, 1, 0.5);
        AABB sw = new AABB(0, 0, 0.5, 0.5, 1, 1);
        AABB se = new AABB(0.5, 0, 0.5, 1, 1, 1);
        return switch (occupancy) {
            case NW -> List.of(nw);
            case NE -> List.of(ne);
            case SW -> List.of(sw);
            case SE -> List.of(se);
            case NW_SE -> List.of(nw, se);
            case NE_SW -> List.of(ne, sw);
        };
    }

    private static BgeColumnBlock.Occupancy expectedGrowth(BgeColumnBlock.Occupancy occupancy,
            Direction face) {
        return switch (occupancy) {
            case NW -> face == Direction.EAST || face == Direction.SOUTH
                    ? BgeColumnBlock.Occupancy.NW_SE : null;
            case NE -> face == Direction.WEST || face == Direction.SOUTH
                    ? BgeColumnBlock.Occupancy.NE_SW : null;
            case SW -> face == Direction.EAST || face == Direction.NORTH
                    ? BgeColumnBlock.Occupancy.NE_SW : null;
            case SE -> face == Direction.WEST || face == Direction.NORTH
                    ? BgeColumnBlock.Occupancy.NW_SE : null;
            case NW_SE, NE_SW -> null;
        };
    }

    private static BgeColumnBlock.Occupancy expectedRotation(BgeColumnBlock.Occupancy occupancy,
            Rotation rotation) {
        return switch (rotation) {
            case NONE -> occupancy;
            case CLOCKWISE_90 -> switch (occupancy) {
                case NW -> BgeColumnBlock.Occupancy.NE;
                case NE -> BgeColumnBlock.Occupancy.SE;
                case SE -> BgeColumnBlock.Occupancy.SW;
                case SW -> BgeColumnBlock.Occupancy.NW;
                case NW_SE -> BgeColumnBlock.Occupancy.NE_SW;
                case NE_SW -> BgeColumnBlock.Occupancy.NW_SE;
            };
            case CLOCKWISE_180 -> switch (occupancy) {
                case NW -> BgeColumnBlock.Occupancy.SE;
                case NE -> BgeColumnBlock.Occupancy.SW;
                case SW -> BgeColumnBlock.Occupancy.NE;
                case SE -> BgeColumnBlock.Occupancy.NW;
                case NW_SE -> BgeColumnBlock.Occupancy.NW_SE;
                case NE_SW -> BgeColumnBlock.Occupancy.NE_SW;
            };
            case COUNTERCLOCKWISE_90 -> switch (occupancy) {
                case NW -> BgeColumnBlock.Occupancy.SW;
                case SW -> BgeColumnBlock.Occupancy.SE;
                case SE -> BgeColumnBlock.Occupancy.NE;
                case NE -> BgeColumnBlock.Occupancy.NW;
                case NW_SE -> BgeColumnBlock.Occupancy.NE_SW;
                case NE_SW -> BgeColumnBlock.Occupancy.NW_SE;
            };
        };
    }

    private static BgeColumnBlock.Occupancy expectedMirror(BgeColumnBlock.Occupancy occupancy,
            Mirror mirror) {
        return switch (mirror) {
            case NONE -> occupancy;
            case LEFT_RIGHT -> switch (occupancy) {
                case NW -> BgeColumnBlock.Occupancy.SW;
                case NE -> BgeColumnBlock.Occupancy.SE;
                case SW -> BgeColumnBlock.Occupancy.NW;
                case SE -> BgeColumnBlock.Occupancy.NE;
                case NW_SE -> BgeColumnBlock.Occupancy.NE_SW;
                case NE_SW -> BgeColumnBlock.Occupancy.NW_SE;
            };
            case FRONT_BACK -> switch (occupancy) {
                case NW -> BgeColumnBlock.Occupancy.NE;
                case NE -> BgeColumnBlock.Occupancy.NW;
                case SW -> BgeColumnBlock.Occupancy.SE;
                case SE -> BgeColumnBlock.Occupancy.SW;
                case NW_SE -> BgeColumnBlock.Occupancy.NE_SW;
                case NE_SW -> BgeColumnBlock.Occupancy.NW_SE;
            };
        };
    }

    private static void assertShape(GameTestHelper helper, BlockState state, BlockPos absolute,
            List<AABB> expected, String label) {
        List<AABB> actual = state.getShape(helper.getLevel(), absolute, CollisionContext.empty()).toAabbs();
        helper.assertTrue(actual.size() == expected.size()
                        && expected.stream().allMatch(box -> containsBox(actual, box)),
                label + " shape changed: expected=" + expected + ", actual=" + actual);
    }

    private static boolean containsBox(List<AABB> boxes, AABB expected) {
        return boxes.stream().anyMatch(actual -> actual.minX == expected.minX
                && actual.minY == expected.minY && actual.minZ == expected.minZ
                && actual.maxX == expected.maxX && actual.maxY == expected.maxY
                && actual.maxZ == expected.maxZ);
    }

    private static void blockAdjacentPositions(GameTestHelper helper, BlockPos target) {
        for (Direction direction : Direction.values()) {
            helper.setBlock(target.relative(direction), Blocks.STONE);
        }
    }

    private static void placeInto(GameTestHelper helper, ServerPlayer player, ItemStack stack,
            BlockPos target, Direction face) {
        helper.placeAt(player, stack, target.relative(face.getOpposite()), face);
    }

    private static void assertCanonicalDrop(GameTestHelper helper, ServerPlayer player,
            BlockState state, BlockPos pos, Block canonical, String label) {
        helper.setBlock(pos, state);
        var drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(pos),
                null, player, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(canonical.asItem())
                        && drops.getFirst().getCount() == 1,
                label + " did not return exactly one canonical source block: " + drops);
    }

    private static BgeCornerBlock corner(String parentId) {
        return corner(profile(parentId));
    }

    private static BgeCornerBlock corner(NibaruMaterialProfile profile) {
        return (BgeCornerBlock) NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER)
                .orElseThrow(() -> new IllegalStateException("Missing Corner for "
                        + profile.canonicalParentId()));
    }

    private static BgeColumnBlock column(String parentId) {
        return column(profile(parentId));
    }

    private static BgeColumnBlock column(NibaruMaterialProfile profile) {
        return (BgeColumnBlock) NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN)
                .orElseThrow(() -> new IllegalStateException("Missing Quarter Column for "
                        + profile.canonicalParentId()));
    }

    private static NibaruMaterialProfile profile(String parentId) {
        Identifier id = Identifier.parse(parentId);
        return NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.canonicalParentId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing profile " + id));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
