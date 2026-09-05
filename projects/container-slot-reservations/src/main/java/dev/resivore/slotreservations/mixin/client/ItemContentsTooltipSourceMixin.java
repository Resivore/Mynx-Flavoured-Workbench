package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.TooltipSourceAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Adds no hard linkage: the string target is present only with Item Interactions. */
@Pseudo
@Mixin(
        targets = "fuzs.iteminteractions.common.api.v2.world.inventory.tooltip.ItemContentsTooltip",
        remap = false
)
abstract class ItemContentsTooltipSourceMixin implements TooltipSourceAccess {
    @Unique private dev.resivore.slotreservations.client.NestedTooltipEditor.Binding containerSlotReservations$host;
    @Override public dev.resivore.slotreservations.client.NestedTooltipEditor.Binding containerSlotReservations$getHost() { return containerSlotReservations$host; }
    @Override public void containerSlotReservations$setHost(dev.resivore.slotreservations.client.NestedTooltipEditor.Binding host) { containerSlotReservations$host = host; }
    @Unique
    private ItemStack containerSlotReservations$sourceStack;

    @Override
    public ItemStack containerSlotReservations$getSourceStack() {
        return containerSlotReservations$sourceStack == null
                ? ItemStack.EMPTY
                : containerSlotReservations$sourceStack;
    }

    @Override
    public void containerSlotReservations$setSourceStack(ItemStack sourceStack) {
        containerSlotReservations$sourceStack = sourceStack.copy();
    }
}
