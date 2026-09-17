package dev.resivore.matchajei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class C5StaticContractTest {
    @Test
    void resolvedRecipeCatalogAndRevisionStayAuthoritative() throws Exception {
        String scanner = source("server/MatchaDataScanner.java");

        assertTrue(scanner.contains("server.getRecipeManager().getRecipes()"));
        assertTrue(scanner.contains("SlotDisplayContext.REGISTRIES"));
        assertTrue(scanner.contains("display.result().resolveForStacks(displayContext)"));
        assertTrue(scanner.contains("MatchaExactCatalog.selectRecipeOutputs(rawFallbacks, resolvedOutputs)"));
        assertTrue(scanner.contains("Multiple possible outputs are intentionally skipped rather than guessed."));
        assertTrue(scanner.contains("revisionOf(recipes, state.trades, lootTables, effectiveRecipeOutputs)"));
        assertTrue(scanner.contains("effectiveRecipeOutputs.entrySet()"));
        assertTrue(scanner.contains("isNonRecipeIdentity(identity, recipeIdentities)"));
        assertTrue(scanner.contains("MatchaFoodDiscoveryCatalog.Source.EFFECTIVE_RECIPE"));
        assertTrue(scanner.contains("MatchaFoodDiscoveryCatalog.Source.NON_RECIPE"));
        assertTrue(scanner.contains("MatchaFoodDiscoveryCatalog.canonicalize(catalog.values())"));
    }

    @Test
    void jeiUsesTheSameCatalogAndRetainsTheExactOwnedSubtypeRoute() throws Exception {
        String runtime = source("client/MatchaJeiRuntimeData.java");
        String plugin = source("client/MatchaJeiPlugin.java");
        String recipeBridge = source("client/MatchaExactRecipeBridge.java");

        assertTrue(runtime.contains("payload.catalog()"));
        assertTrue(runtime.contains("payload.canonicalDefaults()"));
        assertTrue(runtime.contains("!MatchaJeiPlugin.usesJeiOwnedSubtype(candidate)"));
        assertTrue(runtime.contains(".filter(MatchaJeiPlugin::usesJeiOwnedSubtype)"));
        assertTrue(runtime.contains("MatchaExactIngredient.TYPE, exactCandidates"));
        assertTrue(plugin.contains("Identifier.withDefaultNamespace(\"enchanted_book\")"));
        assertTrue(plugin.contains("addInvisibleIngredients(role)"));
        assertTrue(plugin.contains("createFocusLink(visibleSlot, exactIngredient)"));
        assertTrue(plugin.contains("registration.addRecipeManagerPlugin(new MatchaExactRecipeBridge())"));
        assertTrue(recipeBridge.contains("MatchaJeiRuntimeData.findVanillaRecipes("));
        assertTrue(recipeBridge.contains("RecipeTypes.CRAFTING"));
        assertTrue(runtime.contains("createRecipeLookup(recipeType)"));
        assertTrue(runtime.contains("VanillaTypes.ITEM_STACK"));
        assertFalse(plugin.contains("runtime.getIngredientManager().addIngredientsAtRuntime"));
        assertFalse(plugin.contains("runtime.getRecipeManager().addRecipes"));
    }

    @Test
    void creativeSearchUsesOnlyTheSharedCatalogWithoutJeiClassloading() throws Exception {
        String creative = source("client/MatchaCreativeCatalog.java");
        String entries = source("client/MatchaCreativeSearchEntries.java");
        String clientData = source("client/MatchaClientData.java");

        assertTrue(creative.contains("CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)"));
        assertTrue(creative.contains("CreativeModeTabEvents.MODIFY_OUTPUT_ALL"));
        assertTrue(creative.contains("ClientTickEvents.END_CLIENT_TICK"));
        assertTrue(creative.contains("CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY"));
        assertTrue(creative.contains("MatchaClientData.current().catalog()"));
        assertTrue(creative.contains("MatchaClientData.current().canonicalDefaults()"));
        assertTrue(creative.contains("CreativeModeTabs.searchTab().getDisplayItems()"));
        assertTrue(creative.contains("MatchaCreativeSearchEntries.replaceOwned("));
        assertTrue(creative.contains("SUPPRESSED_DEFAULT_SEARCH_ENTRIES"));
        assertEquals(1, occurrences(creative, "MatchaCreativeSearchEntries.retainActiveOwnership("));
        assertTrue(entries.contains("ItemStack.isSameItemSameComponents(existing, contribution)"));
        assertTrue(entries.contains("searchContents.removeIf(ownedEntries::contains)"));
        assertTrue(entries.contains("isCanonicalDefault(candidate, canonicalDefaults)"));
        assertTrue(entries.contains("suppressCanonicalDefaults("));
        assertTrue(entries.contains("restoreSuppressed(searchContents, suppressedDefaultEntries)"));
        assertFalse(creative.contains("CreativeModeTabs.tryRebuildTabContents("));
        assertFalse(creative.contains("CreativeModeTabs.searchTab().buildContents("));
        assertFalse(creative.contains("getDisplayItems().clear("));
        assertFalse(creative.contains("mezz.jei"));
        assertFalse(entries.contains("mezz.jei"));
        assertFalse(clientData.contains("mezz.jei"));
    }

    @Test
    void canonicalFoodCatalogUsesProvenanceAndRestoresJeiDefaults() throws Exception {
        String replacements = source("data/MatchaFoodDiscoveryCatalog.java");
        String runtime = source("client/MatchaJeiRuntimeData.java");

        assertTrue(replacements.contains("DataComponents.FOOD"));
        assertTrue(replacements.contains("DataComponents.CONSUMABLE"));
        assertTrue(replacements.contains("onConsumeEffects().isEmpty()"));
        assertTrue(replacements.contains("Source.EFFECTIVE_RECIPE"));
        assertTrue(replacements.contains("Source.NON_RECIPE"));
        assertTrue(replacements.contains("SOURCE_VARIANT_COMPONENTS"));
        assertTrue(replacements.contains("DEFAULT_ENRICHMENT_COMPONENTS"));
        assertTrue(replacements.contains("stack.getComponents().forEach"));
        assertTrue(replacements.contains("DataComponents.ITEM_MODEL"));
        assertTrue(runtime.contains("removeCanonicalFoodDefaults(canonicalDefaults)"));
        assertTrue(runtime.contains("removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, present)"));
        assertTrue(runtime.contains("addNewIngredients(VanillaTypes.ITEM_STACK, introduced.suppressedVanilla())"));
    }

    @Test
    void c3TradeAndAcquisitionBoundariesRemainPresentWithoutFakeVanillaRecipes() throws Exception {
        String scanner = source("server/MatchaDataScanner.java");
        String integration = source("MatchaJeiIntegration.java");

        assertTrue(scanner.contains("FileToIdConverter.registry(Registries.VILLAGER_TRADE)"));
        assertTrue(scanner.contains("FileToIdConverter.registry(Registries.LOOT_TABLE)"));
        assertTrue(scanner.contains("retainAssignedTrades"));
        assertTrue(scanner.contains("COUNT_ONLY_LOOT_FUNCTIONS"));
        assertTrue(integration.contains("handler.getPlayer().awardRecipes(recipes)"));
        assertFalse(integration.contains("addRecipes("));
    }

    private static String source(String relativePath) throws Exception {
        Path sourceRoot = Path.of(System.getProperty("projectRoot"))
                .resolve("src/main/java/dev/resivore/matchajei");
        return Files.readString(sourceRoot.resolve(relativePath));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
