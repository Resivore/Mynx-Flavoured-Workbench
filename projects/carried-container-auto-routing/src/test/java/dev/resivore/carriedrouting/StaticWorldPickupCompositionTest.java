package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticWorldPickupCompositionTest {
    @Test
    void worldPickupDoesNotCompeteForInventoryAdd() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String mixin = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/ItemEntityMixin.java"
        ));

        assertTrue(mixin.contains("@Inject"));
        assertTrue(mixin.contains("Player;getInventory()"));
        assertTrue(mixin.contains("shift = At.Shift.BEFORE"));
        assertFalse(mixin.contains("@Redirect"));
        assertFalse(mixin.contains("Inventory;add("));
    }

    @Test
    void acquisitionRoutingMergesCompleteDynamicStorageBeforeOpeningEmptyHotbar() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String routing = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/carriedrouting/RoutingService.java"
        ));

        int selectedMerge = routing.indexOf(
                "mergeInventorySlot(inventory, incoming, selected, excludedInventorySlot);"
        );
        int carriedContainers = routing.indexOf(
                "routeCarriedContainers(player, inventory, incoming, excludedInventorySlot, excludeOffhand);"
        );
        int hotbarMerge = routing.indexOf("mergeInventoryRange(inventory, incoming, 0,");
        int offhandMerge = routing.indexOf("mergeMatchingOccupiedOffhand(player, incoming);");
        int ordinaryFallbacks = routing.indexOf(
                "routeOrdinaryInventoryFallbacks(inventory, incoming, context, excludedInventorySlot);"
        );
        int fallbackBody = routing.indexOf("static void routeOrdinaryInventoryFallbacks(");
        int nextMethod = routing.indexOf(
                "public static boolean routePlayerOriginSpecialDestinations(",
                fallbackBody
        );
        String fallback = routing.substring(fallbackBody, nextMethod);
        int priorityGate = fallback.indexOf(
                "boolean prioritizeOccupiedStorage = context == RoutingContext.WORLD_PICKUP"
        );
        int quickMovePriority = fallback.indexOf(
                "|| context == RoutingContext.QUICK_MOVE;",
                priorityGate
        );
        int storageMerge = fallback.indexOf(
                "mergeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,",
                quickMovePriority
        );
        int emptyHotbar = fallback.indexOf("placeInventoryRange(inventory, incoming, 0,");
        int storagePlacement = fallback.indexOf(
                "placeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,",
                emptyHotbar
        );
        assertTrue(selectedMerge >= 0
                        && selectedMerge < carriedContainers
                        && carriedContainers < hotbarMerge
                        && hotbarMerge < offhandMerge
                        && offhandMerge < ordinaryFallbacks
                        && fallbackBody > ordinaryFallbacks
                        && nextMethod > fallbackBody
                        && priorityGate >= 0
                        && priorityGate < quickMovePriority
                        && quickMovePriority < storageMerge
                        && storageMerge < emptyHotbar
                        && emptyHotbar < storagePlacement,
                "World pickup and external QUICK_MOVE must preserve occupied tiers, merge all storage partials, then use empty hotbar and storage");

        assertTrue(fallback.substring(storageMerge, fallback.indexOf(");", storageMerge))
                .contains("inventory.getNonEquipmentItems().size()"),
                "The storage merge end must follow the complete live ordinary inventory");
        assertTrue(fallback.substring(storagePlacement, fallback.indexOf(");", storagePlacement))
                .contains("inventory.getNonEquipmentItems().size()"),
                "The storage placement end must follow the complete live ordinary inventory");
        assertFalse(routing.contains("Inventory.INVENTORY_SIZE"),
                "Compile-time vanilla size 36 would truncate Inventory Extended rows 4-6");
        assertFalse(routing.contains("Inventory.SELECTION_SIZE, 36"));
        assertFalse(routing.contains("Inventory.SELECTION_SIZE, 63"));
        assertTrue(routing.contains("ItemStack.isSameItemSameComponents(target, incoming)"),
                "Ordinary merges must retain exact component identity");
        assertTrue(routing.contains("ItemStack.isSameItemSameComponents(offhand, incoming)"),
                "Offhand merges must retain exact component identity");
    }

    @Test
    void externalQuickMoveUsesPriorityPassWhileCompatibilityTransferRetainsOldFallback() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String routing = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/carriedrouting/RoutingService.java"
        ));

        int fallbackBody = routing.indexOf("static void routeOrdinaryInventoryFallbacks(");
        int nextMethod = routing.indexOf(
                "public static boolean routePlayerOriginSpecialDestinations(",
                fallbackBody
        );
        String fallback = routing.substring(fallbackBody, nextMethod);
        int priorityGate = fallback.indexOf(
                "boolean prioritizeOccupiedStorage = context == RoutingContext.WORLD_PICKUP"
        );
        int quickMovePriority = fallback.indexOf(
                "|| context == RoutingContext.QUICK_MOVE;",
                priorityGate
        );
        int storageMerge = fallback.indexOf(
                "mergeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,",
                quickMovePriority
        );
        int emptyHotbar = fallback.indexOf(
                "placeInventoryRange(inventory, incoming, 0,",
                storageMerge
        );
        int storagePlacement = fallback.indexOf(
                "placeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,",
                emptyHotbar
        );
        int compatibilityBranch = fallback.indexOf("} else {", storagePlacement);
        int ordinaryTier = fallback.indexOf(
                "routeInventoryTier(inventory, incoming, Inventory.SELECTION_SIZE,",
                compatibilityBranch
        );

        assertTrue(priorityGate >= 0
                        && quickMovePriority > priorityGate
                        && storageMerge > quickMovePriority
                        && emptyHotbar > storageMerge
                        && storagePlacement > emptyHotbar
                        && compatibilityBranch > storagePlacement
                        && ordinaryTier > compatibilityBranch,
                "External QUICK_MOVE must use storage merge before empty hotbar while compatibility-only transfers retain their established fallback");
    }

    @Test
    void quickMoveCompositionPreservesPlayerOriginAndMenuOwnership() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String mixin = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerMenuMixin.java"
        ));

        int playerOrigin = mixin.indexOf(
                "RoutingService.routePlayerOriginSpecialDestinations("
        );
        int originalRemainder = mixin.indexOf(
                "changed |= original.call(incoming, startSlot, endSlot, backwards);",
                playerOrigin
        );
        int externalIncoming = mixin.indexOf(
                "RoutingService.routeIncomingStack(",
                originalRemainder
        );
        int quickMoveContext = mixin.indexOf(
                "RoutingContext.QUICK_MOVE,",
                externalIncoming
        );
        int externalReturn = mixin.indexOf("return changed;", quickMoveContext);

        assertTrue(mixin.contains("@WrapOperation"));
        assertTrue(mixin.contains("@WrapMethod(method = \"moveItemStackTo\")"));
        assertTrue(mixin.contains("return original.call(menu, player, slotIndex);"),
                "The concrete menu quickMoveStack must still own source cleanup and callbacks");
        assertFalse(mixin.contains("onTake("));
        assertFalse(mixin.contains("setByPlayer("));
        assertFalse(mixin.contains("onQuickCraft("));
        assertTrue(mixin.contains("incoming != this.carriedRouting$sourceStack"));
        assertTrue(mixin.contains("OffhandCompatibilityHook.isDelegatedToOffhand()"));
        assertTrue(playerOrigin >= 0
                        && originalRemainder > playerOrigin
                        && externalIncoming > originalRemainder
                        && quickMoveContext > externalIncoming
                        && externalReturn > quickMoveContext,
                "Player-origin QUICK_MOVE must remain separate and return its remainder to the menu before the external acquisition path");
    }
}
