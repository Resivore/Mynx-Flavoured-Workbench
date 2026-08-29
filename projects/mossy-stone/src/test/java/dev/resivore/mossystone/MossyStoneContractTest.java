package dev.resivore.mossystone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MossyStoneContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final String TEXTURE_SHA256 =
            "984508A44A92F02CCE254CD58D45905CAE8E625C647D4A5B32F9AB6AE82EE2E8";
    private static final String CNM_SHA256 =
            "41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE";

    @Test
    void productionOwnsTheStandaloneStoneBasedParentAndThreeOrdinaryShapes() throws IOException {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/resivore/mossystone/MossyStoneMod.java"));
        assertTrue(source.contains("BLOCK_ID = id(\"mossy_stone\")"));
        assertTrue(source.contains("new Block(properties(BLOCK_ID))"));
        assertTrue(source.contains("new SlabBlock(properties(SLAB_ID))"));
        assertTrue(source.contains("new StairBlock("));
        assertTrue(source.contains("MOSSY_STONE.defaultBlockState(), properties(STAIRS_ID)"));
        assertTrue(source.contains("new WallBlock(properties(WALL_ID))"));
        assertTrue(source.contains("BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)"));
        assertTrue(source.contains("register(BLOCK_ID, MOSSY_STONE)"));
        assertTrue(source.contains("register(SLAB_ID, MOSSY_STONE_SLAB)"));
        assertTrue(source.contains("register(STAIRS_ID, MOSSY_STONE_STAIRS)"));
        assertTrue(source.contains("register(WALL_ID, MOSSY_STONE_WALL)"));
        assertFalse(source.contains("VerticalSlabBlock"));
        assertFalse(source.contains("StepBlock"));
        assertFalse(source.contains("CanonicalGeometryRegistry"));

        JsonObject dependencies = json("fabric.mod.json").getAsJsonObject("depends");
        assertFalse(dependencies.has("regions_unexplored"));
        assertEquals("=2.0.7+26.2", dependencies.get("clutternomore").getAsString());
        assertEquals("=26.2", dependencies.get("minecraft").getAsString());
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));
        assertFalse(build.contains("ruReference"));
        assertFalse(build.contains("ru_reference_jar"));
    }

    @Test
    void productionHasNoRuRuntimeReferenceOutsideThePackagedProvenanceNotice() throws IOException {
        Path notice = RESOURCES.resolve("META-INF/NOTICE_REGIONS_UNEXPLORED_MOSSY_STONE.txt");
        assertTrue(Files.isRegularFile(notice));
        try (var paths = Files.walk(PROJECT_ROOT.resolve("src/main"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (path.toString().endsWith(".png")) {
                    continue;
                }
                String text = Files.readString(path);
                if (path.equals(notice)) {
                    assertTrue(text.contains("assets/regions_unexplored/textures/block/mossy_stone.png"));
                    assertTrue(text.contains("MIT License"));
                } else {
                    assertFalse(text.contains("regions_unexplored"), path.toString());
                }
            }
        }
        assertFalse(Files.exists(RESOURCES.resolve("assets/regions_unexplored")));
        assertFalse(Files.exists(RESOURCES.resolve("data/regions_unexplored")));
    }

    @Test
    void exactAuditedTextureAndAllModelsUseTheOwnedNamespace() throws Exception {
        Path texture = RESOURCES.resolve("assets/mossy_stone/textures/block/mossy_stone.png");
        byte[] bytes = Files.readAllBytes(texture);
        assertEquals(2_541, bytes.length);
        assertEquals(TEXTURE_SHA256, sha256(bytes));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(image);
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertEquals(3, image.getColorModel().getNumColorComponents());
        assertFalse(image.getColorModel().hasAlpha());

        Path models = RESOURCES.resolve("assets/mossy_stone/models/block");
        try (var paths = Files.list(models)) {
            List<Path> modelFiles = paths.filter(path -> path.toString().endsWith(".json")).toList();
            assertEquals(10, modelFiles.size());
            for (Path model : modelFiles) {
                String text = Files.readString(model);
                assertTrue(text.contains("mossy_stone:block/mossy_stone"), model.toString());
                assertFalse(text.contains("regions_unexplored"), model.toString());
            }
        }
        assertEquals("minecraft:block/cube_all",
                json("assets/mossy_stone/models/block/mossy_stone.json")
                        .get("parent").getAsString());
        assertEquals("mossy_stone:block/mossy_stone",
                json("assets/mossy_stone/blockstates/mossy_stone_slab.json")
                        .getAsJsonObject("variants").getAsJsonObject("type=double")
                        .get("model").getAsString());
    }

    @Test
    void fullBlockSilkTouchPathDropsItself() throws IOException {
        JsonObject silk = fullBlockLootChildren().get(0).getAsJsonObject();
        assertEquals("minecraft:item", silk.get("type").getAsString());
        assertEquals("mossy_stone:mossy_stone", silk.get("name").getAsString());
        JsonArray conditions = silk.getAsJsonArray("conditions");
        assertEquals(1, conditions.size());
        JsonObject matchTool = conditions.get(0).getAsJsonObject();
        assertEquals("minecraft:match_tool", matchTool.get("condition").getAsString());
        JsonObject enchantment = matchTool.getAsJsonObject("predicate")
                .getAsJsonObject("predicates")
                .getAsJsonArray("minecraft:enchantments").get(0).getAsJsonObject();
        assertEquals("minecraft:silk_touch", enchantment.get("enchantments").getAsString());
        assertEquals(1, enchantment.getAsJsonObject("levels").get("min").getAsInt());
    }

    @Test
    void fullBlockNonSilkPathDropsMossyCobblestone() throws IOException {
        JsonObject ordinary = fullBlockLootChildren().get(1).getAsJsonObject();
        assertEquals("minecraft:item", ordinary.get("type").getAsString());
        assertEquals("minecraft:mossy_cobblestone", ordinary.get("name").getAsString());
        JsonArray conditions = ordinary.getAsJsonArray("conditions");
        assertEquals(1, conditions.size());
        assertEquals("minecraft:survives_explosion",
                conditions.get(0).getAsJsonObject().get("condition").getAsString());
        assertFalse(ordinary.toString().contains("minecraft:silk_touch"));
    }

    @Test
    void tagsAndOwnedGeometryLootAreExact() throws IOException {
        assertEquals(Set.of(
                        "mossy_stone:mossy_stone",
                        "mossy_stone:mossy_stone_slab",
                        "mossy_stone:mossy_stone_stairs",
                        "mossy_stone:mossy_stone_wall"),
                Set.copyOf(strings(json("data/minecraft/tags/block/mineable/pickaxe.json")
                        .getAsJsonArray("values"))));
        assertTag("block/slabs", "mossy_stone:mossy_stone_slab");
        assertTag("block/stairs", "mossy_stone:mossy_stone_stairs");
        assertTag("block/walls", "mossy_stone:mossy_stone_wall");
        assertTag("item/slabs", "mossy_stone:mossy_stone_slab");
        assertTag("item/stairs", "mossy_stone:mossy_stone_stairs");
        assertTag("item/walls", "mossy_stone:mossy_stone_wall");

        String slab = json("data/mossy_stone/loot_table/blocks/mossy_stone_slab.json").toString();
        assertTrue(slab.contains("mossy_stone:mossy_stone_slab"));
        assertTrue(slab.contains("minecraft:set_count"));
        assertTrue(slab.contains("double"));
        assertFalse(slab.contains("minecraft:mossy_cobblestone"));
        for (String shape : List.of("stairs", "wall")) {
            String loot = json("data/mossy_stone/loot_table/blocks/mossy_stone_" + shape + ".json")
                    .toString();
            assertTrue(loot.contains("mossy_stone:mossy_stone_" + shape));
            assertTrue(loot.contains("minecraft:survives_explosion"));
            assertFalse(loot.contains("minecraft:mossy_cobblestone"));
        }
    }

    @Test
    void recipesAreTheExactElevenStandaloneContracts() throws IOException {
        Path recipes = RESOURCES.resolve("data/mossy_stone/recipe");
        Set<String> names;
        try (var paths = Files.list(recipes)) {
            names = paths.map(path -> path.getFileName().toString())
                    .collect(TreeSet::new, Set::add, Set::addAll);
        }
        assertEquals(Set.of(
                "mossy_stone.json",
                "mossy_stone_bricks_from_mossy_stone.json",
                "mossy_stone_from_blasting_mossy_cobblestone.json",
                "mossy_stone_from_moss_block.json",
                "mossy_stone_from_smelting_mossy_cobblestone.json",
                "mossy_stone_slab.json",
                "mossy_stone_slab_from_mossy_stone_stonecutting.json",
                "mossy_stone_stairs.json",
                "mossy_stone_stairs_from_mossy_stone_stonecutting.json",
                "mossy_stone_wall.json",
                "mossy_stone_wall_from_mossy_stone_stonecutting.json"), names);

        assertEquals(List.of("minecraft:stone", "minecraft:vine"),
                strings(json("data/mossy_stone/recipe/mossy_stone.json")
                        .getAsJsonArray("ingredients")));
        assertResult(json("data/mossy_stone/recipe/mossy_stone.json"),
                "mossy_stone:mossy_stone", 1);
        assertEquals(List.of("minecraft:stone", "minecraft:moss_block"),
                strings(json("data/mossy_stone/recipe/mossy_stone_from_moss_block.json")
                        .getAsJsonArray("ingredients")));
        assertResult(json("data/mossy_stone/recipe/mossy_stone_from_moss_block.json"),
                "mossy_stone:mossy_stone", 1);
        assertCooking("mossy_stone_from_smelting_mossy_cobblestone", "minecraft:smelting", 200);
        assertCooking("mossy_stone_from_blasting_mossy_cobblestone", "minecraft:blasting", 100);

        JsonObject bricks = json("data/mossy_stone/recipe/mossy_stone_bricks_from_mossy_stone.json");
        assertEquals(List.of("##", "##"), strings(bricks.getAsJsonArray("pattern")));
        assertResult(bricks, "minecraft:mossy_stone_bricks", 4);
        assertShaped("mossy_stone_slab", List.of("###"), 6);
        assertShaped("mossy_stone_stairs", List.of("#  ", "## ", "###"), 4);
        assertShaped("mossy_stone_wall", List.of("###", "###"), 6);
        assertStonecutting("mossy_stone_slab", 2);
        assertStonecutting("mossy_stone_stairs", 1);
        assertStonecutting("mossy_stone_wall", 1);
    }

    @Test
    void everyRecipeHasTheMatchingOwnNamespaceUnlockAdvancement() throws IOException {
        Path advancements = RESOURCES.resolve("data/mossy_stone/advancement/recipes");
        Set<String> names = new TreeSet<>();
        try (var paths = Files.walk(advancements)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                names.add(advancements.relativize(path).toString().replace('\\', '/'));
                JsonObject advancement = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                String recipe = path.getFileName().toString().replaceFirst("\\.json$", "");
                assertEquals(List.of("mossy_stone:" + recipe),
                        strings(advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")));
                assertFalse(advancement.toString().contains("regions_unexplored"));
            }
        }
        assertEquals(Set.of(
                "building_blocks/mossy_stone.json",
                "building_blocks/mossy_stone_bricks_from_mossy_stone.json",
                "building_blocks/mossy_stone_from_blasting_mossy_cobblestone.json",
                "building_blocks/mossy_stone_from_moss_block.json",
                "building_blocks/mossy_stone_from_smelting_mossy_cobblestone.json",
                "building_blocks/mossy_stone_slab.json",
                "building_blocks/mossy_stone_slab_from_mossy_stone_stonecutting.json",
                "building_blocks/mossy_stone_stairs.json",
                "building_blocks/mossy_stone_stairs_from_mossy_stone_stonecutting.json",
                "decorations/mossy_stone_wall.json",
                "decorations/mossy_stone_wall_from_mossy_stone_stonecutting.json"), names);
    }

    @Test
    void shapeMapUsesOwnParentWhilePinnedCnmOwnsOnlyVerticalSlabAndStep() throws Exception {
        JsonObject map = json("data/mossy_stone/shape_map/mossy_stone.json");
        JsonObject add = map.getAsJsonObject("add");
        assertEquals(700, map.get("priority").getAsInt());
        assertEquals(Set.of("mossy_stone:mossy_stone"), add.keySet());
        assertEquals(List.of(
                "mossy_stone:mossy_stone_slab",
                "mossy_stone:mossy_stone_stairs",
                "mossy_stone:mossy_stone_wall"),
                strings(add.getAsJsonArray("mossy_stone:mossy_stone")));
        assertFalse(map.toString().contains("vertical_mossy_stone_slab"));
        assertFalse(map.toString().contains("mossy_stone_step"));
        assertFalse(Files.exists(RESOURCES.resolve("assets/clutternomore")));
        assertFalse(Files.exists(RESOURCES.resolve("data/clutternomore")));

        Path jar = reference("cnmReferenceJar");
        assertEquals(759_419L, Files.size(jar));
        assertEquals(CNM_SHA256, sha256(jar));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            String rules = text(zip, "data/clutternomore/shape_map/clutternomore.json");
            assertTrue(rules.contains("clutternomore:${ns}/vertical_${stem}_slab"));
            assertTrue(rules.contains("clutternomore:${ns}/${stem}_step"));
        }
        String vertical = "clutternomore:mossy_stone/vertical_" + "mossy_stone_slab";
        String step = "clutternomore:mossy_stone/" + "mossy_stone_stairs".replace("stairs", "step");
        assertEquals("clutternomore:mossy_stone/vertical_mossy_stone_slab", vertical);
        assertEquals("clutternomore:mossy_stone/mossy_stone_step", step);
    }

    private static JsonArray fullBlockLootChildren() throws IOException {
        JsonObject table = json("data/mossy_stone/loot_table/blocks/mossy_stone.json");
        assertEquals("minecraft:block", table.get("type").getAsString());
        assertEquals("mossy_stone:blocks/mossy_stone", table.get("random_sequence").getAsString());
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(1, pools.size());
        JsonArray entries = pools.get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(1, entries.size());
        JsonObject alternatives = entries.get(0).getAsJsonObject();
        assertEquals("minecraft:alternatives", alternatives.get("type").getAsString());
        JsonArray children = alternatives.getAsJsonArray("children");
        assertEquals(2, children.size());
        return children;
    }

    private static void assertTag(String path, String expected) throws IOException {
        assertEquals(List.of(expected),
                strings(json("data/minecraft/tags/" + path + ".json").getAsJsonArray("values")));
    }

    private static void assertCooking(String name, String type, int time) throws IOException {
        JsonObject recipe = json("data/mossy_stone/recipe/" + name + ".json");
        assertEquals(type, recipe.get("type").getAsString());
        assertEquals("minecraft:mossy_cobblestone", recipe.get("ingredient").getAsString());
        assertEquals(time, recipe.get("cookingtime").getAsInt());
        assertEquals(0.1, recipe.get("experience").getAsDouble(), 0.000_001);
        assertResult(recipe, "mossy_stone:mossy_stone", 1);
    }

    private static void assertShaped(String name, List<String> pattern, int count) throws IOException {
        JsonObject recipe = json("data/mossy_stone/recipe/" + name + ".json");
        assertEquals(pattern, strings(recipe.getAsJsonArray("pattern")));
        assertEquals("mossy_stone:mossy_stone", recipe.getAsJsonObject("key").get("#").getAsString());
        assertResult(recipe, "mossy_stone:" + name, count);
    }

    private static void assertStonecutting(String name, int count) throws IOException {
        JsonObject recipe = json("data/mossy_stone/recipe/" + name
                + "_from_mossy_stone_stonecutting.json");
        assertEquals("minecraft:stonecutting", recipe.get("type").getAsString());
        assertEquals("mossy_stone:mossy_stone", recipe.get("ingredient").getAsString());
        assertResult(recipe, "mossy_stone:" + name, count);
    }

    private static void assertResult(JsonObject recipe, String id, int count) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(id, result.get("id").getAsString());
        assertEquals(count, result.has("count") ? result.get("count").getAsInt() : 1);
    }

    private static Path reference(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, property);
        Path path = Path.of(value);
        assertTrue(Files.isRegularFile(path), path.toString());
        return path;
    }

    private static JsonObject json(String relative) throws IOException {
        return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
    }

    private static String text(ZipFile zip, String entry) throws IOException {
        ZipEntry exact = zip.getEntry(entry);
        assertNotNull(exact, entry);
        try (InputStream input = zip.getInputStream(exact)) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().withUpperCase().formatHex(digest.digest());
        }
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
