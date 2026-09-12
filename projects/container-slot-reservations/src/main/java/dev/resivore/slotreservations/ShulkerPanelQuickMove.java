package dev.resivore.slotreservations;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Plans one synthetic panel-to-player quick move against the live menu's actual player slots. */
public final class ShulkerPanelQuickMove {
    private ShulkerPanelQuickMove() {}

    public static Plan plan(ServerPlayer player, ShulkerHostResolver.ResolvedHost host, int internalSlot) {
        if (internalSlot < 0 || internalSlot >= ReservationData.SLOT_COUNT) return Plan.unchanged(host.stack());
        NonNullList<ItemStack> contents = ShulkerContents.copy(host.stack());
        ItemStack source = contents.get(internalSlot);
        if (source.isEmpty()) return Plan.unchanged(host.stack());

        Map<Slot, ItemStack> working = new IdentityHashMap<>();
        List<Slot> destinations = playerDestinations(player, host, source, working);
        int remaining = source.getCount();
        List<Move> moves = new ArrayList<>();
        for (boolean mergeOnly : List.of(true, false)) {
            for (Slot target : destinations) {
                if (remaining == 0) break;
                ItemStack current = working.get(target);
                if (mergeOnly && (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, source))) continue;
                if (!mergeOnly && !current.isEmpty()) continue;
                int capacity = Math.min(target.getMaxStackSize(source), source.getMaxStackSize());
                if (!current.isEmpty()) capacity = Math.min(capacity, current.getMaxStackSize());
                int moved = Math.min(remaining, Math.max(0, capacity - current.getCount()));
                if (moved == 0) continue;
                ItemStack updated = current.isEmpty() ? source.copyWithCount(moved) : current.copy();
                if (!current.isEmpty()) updated.grow(moved);
                working.put(target, updated);
                moves.add(new Move(target, current.copy(), updated, moved));
                remaining -= moved;
            }
        }
        int moved = source.getCount() - remaining;
        if (moved == 0) return Plan.unchanged(host.stack());
        ItemStack remainder = source.copy();
        remainder.shrink(moved);
        contents.set(internalSlot, remainder);
        ItemStack changedHost = host.stack().copy();
        ShulkerContents.replace(changedHost, contents);
        return new Plan(changedHost, List.copyOf(contents), List.copyOf(moves), moved);
    }

    private static List<Slot> playerDestinations(ServerPlayer player, ShulkerHostResolver.ResolvedHost host,
                                                 ItemStack source, Map<Slot, ItemStack> working) {
        List<Slot> destinations = new ArrayList<>();
        Set<SlotKey> seen = new java.util.HashSet<>();
        for (Slot candidate : host.menu().slots) {
            if (candidate == host.slot() || candidate.container != player.getInventory()
                    || !ShulkerHostResolver.writableTarget(player, candidate, source)) continue;
            SlotKey key = new SlotKey(candidate.container, candidate.getContainerSlot());
            if (!seen.add(key)) continue;
            working.put(candidate, candidate.getItem().copy());
            destinations.add(candidate);
        }
        return destinations;
    }

    public record Plan(ItemStack shulker, List<ItemStack> contents, List<Move> moves, int moved) {
        static Plan unchanged(ItemStack host) {
            return new Plan(host.copy(), List.copyOf(ShulkerContents.copy(host)), List.of(), 0);
        }
    }

    public record Move(Slot target, ItemStack before, ItemStack after, int moved) {}

    private record SlotKey(Object container, int slot) {}
}
