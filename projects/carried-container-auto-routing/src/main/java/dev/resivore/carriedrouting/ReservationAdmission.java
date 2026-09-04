package dev.resivore.carriedrouting;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.slf4j.LoggerFactory;

/** No provider types are resolved until Fabric confirms the optional mod is loaded. */
interface ReservationAdmission {
    enum Slot { OCCUPIED_COMPATIBLE, RESERVED_MATCH, UNRESERVED_EMPTY, RESERVED_OTHER, NON_WRITABLE, INELIGIBLE }
    Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical);
    boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming);

    static ReservationAdmission load() {
        if (!FabricLoader.getInstance().isModLoaded("container_slot_reservations")) return ABSENT;
        try {
            return new CsrReservationAdmission();
        } catch (LinkageError error) {
            LoggerFactory.getLogger("carried_container_auto_routing").error(
                    "CSR public API is incompatible; carried-shulker routing is disabled", error);
            return DENY;
        }
    }

    ReservationAdmission ABSENT = new ReservationAdmission() {
        public Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical) {
            if (slot < 0 || slot >= 27 || incoming.isEmpty()
                    || !incoming.getItem().canFitInsideContainerItems()) return Slot.NON_WRITABLE;
            if (physical.isEmpty()) return Slot.UNRESERVED_EMPTY;
            return ItemStack.isSameItemSameComponents(physical, incoming)
                    && physical.getCount() < physical.getMaxStackSize()
                    ? Slot.OCCUPIED_COMPATIBLE : Slot.NON_WRITABLE;
        }
        public boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming) { return true; }
    };

    ReservationAdmission DENY = new ReservationAdmission() {
        public Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical) {
            return Slot.INELIGIBLE;
        }
        public boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming) { return false; }
    };
}
