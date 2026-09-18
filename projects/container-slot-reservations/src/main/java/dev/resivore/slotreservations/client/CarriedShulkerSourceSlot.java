package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.OrdinaryPlayerInventorySlots;
import dev.resivore.slotreservations.mixin.client.CreativeSlotWrapperAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Resolves one hovered native Slot to its exact live ordinary-player backing coordinate. */
final class CarriedShulkerSourceSlot {
    private CarriedShulkerSourceSlot() {}

    static Resolution resolve(Player player, AbstractContainerMenu menu, Slot hovered,
                              boolean creative, boolean creativeInventoryTab) {
        if (player == null || menu == null || hovered == null || hovered.isFake()
                || !hovered.isActive() || !menu.canDragTo(hovered)) return null;
        int visible = menu.slots.indexOf(hovered);
        if (visible < 0) return null;

        Slot backing = hovered;
        if (creative && creativeInventoryTab) {
            if (!(hovered instanceof CreativeSlotWrapperAccess wrapper)) return null;
            backing = wrapper.containerSlotReservations$getTarget();
        }
        if (!OrdinaryPlayerInventorySlots.isEligible(player, backing)) return null;
        return new Resolution(hovered, backing, new CarriedShulkerRmbGesture.SlotKey(
                creative ? -1 : visible, backing.getContainerSlot()));
    }

    record Resolution(Slot hovered, Slot backing, CarriedShulkerRmbGesture.SlotKey key) {}
}
