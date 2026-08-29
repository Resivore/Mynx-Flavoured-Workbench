package dev.resivore.dragonbound.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonboundRecipeUnlocksTest {
    private static final List<String> EXPECTED_IDS = List.of(
            "dragonbound_waystone:dragonbound_waystone",
            "dragonbound_waystone:imbued_void_pearl",
            "dragonbound_waystone:dragonbound_staff");

    @BeforeAll
    static void bootstrapVanillaRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void contractContainsExactlyTheThreeDragonboundRecipeKeysInStableOrder() {
        assertEquals(EXPECTED_IDS, identifiers(DragonboundRecipeUnlocks.recipeKeys()));
    }

    @Test
    void onlyLoadedDragonboundRecipesAreSelectedAndMissingIdsAreNotSynthesized() {
        List<ResourceKey<Recipe<?>>> keys = DragonboundRecipeUnlocks.recipeKeys();
        Map<ResourceKey<Recipe<?>>, String> loadedRecipes = new LinkedHashMap<>();
        loadedRecipes.put(keys.get(0), "waystone-holder");
        loadedRecipes.put(keys.get(2), "staff-holder");

        ResourceKey<Recipe<?>> unrelated = ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                Identifier.parse("minecraft:crafting_table"));
        loadedRecipes.put(unrelated, "unrelated-holder");

        List<ResourceKey<Recipe<?>>> queried = new ArrayList<>();
        List<String> selected = DragonboundRecipeUnlocks.selectLoaded(key -> {
            queried.add(key);
            return Optional.ofNullable(loadedRecipes.get(key));
        });

        assertEquals(keys, queried);
        assertEquals(List.of("waystone-holder", "staff-holder"), selected);
        assertFalse(selected.contains("unrelated-holder"));
    }

    @Test
    void repeatedJoinResolutionIsDeterministicAndDoesNotBroadenTheKeySet() {
        Map<ResourceKey<Recipe<?>>, String> loadedRecipes = new LinkedHashMap<>();
        List<ResourceKey<Recipe<?>>> keys = DragonboundRecipeUnlocks.recipeKeys();
        for (int index = 0; index < keys.size(); index++) {
            loadedRecipes.put(keys.get(index), "holder-" + index);
        }

        List<String> firstJoin = DragonboundRecipeUnlocks.selectLoaded(
                key -> Optional.ofNullable(loadedRecipes.get(key)));
        List<String> reconnect = DragonboundRecipeUnlocks.selectLoaded(
                key -> Optional.ofNullable(loadedRecipes.get(key)));

        assertEquals(List.of("holder-0", "holder-1", "holder-2"), firstJoin);
        assertEquals(firstJoin, reconnect);
    }

    @Test
    void vanillaKnownRecipePersistenceIsIdempotentForRepeatedExactKeys() {
        ServerRecipeBook recipeBook = new ServerRecipeBook((recipe, displayConsumer) -> {
        });

        DragonboundRecipeUnlocks.recipeKeys().forEach(recipeBook::add);
        DragonboundRecipeUnlocks.recipeKeys().forEach(recipeBook::add);

        assertEquals(Set.copyOf(DragonboundRecipeUnlocks.recipeKeys()), Set.copyOf(recipeBook.pack().known()));
        assertEquals(3, recipeBook.pack().known().size());
    }

    @Test
    void commonJoinHookAwardsOnlyResolvedHoldersWithoutAdvancementMutation() throws IOException {
        String unlocks = Files.readString(Path.of(
                "src/main/java/dev/resivore/dragonbound/recipe/DragonboundRecipeUnlocks.java"));
        String initializer = Files.readString(Path.of(
                "src/main/java/dev/resivore/dragonbound/DragonboundWaystone.java"));

        assertTrue(unlocks.contains("ServerPlayConnectionEvents.JOIN.register"));
        assertTrue(unlocks.contains("awardRecipes(loadedRecipes(server.getRecipeManager()))"));
        assertTrue(unlocks.contains("selectLoaded(recipeManager::byKey)"));
        assertFalse(unlocks.contains(".getRecipes()"));
        assertFalse(unlocks.contains("awardRecipesByKey"));
        assertFalse(unlocks.toLowerCase(java.util.Locale.ROOT).contains("advancement"));
        assertTrue(initializer.contains("DragonboundRecipeUnlocks.install()"));
    }

    private static List<String> identifiers(List<ResourceKey<Recipe<?>>> keys) {
        return keys.stream()
                .map(ResourceKey::identifier)
                .map(Identifier::toString)
                .toList();
    }
}
