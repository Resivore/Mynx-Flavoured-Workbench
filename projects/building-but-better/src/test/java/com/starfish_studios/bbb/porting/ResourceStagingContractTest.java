package com.starfish_studios.bbb.porting;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceStagingContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("user.dir")).toAbsolutePath();

    @Test
    void exactOriginalIdentityIsCheckedBeforeArrResourcesAreOpened() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));

        assertTrue(build.contains("expectedOriginalName = 'bbb-fabric-2.0pre4.jar'"));
        assertTrue(build.contains("expectedOriginalSize = 1_701_505L"));
        assertTrue(build.contains("expectedOriginalSha256 = '1E7AE114AAEC53475133E11C607FC65DCE493BBA5897EAF0044D53959B508FC0'"));
        int sizeGuard = build.indexOf("originalJar.length() != expectedOriginalSize");
        int hashGuard = build.indexOf("actualHash != expectedOriginalSha256");
        int archiveOpen = build.indexOf("ZipFile zip = new ZipFile(originalJar)");
        assertTrue(sizeGuard >= 0 && hashGuard > sizeGuard && archiveOpen > hashGuard,
                "The exact size and SHA-256 must be checked before reading the ARR archive");
        assertTrue(build.contains("inputs.file(providers.provider { locateOriginalJar() })"),
                "The pristine JAR must participate in Gradle's input fingerprint");
        assertEquals(2, occurrences(build, "requireExactOriginalJar()"),
                "Staging and verification must both require the current exact pristine JAR");
    }

    @Test
    void generatedResourcesStayInIgnoredBuildOutput() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        Path resources = PROJECT.resolve("src/main/resources");

        assertTrue(build.contains("layout.buildDirectory.dir('generated/bbb-resources')"));
        assertTrue(build.contains("main.resources.srcDir(stagedResourcesDir)"));
        assertTrue(build.contains("dependsOn tasks.named('stageBbbResources')"));
        assertFalse(Files.exists(resources.resolve("assets/bbb")), "ARR assets must not be tracked in source");
        assertFalse(Files.exists(resources.resolve("data/bbb")), "ARR data must not be tracked in source");
    }

    @Test
    void stagingAndVerificationAreCuratedRatherThanWholeJarCopies() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));

        assertTrue(build.contains("expected_retained_block_count"));
        assertTrue(build.contains("expected_retained_item_count"));
        assertTrue(build.contains("assets/bbb/blockstates/${it}.json"));
        assertTrue(build.contains("assets/bbb/items/${id}.json"));
        assertTrue(build.contains("replace('/recipes/', '/recipe/')"));
        assertTrue(build.contains("data/bbb/loot_table/blocks/${id}.json"));
        assertTrue(build.contains("referencesRemoved"));
        assertTrue(build.contains("references removed or unknown BBB ID"));
        assertTrue(build.contains("if (blockstates != retainedBlocks.size())"));
        assertTrue(build.contains("if (itemDefinitions != retainedItems.size())"));
        assertTrue(build.contains("if (lootTables != retainedBlocks.size())"));
    }

    @Test
    void generatedRecipesAndLootUseMinecraft262ValueTypes() throws IOException {
        Path recipes = PROJECT.resolve("build/generated/bbb-resources/data/bbb/recipe");
        Path lootTables = PROJECT.resolve("build/generated/bbb-resources/data/bbb/loot_table/blocks");
        Pattern resultObject = Pattern.compile("\\\"result\\\"\\s*:\\s*\\{");
        Pattern stringAdd = Pattern.compile("\\\"add\\\"\\s*:\\s*\\\"");

        List<Path> recipeFiles;
        try (Stream<Path> files = Files.list(recipes)) {
            recipeFiles = files.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
        }
        assertEquals(224, recipeFiles.size());
        for (Path recipe : recipeFiles) {
            assertTrue(resultObject.matcher(Files.readString(recipe)).find(),
                    () -> recipe.getFileName() + " lost the Minecraft 26.2 result object schema");
        }

        List<Path> beamSlabLoot;
        try (Stream<Path> files = Files.list(lootTables)) {
            beamSlabLoot = files.filter(path -> path.getFileName().toString().endsWith("_beam_slab.json")).toList();
        }
        assertEquals(12, beamSlabLoot.size());
        for (Path loot : beamSlabLoot) {
            assertFalse(stringAdd.matcher(Files.readString(loot)).find(),
                    () -> loot.getFileName() + " retained a string-valued loot function flag");
        }
    }

    @Test
    void stagedRecipeUnlockAdvancementsUseMinecraft262ShapesAndResolvableIds() throws IOException {
        Path advancements = PROJECT.resolve("build/generated/bbb-resources/data/bbb/advancement/recipes");
        Path recipes = PROJECT.resolve("build/generated/bbb-resources/data/bbb/recipe");
        Path legacyAdvancements = PROJECT.resolve("build/generated/bbb-resources/data/bbb/advancements");
        Pattern legacyTagKey = Pattern.compile("\\\"tag\\\"\\s*:");
        Pattern arrayValuedItemPredicate = Pattern.compile("\\\"items\\\"\\s*:\\s*\\[\\s*\\\"", Pattern.DOTALL);
        Pattern bbbReference = Pattern.compile("\\\"(#?bbb:[a-z0-9_./-]+)\\\"");
        Pattern rewardRecipes = Pattern.compile(
                "\\\"rewards\\\"\\s*:\\s*\\{\\s*\\\"recipes\\\"\\s*:\\s*\\[(.*?)]\\s*}",
                Pattern.DOTALL
        );
        Pattern criterionRecipe = Pattern.compile("\\\"recipe\\\"\\s*:\\s*\\\"(bbb:[a-z0-9_./-]+)\\\"");
        Pattern quotedValue = Pattern.compile("\\\"([^\\\"]+)\\\"");

        List<Path> advancementFiles;
        try (Stream<Path> files = Files.walk(advancements)) {
            advancementFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
        }
        assertEquals(140, advancementFiles.size());
        assertFalse(Files.exists(legacyAdvancements), "Legacy plural advancement directory must not be staged");

        Set<String> stagedRecipeIds = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(recipes)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(recipes::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .map(path -> "bbb:" + path.substring(0, path.length() - ".json".length()))
                    .forEach(stagedRecipeIds::add);
        }
        assertEquals(recipeFileCount(recipes), stagedRecipeIds.size());

        Set<String> retainedIds = retainedBbbIds();
        Set<String> allowedTagIds = Set.of(
                "balustrades", "beams", "braziers", "columns", "frames", "hammers", "lanterns",
                "lattices", "metal_fences", "mouldings", "pallets", "stone_blocks", "stone_columns",
                "stone_fences", "stone_frames", "supports", "trims", "urns", "wooden_blocks",
                "wooden_frames", "wooden_lanterns", "wooden_walls"
        );

        for (Path advancement : advancementFiles) {
            String json = Files.readString(advancement);
            assertFalse(legacyTagKey.matcher(json).find(),
                    () -> advancement.getFileName() + " retained a legacy item-predicate tag key");
            assertFalse(arrayValuedItemPredicate.matcher(json).find(),
                    () -> advancement.getFileName() + " retained an array-valued item predicate");

            Matcher rewards = rewardRecipes.matcher(json);
            assertTrue(rewards.find(), () -> advancement.getFileName() + " has no recipe reward");
            Matcher rewardedId = quotedValue.matcher(rewards.group(1));
            int rewardCount = 0;
            while (rewardedId.find()) {
                String recipeId = rewardedId.group(1);
                rewardCount++;
                assertRecipeExists(recipes, stagedRecipeIds, recipeId, advancement);
            }
            assertTrue(rewardCount > 0, () -> advancement.getFileName() + " has an empty recipe reward");

            Matcher criterion = criterionRecipe.matcher(json);
            while (criterion.find()) {
                assertRecipeExists(recipes, stagedRecipeIds, criterion.group(1), advancement);
            }

            Matcher reference = bbbReference.matcher(json);
            while (reference.find()) {
                String value = reference.group(1);
                if (value.startsWith("#bbb:")) {
                    String tag = value.substring("#bbb:".length());
                    assertTrue(allowedTagIds.contains(tag),
                            () -> advancement.getFileName() + " references removed or unknown BBB tag " + value);
                } else {
                    String id = value.substring("bbb:".length());
                    assertTrue(retainedIds.contains(id) || stagedRecipeIds.contains(value),
                            () -> advancement.getFileName() + " references removed or unknown BBB ID " + value);
                }
            }
        }
    }

    @Test
    void stagedSpritesUseMinecraft262BinaryAlphaClassificationWithoutInertModelMetadata() throws IOException {
        Path models = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/models");
        Path textures = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/textures");
        int transparentFiles = 0;
        List<Path> pngs;
        try (Stream<Path> files = Files.walk(textures)) {
            pngs = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .toList();
        }
        assertEquals(244, pngs.size());
        for (Path texture : pngs) {
            BufferedImage image = ImageIO.read(texture.toFile());
            assertTrue(image != null, () -> "Unable to decode " + texture);
            boolean hasTransparency = false;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    assertTrue(alpha == 0 || alpha == 255,
                            () -> texture + " contains partial alpha " + alpha);
                    hasTransparency |= alpha == 0;
                }
            }
            if (hasTransparency) transparentFiles++;
        }
        assertEquals(112, transparentFiles,
                "Minecraft 26.2 derives the section layer from these binary-alpha sprites");

        int modelCount = 0;
        try (Stream<Path> files = Files.walk(models)) {
            for (Path model : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                modelCount++;
                assertFalse(Files.readString(model).contains("\"render_type\""),
                        () -> model + " contains unsupported/inert 26.2 render_type metadata");
            }
        }
        assertEquals(1_132, modelCount);
    }

    @Test
    void paleOakAuthoredSheetsPreserveUvAlphaAndMaterialIndependentDetails() throws IOException {
        Path root = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/textures/block");
        List<String> sources = List.of(
                "balustrade/cherry_sides.png", "balustrade/cherry_top.png",
                "beam/cherry.png", "beam/cherry_top.png",
                "frame/cherry.png", "frame/cherry_sticks.png",
                "lantern/cherry.png", "lattice/cherry.png",
                "pallet/cherry_pallet.png", "sanded_planks/cherry.png",
                "trim/cherry.png", "trim/cherry_bottom.png",
                "trim/cherry_middle.png", "trim/cherry_top.png"
        );

        for (String sourceName : sources) {
            String targetName = sourceName.replace("cherry", "pale_oak");
            BufferedImage source = ImageIO.read(root.resolve(sourceName).toFile());
            BufferedImage target = ImageIO.read(root.resolve(targetName).toFile());
            assertTrue(source != null && target != null, () -> "Missing generated sheet " + targetName);
            assertEquals(source.getWidth(), target.getWidth(), () -> targetName + " changed authored UV width");
            assertEquals(source.getHeight(), target.getHeight(), () -> targetName + " changed authored UV height");

            int changedOpaque = 0;
            int preservedOpaque = 0;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int sourcePixel = source.getRGB(x, y);
                    int targetPixel = target.getRGB(x, y);
                    assertEquals(sourcePixel >>> 24, targetPixel >>> 24,
                            targetName + " changed alpha at " + x + "," + y);
                    if ((sourcePixel >>> 24) == 255) {
                        if (sourcePixel == targetPixel) preservedOpaque++;
                        else changedOpaque++;
                    }
                }
            }
            assertTrue(changedOpaque > 0, () -> targetName + " contains no Pale Oak material pixels");
            if (sourceName.equals("lantern/cherry.png")) {
                assertEquals(36, preservedOpaque,
                        "Lantern glow/metal pixels must survive the wood-only palette substitution");
            }
        }

        String staging = Files.readString(PROJECT.resolve("build/generated/bbb-resources/bbb-resource-staging.json"));
        assertTrue(staging.contains("\"pale_oak_texture_source\": \"minecraft:block/pale_oak_planks\""));
        assertEquals(14, occurrences(staging, "mapped_wood_pixels"));

        Path models = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/models/block");
        assertTrue(Files.readString(models.resolve("lantern/pale_oak.json"))
                .contains("bbb:block/lantern/pale_oak"));
        assertTrue(Files.readString(models.resolve("lattice/pale_oak_middle.json"))
                .contains("bbb:block/lattice/pale_oak"));
        assertTrue(Files.readString(models.resolve("frame/pale_oak_inventory.json"))
                .contains("bbb:block/frame/pale_oak"));
        assertTrue(Files.readString(models.resolve("pallet/pale_oak_pallet_bottom.json"))
                .contains("bbb:block/pallet/pale_oak_pallet"));
        for (String model : List.of("beam/pale_oak_beam.json", "beam/pale_oak_beam_stairs.json",
                "beam/pale_oak_beam_stairs_inner.json", "beam/pale_oak_beam_stairs_outer.json",
                "beam/pale_oak_beam_slab.json", "beam/pale_oak_beam_slab_top.json")) {
            String json = Files.readString(models.resolve(model));
            assertTrue(json.contains("bbb:block/beam/pale_oak"), () -> model + " lost BBB Pale Oak beam artwork");
            assertFalse(json.contains("minecraft:block/stripped_pale_oak_log"),
                    () -> model + " must not bind vanilla stripped Pale Oak logs");
        }
        assertTrue(Files.readString(models.resolve("lattice/pale_oak_left.json"))
                .contains("minecraft:block/pale_oak_log_top"));
        try (Stream<Path> files = Files.walk(PROJECT.resolve("build/generated/bbb-resources"))) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                assertFalse(Files.readString(file).contains("minecraft:minecraft:"),
                        () -> file + " contains a malformed duplicated namespace");
            }
        }
    }

    @Test
    void paleOakLatticePlacedBlockClosureUsesOnlyValidCanonicalStateAndResources() throws IOException {
        Path assets = PROJECT.resolve("build/generated/bbb-resources/assets/bbb");
        Path blockstate = assets.resolve("blockstates/pale_oak_lattice.json");
        String json = Files.readString(blockstate);

        // This must retain the Cherry lattice plant contract. LatticePlantType has
        // CHERRY_LEAVES but no PALE_OAK_LEAVES entry, and the plant model is shared.
        assertTrue(json.contains("\"plant_type\": \"cherry_leaves\""));
        assertTrue(json.contains("bbb:block/lattice/plants/cherry_leaves"));
        assertFalse(json.contains("pale_oak_leaves"));

        Set<String> roots = modelReferences(json, "model");
        assertTrue(roots.contains("bbb:block/lattice/pale_oak_left"));
        assertTrue(roots.contains("bbb:block/lattice/pale_oak_middle"));
        assertTrue(roots.contains("bbb:block/lattice/pale_oak_right"));
        assertBbbModelAndTextureClosure(assets, roots);
    }

    @Test
    void paleOakHasCompleteFormRecipeLootTagLanguageAndItemClosure() throws IOException {
        List<String> forms = List.of("balustrade", "lattice", "wall", "beam", "beam_stairs",
                "beam_slab", "support", "pallet", "frame", "lantern", "trim");
        Path generated = PROJECT.resolve("build/generated/bbb-resources");
        Path blockstates = generated.resolve("assets/bbb/blockstates");
        Path itemModels = generated.resolve("assets/bbb/models/item");
        Path itemDefinitions = generated.resolve("assets/bbb/items");
        Path recipes = generated.resolve("data/bbb/recipe");
        Path loot = generated.resolve("data/bbb/loot_table/blocks");

        for (String form : forms) {
            String id = "pale_oak_" + form;
            for (Path file : List.of(blockstates.resolve(id + ".json"), itemModels.resolve(id + ".json"),
                    itemDefinitions.resolve(id + ".json"), recipes.resolve(id + ".json"),
                    loot.resolve(id + ".json"))) {
                assertTrue(Files.isRegularFile(file), () -> "Missing Pale Oak closure file " + file);
                String json = Files.readString(file);
                String materialOnlyJson = file.equals(blockstates.resolve(id + ".json")) && form.equals("lattice")
                        ? json.replace("cherry_leaves", "")
                        : json;
                assertFalse(materialOnlyJson.contains("cherry"), () -> file + " retains a Cherry material reference");
            }
        }

        assertFalse(Files.exists(blockstates.resolve("pale_oak_layer.json")));
        assertFalse(Files.exists(blockstates.resolve("pale_oak_ladder.json")));
        try (Stream<Path> languages = Files.list(generated.resolve("assets/bbb/lang"))) {
            for (Path language : languages.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String json = Files.readString(language);
                for (String form : forms) {
                    assertTrue(json.contains("\"block.bbb.pale_oak_" + form + "\""),
                            () -> language + " lacks Pale Oak " + form);
                }
            }
        }

        try (Stream<Path> tags = Files.walk(generated.resolve("data"))) {
            for (Path tag : tags.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/tags/"))
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String json = Files.readString(tag);
                for (String form : forms) {
                    if (json.contains("bbb:cherry_" + form)) {
                        assertTrue(json.contains("bbb:pale_oak_" + form),
                                () -> tag + " did not mirror the Cherry tag membership for " + form);
                    }
                }
            }
        }

        List<String> paleRecipes;
        try (Stream<Path> files = Files.list(recipes)) {
            paleRecipes = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("pale_oak_"))
                    .sorted()
                    .toList();
        }
        assertEquals(13, paleRecipes.size());
        assertTrue(paleRecipes.contains("pale_oak_planks_from_beam.json"));
        assertTrue(paleRecipes.contains("pale_oak_planks_from_beam_slab.json"));
    }

    @Test
    void darkOakLatticeUsesItsAuthoredMaterialModels() throws IOException {
        Path blockstate = PROJECT.resolve(
                "build/generated/bbb-resources/assets/bbb/blockstates/dark_oak_lattice.json");
        String json = Files.readString(blockstate);
        for (String part : List.of("left", "middle", "right")) {
            assertTrue(json.contains("bbb:block/lattice/dark_oak_" + part));
            assertFalse(json.contains("bbb:block/lattice/oak_" + part));
        }
    }

    @Test
    void stagedBeamBlockstatesCoverEveryNativeAxisAndDirectionalSlabState() throws IOException {
        Path blockstates = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/blockstates");
        String beam = Files.readString(blockstates.resolve("pale_oak_beam.json"));
        String slab = Files.readString(blockstates.resolve("pale_oak_beam_slab.json"));

        for (String axis : List.of("x", "y", "z")) {
            assertTrue(beam.contains("\"axis=" + axis + "\""), () -> "Pale Oak Beam lost axis=" + axis);
        }
        assertEquals(3, occurrences(beam, "\"axis="));

        for (String facing : List.of("up", "down", "north", "south", "east", "west")) {
            for (String type : List.of("bottom", "top", "double")) {
                assertTrue(slab.contains("\"facing=" + facing + ",type=" + type + "\""),
                        () -> "Pale Oak Beam Slab lost facing=" + facing + ",type=" + type);
            }
        }
        assertEquals(18, occurrences(slab, "\"facing="));
    }

    @Test
    void stagedLanguageContainsNoRemovedFamilyKeys() throws IOException {
        Path languages = PROJECT.resolve("build/generated/bbb-resources/assets/bbb/lang");
        try (Stream<Path> files = Files.list(languages)) {
            for (Path language : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String json = Files.readString(language);
                assertFalse(json.contains("block_type.bbb.ladder"));
                assertFalse(json.contains("block_type.bbb.layer"));
                assertFalse(json.contains("item.bbb.chisel"));
                assertFalse(json.contains("item.bbb.bbb"));
                assertFalse(json.contains("description.bbb.frameConfig3"));
                assertFalse(json.contains("description.bbb.frameConfig4"));
                assertFalse(json.contains("description.bbb.frameConfig5"));
            }
        }
    }

    private static int occurrences(String text, String token) {
        int result = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            result++;
            offset += token.length();
        }
        return result;
    }

    private static Set<String> modelReferences(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        Set<String> references = new LinkedHashSet<>();
        while (matcher.find()) references.add(matcher.group(1));
        return references;
    }

    private static void assertBbbModelAndTextureClosure(Path assets, Set<String> roots) throws IOException {
        Deque<String> pendingModels = new ArrayDeque<>(roots);
        Set<String> visitedModels = new LinkedHashSet<>();
        Pattern bbbReference = Pattern.compile("bbb:block/[a-z0-9_./-]+");
        while (!pendingModels.isEmpty()) {
            String reference = pendingModels.removeFirst();
            if (!reference.startsWith("bbb:")) continue;
            Path model = assets.resolve("models/" + reference.substring("bbb:".length()) + ".json");
            assertTrue(Files.isRegularFile(model), () -> "Missing placed-lattice model " + reference);
            if (!visitedModels.add(reference)) continue;

            String json = Files.readString(model);
            for (String parent : modelReferences(json, "parent")) {
                if (parent.startsWith("bbb:")) pendingModels.addLast(parent);
            }
            Matcher matcher = bbbReference.matcher(json);
            while (matcher.find()) {
                String nested = matcher.group();
                Path nestedModel = assets.resolve("models/" + nested.substring("bbb:".length()) + ".json");
                Path texture = assets.resolve("textures/" + nested.substring("bbb:".length()) + ".png");
                assertTrue(Files.isRegularFile(nestedModel) || Files.isRegularFile(texture),
                        () -> "Missing placed-lattice model or texture " + nested);
            }
        }
    }

    private static long recipeFileCount(Path recipes) throws IOException {
        try (Stream<Path> files = Files.walk(recipes)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count();
        }
    }

    private static Set<String> retainedBbbIds() throws IOException {
        String manifest = Files.readString(PROJECT.resolve("src/porting/curated-registry.json"));
        Set<String> result = new LinkedHashSet<>();
        for (String material : stringArray(manifest, "wood_materials")) {
            for (String form : stringArray(manifest, "wood_forms")) result.add(material + "_" + form);
        }
        for (String material : stringArray(manifest, "stone_materials")) {
            for (String form : stringArray(manifest, "stone_forms")) result.add(material + "_" + form);
        }
        result.addAll(stringArray(manifest, "standalone_blocks"));
        result.addAll(stringArray(manifest, "standalone_items"));
        return result;
    }

    private static List<String> stringArray(String json, String key) {
        Matcher field = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
                .matcher(json);
        assertTrue(field.find(), () -> "Missing array " + key);
        Matcher values = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(field.group(1));
        List<String> result = new ArrayList<>();
        while (values.find()) result.add(values.group(1));
        return List.copyOf(result);
    }

    private static void assertRecipeExists(Path recipeRoot, Set<String> stagedRecipeIds,
                                           String recipeId, Path advancement) {
        assertTrue(recipeId.startsWith("bbb:"),
                () -> advancement.getFileName() + " rewards an unexpected recipe namespace " + recipeId);
        assertTrue(stagedRecipeIds.contains(recipeId),
                () -> advancement.getFileName() + " references unstaged recipe " + recipeId);
        String recipePath = recipeId.substring("bbb:".length()) + ".json";
        Path resolved = recipeRoot.resolve(recipePath).normalize();
        assertTrue(resolved.startsWith(recipeRoot) && Files.isRegularFile(resolved),
                () -> advancement.getFileName() + " recipe reward does not resolve to a staged file: " + recipeId);
    }
}
