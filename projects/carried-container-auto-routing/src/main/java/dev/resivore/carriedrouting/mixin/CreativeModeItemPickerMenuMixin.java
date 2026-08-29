package dev.resivore.carriedrouting.mixin;

import dev.resivore.carriedrouting.compat.CreativeMenuSlotTracker;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
abstract class CreativeModeItemPickerMenuMixin extends AbstractContainerMenu
        implements CreativeMenuSlotTracker {
    protected CreativeModeItemPickerMenuMixin() {
        super(null, 0);
    }

    @Override
    @Unique
    public Slot carriedRouting$addTrackedSlot(Slot slot) {
        // Protected vanilla access is normally remapped; no shadow/accessor/refmap is needed.
        return this.addSlot(slot);
    }
}
