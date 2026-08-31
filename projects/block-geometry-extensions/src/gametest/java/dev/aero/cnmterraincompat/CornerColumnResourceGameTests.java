package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.aero.cnmterraincompat.client.CornerColumnModelProjection;
import dev.aero.cnmterraincompat.client.CornerColumnModelProjection.ColumnOccupancy;
import dev.aero.cnmterraincompat.client.CuboidListModelProjection;
import dev.aero.cnmterraincompat.client.LayerModelProjection;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.Half;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Focused C55 ordinary-JSON coverage for Corner/Column material projection. */
public final class CornerColumnResourceGameTests implements CustomTestMethodInvoker {
    private static final Identifier CORNER = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/test_corner");
    private static final Identifier COLUMN = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/test_quarter_column");
    private static final List<Direction> HORIZONTAL = List.of(
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final List<Half> HALVES = List.of(Half.BOTTOM, Half.TOP);
    private static final List<Direction.Axis> AXES = List.of(
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z);
    private static final List<String> OCCUPANCIES = List.of(
            "north_west", "north_east", "south_west", "south_east",
            "north_west_south_east", "north_east_south_west");

    @GameTest(maxTicks = 40)
    public void selectorAndModelClosureIsExactAndDeterministic(GameTestHelper helper) {
        NibaruMaterialProfile stone = profile("minecraft:stone");
        CornerColumnModelProjection.Projection corner =
                CornerColumnModelProjection.projectCorner(stone, CORNER);
        CornerColumnModelProjection.Projection cornerRepeat =
                CornerColumnModelProjection.projectCorner(stone, CORNER);
        List<String> cornerSelectors = new ArrayList<>();
        List<String> cornerModels = new ArrayList<>();
        for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
            cornerSelectors.add("facing=" + facing.getSerializedName()
                    + ",half=" + half.getSerializedName());
            cornerModels.add(modelId(CORNER, "_" + facing.getSerializedName()
                    + "_" + half.getSerializedName()));
        }
        cornerModels.add(modelId(CORNER, "_item"));
        assertExactProjection(helper, corner, cornerRepeat, cornerSelectors, cornerModels,
                "ordinary Corner");
        helper.assertTrue(CornerColumnModelProjection.cornerSelectorCount(stone) == 8,
                "Ordinary Corner selector-count contract changed");

        CornerColumnModelProjection.Projection column =
                CornerColumnModelProjection.projectColumn(stone, COLUMN);
        CornerColumnModelProjection.Projection columnRepeat =
                CornerColumnModelProjection.projectColumn(stone, COLUMN);
        List<String> columnSelectors = OCCUPANCIES.stream()
                .map(occupancy -> "occupancy=" + occupancy).toList();
        List<String> columnModels = new ArrayList<>(OCCUPANCIES.stream()
                .map(occupancy -> modelId(COLUMN, "_" + occupancy)).toList());
        columnModels.add(modelId(COLUMN, "_item"));
        assertExactProjection(helper, column, columnRepeat, columnSelectors, columnModels,
                "ordinary Quarter Column");
        helper.assertTrue(CornerColumnModelProjection.columnSelectorCount(stone) == 6,
                "Ordinary Quarter Column selector-count contract changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void worldBoundsAndDedicatedItemCuboidsAreExact(GameTestHelper helper) {
        NibaruMaterialProfile stone = profile("minecraft:stone");
        CornerColumnModelProjection.Projection corner =
                CornerColumnModelProjection.projectCorner(stone, CORNER);
        List<CuboidListModelProjection.Bounds> expectedCorners = List.of(
                bounds(0, 0, 0, 16, 8, 8),
                bounds(0, 8, 0, 16, 16, 8),
                bounds(8, 0, 0, 16, 8, 16),
                bounds(8, 8, 0, 16, 16, 16),
                bounds(0, 0, 8, 16, 8, 16),
                bounds(0, 8, 8, 16, 16, 16),
                bounds(0, 0, 0, 8, 8, 16),
                bounds(0, 8, 0, 8, 16, 16));
        int cornerIndex = 0;
        for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
            String key = "facing=" + facing.getSerializedName() + ",half=" + half.getSerializedName();
            JsonObject model = selected(corner, key);
            assertElements(helper, model, List.of(expectedCorners.get(cornerIndex)),
                    "Corner " + key);
            helper.assertTrue(CornerColumnModelProjection.cornerBounds(facing, half)
                            .equals(expectedCorners.get(cornerIndex)),
                    "Public Corner bounds disagreed with its model for " + key);
            cornerIndex++;
        }

        CornerColumnModelProjection.Projection column =
                CornerColumnModelProjection.projectColumn(stone, COLUMN);
        List<List<CuboidListModelProjection.Bounds>> expectedColumns = List.of(
                List.of(bounds(0, 0, 0, 8, 16, 8)),
                List.of(bounds(8, 0, 0, 16, 16, 8)),
                List.of(bounds(0, 0, 8, 8, 16, 16)),
                List.of(bounds(8, 0, 8, 16, 16, 16)),
                List.of(bounds(0, 0, 0, 8, 16, 8),
                        bounds(8, 0, 8, 16, 16, 16)),
                List.of(bounds(8, 0, 0, 16, 16, 8),
                        bounds(0, 0, 8, 8, 16, 16)));
        for (int i = 0; i < OCCUPANCIES.size(); i++) {
            assertElements(helper, selected(column, "occupancy=" + OCCUPANCIES.get(i)),
                    expectedColumns.get(i), "Quarter Column " + OCCUPANCIES.get(i));
            helper.assertTrue(ColumnOccupancy.values()[i].bounds().equals(expectedColumns.get(i)),
                    "Public Quarter Column bounds disagreed with its model for " + OCCUPANCIES.get(i));
        }

        JsonObject cornerItem = corner.models().get(corner.itemModel());
        JsonObject columnItem = column.models().get(column.itemModel());
        assertElements(helper, cornerItem, List.of(bounds(0, 4, 4, 16, 12, 12)),
                "centered Corner item");
        assertElements(helper, columnItem, List.of(bounds(4, 0, 4, 12, 16, 12)),
                "centered Quarter Column item");
        helper.assertTrue(cornerItem.has("display") && columnItem.has("display"),
                "Corner/Column dedicated item models omitted their display transforms");
        helper.assertTrue(!selectorModels(corner).contains(corner.itemModel())
                        && !selectorModels(column).contains(column.itemModel()),
                "Dedicated centered item geometry leaked into a placed-world selector");

        JsonObject cornerItemFaces = firstFaces(cornerItem);
        JsonObject columnItemFaces = firstFaces(columnItem);
        assertNumbers(helper, cornerItemFaces.getAsJsonObject("up").getAsJsonArray("uv"),
                0, 8, 16, 16);
        assertNumbers(helper, columnItemFaces.getAsJsonObject("north").getAsJsonArray("uv"),
                0, 0, 8, 16);
        helper.assertTrue(!cornerItemFaces.getAsJsonObject("up").has("cullface")
                        && !columnItemFaces.getAsJsonObject("north").has("cullface"),
                "Item-only cuboids retained world-boundary culling");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void grassColumnKeepsWorldFaceRolesAndUprightSideUvs(GameTestHelper helper) {
        CornerColumnModelProjection.Projection grass = CornerColumnModelProjection.projectColumn(
                profile("minecraft:grass_block"), COLUMN);
        JsonObject single = selected(grass, "occupancy=south_east");
        JsonObject textures = single.getAsJsonObject("textures");
        helper.assertTrue(textures.get("top").getAsString().equals("minecraft:block/grass_block_top")
                        && textures.get("side").getAsString().equals("minecraft:block/grass_block_side")
                        && textures.get("bottom").getAsString().equals("minecraft:block/dirt")
                        && textures.get("overlay").getAsString()
                                .equals("minecraft:block/grass_block_side_overlay"),
                "Grass Quarter Column lost canonical TOP/SIDE/BOTTOM/overlay texture roles");
        helper.assertTrue(single.get("render_type").getAsString().equals("cutout")
                        && single.getAsJsonArray("elements").size() == 2,
                "Grass Quarter Column lost inherited cutout/overlay composition");
        assertGrassCuboid(helper, single, 0, 1, bounds(8, 0, 8, 16, 16, 16),
                "south-east single");

        JsonObject diagonal = selected(grass, "occupancy=north_west_south_east");
        helper.assertTrue(diagonal.getAsJsonArray("elements").size() == 4,
                "Diagonal Grass Quarter Column did not project one base plus overlay per cuboid");
        assertGrassCuboid(helper, diagonal, 0, 1, bounds(0, 0, 0, 8, 16, 8),
                "north-west diagonal member");
        assertGrassCuboid(helper, diagonal, 2, 3, bounds(8, 0, 8, 16, 16, 16),
                "south-east diagonal member");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void axisAndGlazedFramesRemainIndependentOfGeometry(GameTestHelper helper) {
        NibaruMaterialProfile log = profile("minecraft:oak_log");
        CornerColumnModelProjection.Projection axisColumn =
                CornerColumnModelProjection.projectColumn(
                        log, COLUMN, AxisUvPolicy.HORIZONTAL_ROTATED);
        List<String> axisColumnSelectors = new ArrayList<>();
        for (String occupancy : OCCUPANCIES) for (Direction.Axis axis : AXES) {
            axisColumnSelectors.add("occupancy=" + occupancy + ",axis=" + axisName(axis));
        }
        assertKeys(helper, axisColumn.blockState().getAsJsonObject("variants"),
                axisColumnSelectors, "axis Quarter Column selectors");
        assertClosure(helper, axisColumn, 18, 19, "axis Quarter Column");
        helper.assertTrue(CornerColumnModelProjection.columnSelectorCount(log) == 18,
                "Axis Quarter Column selector-count contract changed");
        assertAxisState(helper, axisColumn, "occupancy=south_east,axis=x",
                AxisUvPolicy.HORIZONTAL_ROTATED, Direction.Axis.X,
                List.of(bounds(8, 0, 8, 16, 16, 16)),
                "oak-log X-axis south-east Quarter Column");

        CornerColumnModelProjection.Projection axisCorner =
                CornerColumnModelProjection.projectCorner(
                        log, CORNER, AxisUvPolicy.HORIZONTAL_ROTATED);
        List<String> axisCornerSelectors = new ArrayList<>();
        for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
            for (Direction.Axis axis : AXES) {
                axisCornerSelectors.add("facing=" + facing.getSerializedName()
                        + ",half=" + half.getSerializedName() + ",axis=" + axisName(axis));
            }
        }
        assertKeys(helper, axisCorner.blockState().getAsJsonObject("variants"),
                axisCornerSelectors, "axis Corner selectors");
        assertClosure(helper, axisCorner, 24, 25, "axis Corner");
        assertAxisState(helper, axisCorner, "facing=north,half=top,axis=z",
                AxisUvPolicy.HORIZONTAL_ROTATED, Direction.Axis.Z,
                List.of(bounds(0, 8, 0, 16, 16, 8)),
                "oak-log Z-axis north/top Corner");

        NibaruMaterialProfile glazed = profile("minecraft:white_glazed_terracotta");
        CornerColumnModelProjection.Projection glazedColumn =
                CornerColumnModelProjection.projectColumn(glazed, COLUMN);
        List<String> glazedColumnSelectors = new ArrayList<>();
        for (String occupancy : OCCUPANCIES) for (Direction pattern : HORIZONTAL) {
            glazedColumnSelectors.add("occupancy=" + occupancy + ",pattern_facing="
                    + pattern.getSerializedName());
        }
        assertKeys(helper, glazedColumn.blockState().getAsJsonObject("variants"),
                glazedColumnSelectors, "glazed Quarter Column selectors");
        assertClosure(helper, glazedColumn, 24, 7, "glazed Quarter Column");
        helper.assertTrue(CornerColumnModelProjection.columnSelectorCount(glazed) == 24,
                "Glazed Quarter Column selector-count contract changed");
        JsonObject glazedColumnVariant = glazedColumn.blockState().getAsJsonObject("variants")
                .getAsJsonObject("occupancy=north_west,pattern_facing=south");
        helper.assertTrue(glazedColumnVariant.get("model").getAsString()
                                .endsWith("_relative_south_west_glazed")
                        && glazedColumnVariant.get("y").getAsInt() == 90
                        && !glazedColumnVariant.get("uvlock").getAsBoolean(),
                "Glazed Quarter Column coupled its physical occupancy to its pattern frame");
        assertElements(helper, glazedColumn.models().get(
                        glazedColumnVariant.get("model").getAsString()),
                List.of(bounds(0, 0, 8, 8, 16, 16)),
                "relative glazed Quarter Column model");

        CornerColumnModelProjection.Projection glazedCorner =
                CornerColumnModelProjection.projectCorner(glazed, CORNER);
        List<String> glazedCornerSelectors = new ArrayList<>();
        for (Direction facing : HORIZONTAL) for (Half half : HALVES) {
            for (Direction pattern : HORIZONTAL) {
                glazedCornerSelectors.add("facing=" + facing.getSerializedName()
                        + ",half=" + half.getSerializedName() + ",pattern_facing="
                        + pattern.getSerializedName());
            }
        }
        assertKeys(helper, glazedCorner.blockState().getAsJsonObject("variants"),
                glazedCornerSelectors, "glazed Corner selectors");
        assertClosure(helper, glazedCorner, 32, 9, "glazed Corner");
        JsonObject glazedCornerVariant = glazedCorner.blockState().getAsJsonObject("variants")
                .getAsJsonObject("facing=north,half=bottom,pattern_facing=south");
        helper.assertTrue(glazedCornerVariant.get("model").getAsString()
                                .endsWith("_relative_west_bottom_glazed")
                        && glazedCornerVariant.get("y").getAsInt() == 90
                        && !glazedCornerVariant.get("uvlock").getAsBoolean(),
                "Glazed Corner coupled its physical facing to its pattern frame");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allCanonicalAxisPoliciesPreserveWorldGeometryAndMaterialFrames(
            GameTestHelper helper) {
        assertAxisPolicy(helper, profile("minecraft:crimson_stem"),
                AxisUvPolicy.STANDARD_ROTATED, "crimson stem");
        assertAxisPolicy(helper, profile("minecraft:oak_log"),
                AxisUvPolicy.HORIZONTAL_ROTATED, "oak log");
        assertAxisPolicy(helper, profile("minecraft:bamboo_block"),
                AxisUvPolicy.DIRECT_UV_LOCKED, "bamboo block");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void tintGlassOverlayAndPathProfilesRetainTypedProjection(GameTestHelper helper) {
        CornerColumnModelProjection.Projection leaves = CornerColumnModelProjection.projectCorner(
                profile("minecraft:oak_leaves"), CORNER);
        JsonObject leafModel = selected(leaves, "facing=south,half=bottom");
        helper.assertTrue(leafModel.get("render_type").getAsString().equals("cutout_mipped")
                        && firstFaces(leafModel).entrySet().stream().allMatch(entry ->
                                entry.getValue().getAsJsonObject().get("tintindex").getAsInt() == 0),
                "Leaves Corner lost inherited tint or cutout render type");

        CornerColumnModelProjection.Projection glass = CornerColumnModelProjection.projectColumn(
                profile("minecraft:glass"), COLUMN);
        JsonObject glassModel = selected(glass, "occupancy=south_east");
        JsonArray glassElements = glassModel.getAsJsonArray("elements");
        helper.assertTrue(glassModel.get("render_type").getAsString().equals("translucent")
                        && glassElements.size() == 4,
                "Two-axis cut Glass Quarter Column lost typed four-cell edge projection");
        JsonObject cutCorner = glassElements.get(0).getAsJsonObject();
        assertElement(helper, cutCorner, bounds(8, 0, 8, 9, 16, 9),
                "Glass Quarter Column cut-corner cell");
        JsonObject cutFaces = cutCorner.getAsJsonObject("faces");
        helper.assertTrue(cutFaces.has("north") && cutFaces.has("west")
                        && !cutFaces.getAsJsonObject("north").has("cullface")
                        && !cutFaces.getAsJsonObject("west").has("cullface")
                        && cutFaces.getAsJsonObject("up").get("cullface").getAsString().equals("up")
                        && cutFaces.getAsJsonObject("down").get("cullface").getAsString().equals("down"),
                "Glass Quarter Column treated cut rims as full-block boundaries");
        assertNumbers(helper, cutFaces.getAsJsonObject("north").getAsJsonArray("uv"),
                15, 0, 16, 16);
        assertNumbers(helper, cutFaces.getAsJsonObject("west").getAsJsonArray("uv"),
                0, 0, 1, 16);

        CornerColumnModelProjection.Projection pathCorner = CornerColumnModelProjection.projectCorner(
                profile("minecraft:dirt_path"), CORNER);
        assertElements(helper, selected(pathCorner, "facing=north,half=bottom"),
                List.of(bounds(0, 0, 0, 16, 7, 8)), "bottom Dirt Path Corner");
        assertElements(helper, selected(pathCorner, "facing=north,half=top"),
                List.of(bounds(0, 7, 0, 16, 15, 8)), "top Dirt Path Corner");
        CornerColumnModelProjection.Projection pathColumn = CornerColumnModelProjection.projectColumn(
                profile("minecraft:dirt_path"), COLUMN);
        assertElements(helper, selected(pathColumn, "occupancy=south_east"),
                List.of(bounds(8, 0, 8, 16, 15, 16)), "Dirt Path Quarter Column");

        JsonObject grassCorner = selected(CornerColumnModelProjection.projectCorner(
                profile("minecraft:grass_block"), CORNER), "facing=east,half=bottom");
        JsonObject grassOverlay = grassCorner.getAsJsonArray("elements").get(1).getAsJsonObject();
        helper.assertTrue(grassCorner.getAsJsonArray("elements").size() == 2
                        && grassOverlay.getAsJsonObject("faces").size() == 4
                        && grassOverlay.getAsJsonObject("faces").entrySet().stream().allMatch(entry -> {
                            JsonObject face = entry.getValue().getAsJsonObject();
                            JsonArray uv = face.getAsJsonArray("uv");
                            return face.get("texture").getAsString().equals("#overlay")
                                    && face.get("tintindex").getAsInt() == 0
                                    && uv.get(1).getAsInt() == 0 && uv.get(3).getAsInt() == 8;
                        }),
                "Grass Corner lost its tinted canonical top-band side overlay");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void layerGuiCorrectionDoesNotMovePlacedWorldBounds(GameTestHelper helper) {
        LayerModelProjection.Projection layer = LayerModelProjection.project(
                profile("minecraft:stone"), Identifier.parse(
                        "cnm_terrain_slabs_compat:minecraft/test_layer"), true);
        JsonObject oneUp = selected(layer, "facing=up,layers=1");
        assertLayerElement(helper, oneUp, 0, 0, 0, 16, 4, 16,
                "one-up placed Layer");
        JsonObject item = layer.models().get(layer.itemModel());
        assertLayerElement(helper, item, 0, 0, 0, 16, 4, 16,
                "one-up Layer item target");
        JsonArray translation = item.getAsJsonObject("display").getAsJsonObject("gui")
                .getAsJsonArray("translation");
        double c54X = -1.325;
        double c54Y = 3.25;
        helper.assertTrue(translation.get(0).getAsDouble() == 1.675
                        && translation.get(1).getAsDouble() == -2.75
                        && translation.get(2).getAsDouble() == 0.0
                        && translation.get(0).getAsDouble() - c54X == 3.0
                        && translation.get(1).getAsDouble() - c54Y == -6.0,
                "Layer GUI did not apply exactly +3px right/+6px down from C54: " + translation);

        helper.assertTrue(LayerModelProjection.bounds(Direction.UP, 1).equals(
                                new LayerModelProjection.Bounds(0, 0, 0, 16, 4, 16))
                        && LayerModelProjection.bounds(Direction.DOWN, 1).equals(
                                new LayerModelProjection.Bounds(0, 12, 0, 16, 16, 16))
                        && LayerModelProjection.bounds(Direction.NORTH, 1).equals(
                                new LayerModelProjection.Bounds(0, 0, 12, 16, 16, 16))
                        && LayerModelProjection.bounds(Direction.SOUTH, 1).equals(
                                new LayerModelProjection.Bounds(0, 0, 0, 16, 16, 4))
                        && LayerModelProjection.bounds(Direction.EAST, 1).equals(
                                new LayerModelProjection.Bounds(0, 0, 0, 4, 16, 16))
                        && LayerModelProjection.bounds(Direction.WEST, 1).equals(
                                new LayerModelProjection.Bounds(12, 0, 0, 16, 16, 16)),
                "C55 Layer inventory correction changed a one-layer placed-world bound");
        for (Direction facing : Direction.values()) {
            helper.assertTrue(LayerModelProjection.bounds(facing, 4).isFullCube(),
                    "C55 Layer inventory correction changed the full state for " + facing);
        }
        helper.succeed();
    }

    private static void assertAxisPolicy(GameTestHelper helper, NibaruMaterialProfile profile,
            AxisUvPolicy policy, String description) {
        CornerColumnModelProjection.Projection projection =
                CornerColumnModelProjection.projectColumn(profile, COLUMN, policy);
        assertClosure(helper, projection, 18, 19, description + " axis Quarter Column");
        for (Direction.Axis axis : AXES) {
            assertAxisState(helper, projection, "occupancy=south_east,axis=" + axisName(axis),
                    policy, axis, List.of(bounds(8, 0, 8, 16, 16, 16)),
                    description + " " + axisName(axis) + "-axis Quarter Column");
        }
    }

    private static void assertAxisState(GameTestHelper helper,
            CornerColumnModelProjection.Projection projection, String key,
            AxisUvPolicy policy, Direction.Axis materialAxis,
            List<CuboidListModelProjection.Bounds> expectedWorld, String description) {
        JsonObject variant = projection.blockState().getAsJsonObject("variants")
                .getAsJsonObject(key);
        boolean rotated = policy != AxisUvPolicy.DIRECT_UV_LOCKED
                && materialAxis != Direction.Axis.Y;
        int expectedX = rotated ? 90 : 0;
        int expectedY = rotated && materialAxis == Direction.Axis.X ? 90 : 0;
        helper.assertTrue(rotation(variant, "x") == expectedX
                        && rotation(variant, "y") == expectedY
                        && !variant.has("uvlock"),
                description + " selector material transform changed: " + variant);

        JsonObject model = projection.models().get(variant.get("model").getAsString());
        JsonArray elements = model.getAsJsonArray("elements");
        helper.assertTrue(elements.size() == expectedWorld.size(),
                description + " element count changed: " + elements.size());
        Direction.Axis modelAxis = rotated ? Direction.Axis.Y : materialAxis;
        AxisUvPolicy modelPolicy = rotated ? policy : AxisUvPolicy.DIRECT_UV_LOCKED;
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            CuboidListModelProjection.Bounds modelBounds = elementBounds(element);
            CuboidListModelProjection.Bounds worldBounds = rotated
                    ? rotateMaterialBounds(modelBounds, materialAxis)
                    : modelBounds;
            helper.assertTrue(worldBounds.equals(expectedWorld.get(i)),
                    description + " world geometry changed: model=" + modelBounds
                            + ", world=" + worldBounds + ", expected=" + expectedWorld.get(i));
            assertAxisFaces(helper, element.getAsJsonObject("faces"), modelBounds,
                    modelAxis, modelPolicy, description + " element " + i);
        }
    }

    private static void assertAxisFaces(GameTestHelper helper, JsonObject faces,
            CuboidListModelProjection.Bounds modelBounds, Direction.Axis modelAxis,
            AxisUvPolicy modelPolicy, String description) {
        for (Direction face : Direction.values()) {
            JsonObject encoded = faces.getAsJsonObject(face.getSerializedName());
            String expectedTexture;
            if (face.getAxis() != modelAxis) {
                expectedTexture = "#side";
            } else {
                expectedTexture = switch (face) {
                    case EAST, UP, SOUTH -> "#top";
                    case WEST, DOWN, NORTH -> "#bottom";
                };
            }
            int expectedRotation;
            if (modelPolicy == AxisUvPolicy.HORIZONTAL_ROTATED) {
                expectedRotation = face == Direction.UP ? 180 : 0;
            } else if (modelPolicy == AxisUvPolicy.STANDARD_ROTATED) {
                expectedRotation = 0;
            } else {
                expectedRotation = switch (modelAxis) {
                    case X -> face.getAxis() == Direction.Axis.X ? 0 : 90;
                    case Y -> 0;
                    case Z -> face.getAxis() == Direction.Axis.X ? 90 : 0;
                };
            }
            helper.assertTrue(encoded.get("texture").getAsString().equals(expectedTexture)
                            && rotation(encoded, "rotation") == expectedRotation,
                    description + " canonical face policy changed on " + face + ": " + encoded);
            assertNumbers(helper, encoded.getAsJsonArray("uv"), expectedUv(face, modelBounds));
        }
    }

    private static double[] expectedUv(
            Direction face, CuboidListModelProjection.Bounds bounds) {
        return switch (face) {
            case DOWN -> new double[]{bounds.x0(), 16 - bounds.z1(),
                    bounds.x1(), 16 - bounds.z0()};
            case UP -> new double[]{bounds.x0(), bounds.z0(), bounds.x1(), bounds.z1()};
            case NORTH -> new double[]{16 - bounds.x1(), 16 - bounds.y1(),
                    16 - bounds.x0(), 16 - bounds.y0()};
            case SOUTH -> new double[]{bounds.x0(), 16 - bounds.y1(),
                    bounds.x1(), 16 - bounds.y0()};
            case WEST -> new double[]{bounds.z0(), 16 - bounds.y1(),
                    bounds.z1(), 16 - bounds.y0()};
            case EAST -> new double[]{16 - bounds.z1(), 16 - bounds.y1(),
                    16 - bounds.z0(), 16 - bounds.y0()};
        };
    }

    private static CuboidListModelProjection.Bounds elementBounds(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        return bounds(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                from.get(2).getAsDouble(), to.get(0).getAsDouble(),
                to.get(1).getAsDouble(), to.get(2).getAsDouble());
    }

    private static CuboidListModelProjection.Bounds rotateMaterialBounds(
            CuboidListModelProjection.Bounds model, Direction.Axis materialAxis) {
        var rotation = AxisModelContract.materialRotation(materialAxis);
        double[] modelMin = {model.x0(), model.y0(), model.z0()};
        double[] modelMax = {model.x1(), model.y1(), model.z1()};
        double[] worldMin = new double[3];
        double[] worldMax = new double[3];
        for (Direction.Axis modelAxis : Direction.Axis.values()) {
            Direction worldDirection = rotation.rotate(positiveDirection(modelAxis));
            int modelIndex = axisIndex(modelAxis);
            int worldIndex = axisIndex(worldDirection.getAxis());
            if (isPositive(worldDirection)) {
                worldMin[worldIndex] = modelMin[modelIndex];
                worldMax[worldIndex] = modelMax[modelIndex];
            } else {
                worldMin[worldIndex] = 16 - modelMax[modelIndex];
                worldMax[worldIndex] = 16 - modelMin[modelIndex];
            }
        }
        return bounds(worldMin[0], worldMin[1], worldMin[2],
                worldMax[0], worldMax[1], worldMax[2]);
    }

    private static Direction positiveDirection(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    private static int axisIndex(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private static boolean isPositive(Direction direction) {
        return direction == Direction.EAST || direction == Direction.UP
                || direction == Direction.SOUTH;
    }

    private static int rotation(JsonObject object, String member) {
        return object.has(member) ? object.get(member).getAsInt() : 0;
    }

    private static void assertGrassCuboid(GameTestHelper helper, JsonObject model,
            int baseIndex, int overlayIndex, CuboidListModelProjection.Bounds expected,
            String description) {
        JsonObject base = model.getAsJsonArray("elements").get(baseIndex).getAsJsonObject();
        JsonObject overlay = model.getAsJsonArray("elements").get(overlayIndex).getAsJsonObject();
        assertElement(helper, base, expected, description + " base");
        assertElement(helper, overlay, expected, description + " overlay");
        JsonObject faces = base.getAsJsonObject("faces");
        helper.assertTrue(faces.getAsJsonObject("up").get("texture").getAsString().equals("#top")
                        && faces.getAsJsonObject("up").get("tintindex").getAsInt() == 0
                        && faces.getAsJsonObject("down").get("texture").getAsString().equals("#bottom"),
                "Grass " + description + " did not keep world UP=TOP and DOWN=BOTTOM");
        for (Direction face : HORIZONTAL) {
            JsonObject encoded = faces.getAsJsonObject(face.getSerializedName());
            JsonArray uv = encoded.getAsJsonArray("uv");
            helper.assertTrue(encoded.get("texture").getAsString().equals("#side")
                            && !encoded.has("rotation")
                            && !encoded.has("tintindex")
                            && uv.get(1).getAsDouble() == 0.0
                            && uv.get(3).getAsDouble() == 16.0,
                    "Grass " + description + " rotated or inverted canonical SIDE UVs on " + face);
        }
        JsonObject overlayFaces = overlay.getAsJsonObject("faces");
        helper.assertTrue(overlayFaces.size() == 4
                        && overlayFaces.entrySet().stream().allMatch(entry -> {
                            JsonObject encoded = entry.getValue().getAsJsonObject();
                            JsonArray uv = encoded.getAsJsonArray("uv");
                            return encoded.get("texture").getAsString().equals("#overlay")
                                    && encoded.get("tintindex").getAsInt() == 0
                                    && !encoded.has("rotation")
                                    && uv.get(1).getAsDouble() == 0.0
                                    && uv.get(3).getAsDouble() == 16.0;
                        }),
                "Grass " + description + " overlay did not remain upright in world space");
    }

    private static void assertExactProjection(GameTestHelper helper,
            CornerColumnModelProjection.Projection projection,
            CornerColumnModelProjection.Projection repeat,
            List<String> selectors, List<String> models, String description) {
        assertKeys(helper, projection.blockState().getAsJsonObject("variants"), selectors,
                description + " selectors");
        helper.assertTrue(new ArrayList<>(projection.models().keySet()).equals(models),
                description + " model keys/order changed: " + projection.models().keySet());
        helper.assertTrue(new ArrayList<>(projection.models().keySet())
                                .equals(new ArrayList<>(repeat.models().keySet()))
                        && projection.blockState().equals(repeat.blockState())
                        && projection.models().equals(repeat.models()),
                description + " projection is not deterministic");
        assertClosure(helper, projection, selectors.size(), models.size(), description);
    }

    private static void assertClosure(GameTestHelper helper,
            CornerColumnModelProjection.Projection projection,
            int selectorCount, int modelCount, String description) {
        JsonObject variants = projection.blockState().getAsJsonObject("variants");
        Set<String> selected = selectorModels(projection);
        Set<String> generatedWorld = new LinkedHashSet<>(projection.models().keySet());
        generatedWorld.remove(projection.itemModel());
        helper.assertTrue(variants.size() == selectorCount
                        && projection.models().size() == modelCount
                        && selected.equals(generatedWorld)
                        && projection.models().containsKey(projection.itemModel())
                        && !selected.contains(projection.itemModel()),
                description + " selector/model closure changed: selectors=" + variants.size()
                        + ", models=" + projection.models().size() + ", selected=" + selected
                        + ", generatedWorld=" + generatedWorld);
    }

    private static Set<String> selectorModels(CornerColumnModelProjection.Projection projection) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (var variant : projection.blockState().getAsJsonObject("variants").entrySet()) {
            result.add(variant.getValue().getAsJsonObject().get("model").getAsString());
        }
        return result;
    }

    private static void assertKeys(GameTestHelper helper, JsonObject object,
            List<String> expected, String description) {
        List<String> actual = new ArrayList<>(object.keySet());
        helper.assertTrue(actual.equals(expected),
                description + " changed: expected=" + expected + ", actual=" + actual);
    }

    private static JsonObject selected(CornerColumnModelProjection.Projection projection, String key) {
        String model = projection.blockState().getAsJsonObject("variants")
                .getAsJsonObject(key).get("model").getAsString();
        JsonObject result = projection.models().get(model);
        if (result == null) throw new IllegalStateException("Selector did not resolve: " + key + " -> " + model);
        return result;
    }

    private static JsonObject selected(LayerModelProjection.Projection projection, String key) {
        String model = projection.blockState().getAsJsonObject("variants")
                .getAsJsonObject(key).get("model").getAsString();
        JsonObject result = projection.models().get(model);
        if (result == null) throw new IllegalStateException("Selector did not resolve: " + key + " -> " + model);
        return result;
    }

    private static JsonObject firstFaces(JsonObject model) {
        return model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
    }

    private static void assertElements(GameTestHelper helper, JsonObject model,
            List<CuboidListModelProjection.Bounds> expected, String description) {
        JsonArray elements = model.getAsJsonArray("elements");
        helper.assertTrue(elements.size() == expected.size(),
                description + " element count changed: " + elements.size());
        for (int i = 0; i < expected.size(); i++) {
            assertElement(helper, elements.get(i).getAsJsonObject(), expected.get(i),
                    description + " element " + i);
        }
    }

    private static void assertElement(GameTestHelper helper, JsonObject element,
            CuboidListModelProjection.Bounds expected, String description) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        helper.assertTrue(from.get(0).getAsDouble() == expected.x0()
                        && from.get(1).getAsDouble() == expected.y0()
                        && from.get(2).getAsDouble() == expected.z0()
                        && to.get(0).getAsDouble() == expected.x1()
                        && to.get(1).getAsDouble() == expected.y1()
                        && to.get(2).getAsDouble() == expected.z1(),
                description + " bounds changed: " + from + " -> " + to);
    }

    private static void assertLayerElement(GameTestHelper helper, JsonObject model,
            int x0, int y0, int z0, int x1, int y1, int z1, String description) {
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        helper.assertTrue(from.get(0).getAsInt() == x0 && from.get(1).getAsInt() == y0
                        && from.get(2).getAsInt() == z0 && to.get(0).getAsInt() == x1
                        && to.get(1).getAsInt() == y1 && to.get(2).getAsInt() == z1,
                description + " bounds changed: " + from + " -> " + to);
    }

    private static void assertNumbers(GameTestHelper helper, JsonArray actual, double... expected) {
        boolean equal = actual.size() == expected.length;
        for (int i = 0; equal && i < expected.length; i++) {
            equal = actual.get(i).getAsDouble() == expected[i];
        }
        helper.assertTrue(equal, "Expected " + List.of(expected) + ", found " + actual);
    }

    private static CuboidListModelProjection.Bounds bounds(
            double x0, double y0, double z0, double x1, double y1, double z1) {
        return new CuboidListModelProjection.Bounds(x0, y0, z0, x1, y1, z1);
    }

    private static String modelId(Identifier shape, String suffix) {
        return shape.getNamespace() + ":block/" + shape.getPath() + suffix;
    }

    private static String axisName(Direction.Axis axis) {
        return switch (axis) {
            case X -> "x";
            case Y -> "y";
            case Z -> "z";
        };
    }

    private static NibaruMaterialProfile profile(String id) {
        return NibaruMaterialProfiles.fromId(Identifier.parse(id)).orElseThrow();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
