package dev.resivore.carriedrouting.compat;

import net.minecraft.world.inventory.Slot;

/**
 * Runtime bridge implemented by the mixed Creative item-picker menu.
 *
 * <p>This interface deliberately lives outside the package declared in the
 * Carried Mixin configuration. Minecraft screen classes may reference the
 * bridge normally without causing Mixin to reject a direct class load.</p>
 */
public interface CreativeMenuSlotTracker {
    Slot carriedRouting$addTrackedSlot(Slot slot);
}
