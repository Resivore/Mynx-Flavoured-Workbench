package dev.resivore.slotreservations.client;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * State for one physical RMB gesture whose subject is the exact shulker carried
 * by the cursor. The initial ordinary player slot selects the direction once;
 * later hovered slots never reclassify it.
 */
final class CarriedShulkerRmbGesture {
    enum Mode {
        INACTIVE,
        SHULKER_TO_INVENTORY,
        INVENTORY_TO_SHULKER
    }

    record SlotKey(int menuSlot, int physicalPlayerSlot) {}

    private Mode mode = Mode.INACTIVE;
    private SlotKey currentSlot;
    private ItemStack projectedShulker = ItemStack.EMPTY;
    private String projectedFingerprint;
    private final Set<String> validLiveFingerprints = new LinkedHashSet<>();
    private final Map<SlotKey, ItemStack> projectedSources = new HashMap<>();

    static Mode selectMode(boolean initialSlotOccupied) {
        return initialSlotOccupied ? Mode.INVENTORY_TO_SHULKER : Mode.SHULKER_TO_INVENTORY;
    }

    /**
     * Maps Creative's two player-slot presentations back to the server's physical inventory.
     * The inventory tab wraps InventoryMenu indices; every other tab exposes only direct
     * physical hotbar slots alongside the catalog container.
     */
    static int creativePhysicalPlayerSlot(int creativeCoordinate, boolean inventoryTab) {
        if (!inventoryTab) {
            return creativeCoordinate >= 0 && creativeCoordinate < 9 ? creativeCoordinate : -1;
        }
        if (creativeCoordinate >= 9 && creativeCoordinate <= 35) return creativeCoordinate;
        if (creativeCoordinate >= 36 && creativeCoordinate <= 44) return creativeCoordinate - 36;
        return -1;
    }

    void begin(Mode selectedMode, SlotKey initialSlot, ItemStack carriedShulker,
               String carriedFingerprint) {
        reset();
        if (selectedMode == null || selectedMode == Mode.INACTIVE
                || carriedShulker.isEmpty() || carriedFingerprint == null) return;
        mode = selectedMode;
        currentSlot = initialSlot;
        projectedShulker = carriedShulker.copy();
        projectedFingerprint = carriedFingerprint;
        validLiveFingerprints.add(carriedFingerprint);
    }

    boolean enter(SlotKey slot) {
        if (!isActive() || slot == null) return false;
        if (slot.equals(currentSlot)) return false;
        currentSlot = slot;
        return true;
    }

    /** Observes Mouse Tweaks' selected-slot identity even when its helper is not invoked. */
    void observe(SlotKey slot) {
        if (isActive()) currentSlot = slot;
    }

    void advance(ItemStack changedShulker, String changedFingerprint) {
        if (!isActive() || changedShulker.isEmpty() || changedFingerprint == null) return;
        projectedShulker = changedShulker.copy();
        projectedFingerprint = changedFingerprint;
        validLiveFingerprints.add(changedFingerprint);
    }

    ItemStack projectedSource(SlotKey slot, ItemStack liveSource) {
        ItemStack projected = projectedSources.get(slot);
        return projected == null ? liveSource.copy() : projected.copy();
    }

    void advanceSource(SlotKey slot, ItemStack changedSource) {
        if (isActive() && slot != null && changedSource != null) {
            projectedSources.put(slot, changedSource.copy());
        }
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
        currentSlot = null;
        projectedShulker = ItemStack.EMPTY;
        projectedFingerprint = null;
        validLiveFingerprints.clear();
        projectedSources.clear();
    }
}
