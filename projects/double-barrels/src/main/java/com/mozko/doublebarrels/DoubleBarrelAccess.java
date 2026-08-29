package com.mozko.doublebarrels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.jspecify.annotations.Nullable;

public interface DoubleBarrelAccess {
    boolean isConnected();
    boolean isMainBarrel();
    @Nullable BlockPos getConnectionPos();
    void setConnectionPos(@Nullable BlockPos pos);
    void setMain(boolean main);
    void connectTo(BarrelBlockEntity partner);
    void disconnect();
    @Nullable Container getCombinedInventory();
    NonNullList<ItemStack> doublebarrels$getItems();
    void openLid(ContainerUser user);
    void closeLid(ContainerUser user);
}
