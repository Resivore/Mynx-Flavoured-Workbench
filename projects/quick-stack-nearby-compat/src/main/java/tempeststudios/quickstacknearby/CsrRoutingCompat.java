package tempeststudios.quickstacknearby;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Optional read-only bridge to CSR.  CSR remains the only reservation owner. */
final class CsrRoutingCompat {
    private static final String MOD_ID = "container_slot_reservations";
    private static final String API = "dev.resivore.slotreservations.api.ContainerSlotReservationsApi";

    private CsrRoutingCompat() {}

    static Set<QuickStackMoveEngine.StackKey> augmentAcceptedTypes(Container source, int first, int end,
            QuickStackMoveEngine.SourceRules rules, Container target, Set<QuickStackMoveEngine.StackKey> nativeKeys) {
        if (!available()) return nativeKeys;
        LinkedHashSet<QuickStackMoveEngine.StackKey> expanded = null;
        for (int sourceSlot = Math.max(0, first); sourceSlot < Math.min(end, source.getContainerSize()); sourceSlot++) {
            ItemStack incoming = source.getItem(sourceSlot);
            if (incoming.isEmpty() || rules.isLocked(sourceSlot)
                    || rules.movableCount(sourceSlot, incoming.getCount()) <= 0) continue;
            for (int targetSlot = 0; targetSlot < target.getContainerSize(); targetSlot++) {
                if (!target.getItem(targetSlot).isEmpty() || !is(target, targetSlot, incoming, "RESERVED_MATCH")) continue;
                QuickStackMoveEngine.StackKey key = QuickStackMoveEngine.StackKey.of(incoming);
                if (!nativeKeys.contains(key)) {
                    if (expanded == null) expanded = new LinkedHashSet<>(nativeKeys);
                    expanded.add(key);
                }
            }
        }
        return expanded == null ? nativeKeys : Set.copyOf(expanded);
    }

    /** Returns -1 when no empty slot is governed by CSR, preserving native QSN exactly. */
    static int insertReservedFirst(ItemStack incoming, Container target) {
        if (!available()) return -1;
        boolean governed = false;
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            if (target.getItem(slot).isEmpty() && (is(target, slot, incoming, "RESERVED_MATCH")
                    || is(target, slot, incoming, "RESERVED_OTHER"))) { governed = true; break; }
        }
        if (!governed) return -1;
        int moved = insertPass(incoming, target, "RESERVED_MATCH");
        if (!incoming.isEmpty()) moved += insertPass(incoming, target, "UNRESERVED_EMPTY");
        return moved;
    }

    static boolean available() { return FabricLoader.getInstance().isModLoaded(MOD_ID); }

    static boolean is(Container target, int slot, ItemStack incoming, String name) {
        try {
            Class<?> api = Class.forName(API);
            Method classify = api.getMethod("classify", Container.class, int.class, ItemStack.class);
            Object value = classify.invoke(null, target, slot, incoming);
            return name.equals(String.valueOf(value));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Loaded Container Slot Reservations lacks its audited read-only API", e);
        }
    }

    private static int insertPass(ItemStack incoming, Container target, String classification) {
        int moved = 0;
        for (int slot = 0; slot < target.getContainerSize() && !incoming.isEmpty(); slot++) {
            if (!target.getItem(slot).isEmpty() || !is(target, slot, incoming, classification)
                    || !target.canPlaceItem(slot, incoming)) continue;
            int amount = Math.min(incoming.getCount(), Math.min(incoming.getMaxStackSize(), target.getMaxStackSize(incoming)));
            target.setItem(slot, incoming.copyWithCount(amount));
            incoming.shrink(amount);
            moved += amount;
        }
        return moved;
    }
}
