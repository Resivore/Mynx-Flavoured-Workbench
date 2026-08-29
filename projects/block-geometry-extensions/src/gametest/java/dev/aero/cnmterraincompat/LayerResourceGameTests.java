package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.client.LayerModelProjection;
import dev.aero.cnmterraincompat.client.LayerGeneratedResources;
import dev.tazer.clutternomore.ClutterNoMore;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Focused ordinary-JSON coverage for Layer geometry/material projection and server data. */
public final class LayerResourceGameTests implements CustomTestMethodInvoker {
    private static final Identifier SHAPE = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/test_layer");

    @GameTest(maxTicks = 40)
    public void exactCuboidAndSelectorProjection(GameTestHelper helper) {
        helper.assertTrue(LayerModelProjection.bounds(Direction.UP, 1).equals(
                        new LayerModelProjection.Bounds(0, 0, 0, 16, 4, 16)),
                "Layer 1 UP did not project a 4px cuboid");
        helper.assertTrue(LayerModelProjection.bounds(Direction.DOWN, 2).equals(
                        new LayerModelProjection.Bounds(0, 8, 0, 16, 16, 16)),
                "Layer 2 DOWN did not project an anchored 8px cuboid");
        helper.assertTrue(LayerModelProjection.bounds(Direction.NORTH, 3).equals(
                        new LayerModelProjection.Bounds(0, 0, 4, 16, 16, 16)),
                "Layer 3 NORTH did not project an anchored 12px cuboid");
        for (Direction facing : Direction.values()) {
            helper.assertTrue(LayerModelProjection.bounds(facing, 4).isFullCube(),
                    "Layer 4 was not full for " + facing);
        }

        NibaruMaterialProfile stone = profile("minecraft:stone");
        helper.assertTrue(LayerGeneratedResources.displayName(stone).equals("Stone Layer"),
                "Collision-safe registry path leaked into the normal BGE display name");
        LayerModelProjection.Projection ordinary = LayerModelProjection.project(stone, SHAPE, true);
        JsonObject ordinaryVariants = ordinary.blockState().getAsJsonObject("variants");
        helper.assertTrue(ordinaryVariants.size() == 24 && ordinary.models().size() == 18,
                "Ordinary Layer must have 24 selectors and eighteen 4/8/12px models");
        helper.assertTrue(ordinaryVariants.keySet().stream().noneMatch(key -> key.contains("waterlogged")),
                "Waterlogged leaked into model selection");
        helper.assertTrue(ordinaryVariants.getAsJsonObject("facing=west,layers=4")
                        .get("model").getAsString().equals("minecraft:block/stone"),
                "Faithfully reusable Layer 4 did not select the canonical full model");
        helper.assertTrue(ordinary.itemModel().endsWith("_layer_1_up"),
                "Layer item did not select the one-layer model");

        NibaruMaterialProfile log = profile("minecraft:oak_log");
        LayerModelProjection.Projection axis = LayerModelProjection.project(log, SHAPE, false);
        helper.assertTrue(axis.blockState().getAsJsonObject("variants").size() == 72
                        && axis.models().size() == 57,
                "Pillar Layer did not cover facing x layers x material-axis");
        JsonObject axisFaces = selected(axis, "facing=east,layers=3,axis=x")
                .getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        helper.assertTrue(axisFaces.getAsJsonObject("east").get("texture").getAsString().equals("#top")
                        && axisFaces.getAsJsonObject("up").get("texture").getAsString().equals("#side")
                        && axisFaces.getAsJsonObject("up").get("rotation").getAsInt() == 90,
                "Pillar X axis lost semantic end/side UV projection");

        NibaruMaterialProfile glazed = profile("minecraft:white_glazed_terracotta");
        LayerModelProjection.Projection oriented = LayerModelProjection.project(glazed, SHAPE, false);
        helper.assertTrue(oriented.blockState().getAsJsonObject("variants").size() == 96
                        && oriented.models().size() == 19,
                "Glazed Layer did not cover independent pattern facing");
        JsonObject northPattern = oriented.blockState().getAsJsonObject("variants")
                .getAsJsonObject("facing=east,layers=2,pattern_facing=north");
        helper.assertTrue(northPattern.get("y").getAsInt() == 270
                        && !northPattern.get("uvlock").getAsBoolean(),
                "Glazed pattern orientation was coupled to physical Layer facing");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void typedTexturesTintAndSpecialVisuals(GameTestHelper helper) {
        NibaruMaterialProfile podzol = profile("minecraft:podzol");
        JsonObject podzolModel = LayerModelProjection.cuboidModel(
                podzol, Direction.SOUTH, 2, null, false);
        JsonObject textures = podzolModel.getAsJsonObject("textures");
        helper.assertTrue(textures.get("side").getAsString().endsWith("podzol_side")
                        && textures.get("top").getAsString().endsWith("podzol_top")
                        && textures.get("bottom").getAsString().endsWith("dirt"),
                "TOP/SIDE/BOTTOM Layer lost canonical semantic texture roles");

        NibaruMaterialProfile leaves = profile("minecraft:oak_leaves");
        JsonObject leafModel = LayerModelProjection.cuboidModel(
                leaves, Direction.UP, 1, null, false);
        JsonObject leafFaces = firstFaces(leafModel);
        helper.assertTrue(leafFaces.entrySet().stream().allMatch(entry ->
                        entry.getValue().getAsJsonObject().get("tintindex").getAsInt() == 0)
                        && leafModel.get("render_type").getAsString().equals("cutout_mipped"),
                "Tinted leaves Layer lost face tint or render contract");

        NibaruMaterialProfile grass = profile("minecraft:grass_block");
        JsonObject grassModel = LayerModelProjection.cuboidModel(
                grass, Direction.UP, 1, null, false);
        helper.assertTrue(grassModel.getAsJsonArray("elements").size() == 2
                        && firstFaces(grassModel).getAsJsonObject("up").get("tintindex").getAsInt() == 0,
                "Grass Layer lost tinted top plus overlay composition");
        JsonObject overlayFaces = grassModel.getAsJsonArray("elements").get(1).getAsJsonObject()
                .getAsJsonObject("faces");
        helper.assertTrue(overlayFaces.size() == 4 && overlayFaces.entrySet().stream().allMatch(entry ->
                        entry.getValue().getAsJsonObject().get("tintindex").getAsInt() == 0
                                && entry.getValue().getAsJsonObject().getAsJsonArray("uv").get(1).getAsInt() == 0
                                && entry.getValue().getAsJsonObject().getAsJsonArray("uv").get(3).getAsInt() == 4),
                "Grass Layer overlay lost horizontal tint/top-band UV contract");

        JsonObject path = LayerModelProjection.cuboidModel(
                profile("minecraft:dirt_path"), Direction.DOWN, 1, null, false);
        JsonArray pathTo = path.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("to");
        helper.assertTrue(pathTo.get(1).getAsInt() == 15,
                "Dirt Path Layer model did not preserve its lowered world-top surface");

        JsonObject glassUp = LayerModelProjection.cuboidModel(
                profile("minecraft:glass"), Direction.UP, 1, null, false);
        JsonArray glassElements = glassUp.getAsJsonArray("elements");
        JsonObject glassBody = glassElements.get(0).getAsJsonObject();
        JsonObject glassRim = glassElements.get(1).getAsJsonObject();
        JsonObject glassBodyFaces = glassBody.getAsJsonObject("faces");
        JsonObject glassRimFaces = glassRim.getAsJsonObject("faces");
        helper.assertTrue(glassElements.size() == 2
                        && glassBody.getAsJsonArray("from").get(1).getAsInt() == 0
                        && glassBody.getAsJsonArray("to").get(1).getAsInt() == 3
                        && glassRim.getAsJsonArray("from").get(1).getAsInt() == 3
                        && glassRim.getAsJsonArray("to").get(1).getAsInt() == 4
                        && glassBodyFaces.has("down") && !glassBodyFaces.has("up")
                        && glassRimFaces.has("up") && !glassRimFaces.has("down")
                        && glassBodyFaces.getAsJsonObject("down").get("cullface").getAsString().equals("down")
                        && !glassRimFaces.getAsJsonObject("up").has("cullface"),
                "GLASS_EDGE Layer did not retain the one-pixel exposed-rim topology");
        JsonArray glassRimSideUv = glassRimFaces.getAsJsonObject("north").getAsJsonArray("uv");
        JsonArray glassExposedUv = glassRimFaces.getAsJsonObject("up").getAsJsonArray("uv");
        helper.assertTrue(glassRimSideUv.get(1).getAsInt() == 0
                        && glassRimSideUv.get(3).getAsInt() == 1
                        && glassExposedUv.get(0).getAsInt() == 0
                        && glassExposedUv.get(1).getAsInt() == 0
                        && glassExposedUv.get(2).getAsInt() == 16
                        && glassExposedUv.get(3).getAsInt() == 16,
                "GLASS_EDGE exposed rim did not sample the one-pixel texture border");

        JsonObject glassNorth = LayerModelProjection.cuboidModel(
                profile("minecraft:glass"), Direction.NORTH, 2, null, false);
        JsonObject northBody = glassNorth.getAsJsonArray("elements").get(0).getAsJsonObject();
        JsonObject northRim = glassNorth.getAsJsonArray("elements").get(1).getAsJsonObject();
        helper.assertTrue(northBody.getAsJsonArray("from").get(2).getAsInt() == 9
                        && northBody.getAsJsonArray("to").get(2).getAsInt() == 16
                        && northRim.getAsJsonArray("from").get(2).getAsInt() == 8
                        && northRim.getAsJsonArray("to").get(2).getAsInt() == 9
                        && northBody.getAsJsonObject("faces").getAsJsonObject("south")
                                .get("cullface").getAsString().equals("south")
                        && !northRim.getAsJsonObject("faces").getAsJsonObject("north").has("cullface"),
                "Horizontal GLASS_EDGE Layer lost anchored-body/exposed-rim orientation");
        var glassProfiles = NibaruMaterialProfiles.all().stream()
                .filter(candidate -> candidate.visualProfile() == VisualProfile.GLASS_EDGE).toList();
        helper.assertTrue(glassProfiles.size() == 17 && glassProfiles.stream().allMatch(candidate -> {
            JsonObject projected = LayerModelProjection.cuboidModel(
                    candidate, Direction.UP, 1, null, false);
            return projected.getAsJsonArray("elements").size() == 2
                    && projected.get("render_type").getAsString().equals("translucent");
        }), "Plain/stained GLASS_EDGE Layer inventory lost typed rim projection");

        JsonObject rootsUp = LayerModelProjection.cuboidModel(
                profile("minecraft:mangrove_roots"), Direction.UP, 1, null, false);
        JsonArray rootElements = rootsUp.getAsJsonArray("elements");
        JsonObject rootZPlane = rootElements.get(0).getAsJsonObject();
        JsonObject rootXPlane = rootElements.get(1).getAsJsonObject();
        JsonObject rootUpShell = rootElements.get(2).getAsJsonObject();
        JsonObject rootDownShell = rootElements.get(3).getAsJsonObject();
        helper.assertTrue(rootElements.size() == 8
                        && rootZPlane.getAsJsonArray("from").get(2).getAsDouble() == 8.0
                        && rootZPlane.getAsJsonArray("to").get(2).getAsDouble() == 8.0
                        && rootXPlane.getAsJsonArray("from").get(0).getAsDouble() == 8.0
                        && rootXPlane.getAsJsonArray("to").get(0).getAsDouble() == 8.0
                        && Math.abs(rootUpShell.getAsJsonArray("from").get(1).getAsDouble() - 3.998) < 0.000001
                        && rootUpShell.getAsJsonArray("to").get(1).getAsDouble() == 4.0
                        && !rootUpShell.getAsJsonObject("faces").getAsJsonObject("up").has("cullface")
                        && rootDownShell.getAsJsonObject("faces").getAsJsonObject("down")
                                .get("cullface").getAsString().equals("down")
                        && rootUpShell.getAsJsonObject("faces").getAsJsonObject("up")
                                .get("texture").getAsString().equals("#top")
                        && rootsUp.get("render_type").getAsString().equals("cutout"),
                "ROOTS Layer lost crossed-plane/boundary-shell topology");

        JsonObject rootsWest = LayerModelProjection.cuboidModel(
                profile("minecraft:mangrove_roots"), Direction.WEST, 1, null, false);
        JsonArray westRootElements = rootsWest.getAsJsonArray("elements");
        JsonObject westXPlane = westRootElements.get(1).getAsJsonObject();
        JsonObject eastShell = westRootElements.get(6).getAsJsonObject();
        JsonObject westShell = westRootElements.get(7).getAsJsonObject();
        helper.assertTrue(westXPlane.getAsJsonArray("from").get(0).getAsDouble() == 14.0
                        && westXPlane.getAsJsonArray("to").get(0).getAsDouble() == 14.0
                        && eastShell.getAsJsonObject("faces").getAsJsonObject("east")
                                .get("cullface").getAsString().equals("east")
                        && !westShell.getAsJsonObject("faces").getAsJsonObject("west").has("cullface"),
                "Horizontal ROOTS Layer did not move its planes/shells with Layer bounds");

        JsonObject honey = LayerModelProjection.cuboidModel(
                profile("minecraft:honey_block"), Direction.WEST, 2, null, false);
        JsonObject slimeFull = LayerModelProjection.cuboidModel(
                profile("minecraft:slime_block"), Direction.DOWN, 4, null, false);
        helper.assertTrue(honey.getAsJsonArray("elements").size() == 2
                        && honey.get("render_type").getAsString().equals("translucent"),
                "Honey Layer lost declared shell/inner inset projection");
        helper.assertTrue(slimeFull.getAsJsonArray("elements").size() == 1
                        && slimeFull.get("render_type").getAsString().equals("translucent"),
                "Full Slime Layer ignored its no-inner-layer contract");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void generatedLootAndTagsAreOrdinaryServerData(GameTestHelper helper) {
        Identifier stoneLayer = CnmTerrainCompat.layerId(profile("minecraft:stone"));
        LayerGeneratedData.Binding stoneBinding = binding("minecraft:stone");
        JsonObject loot = LayerGeneratedData.lootTable(stoneBinding);
        JsonArray functions = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions");
        helper.assertTrue(functions.size() == 5
                        && functions.get(0).getAsJsonObject().get("count").getAsInt() == 1
                        && functions.get(3).getAsJsonObject().get("count").getAsInt() == 4
                        && functions.get(3).getAsJsonObject().getAsJsonArray("conditions").get(0)
                                .getAsJsonObject().getAsJsonObject("properties")
                                .get("layers").getAsString().equals("4")
                        && functions.get(4).getAsJsonObject().get("function").getAsString()
                                .equals("minecraft:explosion_decay"),
                "Layer loot does not copy the layers state into self-drop count");

        Identifier dirtLayer = CnmTerrainCompat.layerId(profile("minecraft:dirt"));
        for (String surface : List.of("minecraft:grass_block", "minecraft:mycelium",
                "minecraft:podzol", "minecraft:dirt_path")) {
            LayerGeneratedData.Binding binding = binding(surface);
            JsonObject surfaceLoot = LayerGeneratedData.lootTable(binding);
            String dropped = surfaceLoot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
            helper.assertTrue(LayerGeneratedData.dropItem(binding).equals(dirtLayer)
                            && dropped.equals(dirtLayer.toString()),
                    "Typed dirt-surface Layer did not drop the matching Dirt Layer: " + surface);
        }
        helper.assertTrue(LayerGeneratedData.dropItem(stoneBinding).equals(stoneLayer),
                "Ordinary Layer stopped dropping itself");

        JsonObject layerTag = generatedServerJson(Identifier.parse(
                "cnm_terrain_slabs_compat:tags/block/layers.json"));
        JsonObject layerItemTag = generatedServerJson(Identifier.parse(
                "cnm_terrain_slabs_compat:tags/item/layers.json"));
        helper.assertTrue(!layerTag.get("replace").getAsBoolean()
                        && layerTag.getAsJsonArray("values").size() == 311
                        && layerItemTag.getAsJsonArray("values").size() == 311
                        && layerTag.getAsJsonArray("values").asList().stream()
                                .anyMatch(value -> value.getAsString().equals(stoneLayer.toString())),
                "BGE Layer block/item classification tags changed or omitted stone");
        helper.succeed();
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

    private static JsonObject generatedServerJson(Identifier id) {
        try {
            var supplier = ClutterNoMore.RESOURCES.getResource(PackType.SERVER_DATA, id);
            if (supplier == null) throw new IllegalStateException("Missing generated server resource " + id);
            try (var input = supplier.get();
                    var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect generated server resource " + id, exception);
        }
    }

    private static NibaruMaterialProfile profile(String id) {
        return NibaruMaterialProfiles.fromId(Identifier.parse(id)).orElseThrow();
    }

    private static LayerGeneratedData.Binding binding(String id) {
        NibaruMaterialProfile profile = profile(id);
        return LayerGeneratedData.bindings().stream()
                .filter(binding -> binding.profile() == profile)
                .findFirst().orElseThrow();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
