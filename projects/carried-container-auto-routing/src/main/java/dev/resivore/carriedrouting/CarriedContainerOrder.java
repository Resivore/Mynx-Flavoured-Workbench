package dev.resivore.carriedrouting;

import java.util.ArrayList;
import java.util.List;

/** One authority for CCAR's stable carried-container host order. */
final class CarriedContainerOrder {
    private CarriedContainerOrder() {}

    record Host(int inventorySlot, boolean offhand) {
        static Host inventory(int slot) { return new Host(slot, false); }
        static Host offhandHost() { return new Host(-1, true); }
    }

    static List<Host> hosts(int ordinaryInventorySize, int excludedInventorySlot, boolean excludeOffhand) {
        List<Host> result = new ArrayList<>(ordinaryInventorySize + (excludeOffhand ? 0 : 1));
        for (int slot = 0; slot < ordinaryInventorySize; slot++) {
            if (slot != excludedInventorySlot) result.add(Host.inventory(slot));
        }
        if (!excludeOffhand) result.add(Host.offhandHost());
        return List.copyOf(result);
    }
}
