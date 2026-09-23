package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonObject;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

/** Controlled pack-present and pack-absent Bookshelf texture contracts. */
public final class BookshelfResourceGameTests implements CustomTestMethodInvoker {
    private static final Identifier END_PNG = Identifier.parse(
            "minecraft:textures/block/bookshelf_top.png");
    private static final String CANONICAL = "minecraft:block/bookshelf";
    private static final String SIDE = "minecraft:block/bookshelf";
    private static final String MATCHA_END = "minecraft:block/bookshelf_top";
    private static final String FALLBACK_END = "minecraft:block/oak_planks";
    private static final Identifier LAYER = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/bookshelf_layer");
    private static final Identifier CORNER = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/bookshelf_corner");
    private static final Identifier COLUMN = Identifier.parse(
            "cnm_terrain_slabs_compat:minecraft/bookshelf_quarter_column");

    @GameTest(maxTicks = 40)
    public void bookshelfEndTextureFollowsActivePackAndFallsBack(GameTestHelper helper) {
        NibaruMaterialProfile bookshelf = NibaruMaterialProfiles.fromId(
                Identifier.parse("minecraft:bookshelf")).orElseThrow();
        ResourceProvider withPack = ResourceProvider.fromMap(Map.of(END_PNG,
                new Resource(null, InputStream::nullInputStream)));

        // One fresh projection per reload, as in CNM's normal generation pass.
        assertFamily(helper, bookshelf, withPack, MATCHA_END);
        assertFamily(helper, bookshelf, ResourceProvider.EMPTY, FALLBACK_END);
        assertFamily(helper, bookshelf, withPack, MATCHA_END);

        NibaruMaterialProfile oak = NibaruMaterialProfiles.fromId(
                Identifier.parse("minecraft:oak_planks")).orElseThrow();
        JsonObject unrelated = LayerModelProjection.cuboidModel(oak,
                net.minecraft.core.Direction.UP, 1, null, false);
        JsonObject original = unrelated.deepCopy();
        helper.assertTrue(BookshelfResourceTextures.apply(withPack, oak, unrelated).equals(original),
                "Bookshelf resource discovery changed an unrelated material model");
        helper.succeed();
    }

    private static void assertFamily(GameTestHelper helper, NibaruMaterialProfile profile,
            ResourceProvider resources, String expectedEnd) {
        LayerModelProjection.Projection layer = LayerModelProjection.project(profile, LAYER, true);
        JsonObject layerVariants = layer.blockState().getAsJsonObject("variants");
        helper.assertTrue(layerVariants.size() == 24,
                "Bookshelf Layer lost an orientation/thickness selector");
        for (var entry : layerVariants.entrySet()) {
            String modelId = entry.getValue().getAsJsonObject().get("model").getAsString();
            if (entry.getKey().endsWith("layers=4")) {
                helper.assertTrue(modelId.equals(CANONICAL),
                        "Full Bookshelf Layer stopped using the pack-controlled canonical model");
            } else {
                assertModel(helper, profile, resources, layer.models().get(modelId),
                        expectedEnd, "Layer " + entry.getKey());
            }
        }
        assertModel(helper, profile, resources, layer.models().get(layer.itemModel()),
                expectedEnd, "Layer item");

        CornerColumnModelProjection.Projection corner =
                CornerColumnModelProjection.projectCorner(profile, CORNER);
        CornerColumnModelProjection.Projection column =
                CornerColumnModelProjection.projectColumn(profile, COLUMN);
        helper.assertTrue(corner.blockState().getAsJsonObject("variants").size() == 4,
                "Bookshelf Corner lost a physical orientation");
        helper.assertTrue(column.blockState().getAsJsonObject("variants").size() == 6,
                "Bookshelf Quarter Column lost an occupancy");
        for (var entry : corner.models().entrySet()) {
            assertModel(helper, profile, resources, entry.getValue(), expectedEnd,
                    "Corner " + entry.getKey());
        }
        for (var entry : column.models().entrySet()) {
            assertModel(helper, profile, resources, entry.getValue(), expectedEnd,
                    "Quarter Column " + entry.getKey());
        }
    }

    private static void assertModel(GameTestHelper helper, NibaruMaterialProfile profile,
            ResourceProvider resources, JsonObject projected, String expectedEnd, String label) {
        helper.assertTrue(projected != null, label + " model is missing");
        JsonObject model = BookshelfResourceTextures.apply(resources, profile, projected.deepCopy());
        JsonObject textures = model.getAsJsonObject("textures");
        helper.assertTrue(SIDE.equals(textures.get("side").getAsString())
                        && expectedEnd.equals(textures.get("top").getAsString())
                        && expectedEnd.equals(textures.get("bottom").getAsString())
                        && SIDE.equals(textures.get("particle").getAsString()),
                label + " lost its Bookshelf side/end contract: " + textures);
        for (var element : model.getAsJsonArray("elements")) {
            JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
            for (var face : faces.entrySet()) {
                String role = face.getValue().getAsJsonObject().get("texture").getAsString();
                String expectedRole = switch (face.getKey()) {
                    case "up" -> "#top";
                    case "down" -> "#bottom";
                    default -> "#side";
                };
                helper.assertTrue(role.equals(expectedRole),
                        label + " changed its material face assignment on " + face.getKey());
            }
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
