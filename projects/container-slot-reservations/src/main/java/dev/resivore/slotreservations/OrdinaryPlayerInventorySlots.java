package dev.resivore.slotreservations;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/** Validates exact live menu bindings to ordinary, non-equipment player-inventory slots. */
public final class OrdinaryPlayerInventorySlots {
    private OrdinaryPlayerInventorySlots() {}

    public static boolean isEligible(Player player, Slot slot) {
        if (player == null || slot == null) return false;

        Inventory inventory = player.getInventory();
        int physicalPlayerSlot = slot.getContainerSlot();
        return inventory != null
                && slot.container == inventory
                && physicalPlayerSlot >= 0
                && physicalPlayerSlot < inventory.getNonEquipmentItems().size()
                && slot.isActive()
                && !slot.isFake()
                && inventory.stillValid(player);
    }

    static boolean isExactLiveSlot(
            ServerPlayer player,
            Slot slot,
            int menuSlot,
            int physicalPlayerSlot
    ) {
        return isEligible(player, slot)
                && slot.index == menuSlot
                && slot.getContainerSlot() == physicalPlayerSlot;
    }
}
