package dev.resivore.slotreservations.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * State for CSR's physical RMB collection gesture. Its only owned direction is
 * inventory-to-the exact shulker carried by the cursor; empty-origin outbound
 * dragging remains entirely on the established native/Mouse Tweaks path.
 */
final class CarriedShulkerRmbGesture {
    enum Mode {
        INACTIVE,
        INVENTORY_TO_SHULKER
    }

    record SlotKey(int menuSlot, int physicalPlayerSlot) {}

    private Mode mode = Mode.INACTIVE;
    private ItemStack projectedShulker = ItemStack.EMPTY;
    private String projectedFingerprint;
    private long traceGesture;
    private final Set<String> validLiveFingerprints = new LinkedHashSet<>();
    private final Set<Slot> visitedSlots = Collections.newSetFromMap(new IdentityHashMap<>());

    void begin(Mode selectedMode, Slot initialSlot, ItemStack carriedShulker,
               String carriedFingerprint) {
        reset();
        if (selectedMode != Mode.INVENTORY_TO_SHULKER
                || initialSlot == null
                || carriedShulker.isEmpty() || carriedFingerprint == null) return;
        mode = selectedMode;
        visitedSlots.add(initialSlot);
        projectedShulker = carriedShulker.copy();
        projectedFingerprint = carriedFingerprint;
        validLiveFingerprints.add(carriedFingerprint);
    }

    void traceGesture(long traceGesture) { this.traceGesture = traceGesture; }
    long traceGesture() { return traceGesture; }
    int visitedCount() { return visitedSlots.size(); }

    /** Mirrors Item Interactions: one action per actual hovered Slot identity per held drag. */
    boolean enter(Slot slot) {
        return isActive() && slot != null && visitedSlots.add(slot);
    }

    void advance(ItemStack changedShulker, String changedFingerprint) {
        if (!isActive() || changedShulker.isEmpty() || changedFingerprint == null) return;
        projectedShulker = changedShulker.copy();
        projectedFingerprint = changedFingerprint;
        validLiveFingerprints.add(changedFingerprint);
    }

    boolean acceptsLiveFingerprint(String fingerprint) {
        return isActive() && fingerprint != null && validLiveFingerprints.contains(fingerprint);
    }

    boolean isActive() { return mode != Mode.INACTIVE; }
    Mode mode() { return mode; }
    ItemStack projectedShulker() { return projectedShulker.copy(); }
    String projectedFingerprint() { return projectedFingerprint; }

    void reset() {
        mode = Mode.INACTIVE;
        projectedShulker = ItemStack.EMPTY;
        projectedFingerprint = null;
        traceGesture = 0;
        validLiveFingerprints.clear();
        visitedSlots.clear();
    }
}
