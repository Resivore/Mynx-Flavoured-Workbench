package dev.resivore.inventorycrafting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticImplementationContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void commonMixinIsRequiredAndFailsClosedAtEveryNarrowHook() throws IOException {
        String config = read("src/main/resources/inherent_3x3_inventory_crafting.mixins.json");
        String menu = read("src/main/java/dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.java");

        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"compatibilityLevel\": \"JAVA_25\""));
        assertTrue(config.contains("\"defaultRequire\": 1"));
        assertTrue(menu.contains("@Mixin(value = InventoryMenu.class, priority = 250)"));
        assertTrue(menu.contains("require = 2"));
        assertTrue(menu.contains("expect = 2"));
        assertTrue(menu.contains("verifyPreAppend"));
        assertTrue(menu.contains("verifyPostAppend"));
        assertTrue(menu.contains("inventory3x3$verifySnapshotSizes"));
    }

    @Test
    void implementationOwnsOnlyInventoryMenuAndReusesVanillaCraftingSemantics() throws IOException {
        String menu = read("src/main/java/dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.java");
        String layout = read("src/main/java/dev/resivore/inventorycrafting/InventoryCraftingLayout.java");
        String allMain = readTree("src/main/java");

        assertTrue(menu.contains("extends AbstractCraftingMenu"));
        assertTrue(menu.contains("this.craftSlots"));
        assertTrue(menu.contains("getInputGridSlots"));
        assertTrue(layout.contains("EXPECTED_ROW_MAJOR_MENU_IDS = {1, 2, 74, 3, 4, 75, 76, 77, 78}"));
        assertFalse(allMain.contains("@Mixin(CraftingMenu.class)"));
        assertFalse(allMain.contains("@Mixin(value = CraftingMenu.class"));
        assertFalse(allMain.contains("addAdditionalSaveData"));
        assertFalse(allMain.contains("readAdditionalSaveData"));
        assertFalse(allMain.contains("CompoundTag"));
    }

    @Test
    void routingAndCreativeGuardsAreContainerIdentityBased() throws IOException {
        String menu = read("src/main/java/dev/resivore/inventorycrafting/mixin/InventoryMenuMixin.java");
        String creative = read("src/main/java/dev/resivore/inventorycrafting/mixin/client/CreativeModeInventoryScreenMixin.java");
        String layout = read("src/main/java/dev/resivore/inventorycrafting/InventoryCraftingLayout.java");

        assertTrue(layout.contains("slot.container == menu.getCraftSlots()"));
        assertTrue(layout.contains("slot.container instanceof Inventory inventory"));
        assertTrue(menu.contains("player.getInventory().getNonEquipmentItems().size()"));
        assertTrue(menu.indexOf("isCraftingInput(menu, slot)") < menu.indexOf("moveItemStackTo("));
        assertTrue(creative.contains("hideCraftingWrappersByIdentity"));
        assertTrue(creative.contains("rejectDirectCraftingWrapperMutation"));
        assertTrue(creative.contains("excludeCraftInputsFromBulkClear"));
        assertTrue(creative.contains("excludeCraftInputsFromCreativeWrites"));
        assertTrue(creative.contains("args.set(2, -2000)"));
        assertTrue(creative.contains("args.set(3, -2000)"));
    }

    @Test
    void jeiRegistrationReplacesOnlyInventoryMenuCraftingTransfer() throws IOException {
        String plugin = read("src/main/java/dev/resivore/inventorycrafting/client/jei/Inventory3x3JeiPlugin.java");
        String info = read("src/main/java/dev/resivore/inventorycrafting/client/jei/Inventory3x3RecipeTransferInfo.java");
        String handler = read("src/main/java/dev/resivore/inventorycrafting/client/jei/Inventory3x3RecipeTransferHandler.java");

        assertTrue(plugin.contains("registration.addRecipeTransferHandler(handler, RecipeTypes.CRAFTING)"));
        assertTrue(info.contains("return InventoryMenu.class"));
        assertTrue(info.contains("return menu.getInputGridSlots()"));
        assertTrue(info.contains("InventoryCraftingLayout.ordinaryInventorySlots(menu)"));
        assertTrue(handler.contains("createUnregisteredRecipeTransferHandler"));
        assertTrue(handler.contains("recipeTransferHasServerSupport"));
        assertFalse(plugin.contains("CraftingMenu.class"));
    }

    @Test
    void metadataDeclaresCommonTargetStackAndUntestedClassification() throws IOException {
        String metadata = read("src/main/resources/fabric.mod.json");
        String gradle = read("build.gradle");

        assertTrue(metadata.contains("\"environment\": \"*\""));
        assertTrue(metadata.contains("\"inventoryextended\": \"=1.1.2\""));
        assertTrue(metadata.contains("\"simple_trash_slot\": \">=1.0.4\""));
        assertTrue(metadata.contains("\"workbench:classification\": \"GENERATED / UNTESTED\""));
        assertTrue(metadata.contains("\"workbench:deployment\": \"NOT DEPLOYED\""));
        assertTrue(gradle.contains("options.release = 25"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String readTree(String relative) throws IOException {
        StringBuilder result = new StringBuilder();
        try (var paths = Files.walk(ROOT.resolve(relative))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                result.append(Files.readString(path));
            }
        }
        return result.toString();
    }
}
