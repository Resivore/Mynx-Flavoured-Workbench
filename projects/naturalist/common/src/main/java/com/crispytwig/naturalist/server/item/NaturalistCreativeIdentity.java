package com.crispytwig.naturalist.server.item;

import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Exact identities while emitting this mod's creative inventory. */
public final class NaturalistCreativeIdentity {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private NaturalistCreativeIdentity() { }
    public static boolean isActive() { return Boolean.TRUE.equals(ACTIVE.get()); }

    public static void generate(Runnable generator) {
        Boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try { generator.run(); }
        finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static boolean sameStack(ItemStack first, ItemStack second) {
        return first == second || first != null && second != null
                && first.isEmpty() == second.isEmpty() && first.getItem() == second.getItem()
                && (first.isEmpty() || Objects.equals(first.getComponents(), second.getComponents()));
    }
}
