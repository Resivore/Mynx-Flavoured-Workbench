package dev.aero.shulkertrowel.palette;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public final class ShulkerPaletteContents {
    private ShulkerPaletteContents() {}

    public static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.typeHolder().is(ItemTags.SHULKER_BOXES);
    }

    public static NonNullList<ItemStack> read(ItemStack shulker) {
        NonNullList<ItemStack> items = NonNullList.withSize(
                ShulkerBoxBlockEntity.CONTAINER_SIZE,
                ItemStack.EMPTY
        );
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void write(ItemStack shulker, NonNullList<ItemStack> items) {
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
}
