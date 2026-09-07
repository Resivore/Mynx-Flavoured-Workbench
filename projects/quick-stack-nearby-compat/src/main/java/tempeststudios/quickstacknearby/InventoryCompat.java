package tempeststudios.quickstacknearby;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class InventoryCompat {
    private InventoryCompat() {
    }

    static boolean sameItemAndComponents(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    static int maxStackSize(Container container, ItemStack stack) {
        return container.getMaxStackSize(stack);
    }

    static Object stackIdentity(ItemStack stack) {
        return new StackIdentity(stack.getItem(), stack.getComponentsPatch());
    }

    private record StackIdentity(Item item, DataComponentPatch components) {
    }
}
