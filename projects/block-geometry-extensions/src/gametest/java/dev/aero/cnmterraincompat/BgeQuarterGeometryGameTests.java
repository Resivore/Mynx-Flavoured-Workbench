package dev.aero.cnmterraincompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
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
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Focused catalog, physical, economy, and material-contract coverage for C56 quarter geometry. */
public final class BgeQuarterGeometryGameTests implements CustomTestMethodInvoker {
    private static final BlockPos SHAPE_POS = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 40)
    public void catalogBindingsAndShapeMapOrderAreExact(GameTestHelper helper) {
        List<NibaruMaterialProfile> profiles = NibaruMaterialProfiles.all();
        int expectedProfiles = 311 + (int) ExternalMaterialFamilies.all().stream()
                .filter(binding -> binding.profile().family() == null).count();
        helper.assertTrue(profiles.size() == expectedProfiles,
                "Expected " + expectedProfiles + " canonical material profiles, found " + profiles.size());

        List<BgeGeometryCatalog.Descriptor> catalog = BgeGeometryCatalog.ordered();
        helper.assertTrue(catalog.stream().map(entry -> entry.key().getPath()).toList().equals(
                            List.of("vertical_slab", "step", "corner", "quarter_column", "layer"))
                        && catalog.stream().map(BgeGeometryCatalog.Descriptor::persistenceId).toList()
                                .equals(List.of(4, 5, 7, 8, 6))
                        && catalog.stream().map(BgeGeometryCatalog.Descriptor::selectorOrder).toList()
                                .equals(List.of(4, 5, 6, 7, 8))
                        && BgeGeometryCatalog.byPersistenceId(6).orElseThrow().role()
                                == BgeGeometryRole.LAYER
                        && BgeGeometryCatalog.byPersistenceId(7).orElseThrow().role()
                                == BgeGeometryRole.CORNER
                        && BgeGeometryCatalog.byKey(Identifier.fromNamespaceAndPath(
                                CnmTerrainCompat.MOD_ID, "quarter_column")).orElseThrow().persistenceId() == 8,
                "Stable BGE consumer catalog identity/order changed: " + catalog);

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
                    "Provider ShapeMap segment is not parent/native/Vertical/Step/Corner/Column/Layer for "
                            + profile.canonicalParentId() + ": expected=" + ids(expectedSegment)
                            + ", actual=" + ids(actualSegment));
            helper.assertTrue(catalog.stream().allMatch(entry -> entry.isAvailable(profile)
                            && entry.resolve(profile).orElseThrow() == entry.resolveBlock(profile).orElseThrow()
                            && entry.resolveItem(profile).orElseThrow()
                                    == entry.resolveBlock(profile).orElseThrow().asItem()),
                    "Stable BGE catalog did not resolve exact block/item identities for "
                            + profile.canonicalParentId());
        }

        long cornerTraits = NibaruProviderAdapter.localMaterialTraits().stream()
                .filter(trait -> trait.role() == BgeGeometryRole.CORNER
                        && trait.fuelDivisor() == 1 && corners.contains(trait.derived())).count();
        long columnTraits = NibaruProviderAdapter.localMaterialTraits().stream()
                .filter(trait -> trait.role() == BgeGeometryRole.QUARTER_COLUMN
                        && trait.fuelDivisor() == 4 && columns.contains(trait.derived())).count();
        helper.assertTrue(corners.size() == expectedProfiles && columns.size() == expectedProfiles
                        && allQuarterGeometry.size() == expectedProfiles * 2
                        && cornerTraits == expectedProfiles && columnTraits == expectedProfiles,
                "Quarter geometry population/trait count mismatch: corners=" + corners.size()
                        + ", columns=" + columns.size() + ", total=" + allQuarterGeometry.size()
                        + ", traits=" + cornerTraits + "/" + columnTraits);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void localFuelEconomyMatchesReferenceCompleteness(GameTestHelper helper) {
        BgeCornerBlock corner = corner("minecraft:oak_planks");
        BgeColumnBlock column = column("minecraft:oak_planks");
        int parentDuration = helper.getLevel().fuelValues()
                .burnDuration(new ItemStack(Blocks.OAK_PLANKS));
        int cornerDuration = helper.getLevel().fuelValues()
                .burnDuration(new ItemStack(corner));
        int columnDuration = helper.getLevel().fuelValues()
                .burnDuration(new ItemStack(column));

        helper.assertTrue(parentDuration > 0
                        && cornerDuration == parentDuration
                        && columnDuration == parentDuration / 4,
                "Local fuel inheritance does not match Vertical Stairs/Quarter Column contracts: parent="
                        + parentDuration + ", corner=" + cornerDuration
                        + ", column=" + columnDuration);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cornerHasFourFullHeightOrientationsAndLoadsLegacyCardinalStates(GameTestHelper helper) {
        BgeCornerBlock corner = corner("minecraft:stone");
        helper.assertTrue(corner.getStateDefinition().getProperties().size() == 2
                        && corner.getStateDefinition().getPossibleStates().size() == 8
                        && corner.getStateDefinition().getProperty("half") == null,
                "Vertical Stairs state space is not four cardinal carriers x waterlogged without HALF");

        BlockPos absolute = helper.absolutePos(SHAPE_POS);
        int checked = 0;
        for (BgeCornerBlock.Orientation orientation : BgeCornerBlock.Orientation.values()) {
            BlockState state = corner.defaultBlockState()
                    .setValue(BgeCornerBlock.FACING, orientation.stateFacing())
                    .setValue(BgeCornerBlock.WATERLOGGED, true);
            List<AABB> expected = expectedCorner(orientation, 1.0);
            assertShape(helper, state, absolute, expected, "Vertical Stairs " + orientation);
            helper.assertTrue(shapeVolume(state.getShape(helper.getLevel(), absolute,
                                    CollisionContext.empty()).toAabbs()) == 0.75,
                    "Vertical Stairs is not exactly full cube minus one quarter for " + orientation);
            for (Rotation rotation : Rotation.values()) {
                BlockState transformed = corner.rotate(state, rotation);
                helper.assertTrue(BgeCornerBlock.orientation(transformed) == orientation.rotate(rotation)
                                && transformed.getValue(BgeCornerBlock.WATERLOGGED),
                        "Vertical Stairs rotation changed geometry/water for " + orientation
                                + "/" + rotation + ": " + transformed);
            }
            for (Mirror mirror : Mirror.values()) {
                BlockState transformed = corner.mirror(state, mirror);
                helper.assertTrue(BgeCornerBlock.orientation(transformed) == orientation.mirror(mirror)
                                && transformed.getValue(BgeCornerBlock.WATERLOGGED),
                        "Vertical Stairs mirror changed geometry/water for " + orientation
                                + "/" + mirror + ": " + transformed);
            }
            assertLegacyCornerDecode(helper, state, "top");
            assertLegacyCornerDecode(helper, state.setValue(BgeCornerBlock.WATERLOGGED, false), "bottom");
            checked++;
        }
        helper.assertTrue(BgeCornerBlock.Orientation.fromHit(0.25, 0.25)
                                == BgeCornerBlock.Orientation.NORTH_WEST
                        && BgeCornerBlock.Orientation.fromHit(0.75, 0.25)
                                == BgeCornerBlock.Orientation.NORTH_EAST
                        && BgeCornerBlock.Orientation.fromHit(0.25, 0.75)
                                == BgeCornerBlock.Orientation.SOUTH_WEST
                        && BgeCornerBlock.Orientation.fromHit(0.75, 0.75)
                                == BgeCornerBlock.Orientation.SOUTH_EAST,
                "Vertical Stairs placement does not map the four horizontal hit quadrants exactly");
        helper.assertTrue(checked == 4,
                "Expected exactly four Vertical Stairs physical orientations, checked " + checked);
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
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(BgeCornerBlock.WATERLOGGED, true);
        BlockState columnAxis = logColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW)
                .setValue(MaterialAxisState.AXIS, Direction.Axis.X)
                .setValue(BgeColumnBlock.WATERLOGGED, true);
        BlockState rotatedCornerAxis = logCorner.rotate(cornerAxis, Rotation.CLOCKWISE_90);
        BlockState rotatedColumnAxis = logColumn.rotate(columnAxis, Rotation.CLOCKWISE_90);
        helper.assertTrue(rotatedCornerAxis.getValue(BgeCornerBlock.FACING) == Direction.EAST
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

    @GameTest(maxTicks = 40)
    public void cornerMaterialCollisionErodesEveryHoneyBoundary(GameTestHelper helper) {
        BgeCornerBlock ordinary = corner("minecraft:stone");
        BgeCornerBlock soul = corner("minecraft:soul_sand");
        BgeCornerBlock honey = corner("minecraft:honey_block");
        BlockPos absolute = helper.absolutePos(SHAPE_POS);
        for (BgeCornerBlock.Orientation orientation : BgeCornerBlock.Orientation.values()) {
            BlockState ordinaryState = ordinary.defaultBlockState()
                    .setValue(BgeCornerBlock.FACING, orientation.stateFacing());
            BlockState soulState = soul.defaultBlockState()
                    .setValue(BgeCornerBlock.FACING, orientation.stateFacing());
            BlockState honeyState = honey.defaultBlockState()
                    .setValue(BgeCornerBlock.FACING, orientation.stateFacing());
            assertCollisionShape(helper, ordinaryState, absolute, expectedCorner(orientation, 1.0),
                    "ordinary Vertical Stairs " + orientation);
            assertCollisionShape(helper, soulState, absolute, expectedCorner(orientation, 14.0 / 16.0),
                    "Soul Sand Vertical Stairs " + orientation);
            assertCollisionShape(helper, honeyState, absolute, expectedHoneyCorner(orientation),
                    "Honey Vertical Stairs " + orientation);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void geometryAwareSpreadableExposureUsesOnlyOwnedTopFootprints(GameTestHelper helper) {
        BgeCornerBlock grassCorner = corner("minecraft:grass_block");
        BgeCornerBlock dirtCorner = corner("minecraft:dirt");
        BgeColumnBlock grassColumn = column("minecraft:grass_block");
        BgeColumnBlock dirtColumn = column("minecraft:dirt");
        Block grassVertical = NibaruProviderAdapter.derived(profile("minecraft:grass_block"),
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        Block dirtVertical = NibaruProviderAdapter.derived(profile("minecraft:dirt"),
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB).orElseThrow();
        helper.assertTrue(grassCorner instanceof SpreadableGeometry
                        && grassColumn instanceof SpreadableGeometry
                        && grassVertical instanceof SpreadableGeometry,
                "Grass Corner/Column/Vertical Slab do not share the geometry exposure contract");

        BlockPos cornerPos = new BlockPos(1, 2, 1);
        BlockState drySouthWest = grassCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING,
                        BgeCornerBlock.Orientation.SOUTH_WEST.stateFacing())
                .setValue(BgeCornerBlock.WATERLOGGED, false);
        BlockState wetSouthWest = grassCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING,
                        BgeCornerBlock.Orientation.SOUTH_WEST.stateFacing())
                .setValue(BgeCornerBlock.WATERLOGGED, true);
        helper.setBlock(cornerPos, drySouthWest);
        helper.setBlock(cornerPos.above(), Blocks.AIR);
        GeometrySpreadableBehavior.randomTick(drySouthWest, helper.getLevel(),
                helper.absolutePos(cornerPos), RandomSource.create(0x43524E445259L));
        helper.assertTrue(helper.getBlockState(cornerPos).equals(drySouthWest),
                "Dry top-exposed Vertical Stairs did not survive its actual random-tick lifecycle");

        helper.setBlock(cornerPos, wetSouthWest);
        helper.setBlock(cornerPos.above(), Blocks.AIR);
        assertExposure(helper, wetSouthWest, cornerPos, SpreadableGeometry.Exposure.EXPOSED,
                "self-waterlogged Vertical Stairs with air above");
        helper.setBlock(cornerPos.above(), Blocks.WATER);
        assertExposure(helper, wetSouthWest, cornerPos, SpreadableGeometry.Exposure.BLOCKED,
                "Vertical Stairs under actual water");
        GeometrySpreadableBehavior.randomTick(wetSouthWest, helper.getLevel(),
                helper.absolutePos(cornerPos), RandomSource.create(0x43524E574154L));
        BlockState revertedCorner = helper.getBlockState(cornerPos);
        helper.assertTrue(revertedCorner.is(dirtCorner)
                        && revertedCorner.getValue(BgeCornerBlock.FACING)
                                == wetSouthWest.getValue(BgeCornerBlock.FACING)
                        && revertedCorner.getValue(BgeCornerBlock.WATERLOGGED),
                "Water-covered Vertical Stairs did not actually revert to typed Dirt with state intact");
        helper.setBlock(cornerPos, wetSouthWest);
        helper.setBlock(cornerPos.above(), Blocks.STONE);
        assertExposure(helper, wetSouthWest, cornerPos, SpreadableGeometry.Exposure.BLOCKED,
                "Vertical Stairs under a solid full block");
        helper.setBlock(cornerPos.above(), Blocks.SNOW.defaultBlockState());
        assertExposure(helper, wetSouthWest, cornerPos, SpreadableGeometry.Exposure.EXPOSED,
                "Vertical Stairs under vanilla one-layer snow");

        BlockPos columnPos = new BlockPos(4, 2, 1);
        BlockState grassNorthWest = grassColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW)
                .setValue(BgeColumnBlock.WATERLOGGED, true);
        helper.setBlock(columnPos, grassNorthWest);
        helper.setBlock(columnPos.above(), dirtColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.SE));
        assertExposure(helper, grassNorthWest, columnPos, SpreadableGeometry.Exposure.EXPOSED,
                "Quarter Column below non-overlapping partial coverage");
        helper.setBlock(columnPos.above(), dirtColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NW));
        assertExposure(helper, grassNorthWest, columnPos, SpreadableGeometry.Exposure.BLOCKED,
                "Quarter Column below exact partial coverage");
        GeometrySpreadableBehavior.randomTick(grassNorthWest, helper.getLevel(),
                helper.absolutePos(columnPos), RandomSource.create(0x434F4C534F4CL));
        BlockState revertedColumn = helper.getBlockState(columnPos);
        helper.assertTrue(revertedColumn.is(dirtColumn)
                        && revertedColumn.getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NW
                        && revertedColumn.getValue(BgeColumnBlock.WATERLOGGED),
                "Solid-covered Quarter Column did not actually revert to typed Dirt with state intact");

        BlockPos verticalPos = new BlockPos(7, 2, 1);
        BlockState wetNorthVertical = grassVertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(VerticalSlabBlock.WATERLOGGED, true);
        helper.setBlock(verticalPos, wetNorthVertical);
        helper.setBlock(verticalPos.above(), Blocks.AIR);
        assertExposure(helper, wetNorthVertical, verticalPos, SpreadableGeometry.Exposure.EXPOSED,
                "accepted Vertical Slab self-waterlogged oracle");
        helper.setBlock(verticalPos.above(), dirtVertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.SOUTH)
                .setValue(VerticalSlabBlock.DOUBLE, false));
        assertExposure(helper, wetNorthVertical, verticalPos, SpreadableGeometry.Exposure.EXPOSED,
                "Vertical Slab below non-overlapping partial coverage");
        helper.setBlock(verticalPos.above(), dirtVertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                .setValue(VerticalSlabBlock.DOUBLE, false));
        assertExposure(helper, wetNorthVertical, verticalPos, SpreadableGeometry.Exposure.BLOCKED,
                "Vertical Slab below exact partial coverage");

        BlockPos spreadTarget = new BlockPos(10, 2, 1);
        BlockState wetDirt = dirtCorner.defaultBlockState()
                .setValue(BgeCornerBlock.FACING, Direction.EAST)
                .setValue(BgeCornerBlock.WATERLOGGED, true);
        helper.setBlock(spreadTarget, wetDirt);
        helper.setBlock(spreadTarget.above(), Blocks.AIR);
        helper.assertTrue(GeometrySpreadableBehavior.trySpread(helper.getLevel(),
                                Blocks.GRASS_BLOCK, helper.absolutePos(spreadTarget))
                        && helper.getBlockState(spreadTarget).is(grassCorner)
                        && helper.getBlockState(spreadTarget).getValue(BgeCornerBlock.FACING)
                                == Direction.EAST
                        && helper.getBlockState(spreadTarget).getValue(BgeCornerBlock.WATERLOGGED),
                "Geometry-aware spread did not preserve Vertical Stairs orientation/waterlogging");

        BgeColumnBlock myceliumColumn = column("minecraft:mycelium");
        BlockPos analogousTarget = new BlockPos(10, 2, 4);
        BlockState diagonalDirt = dirtColumn.defaultBlockState()
                .setValue(BgeColumnBlock.OCCUPANCY, BgeColumnBlock.Occupancy.NE_SW)
                .setValue(BgeColumnBlock.WATERLOGGED, true);
        helper.setBlock(analogousTarget, diagonalDirt);
        helper.setBlock(analogousTarget.above(), Blocks.AIR);
        helper.assertTrue(myceliumColumn instanceof SpreadableGeometry
                        && GeometrySpreadableBehavior.trySpread(helper.getLevel(), Blocks.MYCELIUM,
                                helper.absolutePos(analogousTarget))
                        && helper.getBlockState(analogousTarget).is(myceliumColumn)
                        && helper.getBlockState(analogousTarget).getValue(BgeColumnBlock.OCCUPANCY)
                                == BgeColumnBlock.Occupancy.NE_SW
                        && helper.getBlockState(analogousTarget).getValue(BgeColumnBlock.WATERLOGGED),
                "Analogous Mycelium typed spread did not share geometry-aware survival/state transfer");

        Block dirtStep = NibaruProviderAdapter.derived(profile("minecraft:dirt"),
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        Block grassStep = NibaruProviderAdapter.derived(profile("minecraft:grass_block"),
                DerivedGeometrySupport.Geometry.STEP).orElseThrow();
        BlockPos legacyStepTarget = new BlockPos(13, 2, 4);
        BlockState dryBottomStep = dirtStep.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false);
        helper.setBlock(legacyStepTarget, dryBottomStep);
        helper.setBlock(legacyStepTarget.above(), Blocks.WATER);
        helper.assertTrue(grassVertical instanceof GeometryAwareSpreadable
                        && !(grassStep instanceof GeometryAwareSpreadable)
                        && !GeometrySpreadableBehavior.trySpread(helper.getLevel(), Blocks.GRASS_BLOCK,
                                helper.absolutePos(legacyStepTarget))
                        && helper.getBlockState(legacyStepTarget).equals(dryBottomStep),
                "Exact-footprint exemption leaked into accepted Step water-above spreading");
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
        addUnique(expected, NibaruProviderAdapter.derived(profile, BgeGeometryRole.CORNER)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, BgeGeometryRole.QUARTER_COLUMN)
                .orElseThrow().asItem());
        addUnique(expected, NibaruProviderAdapter.derived(profile, DerivedGeometrySupport.Geometry.LAYER)
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

    private static List<AABB> expectedCorner(BgeCornerBlock.Orientation orientation, double maxY) {
        return switch (orientation) {
            case SOUTH_WEST -> List.of(
                    new AABB(0, 0, 0, 0.5, maxY, 0.5),
                    new AABB(0, 0, 0.5, 1, maxY, 1));
            case NORTH_WEST -> List.of(
                    new AABB(0, 0, 0, 1, maxY, 0.5),
                    new AABB(0, 0, 0.5, 0.5, maxY, 1));
            case NORTH_EAST -> List.of(
                    new AABB(0, 0, 0, 1, maxY, 0.5),
                    new AABB(0.5, 0, 0.5, 1, maxY, 1));
            case SOUTH_EAST -> List.of(
                    new AABB(0.5, 0, 0, 1, maxY, 0.5),
                    new AABB(0, 0, 0.5, 1, maxY, 1));
        };
    }

    private static List<AABB> expectedHoneyCorner(BgeCornerBlock.Orientation orientation) {
        double one = 1.0 / 16.0;
        double seven = 7.0 / 16.0;
        double nine = 9.0 / 16.0;
        double fifteen = 15.0 / 16.0;
        return switch (orientation) {
            case SOUTH_WEST -> List.of(
                    new AABB(one, 0, one, seven, fifteen, fifteen),
                    new AABB(seven, 0, nine, fifteen, fifteen, fifteen));
            case NORTH_WEST -> List.of(
                    new AABB(one, 0, one, seven, fifteen, fifteen),
                    new AABB(seven, 0, one, fifteen, fifteen, seven));
            case NORTH_EAST -> List.of(
                    new AABB(nine, 0, one, fifteen, fifteen, fifteen),
                    new AABB(one, 0, one, nine, fifteen, seven));
            case SOUTH_EAST -> List.of(
                    new AABB(nine, 0, one, fifteen, fifteen, fifteen),
                    new AABB(one, 0, nine, nine, fifteen, fifteen));
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
        VoxelShape actual = state.getShape(helper.getLevel(), absolute, CollisionContext.empty());
        VoxelShape expectedShape = shape(expected);
        helper.assertTrue(!Shapes.joinIsNotEmpty(actual, expectedShape, BooleanOp.NOT_SAME)
                        && shapeVolume(actual.toAabbs()) == shapeVolume(expected),
                label + " shape changed: expected=" + expected + ", actual=" + actual.toAabbs());
    }

    private static void assertCollisionShape(GameTestHelper helper, BlockState state,
            BlockPos absolute, List<AABB> expected, String label) {
        VoxelShape actual = state.getCollisionShape(
                helper.getLevel(), absolute, CollisionContext.empty());
        VoxelShape expectedShape = shape(expected);
        helper.assertTrue(!Shapes.joinIsNotEmpty(actual, expectedShape, BooleanOp.NOT_SAME)
                        && shapeVolume(actual.toAabbs()) == shapeVolume(expected),
                label + " collision changed: expected=" + expected + ", actual=" + actual.toAabbs());
    }

    private static void assertExposure(GameTestHelper helper, BlockState state, BlockPos pos,
            SpreadableGeometry.Exposure expected, String label) {
        SpreadableGeometry geometry = (SpreadableGeometry) state.getBlock();
        SpreadableGeometry.Exposure actual = geometry.spreadableExposure(
                state, helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(actual == expected,
                label + " exposure changed: expected=" + expected + ", actual=" + actual);
    }

    private static void assertLegacyCornerDecode(GameTestHelper helper,
            BlockState current, String legacyHalf) {
        JsonElement encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, current).result()
                .orElseThrow(() -> new IllegalStateException("Could not encode current Corner state"));
        JsonObject legacy = encoded.getAsJsonObject().deepCopy();
        legacy.getAsJsonObject("Properties").addProperty("half", legacyHalf);
        BlockState decoded = BlockState.CODEC.parse(JsonOps.INSTANCE, legacy).result()
                .orElseThrow(() -> new IllegalStateException(
                        "C55 Corner palette state no longer decodes: " + legacy));
        helper.assertTrue(decoded.equals(current),
                "Legacy C55 HALF was not safely ignored while preserving cardinal/water state: "
                        + legacy + " -> " + decoded);
    }

    private static double shapeVolume(List<AABB> boxes) {
        return boxes.stream().mapToDouble(box -> (box.maxX - box.minX)
                * (box.maxY - box.minY) * (box.maxZ - box.minZ)).sum();
    }

    private static VoxelShape shape(List<AABB> boxes) {
        VoxelShape result = Shapes.empty();
        for (AABB box : boxes) {
            result = Shapes.or(result, Block.box(box.minX * 16, box.minY * 16, box.minZ * 16,
                    box.maxX * 16, box.maxY * 16, box.maxZ * 16));
        }
        return result.optimize();
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
