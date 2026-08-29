package dev.aero.shulkertrowel.network;

import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import dev.aero.shulkertrowel.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Validates and applies the only client-originating trowel state mutation. */
public final class TrowelGeometryAuthority {
    private TrowelGeometryAuthority() {}

    public static boolean apply(ServerPlayer player, int geometryId) {
        if (!player.isAlive()) return false;

        TargetGeometry geometry = TargetGeometry.fromNetworkId(geometryId).orElse(null);
        if (geometry == null) return false;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!mainHand.is(ModItems.TROWEL)) return false;

        TrowelGeometryState.set(mainHand, geometry);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }
}
