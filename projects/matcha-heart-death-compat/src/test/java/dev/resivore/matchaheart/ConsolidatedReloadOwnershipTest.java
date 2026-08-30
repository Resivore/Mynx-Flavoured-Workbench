package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ConsolidatedReloadOwnershipTest {
    private static final Path WORKBENCH = Path.of(System.getProperty("workbenchRoot"));
    private static final Path PROJECT = WORKBENCH.resolve("projects/matcha-heart-death-compat");

    @Test
    void exactlyOneHeartHookOwnsEachReloadBoundary() throws Exception {
        JsonObject mixins = packagedJson("/matcha_heart_death_compat.mixins.json");
        assertTrue(mixins.get("required").getAsBoolean());
        assertEquals(1, mixins.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertEquals(List.of(
                "RecipeManagerMixin",
                "ServerAdvancementManagerMixin",
                "ServerFunctionManagerMixin"), strings(mixins.getAsJsonArray("mixins")));

        String recipeMixin = source("src/main/java/dev/resivore/matchaheart/mixin/RecipeManagerMixin.java");
        assertEquals(1, occurrences(recipeMixin, "@ModifyVariable("));
        assertTrue(recipeMixin.contains(
                "method = \"apply(Lnet/minecraft/world/item/crafting/RecipeMap;"
                        + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                        + "Lnet/minecraft/util/profiling/ProfilerFiller;)V\""));
        assertTrue(recipeMixin.contains("at = @At(\"HEAD\")"));
        assertTrue(recipeMixin.contains("argsOnly = true"));
        assertTrue(recipeMixin.contains("ordinal = 0"));
        assertTrue(recipeMixin.contains("require = 1"));
        assertTrue(recipeMixin.contains("RecipeMapEnforcer.enforce(resolved, this.registries)"));
        assertFalse(recipeMixin.contains("method = \"prepare"));
        assertFalse(recipeMixin.contains("CallbackInfoReturnable"));
        assertFalse(recipeMixin.contains("cancellable"));
        assertFalse(recipeMixin.contains("setReturnValue"));

        String recipeEnforcer = source(
                "src/main/java/dev/resivore/matchaheart/RecipeMapEnforcer.java");
        assertTrue(recipeEnforcer.contains("HeartDataContract.RECIPE_RESOURCES"));
        assertTrue(recipeEnforcer.contains("REQUIRED_OWN_RECIPES"));
        assertTrue(recipeEnforcer.contains("HeartDataContract.REINFORCED_RECIPE_ID"));
        assertTrue(recipeEnforcer.contains("HeartDataContract.RESONANT_FAVOUR_RECIPE_ID"));
        assertTrue(recipeEnforcer.contains("Required owned recipes did not decode"));
        assertTrue(recipeEnforcer.contains("RecipeMap.create(recipes)"));
        assertTrue(recipeEnforcer.contains("Unsafe Matcha recipe contracts"));
        assertFalse(recipeEnforcer.contains("AUTHORITATIVE_RECIPE"));

        String advancementMixin = source(
                "src/main/java/dev/resivore/matchaheart/mixin/ServerAdvancementManagerMixin.java");
        assertEquals(1, occurrences(advancementMixin, "@ModifyVariable("));
        assertTrue(advancementMixin.contains("HeartDataContract.ADVANCEMENT_RESOURCES"));
        assertTrue(advancementMixin.contains("at = @At(\"HEAD\")"));
        assertFalse(advancementMixin.contains("NEUTRAL_ADVANCEMENT"));

        String initializer = source(
                "src/main/java/dev/resivore/matchaheart/MatchaHeartDeathCompat.java");
        assertEquals(1, occurrences(initializer, "LootTableEvents.REPLACE.register("));
        assertTrue(initializer.contains("HeartDataContract.LOOT_TABLE_RESOURCES"));

        String authoritativeData = source(
                "src/main/java/dev/resivore/matchaheart/AuthoritativeData.java");
        assertTrue(authoritativeData.contains("getModContainer(MatchaHeartDeathCompat.MOD_ID)"));
        assertFalse(authoritativeData.contains("getResourceAsStream"));
    }

    @Test
    void recipeOwnershipRunsAfterTheAcceptedDramaticDoorsPrepareReturnHook() throws Exception {
        String heartMixin = source(
                "src/main/java/dev/resivore/matchaheart/mixin/RecipeManagerMixin.java");
        String dramaticDoorsMixin = Files.readString(WORKBENCH.resolve(
                "projects/dramatic-doors/common/src/main/java/com/fizzware/dramaticdoors/mixin/RecipeManagerMixin.java"),
                StandardCharsets.UTF_8);

        assertTrue(dramaticDoorsMixin.contains("method = \"prepare("));
        assertTrue(dramaticDoorsMixin.contains("at = @At(\"RETURN\")"));
        assertTrue(dramaticDoorsMixin.contains("cancellable = true"));
        assertTrue(dramaticDoorsMixin.contains("cir.setReturnValue("));

        assertTrue(heartMixin.contains("method = \"apply("));
        assertTrue(heartMixin.contains("at = @At(\"HEAD\")"));
        assertFalse(heartMixin.contains("method = \"prepare("));
        assertFalse(heartMixin.contains("setReturnValue("));
    }

    @Test
    void unifiedHeartArtifactRejectsConcurrentStandaloneRuntimeMod() throws Exception {
        JsonObject metadata = JsonParser.parseString(
                source("build/resources/main/fabric.mod.json")).getAsJsonObject();
        assertEquals("*", metadata.getAsJsonObject("breaks")
                .get("matcha_echo_shard_scarcity").getAsString());
        JsonArray mainEntrypoints = metadata.getAsJsonObject("entrypoints").getAsJsonArray("main");
        assertEquals(1, mainEntrypoints.size());
        assertEquals("dev.resivore.matchaheart.MatchaHeartDeathCompat",
                mainEntrypoints.get(0).getAsString());
    }

    private static JsonObject packagedJson(String path) throws IOException {
        var stream = ConsolidatedReloadOwnershipTest.class.getResourceAsStream(path);
        assertTrue(stream != null, "Missing packaged resource " + path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(PROJECT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(element -> element.getAsString()).toList();
    }
}
