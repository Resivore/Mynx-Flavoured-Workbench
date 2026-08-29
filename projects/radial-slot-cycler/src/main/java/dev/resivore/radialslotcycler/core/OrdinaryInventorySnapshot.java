package dev.resivore.radialslotcycler.core;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Client-only value snapshot used to cancel a wheel whose contents changed. */
public final class OrdinaryInventorySnapshot {
    private OrdinaryInventorySnapshot() {}

    public static List<ItemStack> capture(List<ItemStack> liveItems) {
        Objects.requireNonNull(liveItems, "liveItems");
        return liveItems.stream().map(ItemStack::copy).toList();
    }

    public static boolean matches(List<ItemStack> snapshot, List<ItemStack> liveItems) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(liveItems, "liveItems");
        if (snapshot.size() != liveItems.size()) {
            return false;
        }
        for (int index = 0; index < snapshot.size(); index++) {
            if (!ItemStack.matches(snapshot.get(index), liveItems.get(index))) {
                return false;
            }
        }
        return true;
    }
}
