package dev.aero.shulkertrowel.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Canary2ResourceContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void recipeIsTheExactRequestedThreeByThreeLayout() throws IOException {
        JsonObject recipe = json("src/main/resources/data/shulker_trowel/recipe/trowel.json");
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        JsonObject key = recipe.getAsJsonObject("key");
        JsonObject result = recipe.getAsJsonObject("result");

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(3, pattern.size());
        assertEquals("S  ", pattern.get(0).getAsString());
        assertEquals(" II", pattern.get(1).getAsString());
        assertEquals("   ", pattern.get(2).getAsString());
        assertEquals(Set.of("S", "I"), key.keySet());
        assertEquals("minecraft:stick", key.get("S").getAsString());
        assertEquals("minecraft:iron_ingot", key.get("I").getAsString());
        assertEquals("shulker_trowel:trowel", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
        assertFalse(pattern.toString().contains("\" II\",\" S \""));
    }

    @Test
    void vanillaShapedPatternNormalizesOnlyTheOuterBlankRow() {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(
                Map.of(
                        'S', Ingredient.of(Items.STICK),
                        'I', Ingredient.of(Items.IRON_INGOT)
                ),
                List.of("S  ", " II", "   ")
        );

        assertEquals(3, pattern.width());
        assertEquals(2, pattern.height());
        assertEquals(6, pattern.ingredients().size());
        assertTrue(pattern.ingredients().get(0).orElseThrow().acceptsItem(Items.STICK.builtInRegistryHolder()));
        assertTrue(pattern.ingredients().get(1).isEmpty());
        assertTrue(pattern.ingredients().get(2).isEmpty());
        assertTrue(pattern.ingredients().get(3).isEmpty());
        assertTrue(pattern.ingredients().get(4).orElseThrow().acceptsItem(Items.IRON_INGOT.builtInRegistryHolder()));
        assertTrue(pattern.ingredients().get(5).orElseThrow().acceptsItem(Items.IRON_INGOT.builtInRegistryHolder()));
    }

    @Test
    void trackedModelRequiresOnlyThePrivateAssemblyTexture() throws IOException {
        JsonObject model = json("src/main/resources/assets/shulker_trowel/models/item/trowel.json");
        Path protectedTexture = PROJECT_ROOT.resolve(
                "src/main/resources/assets/shulker_trowel/textures/item/trowel.png"
        );

        assertEquals(
                "shulker_trowel:item/trowel",
                model.getAsJsonObject("textures").get("layer0").getAsString()
        );
        assertFalse(Files.exists(protectedTexture));
    }

    @Test
    void metadataMarksTheAssemblyPrivateAndLeavesDeploymentTruthToTheManager() throws IOException {
        JsonObject metadata = json("src/main/resources/fabric.mod.json");
        JsonObject custom = metadata.getAsJsonObject("custom");

        assertEquals("${version}", metadata.get("version").getAsString());
        assertEquals(2, metadata.getAsJsonArray("license").size());
        assertEquals(
                "GENERATED / STATICALLY VALIDATED / RUNTIME UNTESTED / PRIVATE / NOT REDISTRIBUTABLE",
                custom.get("workbench:classification").getAsString()
        );
        assertFalse(custom.has("workbench:deployment"));
        assertEquals(
                "assets/shulker_trowel/textures/item/trowel.png",
                custom.get("workbench:private-resource-required").getAsString()
        );
        assertEquals(
                "D754DBAB87A0FE923016268F0CDDC4A785B022A1184837F9C3CA2BE6732F7789",
                custom.get("workbench:private-resource-origin-sha256").getAsString()
        );
    }

    @Test
    void mixinsKeepTheCanonicalSoundRouteAndAddOnlyTheTwoNarrowCompatibilitySeams() throws IOException {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/mixin/BlockItemPlacementSoundMixin.java"
        ));
        String offhand = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/mixin/OffhandShulkerPlacementMixin.java"
        ));
        String quickRightClick = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/mixin/QuickRightClickShulkerCompatibilityMixin.java"
        ));
        JsonObject mixins = json("src/main/resources/shulker_trowel.mixins.json");

        assertTrue(source.contains("@ModifyArg("));
        assertTrue(source.contains("index = 0"));
        assertTrue(source.contains("require = 1"));
        assertTrue(source.contains("Level;playSound("));
        assertTrue(source.contains("PlacementSoundBroadcastScope.routeExcludedSource"));
        assertFalse(source.contains(".playSound("));
        assertTrue(offhand.contains("@Inject(method = \"place\", at = @At(\"HEAD\")"));
        assertTrue(offhand.contains("OffhandShulkerPlacementPolicy.blocks"));
        assertTrue(offhand.contains("InteractionResult.FAIL"));
        assertTrue(quickRightClick.contains("@Pseudo"));
        assertTrue(quickRightClick.contains("QuickEvent"));
        assertTrue(quickRightClick.contains("ShulkerBoxBlock"));
        assertTrue(quickRightClick.contains("InteractionResult.PASS"));
        assertEquals("dev.aero.shulkertrowel.mixin.ShulkerTrowelMixinPlugin",
                mixins.get("plugin").getAsString());
        assertEquals(List.of(
                        "BlockItemPlacementSoundMixin",
                        "OffhandShulkerPlacementMixin",
                        "QuickRightClickShulkerCompatibilityMixin"),
                mixins.getAsJsonArray("mixins").asList().stream()
                        .map(element -> element.getAsString()).toList());
        assertFalse(Files.readString(PROJECT_ROOT.resolve("build.gradle"))
                .contains("shulker-trowel-0.1.0-canary3.jar"));
    }

    @Test
    void packagedMixinMetadataMatchesTheCurrentCompatibilitySource() throws IOException {
        Path candidate = PROJECT_ROOT.resolve("build/libs/shulker-trowel-0.1.0-canary10.jar");
        if (!Files.isRegularFile(candidate)) return;
        try (JarFile jar = new JarFile(candidate.toFile())) {
            var entry = jar.getJarEntry("shulker_trowel.mixins.json");
            assertTrue(entry != null, "Candidate is missing mixin metadata");
            try (var input = jar.getInputStream(entry)) {
                JsonObject packaged = JsonParser.parseString(new String(input.readAllBytes()))
                        .getAsJsonObject();
                assertEquals("dev.aero.shulkertrowel.mixin.ShulkerTrowelMixinPlugin",
                        packaged.get("plugin").getAsString());
                assertTrue(packaged.getAsJsonArray("mixins").asList().stream()
                        .anyMatch(element -> element.getAsString()
                                .equals("OffhandShulkerPlacementMixin")));
                assertTrue(packaged.getAsJsonArray("mixins").asList().stream()
                        .anyMatch(element -> element.getAsString()
                                .equals("QuickRightClickShulkerCompatibilityMixin")));
            }
        }
    }

    private static JsonObject json(String relativePath) throws IOException {
        return JsonParser.parseString(Files.readString(PROJECT_ROOT.resolve(relativePath))).getAsJsonObject();
    }
}
