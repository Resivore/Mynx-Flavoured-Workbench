package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class Canary9ContentContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("workbenchRoot"))
            .resolve("projects/matcha-heart-death-compat");
    private static final Path RESOURCES = PROJECT.resolve("src/main/resources");
    private static final Path RESONANT_TEXTURE = RESOURCES.resolve(
            "assets/matcha_heart_death_compat/textures/item/resonant_favour.png");
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final String RESONANT_TEXTURE_SHA256 =
            "32CFC7D3247E2E81E6B13FD5D81E5EE32419F88CD47DE397AC40D087A28E3859";

    private static Recipe<CraftingInput> resonantRecipe;
    private static Recipe<CraftingInput> crystalRecipe;

    @BeforeAll
    static void bootstrapProductionRecipes() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!BuiltInRegistries.ITEM.containsKey(HeartItems.RESONANT_FAVOUR_ID)) {
            HeartItems.register();
        }

        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(
                                lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());

        resonantRecipe = decodeRecipe(
                "/data/matcha_heart_death_compat/recipe/resonant_favour.json", registries);
        crystalRecipe = decodeRecipe("/data/crafting/recipe/crystal_heart.json", registries);
    }

    @Test
    void resonantFavourUsesTheCanonicalRegisteredIdentityModelAndLocalization() throws Exception {
        assertEquals("matcha_heart_death_compat:resonant_favour",
                HeartItems.RESONANT_FAVOUR_ID.toString());
        assertEquals(HeartItems.RESONANT_FAVOUR,
                BuiltInRegistries.ITEM.getValue(HeartItems.RESONANT_FAVOUR_ID));

        ItemStack defaultStack = HeartItems.RESONANT_FAVOUR.getDefaultInstance();
        assertEquals(Boolean.TRUE,
                defaultStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertTrue(defaultStack.hasFoil());
        assertFalse(defaultStack.isEnchanted());

        JsonObject itemDefinition = json(
                "assets/matcha_heart_death_compat/items/resonant_favour.json");
        assertEquals("matcha_heart_death_compat:item/resonant_favour",
                itemDefinition.getAsJsonObject("model").get("model").getAsString());

        JsonObject model = json(
                "assets/matcha_heart_death_compat/models/item/resonant_favour.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("matcha_heart_death_compat:item/resonant_favour",
                model.getAsJsonObject("textures").get("layer0").getAsString());

        JsonObject language = json("assets/matcha_heart_death_compat/lang/en_us.json");
        assertEquals("Resonant Favour",
                language.get("item.matcha_heart_death_compat.resonant_favour").getAsString());
    }

    @Test
    void authoritativeUserSuppliedResonantFavourTextureIsPresentAndPng() throws Exception {
        assertTrue(Files.isRegularFile(RESONANT_TEXTURE),
                () -> "Missing authoritative user-supplied Resonant Favour sprite: "
                        + RESONANT_TEXTURE);
        byte[] signature = new byte[PNG_SIGNATURE.length];
        try (var stream = Files.newInputStream(RESONANT_TEXTURE)) {
            assertEquals(PNG_SIGNATURE.length, stream.read(signature),
                    "Resonant Favour texture is shorter than a PNG signature");
        }
        assertArrayEquals(PNG_SIGNATURE, signature,
                "Authoritative Resonant Favour texture must remain a PNG");
        assertEquals(RESONANT_TEXTURE_SHA256, sha256(RESONANT_TEXTURE),
                "Resonant Favour texture bytes changed from the supplied asset");
        var image = ImageIO.read(RESONANT_TEXTURE.toFile());
        assertTrue(image != null, "Resonant Favour texture must decode as a PNG image");
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
    }

    @Test
    void resonantFavourMatchesOnlyTheExactFourShardAndDivineFavourPattern() {
        CraftingInput exact = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY,
                echoShard(), new ItemStack(Items.NETHER_STAR), echoShard(),
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY));
        assertTrue(resonantRecipe.matches(exact, null));

        assertFalse(resonantRecipe.matches(CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY,
                echoShard(), new ItemStack(Items.TURTLE_SCUTE), echoShard(),
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY)), null));
        assertFalse(resonantRecipe.matches(CraftingInput.of(3, 3, List.of(
                echoShard(), echoShard(), ItemStack.EMPTY,
                echoShard(), new ItemStack(Items.NETHER_STAR), echoShard(),
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY)), null));

        ItemStack assembled = resonantRecipe.assemble(exact);
        assertTrue(assembled.is(HeartItems.RESONANT_FAVOUR));
        assertEquals(1, assembled.getCount());
        assertEquals(Boolean.TRUE,
                assembled.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertTrue(assembled.hasFoil());
        assertFalse(assembled.isEnchanted());
    }

    @Test
    void crystalHeartMatchesOnlyTheExactFiveFragmentAndDivineFavourPattern() {
        CraftingInput exact = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.TURTLE_SCUTE), ItemStack.EMPTY,
                        new ItemStack(Items.TURTLE_SCUTE),
                new ItemStack(Items.TURTLE_SCUTE), new ItemStack(Items.NETHER_STAR),
                        new ItemStack(Items.TURTLE_SCUTE),
                ItemStack.EMPTY, new ItemStack(Items.TURTLE_SCUTE), ItemStack.EMPTY));
        assertTrue(crystalRecipe.matches(exact, null));

        assertFalse(crystalRecipe.matches(CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND), echoShard(), new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.DIAMOND))), null),
                "The superseded Canary 8 Crystal Heart grid must not match");
    }

    @Test
    void noStaleRecipeResourceStillProducesEitherHeartOrResonantFavour() throws Exception {
        List<String> crystalOutputs = new ArrayList<>();
        List<String> reinforcedOutputs = new ArrayList<>();
        List<String> resonantOutputs = new ArrayList<>();
        Path data = RESOURCES.resolve("data");
        try (var files = Files.walk(data)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/recipe/"))
                    .forEach(path -> classifyRecipeOutput(
                            data, path, crystalOutputs, reinforcedOutputs, resonantOutputs));
        }

        assertEquals(List.of("crafting/recipe/crystal_heart.json"), crystalOutputs);
        assertEquals(List.of(
                "matcha_heart_death_compat/recipe/reinforced_crystal_heart.json"),
                reinforcedOutputs);
        assertEquals(List.of("matcha_heart_death_compat/recipe/resonant_favour.json"),
                resonantOutputs);
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> decodeRecipe(
            String path, HolderLookup.Provider registries) throws IOException {
        try (var stream = Canary9ContentContractTest.class.getResourceAsStream(path)) {
            assertTrue(stream != null, () -> "Missing packaged recipe " + path);
            JsonObject recipe = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return (Recipe<CraftingInput>) Recipe.CODEC.parse(
                    registries.createSerializationContext(JsonOps.INSTANCE), recipe).getOrThrow();
        }
    }

    private static JsonObject json(String relativePath) throws IOException {
        return JsonParser.parseString(Files.readString(
                RESOURCES.resolve(relativePath), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void classifyRecipeOutput(
            Path data,
            Path path,
            List<String> crystalOutputs,
            List<String> reinforcedOutputs,
            List<String> resonantOutputs) {
        try {
            JsonObject recipe = JsonParser.parseString(
                    Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject result = recipe.getAsJsonObject("result");
            if (result == null) {
                return;
            }
            String relative = data.relativize(path).toString().replace('\\', '/');
            if (HeartDataContract.RESONANT_FAVOUR_RECIPE_ID.equals(
                    result.get("id").getAsString())) {
                resonantOutputs.add(relative);
            }
            JsonObject components = result.getAsJsonObject("components");
            if (components == null || !components.has("minecraft:item_model")) {
                return;
            }
            JsonElement model = components.get("minecraft:item_model");
            if ("minecraft:heart_container".equals(model.getAsString())) {
                crystalOutputs.add(relative);
            } else if ("matcha_heart_death_compat:reinforced_crystal_heart"
                    .equals(model.getAsString())) {
                reinforcedOutputs.add(relative);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect recipe " + path, exception);
        }
    }

    private static ItemStack echoShard() {
        return new ItemStack(Items.ECHO_SHARD);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
