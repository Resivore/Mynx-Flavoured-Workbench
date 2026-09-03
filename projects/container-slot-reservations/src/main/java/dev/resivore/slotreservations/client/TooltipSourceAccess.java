package dev.resivore.slotreservations.client;

import net.minecraft.world.item.ItemStack;

/** Optional-mixin bridge that carries the source container stack into its client tooltip. */
public interface TooltipSourceAccess {
    ItemStack containerSlotReservations$getSourceStack();

    void containerSlotReservations$setSourceStack(ItemStack sourceStack);
}
