package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MixinScopeTest {
    @Test
    void traversalMixinTargetsOnlyVanillasWalkabilityDecision() throws IOException {
        String source = readSource("mixin/PowderSnowBlockMixin.java");
        assertTrue(source.contains("@Mixin(PowderSnowBlock.class)"));
        assertTrue(source.contains("canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z"));
        assertTrue(source.contains("@At(\"RETURN\")"));
        assertTrue(source.contains("!callbackInfo.getReturnValueZ()"));
        assertFalse(source.contains("getCollisionShape"));
        assertFalse(source.contains("entityInside"));
        assertFalse(source.contains("canFreeze"));
        assertFalse(source.contains("FROST_WALKER"));
    }

    @Test
    void freezingMixinUsesVanillasLeatherWearableDecisionWithoutMutatingTicksOrHud()
            throws IOException {
        String source = readSource("mixin/LivingEntityMixin.java");
        assertTrue(source.contains("@Mixin(LivingEntity.class)"));
        assertTrue(source.contains("canFreeze()Z"));
        assertTrue(source.contains("@At(\"RETURN\")"));
        assertTrue(source.contains("callbackInfo.getReturnValueZ()"));
        assertTrue(source.contains("preventsVanillaFreezing"));
        assertFalse(source.contains("setTicksFrozen"));
        assertFalse(source.contains("clearFreeze"));
        assertFalse(source.contains("hurtServer"));
        assertFalse(source.contains("entityInside"));
        assertFalse(source.contains("Hud"));
        assertFalse(source.contains("@Overwrite"));
        assertFalse(source.contains("@Redirect"));
    }

    @Test
    void coldWaterMixinRedirectsOnlyTheExactResolvedMatchaConditionFunction()
            throws IOException {
        String source = readSource("mixin/ServerFunctionManagerMixin.java");
        String enforcer = readSource("MatchaFreezingWaterFunctionEnforcer.java");
        assertTrue(source.contains("@Mixin(ServerFunctionManager.class)"));
        assertTrue(source.contains("method = \"get("));
        assertTrue(source.contains("MatchaFreezingWaterFunctionEnforcer.targets"));
        assertTrue(source.contains("library.getFunctions()"));
        assertTrue(enforcer.contains(
                "main:environmental/check_freezing_water_conditions"));
        assertTrue(enforcer.contains(
                "matcha_frost_protection:environmental/check_freezing_water_conditions"));
        assertFalse(source.contains("freezing_water"));
        assertFalse(source.contains("effect"));
        assertFalse(source.contains("damage"));
    }

    @Test
    void recipeMixinTargetsTheResolvedRecipeMapArgument() throws IOException {
        String source = readSource("mixin/RecipeManagerMixin.java");
        assertTrue(source.contains("@Mixin(RecipeManager.class)"));
        assertTrue(source.contains("@ModifyVariable"));
        assertTrue(source.contains("method = \"apply("));
        assertTrue(source.contains("argsOnly = true"));
        assertTrue(source.contains("ordinal = 0"));
        assertTrue(source.contains("BlessingRecipeEnforcer.enforce"));
    }

    @Test
    void registersExactlyTheFourFocusedMixins() throws IOException {
        Path config = projectRoot().resolve("src/main/resources/matcha_frost_protection.mixins.json");
        JsonObject json = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
        JsonArray mixins = json.getAsJsonArray("mixins");
        Set<String> names = mixins.asList().stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of(
                "LivingEntityMixin",
                "PowderSnowBlockMixin",
                "RecipeManagerMixin",
                "ServerFunctionManagerMixin"), names);
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(projectRoot()
                .resolve("src/main/java/dev/resivore/matchafrost")
                .resolve(relativePath));
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }
}
