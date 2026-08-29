package dev.resivore.carriedrouting.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
interface AbstractContainerMenuSnapshotAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> carriedRouting$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<RemoteSlot> carriedRouting$getRemoteSlots();

    @Accessor("synchronizer")
    ContainerSynchronizer carriedRouting$getSynchronizer();
}
