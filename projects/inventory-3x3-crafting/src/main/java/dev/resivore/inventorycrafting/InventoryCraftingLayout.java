package dev.resivore.inventorycrafting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Frozen menu and screen contract for the audited Workbench stack.
 * Public menu ordinals are deliberately non-contiguous; backing-cell identity
 * is authoritative everywhere outside these assertions.
 */
public final class InventoryCraftingLayout {
    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;

    public static final int RESULT_SLOT = 0;
    public static final int TRASH_SLOT = 73;
    public static final int FIRST_APPENDED_CRAFT_SLOT = 74;
    public static final int LAST_APPENDED_CRAFT_SLOT = 78;
    public static final int TRINKETS_START = 79;

    public static final int ORDINARY_MENU_START = 9;
    public static final int EXPECTED_ORDINARY_MENU_END = 72;
    public static final int EXPECTED_TRASH_BACKING_SLOT = 68;

    public static final int GRID_X = 77;
    public static final int GRID_Y = 8;
    public static final int RESULT_X = 152;
    public static final int RESULT_Y = 27;
    public static final int RECIPE_BOOK_X = 100;
    public static final int RECIPE_BOOK_Y = 62;

    public static final int[] LEGACY_LOGICAL_CELLS = {0, 1, 3, 4};
    public static final int[] APPENDED_LOGICAL_CELLS = {2, 5, 6, 7, 8};
    public static final int[] EXPECTED_ROW_MAJOR_MENU_IDS = {1, 2, 74, 3, 4, 75, 76, 77, 78};

    private InventoryCraftingLayout() {
    }

    public static int gridX(int logicalCell) {
        return GRID_X + (logicalCell % GRID_WIDTH) * 18;
    }

    public static int gridY(int logicalCell) {
        return GRID_Y + (logicalCell / GRID_WIDTH) * 18;
    }

    public static boolean isCraftingInput(InventoryMenu menu, Slot slot) {
        return slot.container == menu.getCraftSlots()
                && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < GRID_WIDTH * GRID_HEIGHT;
    }

    public static List<Slot> orderedCraftingSlots(InventoryMenu menu) {
        Slot[] byLogicalCell = new Slot[GRID_WIDTH * GRID_HEIGHT];
        for (Slot slot : menu.slots) {
            if (!isCraftingInput(menu, slot)) {
                continue;
            }

            int logicalCell = slot.getContainerSlot();
            if (byLogicalCell[logicalCell] != null) {
                throw layoutFailure("duplicate crafting view for logical cell " + logicalCell);
            }
            byLogicalCell[logicalCell] = slot;
        }

        if (Arrays.stream(byLogicalCell).anyMatch(slot -> slot == null)) {
            throw layoutFailure("crafting views do not cover logical cells 0 through 8");
        }

        List<Slot> ordered = List.copyOf(Arrays.asList(byLogicalCell));
        int[] actualIds = ordered.stream().mapToInt(slot -> slot.index).toArray();
        if (!Arrays.equals(actualIds, EXPECTED_ROW_MAJOR_MENU_IDS)) {
            throw layoutFailure("row-major menu IDs were " + Arrays.toString(actualIds));
        }
        return ordered;
    }

    public static void verifyPreAppend(InventoryMenu menu, Inventory inventory) {
        verifyStableIndexes(menu);
        if (menu.getGridWidth() != GRID_WIDTH || menu.getGridHeight() != GRID_HEIGHT) {
            throw layoutFailure("superclass crafting dimensions are not 3x3");
        }
        if (menu.getCraftSlots().getContainerSize() != GRID_WIDTH * GRID_HEIGHT) {
            throw layoutFailure("InventoryMenu does not own one nine-cell crafting container");
        }
        if (menu.slots.size() != FIRST_APPENDED_CRAFT_SLOT) {
            throw layoutFailure("expected Trash-complete prefix size 74 before append, found " + menu.slots.size());
        }

        for (int menuId = 0; menuId <= TRASH_SLOT; menuId++) {
            Slot slot = menu.slots.get(menuId);
            if (menuId >= 1 && menuId <= 4) {
                int expectedCell = LEGACY_LOGICAL_CELLS[menuId - 1];
                if (!isCraftingInput(menu, slot) || slot.getContainerSlot() != expectedCell) {
                    throw layoutFailure("legacy crafting menu " + menuId + " does not view logical cell " + expectedCell);
                }
            } else if (menuId >= 5 && slot.container != inventory) {
                throw layoutFailure("preserved player/Trash prefix menu " + menuId + " changed container ownership");
            }
        }

        Slot trash = menu.slots.get(TRASH_SLOT);
        if (trash.container != inventory || trash.getContainerSlot() != EXPECTED_TRASH_BACKING_SLOT) {
            throw layoutFailure("menu 73 is not the audited Simple Trash slot over backing index 68");
        }
        int ordinaryEnd = ORDINARY_MENU_START + inventory.getNonEquipmentItems().size();
        if (ordinaryEnd != EXPECTED_ORDINARY_MENU_END) {
            throw layoutFailure("Inventory Extended ordinary end was " + ordinaryEnd + " instead of 72");
        }
    }

    public static void verifyPostAppend(InventoryMenu menu) {
        verifyStableIndexes(menu);
        if (menu.slots.size() != TRINKETS_START) {
            throw layoutFailure("pre-Trinkets prefix size was " + menu.slots.size() + " instead of 79");
        }
        orderedCraftingSlots(menu);
    }

    public static List<Slot> ordinaryInventorySlots(InventoryMenu menu) {
        List<Slot> ordinary = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory inventory) {
                int backing = slot.getContainerSlot();
                if (backing >= 0 && backing < inventory.getNonEquipmentItems().size()) {
                    ordinary.add(slot);
                }
            }
        }
        return List.copyOf(ordinary);
    }

    private static void verifyStableIndexes(InventoryMenu menu) {
        for (int menuId = 0; menuId < menu.slots.size(); menuId++) {
            if (menu.slots.get(menuId).index != menuId) {
                throw layoutFailure("slot at ordinal " + menuId + " retained index " + menu.slots.get(menuId).index);
            }
        }
    }

    public static IllegalStateException layoutFailure(String detail) {
        return new IllegalStateException("Unsafe InventoryMenu layout; refusing to expose crafting storage: " + detail);
    }
}
