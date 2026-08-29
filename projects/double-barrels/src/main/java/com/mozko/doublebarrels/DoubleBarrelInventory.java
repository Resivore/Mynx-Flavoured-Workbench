package com.mozko.doublebarrels;

import java.util.function.Consumer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DoubleBarrelInventory implements Container {
    private final NonNullList<ItemStack> first;
    private final NonNullList<ItemStack> second;
    private final int firstSize;
    private final int totalSize;
    private final Runnable onChanged;
    private final Consumer<ContainerUser> onOpen;
    private final Consumer<ContainerUser> onClose;

    public DoubleBarrelInventory(
            NonNullList<ItemStack> first,
            NonNullList<ItemStack> second,
            Runnable onChanged,
            Consumer<ContainerUser> onOpen,
            Consumer<ContainerUser> onClose) {
        this.first = first;
        this.second = second;
        this.firstSize = first.size();
        this.totalSize = firstSize + second.size();
        this.onChanged = onChanged;
        this.onOpen = onOpen;
        this.onClose = onClose;
    }

    @Override
    public int getContainerSize() {
        return totalSize;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : first) if (!stack.isEmpty()) return false;
        for (ItemStack stack : second) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getList(slot).get(slot % firstSize);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(getList(slot), slot % firstSize, amount);
        if (!result.isEmpty()) onChanged.run();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        NonNullList<ItemStack> list = getList(slot);
        int index = slot % firstSize;
        ItemStack result = list.get(index);
        if (!result.isEmpty()) {
            list.set(index, ItemStack.EMPTY);
            onChanged.run();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getList(slot).set(slot % firstSize, stack);
        onChanged.run();
    }

    @Override
    public void setChanged() {
        onChanged.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(ContainerUser user) {
        onOpen.accept(user);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        onClose.accept(user);
    }

    @Override
    public void clearContent() {
        first.clear();
        second.clear();
        onChanged.run();
    }

    private NonNullList<ItemStack> getList(int slot) {
        return slot < firstSize ? first : second;
    }
}
