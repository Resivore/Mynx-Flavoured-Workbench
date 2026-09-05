package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShulkerPanelOverlay {
    private ShulkerPanelOverlay() {}

    public static List<SlotOverlay> plan(ItemStack shulker, List<ItemStack> physical) {
        if (physical.size() != ReservationData.SLOT_COUNT) return List.of();
        ReservationData data = ReservationStore.getData(shulker);
        List<SlotOverlay> result = new ArrayList<>();
        for (int slot = 0; slot < ReservationData.SLOT_COUNT; slot++) {
            Optional<ItemStack> reservation = data.get(slot);
            result.add(new SlotOverlay(slot, physical.get(slot), reservation,
                    ReservationVisualRenderer.state(physical.get(slot), reservation)));
        }
        return List.copyOf(result);
    }

    public record SlotOverlay(int slot, ItemStack physical, Optional<ItemStack> reservation,
                              ReservationVisualRenderer.SlotVisualState state) {}
}
