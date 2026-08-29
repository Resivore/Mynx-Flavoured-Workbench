package dev.aero.shulkertrowel.geometry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persistent stack state; the server is the only network-authoritative writer. */
public final class TrowelGeometryState {
    private static final String KEY = "shulker_trowel.geometry";

    private TrowelGeometryState() {}

    public static TargetGeometry get(ItemStack stack) {
        int id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(KEY, TargetGeometry.FULL.networkId());
        return TargetGeometry.byNetworkId(id);
    }

    public static void set(ItemStack stack, TargetGeometry geometry) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(KEY, geometry.networkId()));
    }
}
