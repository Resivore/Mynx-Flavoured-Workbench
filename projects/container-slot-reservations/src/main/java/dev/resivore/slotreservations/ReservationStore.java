package dev.resivore.slotreservations;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;

/** Narrow component mutation seam that preserves every unrelated block/item component. */
public final class ReservationStore {
    private ReservationStore() {
    }

    public static ReservationData getData(SupportedContainerResolver.ResolvedSlot slot) {
        return getOwnerData(Objects.requireNonNull(slot, "slot").owner());
    }

    private static ReservationData getOwnerData(Container owner) {
        Objects.requireNonNull(owner, "owner");
        if (owner instanceof BlockEntity blockEntity) {
            return getData(blockEntity);
        }
        if (owner instanceof EnderChestReservationHolder holder) {
            return holder.containerSlotReservations$getReservations();
        }
        return ReservationData.EMPTY;
    }

    public static ReservationData getData(BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity");
        ReservationData data = blockEntity.components().get(ModComponents.RESERVATIONS);
        return data == null ? ReservationData.EMPTY : data;
    }

    public static ReservationData getData(ItemStack shulker) {
        Objects.requireNonNull(shulker, "shulker");
        ReservationData data = shulker.get(ModComponents.RESERVATIONS);
        return data == null ? ReservationData.EMPTY : data;
    }

    public static void setData(BlockEntity blockEntity, ReservationData data) {
        Objects.requireNonNull(blockEntity, "blockEntity");
        Objects.requireNonNull(data, "data");
        DataComponentMap.Builder components = DataComponentMap.builder().addAll(blockEntity.components());
        components.set(ModComponents.RESERVATIONS, data.isEmpty() ? null : data);
        blockEntity.setComponents(components.build());
        blockEntity.setChanged();
    }

    public static void setOwnerData(Container owner, ReservationData data) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(data, "data");
        if (owner instanceof BlockEntity blockEntity) {
            setData(blockEntity, data);
            return;
        }
        if (owner instanceof EnderChestReservationHolder holder) {
            holder.containerSlotReservations$setReservations(data);
            owner.setChanged();
            return;
        }
        throw new IllegalArgumentException("Unsupported reservation owner: " + owner.getClass().getName());
    }

    public static void setData(ItemStack shulker, ReservationData data) {
        Objects.requireNonNull(shulker, "shulker");
        Objects.requireNonNull(data, "data");
        if (data.isEmpty()) {
            shulker.remove(ModComponents.RESERVATIONS);
        } else {
            shulker.set(ModComponents.RESERVATIONS, data);
        }
    }

    public static Optional<ItemStack> get(SupportedContainerResolver.ResolvedSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return getData(slot).get(slot.localSlot());
    }

    public static ReservationData set(SupportedContainerResolver.ResolvedSlot slot, ItemStack template) {
        Objects.requireNonNull(slot, "slot");
        ReservationData changed = getData(slot).with(slot.localSlot(), template);
        setOwnerData(slot.owner(), changed);
        return changed;
    }

    public static ReservationData clear(SupportedContainerResolver.ResolvedSlot slot) {
        Objects.requireNonNull(slot, "slot");
        ReservationData changed = getData(slot).without(slot.localSlot());
        setOwnerData(slot.owner(), changed);
        return changed;
    }

    public static boolean reservationAllows(SupportedContainerResolver.ResolvedSlot slot, ItemStack incoming) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(incoming, "incoming");
        return get(slot)
                .map(template -> !incoming.isEmpty()
                        && ItemStack.isSameItemSameComponents(template, incoming))
                .orElse(true);
    }
}
