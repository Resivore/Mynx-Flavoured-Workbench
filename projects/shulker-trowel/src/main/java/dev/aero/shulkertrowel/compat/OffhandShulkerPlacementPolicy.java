package dev.aero.shulkertrowel.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** The one global reservation rule for a live offhand palette shulker. */
public final class OffhandShulkerPlacementPolicy {
    private OffhandShulkerPlacementPolicy() {}

    public static boolean blocks(BlockItem blockItem, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}
