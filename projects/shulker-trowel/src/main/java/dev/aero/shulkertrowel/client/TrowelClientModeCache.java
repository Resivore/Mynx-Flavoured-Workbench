package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import net.minecraft.world.item.ItemStack;

/**
 * Transient UI acknowledgement cache for rapid PRESS/reopen input. This never
 * mutates stack state; the synchronized server component remains authoritative.
 */
final class TrowelClientModeCache {
    private static final long ACK_TIMEOUT_NANOS = 2_000_000_000L;

    private static ItemStack pendingStack;
    private static int pendingSlot = -1;
    private static TargetGeometry pendingMode;
    private static long deadlineNanos;

    private TrowelClientModeCache() {}

    static TargetGeometry displayed(ItemStack stack, int slot) {
        TargetGeometry authoritative = TrowelGeometryState.get(stack);
        if (pendingMode == null) return authoritative;

        if (pendingStack != stack
                || pendingSlot != slot
                || System.nanoTime() - deadlineNanos >= 0) {
            clear();
            return authoritative;
        }
        if (authoritative == pendingMode) {
            clear();
            return authoritative;
        }
        return pendingMode;
    }

    static void record(ItemStack stack, int slot, TargetGeometry mode) {
        pendingStack = stack;
        pendingSlot = slot;
        pendingMode = mode;
        deadlineNanos = System.nanoTime() + ACK_TIMEOUT_NANOS;
    }

    private static void clear() {
        pendingStack = null;
        pendingSlot = -1;
        pendingMode = null;
        deadlineNanos = 0L;
    }
}
