package dev.resivore.inventorycrafting.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
interface AbstractContainerMenuSnapshotAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> inventory3x3$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<RemoteSlot> inventory3x3$getRemoteSlots();
}
