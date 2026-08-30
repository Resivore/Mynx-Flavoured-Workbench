package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlessingRecipeEnforcerTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void replacesOnlyTheExactResolvedDemeterRecipe() {
        ResourceKey<Recipe<?>> targetKey = recipeKey(BlessingRecipeEnforcer.TARGET_RECIPE);
        ResourceKey<Recipe<?>> unrelatedKey = recipeKey(Identifier.parse("example:unrelated"));
        Recipe<?> original = stubRecipe("original");
        Recipe<?> unrelated = stubRecipe("unrelated");
        Recipe<?> replacement = stubRecipe("replacement");
        RecipeMap resolved = RecipeMap.create(List.of(
                new RecipeHolder<>(targetKey, original),
                new RecipeHolder<>(unrelatedKey, unrelated)));

        RecipeMap enforced = BlessingRecipeEnforcer.enforce(resolved, () -> replacement);

        assertEquals(2, enforced.values().size());
        assertSame(replacement, enforced.byKey(targetKey).value());
        assertSame(unrelated, enforced.byKey(unrelatedKey).value());
    }

    @Test
    void failsClosedWhenMatchasTargetRecipeIsMissing() {
        AtomicBoolean loaderCalled = new AtomicBoolean();
        RecipeMap unrelatedOnly = RecipeMap.create(List.of(new RecipeHolder<>(
                recipeKey(Identifier.parse("example:unrelated")),
                stubRecipe("unrelated"))));

        assertThrows(IllegalStateException.class, () -> BlessingRecipeEnforcer.enforce(
                unrelatedOnly,
                () -> {
                    loaderCalled.set(true);
                    return stubRecipe("replacement");
                }));
        assertFalse(loaderCalled.get(), "Do not decode or add Demeter when Matcha did not resolve it");
    }

    private static ResourceKey<Recipe<?>> recipeKey(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }

    private static Recipe<?> stubRecipe(String name) {
        return (Recipe<?>) Proxy.newProxyInstance(
                BlessingRecipeEnforcerTest.class.getClassLoader(),
                new Class<?>[] {Recipe.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getType" -> RecipeType.CRAFTING;
                    case "toString" -> name;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
