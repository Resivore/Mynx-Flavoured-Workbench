package dev.aero.cnmterraincompat.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.aero.cnmterraincompat.AxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialAxisSemantics;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.AxisUvPolicy;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.GeneratedBlockResources;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.Geometry;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract.VariantSelection;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.axis.AxisSlab;
import games.twinhead.moreslabsstairsandwalls.block.axis.AxisStairs;
import games.twinhead.moreslabsstairsandwalls.block.axis.AxisStrippableSlab;
import games.twinhead.moreslabsstairsandwalls.block.axis.AxisStrippableStairs;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Focused regression coverage for Nibaru-native material-axis state and generated resources. */
public final class NativeAxisGameTests implements CustomTestMethodInvoker {
    private static final List<Direction> HORIZONTAL = List.of(
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final List<Identifier> REPRESENTATIVE_PARENTS = List.of(
            id("minecraft:oak_log"),
            id("minecraft:stripped_oak_log"),
            id("minecraft:crimson_stem"),
            id("minecraft:bamboo_block"),
            id("minecraft:basalt"));

    /**
     * This is deliberately a production-pack closure audit, not a registry-count assertion.
     * Every native BGE block must have an item definition, item model, blockstate, loot table,
     * selector coverage, and recursively resolvable package-owned models.  It catches the prior
     * failure mode where Java registration succeeded but the copied upstream asset tree did not
     * contain the newly admitted material family.
     */
    @GameTest(maxTicks = 200)
    public void nativeProductionResourcesCloseEveryRegisteredFamily(GameTestHelper helper) {
        Set<Identifier> visitedModels = new LinkedHashSet<>();
        int roles = 0;
        for (ModBlocks family : ModBlocks.values()) {
            for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
                if (!family.hasBlock(type)) continue;
                var adoptedId = NibaruMaterialProfiles.externalStandardRoleId(family, type);
                if (adoptedId.isPresent()) {
                    NibaruMaterialProfile adoptedProfile = NibaruMaterialProfiles.fromFamily(family)
                            .orElseThrow();
                    Block adopted = switch (type) {
                        case SLAB -> adoptedProfile.nativeSlab().orElseThrow();
                        case STAIRS -> adoptedProfile.nativeStair().orElseThrow();
                        case WALL -> adoptedProfile.nativeWall().orElseThrow();
                    };
                    helper.assertTrue(BuiltInRegistries.BLOCK.getKey(adopted)
                                    .equals(adoptedId.orElseThrow())
                                    && !family.getId(type).equals(
                                            BuiltInRegistries.BLOCK.getKey(family.getBlock(type))),
                            "Externally superseded native role was not adopted/suppressed: "
                                    + family + "/" + type);
                    continue;
                }
                Block block = family.getBlock(type);
                Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                helper.assertTrue(blockId != null && blockId.equals(family.getId(type)),
                        "Native role identity drifted: " + family + "/" + type);
                JsonObject state = productionJson("assets/" + blockId.getNamespace()
                        + "/blockstates/" + blockId.getPath() + ".json");
                JsonObject item = productionJson("assets/" + blockId.getNamespace()
                        + "/items/" + blockId.getPath() + ".json");
                productionJson("assets/" + blockId.getNamespace() + "/models/item/"
                        + blockId.getPath() + ".json");
                JsonObject loot = productionJson("data/" + blockId.getNamespace()
                        + "/loot_table/blocks/" + blockId.getPath() + ".json");
                helper.assertTrue("minecraft:block".equals(loot.get("type").getAsString()),
                        "Native role has an invalid loot-table root: " + blockId);
                assertStateCoverage(helper, block, state, blockId);
                Set<String> referencedModels = new LinkedHashSet<>();
                collectModelReferences(state, referencedModels);
                collectModelReferences(item, referencedModels);
                helper.assertTrue(!referencedModels.isEmpty(),
                        "Native role does not resolve any item/block model: " + blockId);
                for (String model : referencedModels) {
                    Identifier modelId = Identifier.parse(model);
                    assertModelClosure(helper, modelId, visitedModels, new LinkedHashSet<>());
                }
                roles++;
            }
        }
        for (Identifier source : List.of(id("minecraft:chiseled_resin_bricks"),
                id("minecraft:chiseled_cinnabar"), id("minecraft:purpur_pillar"))) {
            NibaruMaterialProfile profile = profile(source);
            helper.assertTrue(profile.nativeSlab().isPresent() && profile.nativeStair().isPresent()
                            && profile.nativeWall().isPresent(),
                    "C81 production closure did not retain all standard roles for " + source);
        }
        NibaruMaterialProfile purpur = profile(id("minecraft:purpur_pillar"));
        Block purpurWall = purpur.nativeWall().orElseThrow();
        helper.assertTrue(!purpurWall.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                "Purpur Pillar wall acquired an illegal material AXIS state");
        helper.assertTrue("purpur_pillar_side".equals(purpur.textureRoles().side())
                        && "purpur_pillar_top".equals(purpur.textureRoles().top())
                        && "purpur_pillar_top".equals(purpur.textureRoles().bottom()),
                "Purpur Pillar did not inherit canonical cube_column side/end texture variables: "
                        + purpur.textureRoles());
        NibaruMaterialProfile quartz = profile(id("minecraft:quartz_pillar"));
        NibaruMaterialProfile oak = profile(id("minecraft:oak_log"));
        helper.assertTrue("quartz_pillar_side".equals(quartz.textureRoles().side())
                        && "quartz_pillar_top".equals(quartz.textureRoles().top())
                        && "oak_log".equals(oak.textureRoles().side())
                        && "oak_log_top".equals(oak.textureRoles().top()),
                "Canonical pillar resolver diverged between Purpur, Quartz, and a working log");
        helper.assertTrue(roles == 871 && visitedModels.size() >= roles,
                "Native production resource closure inventory drifted: roles=" + roles
                        + ", models=" + visitedModels.size());
        System.out.println("NATIVE_C93_PRODUCTION_RESOURCE_CLOSURE|roles=" + roles
                + "|models=" + visitedModels.size() + "|families=" + ModBlocks.values().length);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeAxisInventoryIsExactAndExclusive(GameTestHelper helper) {
        Set<Identifier> expected = expectedAxisParents();
        Set<Identifier> actual = new LinkedHashSet<>();
        int nativeSlabs = 0;
        int nativeStairs = 0;
        int nativeWalls = 0;
        int axisSlabs = 0;
        int axisStairs = 0;
        int nonAxisSlabs = 0;
        int nonAxisStairs = 0;

        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all().stream()
                .filter(profile -> profile.family() != null).toList()) {
            boolean axis = MaterialAxisSemantics.applies(profile);
            if (axis) actual.add(profile.canonicalParentId());

            if (profile.nativeSlab().isPresent()) {
                nativeSlabs++;
                Block slab = profile.nativeSlab().orElseThrow();
                if (axis) {
                    axisSlabs++;
                    assertAxisBlock(helper, slab, AxisSlab.class, profile, "slab");
                } else {
                    nonAxisSlabs++;
                    assertNoAxis(helper, slab, profile, "slab");
                    assertRejected(helper, () -> NativeAxisModelContract.slab(
                            profile, AxisUvPolicy.STANDARD_ROTATED), profile + " non-axis slab");
                }
            }

            if (profile.nativeStair().isPresent()) {
                nativeStairs++;
                Block stairs = profile.nativeStair().orElseThrow();
                if (axis) {
                    axisStairs++;
                    assertAxisBlock(helper, stairs, AxisStairs.class, profile, "stairs");
                } else {
                    nonAxisStairs++;
                    assertNoAxis(helper, stairs, profile, "stairs");
                    assertRejected(helper, () -> NativeAxisModelContract.stairs(
                            profile, AxisUvPolicy.STANDARD_ROTATED), profile + " non-axis stairs");
                }
            }

            if (profile.nativeWall().isPresent()) {
                nativeWalls++;
                assertNoAxis(helper, profile.nativeWall().orElseThrow(), profile, "wall");
            }
        }

        helper.assertTrue(expected.size() == 57 && actual.equals(expected),
                "Native material-axis parents changed: expected=" + expected + ", actual=" + actual);
        helper.assertTrue(axisSlabs == 57 && axisStairs == 57,
                "Expected exact 57/57 native axis slab/stair subset, found "
                        + axisSlabs + "/" + axisStairs);
        helper.assertTrue(nativeSlabs == 280 && nativeStairs == 283 && nativeWalls == 314,
                "Native geometry inventory changed: slabs=" + nativeSlabs + ", stairs=" + nativeStairs
                        + ", walls=" + nativeWalls);
        helper.assertTrue(nonAxisSlabs == 223 && nonAxisStairs == 226,
                "Non-axis native geometry exclusion changed: slabs=" + nonAxisSlabs
                        + ", stairs=" + nonAxisStairs);

        for (String path : List.of("oak_leaves", "white_glazed_terracotta", "copper_block",
                "dirt", "oak_planks")) {
            NibaruMaterialProfile special = profile(id("minecraft:" + path));
            helper.assertTrue(!MaterialAxisSemantics.applies(special),
                    "Special/non-axis family entered native axis scope: " + special.canonicalParentId());
        }
        System.out.println("NATIVE_AXIS_INVENTORY|profiles=57|slabs=57|stairs=57|walls=0"
                + "|nonAxisSlabs=222|nonAxisStairs=225|allWalls=314");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeAxisWallItemsPreserveColumnEndGrainPresentation(GameTestHelper helper) {
        int distinctEndGrainPreviews = 0;
        for (Identifier parent : expectedAxisParents()) {
            NibaruMaterialProfile profile = profile(parent);
            Block wall = profile.nativeWall().orElseThrow();
            Identifier wallId = BuiltInRegistries.BLOCK.getKey(wall);
            String resource = "assets/more_slabs_stairs_and_walls/models/block/"
                    + wallId.getPath() + "_inventory.json";
            try (InputStream input = NativeAxisGameTests.class.getClassLoader().getResourceAsStream(resource)) {
                helper.assertTrue(input != null, "Missing generated native axis wall item model: " + resource);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                Map<String, String> expected = NativeAxisModelContract.semanticTextures(profile);
                JsonObject textures = json.getAsJsonObject("textures");
                helper.assertTrue("more_slabs_stairs_and_walls:block/template_column_wall_inventory"
                                .equals(json.get("parent").getAsString())
                                && textures.size() == expected.size()
                                && expected.entrySet().stream().allMatch(entry ->
                                        textures.has(entry.getKey())
                                                && textures.get(entry.getKey()).getAsString()
                                                        .equals(entry.getValue())),
                        "Axis wall item did not preserve the column silhouette and side/end roles: "
                                + wallId + " expected=" + expected + " actual=" + textures);
                if (!expected.get("side").equals(expected.get("top"))) distinctEndGrainPreviews++;

                if (parent.equals(id("minecraft:oak_log"))) {
                    helper.assertTrue("minecraft:block/oak_log".equals(textures.get("side").getAsString())
                                    && "minecraft:block/oak_log_top".equals(
                                            textures.get("top").getAsString())
                                    && "minecraft:block/oak_log_top".equals(
                                            textures.get("bottom").getAsString())
                                    && "minecraft:block/oak_log".equals(
                                            textures.get("particle").getAsString()),
                            "Oak Log wall preview no longer exposes bark sides and end-grain caps: "
                                    + textures);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot inspect generated axis wall item model " + resource,
                        exception);
            }
        }
        helper.assertTrue(distinctEndGrainPreviews > 0,
                "Native axis wall preview audit never exercised a distinct end-grain material");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeAxisStateIsIndependentRotatableMirrorSafeAndCodecStable(GameTestHelper helper) {
        int codecStates = 0;
        for (Identifier parent : expectedAxisParents()) {
            NibaruMaterialProfile profile = profile(parent);
            for (Block block : List.of(profile.nativeSlab().orElseThrow(), profile.nativeStair().orElseThrow())) {
                for (Direction.Axis axis : Direction.Axis.values()) {
                    BlockState state = block.defaultBlockState().setValue(MaterialAxisSemantics.AXIS, axis);
                    assertCodecRoundTrip(helper, state, parent + " " + BuiltInRegistries.BLOCK.getKey(block)
                            + " axis=" + axis);
                    codecStates++;
                }
            }
        }
        helper.assertTrue(codecStates == 342, "Expected 342 native axis CODEC states, found " + codecStates);

        for (Identifier parent : REPRESENTATIVE_PARENTS) {
            NibaruMaterialProfile profile = profile(parent);
            AxisSlab slab = (AxisSlab) profile.nativeSlab().orElseThrow();
            AxisStairs stairs = (AxisStairs) profile.nativeStair().orElseThrow();

            for (Direction.Axis axis : Direction.Axis.values()) {
                BlockState slabState = slab.defaultBlockState()
                        .setValue(MaterialAxisSemantics.AXIS, axis)
                        .setValue(SlabBlock.TYPE, SlabType.TOP)
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                helper.assertTrue(slabState.getValue(MaterialAxisSemantics.AXIS) == axis
                                && slabState.getValue(SlabBlock.TYPE) == SlabType.TOP
                                && slabState.getValue(BlockStateProperties.WATERLOGGED),
                        parent + " slab material axis is not independent of type/waterlogged");

                BlockState stairState = stairs.defaultBlockState()
                        .setValue(MaterialAxisSemantics.AXIS, axis)
                        .setValue(StairBlock.FACING, Direction.EAST)
                        .setValue(StairBlock.HALF, Half.TOP)
                        .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT)
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                helper.assertTrue(stairState.getValue(MaterialAxisSemantics.AXIS) == axis
                                && stairState.getValue(StairBlock.FACING) == Direction.EAST
                                && stairState.getValue(StairBlock.HALF) == Half.TOP
                                && stairState.getValue(StairBlock.SHAPE) == StairsShape.INNER_LEFT
                                && stairState.getValue(BlockStateProperties.WATERLOGGED),
                        parent + " stair material axis is not independent of facing/half/shape/waterlogged");
                assertCodecRoundTrip(helper, slabState, parent + " rich slab state");
                assertCodecRoundTrip(helper, stairState, parent + " rich stair state");
            }

            BlockState slabState = slab.defaultBlockState()
                    .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.X)
                    .setValue(SlabBlock.TYPE, SlabType.TOP)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            BlockState slabRotated = slab.rotate(slabState, Rotation.CLOCKWISE_90);
            helper.assertTrue(slabRotated.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.Z,
                    parent + " slab quarter-turn did not rotate material X to Z");
            assertPropertiesEqual(helper, slabState, slabRotated,
                    Set.of(MaterialAxisSemantics.AXIS), parent + " rotated slab");
            BlockState slabMirrored = slabState.mirror(Mirror.FRONT_BACK);
            helper.assertTrue(slabMirrored.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.X,
                    parent + " slab mirror changed material axis");
            assertPropertiesEqual(helper, slabState, slabMirrored, Set.of(), parent + " mirrored slab");

            BlockState stairState = stairs.defaultBlockState()
                    .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.Z)
                    .setValue(StairBlock.FACING, Direction.SOUTH)
                    .setValue(StairBlock.HALF, Half.TOP)
                    .setValue(StairBlock.SHAPE, StairsShape.INNER_RIGHT)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            BlockState stairRotated = stairs.rotate(stairState, Rotation.COUNTERCLOCKWISE_90);
            helper.assertTrue(stairRotated.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.X
                            && stairRotated.getValue(StairBlock.FACING)
                            == Rotation.COUNTERCLOCKWISE_90.rotate(Direction.SOUTH),
                    parent + " stair quarter-turn did not rotate facing and material Z to X");
            assertPropertiesEqual(helper, stairState, stairRotated,
                    Set.of(MaterialAxisSemantics.AXIS, StairBlock.FACING), parent + " rotated stairs");
            BlockState stairMirrored = stairState.mirror(Mirror.LEFT_RIGHT);
            helper.assertTrue(stairMirrored.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.Z,
                    parent + " stair mirror changed material axis");
            for (Property<?> property : List.of(StairBlock.HALF, BlockStateProperties.WATERLOGGED)) {
                assertPropertyEqual(helper, stairState, stairMirrored, property, parent + " mirrored stairs");
            }
        }
        System.out.println("NATIVE_AXIS_STATE|codecStates=342|representatives=" + REPRESENTATIVE_PARENTS);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeSlabMergeAndStairShapeUpdateRetainAxis(GameTestHelper helper)
            throws ReflectiveOperationException {
        NibaruMaterialProfile oak = profile(id("minecraft:oak_log"));
        AxisSlab slab = (AxisSlab) oak.nativeSlab().orElseThrow();
        AxisStairs stairs = (AxisStairs) oak.nativeStair().orElseThrow();

        for (Direction clickedFace : Direction.values()) {
            helper.assertTrue(MaterialAxisSemantics.placementAxis(
                            Blocks.AIR.defaultBlockState(), slab, clickedFace) == clickedFace.getAxis(),
                    "New native slab placement did not follow clicked face " + clickedFace);
        }
        for (Direction.Axis retained : Direction.Axis.values()) {
            BlockState existing = slab.defaultBlockState()
                    .setValue(MaterialAxisSemantics.AXIS, retained)
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            for (Direction clickedFace : Direction.values()) {
                helper.assertTrue(MaterialAxisSemantics.placementAxis(existing, slab, clickedFace) == retained,
                        "Native slab merge replaced retained " + retained + " from face " + clickedFace);
            }
        }
        BlockState otherPillar = Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
        helper.assertTrue(MaterialAxisSemantics.placementAxis(otherPillar, slab, Direction.EAST)
                        == Direction.Axis.X,
                "Native slab merge helper retained axis from a different block");

        BlockPos origin = new BlockPos(2, 1, 2);
        BlockState current = stairs.defaultBlockState()
                .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.Z)
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        int shaped = 0;
        for (Direction neighborDirection : HORIZONTAL) {
            for (Direction neighborFacing : HORIZONTAL) {
                for (Direction direction : HORIZONTAL) helper.setBlock(origin.relative(direction), Blocks.AIR);
                helper.setBlock(origin, current);
                BlockPos neighbor = origin.relative(neighborDirection);
                BlockState neighborState = stairs.defaultBlockState()
                        .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.X)
                        .setValue(StairBlock.FACING, neighborFacing)
                        .setValue(StairBlock.HALF, Half.BOTTOM)
                        .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
                helper.setBlock(neighbor, neighborState);
                BlockState updated = invokeUpdateShape(stairs, current, helper,
                        origin, neighborDirection, neighbor, neighborState);
                helper.assertTrue(updated.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.Z,
                        "Native stair neighbor shape update lost material axis for neighbor="
                                + neighborDirection + "/" + neighborFacing);
                if (updated.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT) shaped++;
            }
        }
        helper.assertTrue(shaped > 0,
                "Native stair shape-update matrix never exercised a corner transition");
        System.out.println("NATIVE_AXIS_PLACEMENT|slabMerge=PASS|stairCornerUpdates=" + shaped);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeStrippingMappingsAreExactAndStateSafe(GameTestHelper helper)
            throws ReflectiveOperationException {
        Map<Identifier, Identifier> expected = expectedStrippingMappings();
        Set<Identifier> actualSources = new LinkedHashSet<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            if (profile.family() != null
                    && profile.capabilities().contains(BehaviorCapability.STRIPPABLE)) {
                actualSources.add(profile.canonicalParentId());
            }
        }
        helper.assertTrue(actualSources.equals(expected.keySet()),
                "Exact 23 native stripping sources changed: expected=" + expected.keySet()
                        + ", actual=" + actualSources);

        int mappings = 0;
        for (Map.Entry<Identifier, Identifier> entry : expected.entrySet()) {
            NibaruMaterialProfile source = profile(entry.getKey());
            MaterialTransition transition = source.transition(MaterialTransition.Type.STRIPPED).orElseThrow();
            NibaruMaterialProfile target = NibaruMaterialProfiles.fromFamily(transition.target()).orElseThrow();
            helper.assertTrue(target.canonicalParentId().equals(entry.getValue()),
                    "Native stripping target changed for " + entry.getKey() + ": expected="
                            + entry.getValue() + ", actual=" + target.canonicalParentId());

            Block sourceSlab = source.nativeSlab().orElseThrow();
            Block sourceStairs = source.nativeStair().orElseThrow();
            Block targetSlab = target.nativeSlab().orElseThrow();
            Block targetStairs = target.nativeStair().orElseThrow();
            helper.assertTrue(sourceSlab instanceof AxisStrippableSlab
                            && sourceStairs instanceof AxisStrippableStairs,
                    "Strippable axis source has wrong native classes: " + entry.getKey());
            helper.assertTrue(targetSlab instanceof AxisSlab && !(targetSlab instanceof AxisStrippableSlab)
                            && targetStairs instanceof AxisStairs
                            && !(targetStairs instanceof AxisStrippableStairs),
                    "Stripped target has wrong native axis classes: " + entry.getValue());
            helper.assertTrue(strippedFamily(sourceSlab) == transition.target()
                            && strippedFamily(sourceStairs) == transition.target(),
                    "Native stripping block binding disagrees with profile transition for " + entry.getKey());

            BlockState slabState = sourceSlab.defaultBlockState()
                    .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.Z)
                    .setValue(SlabBlock.TYPE, SlabType.TOP)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            BlockState strippedSlab = PathSemantics.copySharedProperties(
                    slabState, targetSlab.defaultBlockState());
            helper.assertTrue(strippedSlab.is(targetSlab),
                    "Slab stripping did not bind exact target for " + entry.getKey());
            assertPropertiesEqual(helper, slabState, strippedSlab, Set.of(), entry.getKey() + " slab strip");

            BlockState stairState = sourceStairs.defaultBlockState()
                    .setValue(MaterialAxisSemantics.AXIS, Direction.Axis.X)
                    .setValue(StairBlock.FACING, Direction.EAST)
                    .setValue(StairBlock.HALF, Half.TOP)
                    .setValue(StairBlock.SHAPE, StairsShape.INNER_RIGHT)
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            BlockState strippedStairs = PathSemantics.copySharedProperties(
                    stairState, targetStairs.defaultBlockState());
            helper.assertTrue(strippedStairs.is(targetStairs),
                    "Stair stripping did not bind exact target for " + entry.getKey());
            assertPropertiesEqual(helper, stairState, strippedStairs, Set.of(), entry.getKey() + " stair strip");
            mappings++;
        }
        helper.assertTrue(mappings == 23, "Expected 23 native stripping mappings, found " + mappings);
        System.out.println("NATIVE_AXIS_STRIPPING|mappings=23|geometries=46");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nativeAxisModelSelectorsCloseAndGeometryIsExhaustive(GameTestHelper helper) {
        assertRuntimeStairGeometry(helper);
        int textureProfiles = 0;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            if (profile.family() == null || !MaterialAxisSemantics.applies(profile)) continue;
            helper.assertTrue(NativeAxisModelContract.semanticTextures(profile)
                            .equals(AxisModelContract.semanticTextures(profile)),
                    "Provider-native texture roles diverged from the canonical profile contract for "
                            + profile.canonicalParentId());
            textureProfiles++;
        }
        helper.assertTrue(textureProfiles == 57,
                "Expected exact 57 native texture-role profiles, found " + textureProfiles);
        int slabSelectors = 0;
        int stairSelectors = 0;
        for (AxisUvPolicy policy : AxisUvPolicy.values()) {
            NibaruMaterialProfile profile = representativePolicyProfile(policy);
            GeneratedBlockResources slabs = NativeAxisModelContract.slab(profile, policy);
            GeneratedBlockResources stairs = NativeAxisModelContract.stairs(profile, policy);
            assertResourceClosure(helper, slabs, profile, profile.nativeSlabId().orElseThrow(),
                    "minecraft:block/block", 9, "slab " + policy);
            assertResourceClosure(helper, stairs, profile, profile.nativeStairId().orElseThrow(),
                    "minecraft:block/stairs", 120, "stairs " + policy);

            String slabBase = modelId(profile.nativeSlabId().orElseThrow());
            String stairBase = modelId(profile.nativeStairId().orElseThrow());
            helper.assertTrue(slabs.models().containsKey(slabBase)
                            && slabs.selectors().get(NativeAxisModelContract.slabVariantKey(
                            SlabType.BOTTOM, Direction.Axis.Y)).model().equals(slabBase),
                    policy + " native slab base/item model is not reserved for bottom/Y");
            helper.assertTrue(stairs.models().containsKey(stairBase)
                            && stairs.selectors().get(NativeAxisModelContract.stairVariantKey(
                            Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT,
                            Direction.Axis.Y)).model().equals(stairBase),
                    policy + " native stair base/item model is not reserved for east/bottom/straight/Y");

            for (SlabType type : SlabType.values()) {
                for (Direction.Axis axis : Direction.Axis.values()) {
                    String key = NativeAxisModelContract.slabVariantKey(type, axis);
                    assertGeneratedState(helper, slabs, key,
                            NativeAxisModelContract.slabGeometry(type), policy, axis,
                            "slab " + type + "/" + axis);
                    slabSelectors++;
                }
            }

            for (Direction facing : HORIZONTAL) {
                for (Half half : Half.values()) {
                    for (StairsShape shape : StairsShape.values()) {
                        for (Direction.Axis axis : Direction.Axis.values()) {
                            String key = NativeAxisModelContract.stairVariantKey(facing, half, shape, axis);
                            assertGeneratedState(helper, stairs, key,
                                    NativeAxisModelContract.stairGeometry(facing, half, shape),
                                    policy, axis, "stairs " + facing + "/" + half + "/" + shape + "/" + axis);
                            stairSelectors++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(slabSelectors == 27 && stairSelectors == 360,
                "Exhaustive native selector matrix changed: slabs=" + slabSelectors
                        + ", stairs=" + stairSelectors);
        System.out.println("NATIVE_AXIS_MODELS|policies=3|textureProfiles=57"
                + "|slabSelectors=27|stairSelectors=360"
                + "|geometry=PASS|faces=PASS|closure=PASS");
        helper.succeed();
    }

    private static void assertRuntimeStairGeometry(GameTestHelper helper) {
        AxisStairs oakStairs = (AxisStairs) profile(id("minecraft:oak_log"))
                .nativeStair().orElseThrow();
        BlockPos absolute = helper.absolutePos(new BlockPos(2, 1, 2));
        var context = net.minecraft.world.phys.shapes.CollisionContext.empty();
        int states = 0;
        for (Direction facing : HORIZONTAL) {
            for (Half half : Half.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    Geometry expected = NativeAxisModelContract.stairGeometry(facing, half, shape);
                    net.minecraft.world.phys.shapes.VoxelShape expectedShape =
                            net.minecraft.world.phys.shapes.Shapes.empty();
                    for (NativeAxisModelContract.Cuboid cuboid : expected.cuboids()) {
                        int[] bounds = cuboid.bounds();
                        expectedShape = net.minecraft.world.phys.shapes.Shapes.or(expectedShape,
                                net.minecraft.world.phys.shapes.Shapes.box(
                                        bounds[0] / 16.0, bounds[1] / 16.0, bounds[2] / 16.0,
                                        bounds[3] / 16.0, bounds[4] / 16.0, bounds[5] / 16.0));
                    }
                    for (Direction.Axis axis : Direction.Axis.values()) {
                        BlockState state = oakStairs.defaultBlockState()
                                .setValue(StairBlock.FACING, facing)
                                .setValue(StairBlock.HALF, half)
                                .setValue(StairBlock.SHAPE, shape)
                                .setValue(MaterialAxisSemantics.AXIS, axis);
                        var actualShape = state.getShape(helper.getLevel(), absolute, context);
                        helper.assertTrue(net.minecraft.world.phys.shapes.Shapes.equal(expectedShape, actualShape),
                                "Native stair contract disagrees with registered Oak Log runtime shape for "
                                        + facing + "/" + half + "/" + shape + "/axis=" + axis
                                        + ": contract=" + expectedShape.toAabbs()
                                        + ", runtime=" + actualShape.toAabbs());
                    }
                    states++;
                }
            }
        }
        helper.assertTrue(states == 40, "Expected exact 40 native runtime stair geometry states, found " + states);
    }

    private static void assertAxisBlock(GameTestHelper helper, Block block, Class<?> expectedClass,
            NibaruMaterialProfile profile, String geometry) {
        BlockState state = block.defaultBlockState();
        helper.assertTrue(expectedClass.isInstance(block),
                "Axis " + geometry + " has wrong class for " + profile.canonicalParentId()
                        + ": " + block.getClass().getName());
        helper.assertTrue(state.hasProperty(MaterialAxisSemantics.AXIS)
                        && state.getValue(MaterialAxisSemantics.AXIS) == Direction.Axis.Y,
                "Axis " + geometry + " does not expose default Y for " + profile.canonicalParentId());
        for (Direction.Axis axis : Direction.Axis.values()) {
            helper.assertTrue(state.setValue(MaterialAxisSemantics.AXIS, axis)
                            .getValue(MaterialAxisSemantics.AXIS) == axis,
                    "Axis " + geometry + " cannot represent " + axis + " for " + profile.canonicalParentId());
        }
    }

    private static void assertNoAxis(GameTestHelper helper, Block block,
            NibaruMaterialProfile profile, String geometry) {
        helper.assertTrue(!block.defaultBlockState().hasProperty(MaterialAxisSemantics.AXIS),
                "Excluded " + geometry + " gained material axis for " + profile.canonicalParentId()
                        + ": " + BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void assertCodecRoundTrip(GameTestHelper helper, BlockState state, String label) {
        JsonElement encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result()
                .orElseThrow(() -> new IllegalStateException("BlockState.CODEC could not encode " + label));
        BlockState decoded = BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).result()
                .orElseThrow(() -> new IllegalStateException("BlockState.CODEC could not decode " + label));
        helper.assertTrue(decoded.equals(state),
                "BlockState.CODEC roundtrip changed " + label + ": encoded=" + encoded
                        + ", decoded=" + decoded);
    }

    private static BlockState invokeUpdateShape(AxisStairs stairs, BlockState state,
            GameTestHelper helper, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState) throws ReflectiveOperationException {
        Method update = null;
        Class<?> type = stairs.getClass();
        while (type != null && update == null) {
            update = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.getName().equals("updateShape")
                            && method.getParameterCount() == 8
                            && method.getParameterTypes()[0] == BlockState.class)
                    .findFirst().orElse(null);
            type = type.getSuperclass();
        }
        if (update == null) throw new NoSuchMethodException("Native stairs updateShape");
        update.setAccessible(true);
        return (BlockState) update.invoke(stairs, state, helper.getLevel(), helper.getLevel(),
                helper.absolutePos(pos), direction, helper.absolutePos(neighborPos), neighborState,
                RandomSource.create(0x4E41544956454158L));
    }

    private static ModBlocks strippedFamily(Block block) throws ReflectiveOperationException {
        Field field = block.getClass().getDeclaredField("strippedBlock");
        field.setAccessible(true);
        return (ModBlocks) field.get(block);
    }

    private static void assertResourceClosure(GameTestHelper helper, GeneratedBlockResources resources,
            NibaruMaterialProfile profile, Identifier blockId, String expectedParent,
            int expectedSelectors, String label) {
        helper.assertTrue(resources.selectors().size() == expectedSelectors,
                label + " selector count changed: " + resources.selectors().size());
        JsonObject variants = resources.blockState().getAsJsonObject("variants");
        helper.assertTrue(variants.size() == expectedSelectors,
                label + " encoded variant count changed: " + variants.size());
        Set<String> referenced = new LinkedHashSet<>();
        for (Map.Entry<String, VariantSelection> entry : resources.selectors().entrySet()) {
            VariantSelection selection = entry.getValue();
            helper.assertTrue(variants.getAsJsonObject(entry.getKey()).equals(selection.json()),
                    label + " blockstate JSON changed selector " + entry.getKey());
            helper.assertTrue(!selection.json().has("uvlock"),
                    label + " selector introduced blockstate uvlock: " + entry.getKey());
            helper.assertTrue(resources.models().containsKey(selection.model()),
                    label + " selector references missing model " + entry.getKey() + " -> " + selection.model());
            Identifier resource = NativeAxisModelContract.modelResource(selection.model());
            helper.assertTrue(resource.getNamespace().equals(blockId.getNamespace())
                            && resource.getPath().startsWith("models/block/" + blockId.getPath()),
                    label + " model escaped provider block path: " + resource);
            referenced.add(selection.model());
        }
        helper.assertTrue(referenced.equals(resources.models().keySet()),
                label + " generated unreferenced models or dangling selectors: referenced="
                        + referenced + ", models=" + resources.models().keySet());
        Map<String, String> semanticTextures = AxisModelContract.semanticTextures(profile);
        for (Map.Entry<String, JsonObject> entry : resources.models().entrySet()) {
            JsonObject model = entry.getValue();
            helper.assertTrue(model.get("parent").getAsString().equals(expectedParent),
                    label + " model changed native parent: " + entry.getKey() + " -> " + model.get("parent"));
            JsonObject textures = model.getAsJsonObject("textures");
            for (Map.Entry<String, String> texture : semanticTextures.entrySet()) {
                helper.assertTrue(textures.has(texture.getKey())
                                && textures.get(texture.getKey()).getAsString().equals(texture.getValue()),
                        label + " model changed canonical semantic texture " + texture.getKey()
                                + ": " + entry.getKey() + " -> " + textures);
            }
        }
        Identifier blockState = NativeAxisModelContract.blockStateResource(blockId);
        helper.assertTrue(blockState.getNamespace().equals(blockId.getNamespace())
                        && blockState.getPath().equals("blockstates/" + blockId.getPath() + ".json"),
                label + " blockstate resource escaped exact provider ID: " + blockState);
    }

    private static void assertGeneratedState(GameTestHelper helper, GeneratedBlockResources resources,
            String key, Geometry expected, AxisUvPolicy policy, Direction.Axis axis, String label) {
        VariantSelection selection = resources.selectors().get(key);
        helper.assertTrue(selection != null, policy + " missing native selector " + key);
        JsonObject model = resources.models().get(selection.model());
        helper.assertTrue(model != null, policy + " selector did not close over a model: " + key);

        int expectedX = policy != AxisUvPolicy.DIRECT_UV_LOCKED && axis != Direction.Axis.Y ? 90 : 0;
        int expectedY = policy != AxisUvPolicy.DIRECT_UV_LOCKED && axis == Direction.Axis.X ? 90 : 0;
        helper.assertTrue(selection.x() == expectedX && selection.y() == expectedY,
                policy + " " + label + " material transform changed: expected="
                        + expectedX + "/" + expectedY + ", actual=" + selection.x() + "/" + selection.y());

        JsonArray elements = model.getAsJsonArray("elements");
        helper.assertTrue(elements != null && elements.size() == expected.cuboids().size(),
                policy + " " + label + " cuboid count changed: expected=" + expected.cuboids().size()
                        + ", actual=" + (elements == null ? -1 : elements.size()));
        Set<Set<Direction>> actualGeometry = modelGeometry(model, selection, axis);
        Set<Set<Direction>> expectedGeometry = geometrySignature(expected);
        helper.assertTrue(actualGeometry.equals(expectedGeometry),
                policy + " " + label + " inverse/world geometry changed: expected="
                        + expectedGeometry + ", actual=" + actualGeometry);
        assertModelFaces(helper, model, policy, axis, label);
    }

    private static Set<Set<Direction>> modelGeometry(JsonObject model,
            VariantSelection selection, Direction.Axis axis) {
        Set<Set<Direction>> result = new LinkedHashSet<>();
        for (JsonElement encoded : model.getAsJsonArray("elements")) {
            JsonObject element = encoded.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            EnumSet<Direction> modelCuboid = EnumSet.noneOf(Direction.class);
            addHalfSpace(modelCuboid, from.get(0).getAsInt(), to.get(0).getAsInt(),
                    Direction.WEST, Direction.EAST);
            addHalfSpace(modelCuboid, from.get(1).getAsInt(), to.get(1).getAsInt(),
                    Direction.DOWN, Direction.UP);
            addHalfSpace(modelCuboid, from.get(2).getAsInt(), to.get(2).getAsInt(),
                    Direction.NORTH, Direction.SOUTH);
            EnumSet<Direction> worldCuboid = EnumSet.noneOf(Direction.class);
            for (Direction direction : modelCuboid) {
                worldCuboid.add(selection.x() == 0 && selection.y() == 0
                        ? direction : AxisModelContract.materialRotation(axis).rotate(direction));
            }
            result.add(Set.copyOf(worldCuboid));
        }
        return Set.copyOf(result);
    }

    private static Set<Set<Direction>> geometrySignature(Geometry geometry) {
        Set<Set<Direction>> result = new LinkedHashSet<>();
        geometry.cuboids().forEach(cuboid -> result.add(Set.copyOf(cuboid.halfSpaces())));
        return Set.copyOf(result);
    }

    private static void addHalfSpace(Set<Direction> target, int from, int to,
            Direction negative, Direction positive) {
        if (from == 0 && to == 8) target.add(negative);
        else if (from == 8 && to == 16) target.add(positive);
        else if (from != 0 || to != 16) {
            throw new IllegalStateException("Non-canonical native cuboid interval [" + from + "," + to + "]");
        }
    }

    private static void assertModelFaces(GameTestHelper helper, JsonObject model,
            AxisUvPolicy policy, Direction.Axis axis, String label) {
        for (JsonElement encoded : model.getAsJsonArray("elements")) {
            JsonObject element = encoded.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            JsonObject faces = element.getAsJsonObject("faces");
            for (Direction face : Direction.values()) {
                JsonObject actual = faces.getAsJsonObject(face.getSerializedName());
                String expectedTexture = expectedTexture(policy, axis, face);
                int expectedRotation = expectedFaceRotation(policy, axis, face);
                int actualRotation = actual.has("rotation") ? actual.get("rotation").getAsInt() : 0;
                helper.assertTrue(actual.get("texture").getAsString().equals(expectedTexture)
                                && actualRotation == expectedRotation,
                        policy + " " + label + " face policy changed for " + face
                                + ": expected=" + expectedTexture + "/" + expectedRotation
                                + ", actual=" + actual);
                boolean boundary = liesOnBoundary(from, to, face);
                helper.assertTrue(actual.has("cullface") == boundary,
                        policy + " " + label + " cullface boundary changed for " + face + ": " + actual);
                if (boundary) {
                    helper.assertTrue(actual.get("cullface").getAsString().equals(face.getSerializedName()),
                            policy + " " + label + " cullface points at wrong face: " + actual);
                }
            }
        }
    }

    private static String expectedTexture(AxisUvPolicy policy, Direction.Axis axis, Direction face) {
        if (policy != AxisUvPolicy.DIRECT_UV_LOCKED && axis != Direction.Axis.Y) {
            return switch (face) {
                case UP -> "#top";
                case DOWN -> "#bottom";
                default -> "#side";
            };
        }
        if (face.getAxis() != axis) return "#side";
        return switch (face) {
            case EAST, UP, SOUTH -> "#top";
            case WEST, DOWN, NORTH -> "#bottom";
        };
    }

    private static int expectedFaceRotation(AxisUvPolicy policy, Direction.Axis axis, Direction face) {
        if (policy == AxisUvPolicy.HORIZONTAL_ROTATED && axis != Direction.Axis.Y) {
            return face == Direction.UP ? 180 : 0;
        }
        if (policy != AxisUvPolicy.DIRECT_UV_LOCKED || axis == Direction.Axis.Y) return 0;
        return switch (axis) {
            case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
            case Y -> 0;
            case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
        };
    }

    private static boolean liesOnBoundary(JsonArray from, JsonArray to, Direction face) {
        return switch (face) {
            case WEST -> from.get(0).getAsInt() == 0;
            case DOWN -> from.get(1).getAsInt() == 0;
            case NORTH -> from.get(2).getAsInt() == 0;
            case EAST -> to.get(0).getAsInt() == 16;
            case UP -> to.get(1).getAsInt() == 16;
            case SOUTH -> to.get(2).getAsInt() == 16;
        };
    }

    private static void assertPropertiesEqual(GameTestHelper helper, BlockState expected,
            BlockState actual, Set<Property<?>> excluded, String label) {
        expected.getProperties().stream().filter(actual::hasProperty).filter(property -> !excluded.contains(property))
                .forEach(property -> assertPropertyEqual(helper, expected, actual, property, label));
    }

    private static void assertPropertyEqual(GameTestHelper helper,
            BlockState expected, BlockState actual, Property<?> property, String label) {
        helper.assertTrue(expected.getValue(property).equals(actual.getValue(property)),
                label + " changed " + property.getName() + ": expected=" + expected.getValue(property)
                        + ", actual=" + actual.getValue(property));
    }

    private static void assertRejected(GameTestHelper helper, Runnable action, String label) {
        boolean rejected = false;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Native axis contract accepted " + label);
    }

    private static JsonObject productionJson(String resource) {
        try (InputStream input = NativeAxisGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing production resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read production resource " + resource, exception);
        }
    }

    private static void assertStateCoverage(GameTestHelper helper, Block block, JsonObject state,
            Identifier blockId) {
        if (state.has("multipart")) {
            JsonArray parts = state.getAsJsonArray("multipart");
            helper.assertTrue(parts != null && !parts.isEmpty(),
                    "Multipart blockstate has no placed-state topology: " + blockId);
            return;
        }
        JsonObject variants = state.getAsJsonObject("variants");
        helper.assertTrue(variants != null && !variants.entrySet().isEmpty(),
                "Blockstate has neither variants nor multipart topology: " + blockId);
        for (BlockState candidate : block.getStateDefinition().getPossibleStates()) {
            boolean covered = variants.entrySet().stream().anyMatch(entry -> selectorMatches(candidate, entry.getKey()));
            helper.assertTrue(covered, "Blockstate selector does not cover " + candidate + " for " + blockId);
        }
    }

    private static boolean selectorMatches(BlockState state, String selector) {
        if (selector.isEmpty()) return true;
        Map<String, String> expected = new HashMap<>();
        for (String term : selector.split(",")) {
            String[] pair = term.split("=", 2);
            if (pair.length != 2) return false;
            expected.put(pair[0], pair[1]);
        }
        for (Map.Entry<String, String> term : expected.entrySet()) {
            Property<?> property = state.getProperties().stream()
                    .filter(candidate -> candidate.getName().equals(term.getKey()))
                    .findFirst().orElse(null);
            if (property == null || !propertyValue(state, property).equals(term.getValue())) return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String propertyValue(BlockState state, Property<?> property) {
        Property raw = property;
        return raw.getName(state.getValue(raw));
    }

    private static void collectModelReferences(JsonElement json, Set<String> target) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(value -> collectModelReferences(value, target));
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("model") && object.get("model").isJsonPrimitive()
                    && object.get("model").getAsJsonPrimitive().isString()) {
                String model = object.get("model").getAsString();
                if (model.contains(":")) target.add(model);
            }
            object.entrySet().forEach(entry -> collectModelReferences(entry.getValue(), target));
        }
    }

    private static void assertModelClosure(GameTestHelper helper, Identifier model,
            Set<Identifier> visited, Set<Identifier> visiting) {
        if (!model.getNamespace().equals(NativeAxisModelContract.PROVIDER_NAMESPACE)) return;
        helper.assertTrue(visiting.add(model), "Cyclic package-owned model parent chain: " + model);
        JsonObject json = productionJson("assets/" + model.getNamespace() + "/models/"
                + model.getPath() + ".json");
        JsonObject textures = json.getAsJsonObject("textures");
        if (textures != null) {
            for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                helper.assertTrue(texture.getValue().isJsonPrimitive()
                                && texture.getValue().getAsJsonPrimitive().isString(),
                        "Model has a non-string texture reference: " + model + " " + texture);
            }
        }
        if (json.has("parent")) {
            helper.assertTrue(json.get("parent").isJsonPrimitive()
                            && json.get("parent").getAsJsonPrimitive().isString(),
                    "Model parent is not a resource identifier: " + model);
            Identifier parent = Identifier.parse(json.get("parent").getAsString());
            assertModelClosure(helper, parent, visited, visiting);
        }
        visiting.remove(model);
        visited.add(model);
    }

    private static NibaruMaterialProfile representativePolicyProfile(AxisUvPolicy policy) {
        return profile(switch (policy) {
            case STANDARD_ROTATED -> id("minecraft:crimson_stem");
            case HORIZONTAL_ROTATED -> id("minecraft:oak_log");
            case DIRECT_UV_LOCKED -> id("minecraft:bamboo_block");
        });
    }

    private static NibaruMaterialProfile profile(Identifier parent) {
        return NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.canonicalParentId().equals(parent))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing Nibaru profile " + parent));
    }

    private static Set<Identifier> expectedAxisParents() {
        LinkedHashSet<Identifier> expected = new LinkedHashSet<>(expectedStrippingMappings().keySet());
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "pale_oak", "mangrove", "cherry")) {
            addMinecraftIds(expected, "stripped_" + wood + "_log", "stripped_" + wood + "_wood");
        }
        for (String fungus : List.of("crimson", "warped")) {
            addMinecraftIds(expected, "stripped_" + fungus + "_stem", "stripped_" + fungus + "_hyphae");
        }
        addMinecraftIds(expected, "stripped_bamboo_block", "basalt", "polished_basalt", "bone_block",
                "deepslate", "hay_block", "muddy_mangrove_roots", "quartz_pillar", "ochre_froglight",
                "verdant_froglight", "pearlescent_froglight", "purpur_pillar");
        return Collections.unmodifiableSet(expected);
    }

    private static Map<Identifier, Identifier> expectedStrippingMappings() {
        LinkedHashMap<Identifier, Identifier> expected = new LinkedHashMap<>();
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "pale_oak", "mangrove", "cherry")) {
            addStrip(expected, wood + "_log", "stripped_" + wood + "_log");
            addStrip(expected, wood + "_wood", "stripped_" + wood + "_wood");
        }
        for (String fungus : List.of("crimson", "warped")) {
            addStrip(expected, fungus + "_stem", "stripped_" + fungus + "_stem");
            addStrip(expected, fungus + "_hyphae", "stripped_" + fungus + "_hyphae");
        }
        addStrip(expected, "bamboo_block", "stripped_bamboo_block");
        return Collections.unmodifiableMap(expected);
    }

    private static void addStrip(Map<Identifier, Identifier> target, String source, String stripped) {
        target.put(id("minecraft:" + source), id("minecraft:" + stripped));
    }

    private static void addMinecraftIds(Set<Identifier> target, String... paths) {
        for (String path : paths) target.add(id("minecraft:" + path));
    }

    private static String modelId(Identifier blockId) {
        return blockId.getNamespace() + ":block/" + blockId.getPath();
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
