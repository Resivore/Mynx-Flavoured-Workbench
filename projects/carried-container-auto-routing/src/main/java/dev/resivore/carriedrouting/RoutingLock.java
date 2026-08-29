package dev.resivore.carriedrouting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class RoutingLock {
    private static final String KEY = "carried_container_auto_routing.locked";
    private RoutingLock() {}
    public static boolean isLocked(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBooleanOr(KEY, false);
    }
    public static void setLocked(ItemStack stack, boolean locked) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (locked) tag.putBoolean(KEY, true); else tag.remove(KEY);
        });
    }
}
