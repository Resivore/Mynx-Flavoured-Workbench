package dev.resivore.mapmarkerextension.core;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Stable, deterministic recognition table for externally owned native map identities. */
public final class ExternalNativeMapIdentities {
    public static final ExternalNativeMapIdentity RIBBIT_VILLAGE =
        new ExternalNativeMapIdentity(
            "ribbits:ribbit_village_explorer_map",
            "ribbits:ribbit_village_explorer_map",
            "ribbits:ribbit_village"
        );

    private static final List<ExternalNativeMapIdentity> VALUES = List.of(RIBBIT_VILLAGE);

    private ExternalNativeMapIdentities() {
    }

    public static List<ExternalNativeMapIdentity> values() {
        return VALUES;
    }

    public static Optional<ExternalNativeMapIdentity> find(ItemStack stack) {
        return VALUES.stream().filter(identity -> identity.matches(stack)).findFirst();
    }
}
