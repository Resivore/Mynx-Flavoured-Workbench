package dev.resivore.slotreservations.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Creative's real InventoryMenu Slot instead of its presentation coordinate. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeSlotWrapperAccess {
    @Accessor("target")
    Slot containerSlotReservations$getTarget();
}
