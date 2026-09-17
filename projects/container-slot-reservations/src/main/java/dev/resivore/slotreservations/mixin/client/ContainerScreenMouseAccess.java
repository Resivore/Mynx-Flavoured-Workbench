package dev.resivore.slotreservations.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exact native hovered-slot lookup used to classify the carried-shulker press origin. */
@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public interface ContainerScreenMouseAccess {
    @Invoker("getHoveredSlot")
    Slot containerSlotReservations$slotAt(double mouseX, double mouseY);
}
