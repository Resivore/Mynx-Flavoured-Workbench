package dev.resivore.slotreservations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Always-on, deliberately narrow Canary 26 diagnostic for cursor-held-shulker RMB. */
public final class CarriedShulkerRmbTrace {
    public static final String PREFIX = "[CSR-C26-RMB]";
    private static final Logger LOGGER = LoggerFactory.getLogger("Container Slot Reservations");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final AtomicLong GESTURES = new AtomicLong();

    private CarriedShulkerRmbTrace() {}

    public static long nextGesture() { return GESTURES.incrementAndGet(); }

    public static void client(long gesture, String stage, String details) {
        emit(gesture, "CLIENT", stage, details);
    }

    public static void server(String stage, String details) {
        emit(0, "SERVER", stage, details);
    }

    public static void server(long gesture, String stage, String details) {
        emit(gesture, "SERVER", stage, details);
    }

    public static String stack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        return "item=" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + " count=" + stack.getCount();
    }

    public static String shortFingerprint(String fingerprint) {
        if (fingerprint == null) return "null";
        return fingerprint.substring(0, Math.min(12, fingerprint.length())).toLowerCase(Locale.ROOT);
    }

    public static String slot(Slot slot, int listPosition) {
        if (slot == null) return "slot=null";
        return "slotIdentity=" + Integer.toHexString(System.identityHashCode(slot))
                + " listPosition=" + listPosition
                + " slotIndex=" + slot.index
                + " physicalSlot=" + slot.getContainerSlot()
                + " container=" + slot.container.getClass().getName()
                + " active=" + slot.isActive() + " fake=" + slot.isFake()
                + " hasItem=" + slot.hasItem() + " " + stack(slot.getItem());
    }

    private static void emit(long gesture, String side, String stage, String details) {
        LOGGER.info("{} seq={} gesture={} side={} stage={} {}", PREFIX,
                SEQUENCE.incrementAndGet(), gesture, side, stage, details);
    }
}
