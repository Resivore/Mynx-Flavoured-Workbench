package dev.resivore.offhandqol;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticMixinCompositionTest {
    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }

    @Test
    void quickMoveHooksAreComposableAndMenuOwned() throws Exception {
        Path root = Path.of(System.getProperty("workspaceRoot"));
        String offhand = read(root, "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/mixin/AbstractContainerMenuMixin.java");
        String carried = read(root, "projects/carried-container-auto-routing/src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerMenuMixin.java");

        assertTrue(offhand.contains("@WrapOperation"));
        assertTrue(carried.contains("@WrapOperation"));
        assertTrue(offhand.contains("@WrapMethod(method = \"moveItemStackTo\")"));
        assertTrue(carried.contains("@WrapMethod(method = \"moveItemStackTo\")"));
        assertFalse(offhand.contains("@Redirect"));
        assertFalse(carried.contains("@Redirect"));
        assertFalse(offhand.contains("@Shadow"));
        assertFalse(carried.contains("@Shadow"));
        assertTrue(offhand.contains("original.call(menu, player, slotIndex)"));
        assertTrue(carried.contains("original.call(menu, player, slotIndex)"));
        assertTrue(offhand.contains("incoming != this.offhandQol$sourceStack"));
        assertTrue(carried.contains("incoming != this.carriedRouting$sourceStack"));
        assertTrue(offhand.contains("changed |= original.call(incoming, startSlot, endSlot, backwards)"));
        assertTrue(carried.contains("changed |= original.call(incoming, startSlot, endSlot, backwards)"));
        assertTrue(offhand.contains("OffhandCarriedCompatibility.isPlayerOriginAvailable()"));
        assertTrue(offhand.contains("OffhandCarriedCompatibility.routePlayerOrigin("));
        assertTrue(carried.indexOf("routePlayerOriginSpecialDestinations")
                < carried.indexOf("RoutingService.routeIncomingStack("));
    }

    @Test
    void productionCrashRegressionNeverLoadsAnOffhandMixinPackageClass() throws Exception {
        Path root = Path.of(System.getProperty("workspaceRoot"));
        Path oldUnsafeHelper = root.resolve(
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/mixin/OffhandCarriedCompatibility.java");
        String safeHelper = read(root,
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/compat/OffhandCarriedCompatibility.java");
        String carriedHook = read(root,
                "projects/carried-container-auto-routing/src/main/java/dev/resivore/carriedrouting/api/OffhandCompatibilityHook.java");

        assertFalse(Files.exists(oldUnsafeHelper));
        assertTrue(safeHelper.startsWith("package dev.resivore.offhandqol.compat;"));
        assertFalse(safeHelper.contains("dev.resivore.offhandqol.mixin."));
        assertFalse(carriedHook.contains("Class.forName"));
        assertFalse(carriedHook.contains("dev.resivore.offhandqol.mixin."));
        assertTrue(carriedHook.contains("isModLoaded(\"offhand_shift_click_qol\")"));
    }

    @Test
    void hierarchyUsesLiveOrdinaryInventoryAndExcludesClickedSource() throws Exception {
        Path root = Path.of(System.getProperty("workspaceRoot"));
        String routing = read(root,
                "projects/carried-container-auto-routing/src/main/java/dev/resivore/carriedrouting/RoutingService.java");
        String mixin = read(root,
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/mixin/AbstractContainerMenuMixin.java");
        String carriedPickup = read(root,
                "projects/carried-container-auto-routing/src/main/java/dev/resivore/carriedrouting/mixin/ItemEntityMixin.java");
        String offhandPickup = read(root,
                "projects/offhand-shift-click-qol/src/main/java/dev/resivore/offhandqol/mixin/ItemEntityMixin.java");

        int main = routing.indexOf("mergeInventorySlot(inventory, incoming, selected, excludedInventorySlot)");
        int carrier = routing.indexOf("Existing matching destinations always precede ordinary empty slots");
        int hotbar = routing.indexOf("mergeInventoryRange(inventory, incoming, 0");
        int offhand = routing.indexOf("mergeMatchingOccupiedOffhand(player, incoming)");
        int emptyHotbar = routing.indexOf("placeInventoryRange(inventory, incoming, 0");
        int ordinary = routing.indexOf("routeInventoryTier(inventory, incoming, Inventory.SELECTION_SIZE");
        assertTrue(main >= 0 && main < carrier && carrier < hotbar && hotbar < offhand
                && offhand < emptyHotbar && emptyHotbar < ordinary);
        assertTrue(routing.contains("inventory.getNonEquipmentItems().size()"));
        assertTrue(routing.contains("i == excludedInventorySlot"));
        assertTrue(mixin.contains("containerSlot < inventory.getNonEquipmentItems().size()"));
        assertTrue(mixin.contains("containerSlot == Inventory.SLOT_OFFHAND"));
        assertTrue(carriedPickup.contains("RoutingContext.WORLD_PICKUP"));
        assertTrue(offhandPickup.contains("OffhandRoutingService.routeIncoming(player, incoming, -1, false)"));
        assertTrue(offhandPickup.contains("isModLoaded(\"carried_container_auto_routing\")"));
    }
}
