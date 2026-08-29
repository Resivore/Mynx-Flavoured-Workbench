package dev.resivore.dragonbound.channel;

import net.minecraft.world.item.ItemStack;

public final class ChannelRules {
    private ChannelRules() {
    }

    public static boolean exceedsMovementTolerance(
            double startX,
            double startY,
            double startZ,
            double currentX,
            double currentY,
            double currentZ,
            double tolerance
    ) {
        double dx = currentX - startX;
        double dy = currentY - startY;
        double dz = currentZ - startZ;
        return dx * dx + dy * dy + dz * dz > tolerance * tolerance;
    }

    public static boolean acceptedDamageCancels(boolean cancelOnDamage, boolean damageAccepted) {
        return cancelOnDamage && damageAccepted;
    }

    /**
     * Validates the main-hand source item after a click has started the server-owned channel.
     * Releasing the use button is deliberately absent from this contract. Minecraft may replace
     * the live stack object while preserving its item and components; changing the selected slot,
     * item, or components remains a real channel-breaking transition.
     */
    public static boolean sourceItemIsStillValid(
            int channelSelectedSlot,
            int liveSelectedSlot,
            ItemStack liveMainHand,
            ItemStack heldSnapshot
    ) {
        return channelSelectedSlot == liveSelectedSlot
                && stackSemanticallyMatches(liveMainHand, heldSnapshot);
    }

    public static boolean stackSemanticallyMatches(ItemStack liveStack, ItemStack snapshot) {
        return !liveStack.isEmpty()
                && !snapshot.isEmpty()
                && ItemStack.isSameItemSameComponents(liveStack, snapshot);
    }

    public static boolean completionTickReached(long currentTick, long completionTick) {
        return currentTick >= completionTick;
    }

    public static boolean shouldApplySuccessEffect(boolean exactArrivalConfirmed) {
        return exactArrivalConfirmed;
    }
}
