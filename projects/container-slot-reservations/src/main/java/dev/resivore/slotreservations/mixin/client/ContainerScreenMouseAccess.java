package dev.resivore.slotreservations.mixin.client;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exact native entrypoints used by Mouse Tweaks and by CSR's optional bridge. */
@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public interface ContainerScreenMouseAccess {
    @Invoker("getHoveredSlot")
    Slot containerSlotReservations$slotAt(double mouseX, double mouseY);

    @Invoker("slotClicked")
    void containerSlotReservations$clickSlot(Slot slot, int slotIndex, int button, ContainerInput input);
}
