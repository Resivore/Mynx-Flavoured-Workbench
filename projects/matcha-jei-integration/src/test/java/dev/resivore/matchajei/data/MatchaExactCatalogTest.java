package dev.resivore.matchajei.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MatchaExactCatalogTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!Items.ENCHANTED_BOOK.builtInRegistryHolder().areComponentsBound()) {
            Items.ENCHANTED_BOOK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    @Test
    void exactCatalogDeduplicatesByFullComponentIdentityAndNormalizesCount() {
        ItemStack demeter = namedBook("Prayer of Demeter", "Frost Protection III");
        demeter.setCount(7);
        ItemStack duplicate = demeter.copyWithCount(1);
        ItemStack differentLore = namedBook("Prayer of Demeter", "Frost Walker II");

        List<ItemStack> catalog = MatchaExactCatalog.normalizeAndDeduplicate(
                List.of(demeter, duplicate, differentLore));

        assertEquals(2, catalog.size());
        assertEquals(1, catalog.getFirst().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(duplicate, catalog.getFirst()));
        assertFalse(ItemStack.isSameItemSameComponents(catalog.getFirst(), catalog.get(1)));
    }

    @Test
    void knownResolvedOutputSupersedesRawRecipeIdentityWithoutRetainingTheOldStack() {
        ItemStack rawDemeter = namedBook("Prayer of Demeter", "Frost Walker II");
        ItemStack effectiveDemeter = namedBook("Prayer of Demeter", "Frost Protection III");
        effectiveDemeter.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("blessing_demeter"));

        Map<String, ItemStack> raw = new LinkedHashMap<>();
        raw.put("blessings:frost_walker_frost_protection", rawDemeter);
        Map<String, Optional<ItemStack>> resolved = new LinkedHashMap<>();
        resolved.put("blessings:frost_walker_frost_protection", Optional.of(effectiveDemeter));

        Map<String, ItemStack> selected = MatchaExactCatalog.selectRecipeOutputs(raw, resolved);

        assertEquals(1, selected.size());
        ItemStack actual = selected.get("blessings:frost_walker_frost_protection");
        assertEquals(1, actual.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(effectiveDemeter, actual));
        assertFalse(ItemStack.isSameItemSameComponents(rawDemeter, actual));
        assertEquals(Identifier.withDefaultNamespace("blessing_demeter"), actual.get(DataComponents.ITEM_MODEL));
    }

    @Test
    void canonicalFrostReplacementPreservesDemeterPresentationWhileReplacingStoredEnchantments() throws Exception {
        Path canonicalRecipe = Path.of(System.getProperty("workbenchRoot"))
                .resolve("projects/matcha-frost-protection/src/main/resources")
                .resolve("matcha_frost_protection/canonical/blessing_of_demeter.json");
        String recipe = Files.readString(canonicalRecipe);

        assertTrue(recipe.contains("\"main:freezing_protection\": 3"));
        assertFalse(recipe.contains("minecraft:frost_walker"));
        assertFalse(recipe.contains("minecraft:frost_protection"));
        assertTrue(recipe.contains("\"minecraft:item_name\""));
        assertTrue(recipe.contains("\"minecraft:lore\""));
        assertTrue(recipe.contains("\"minecraft:item_model\": \"minecraft:blessing_demeter\""));
    }

    private static ItemStack namedBook(String name, String lore) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        return stack;
    }
}
