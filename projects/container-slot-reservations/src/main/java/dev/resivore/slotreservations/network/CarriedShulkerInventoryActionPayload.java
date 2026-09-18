package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Requests one fingerprint-bound player-inventory stack insertion into the carried shulker. */
public record CarriedShulkerInventoryActionPayload(
        int menuId,
        int menuSlot,
        int physicalPlayerSlot,
        String carriedFingerprint,
        String sourceFingerprint,
        ItemStack creativeCursor
) implements CustomPacketPayload {
    public static final Type<CarriedShulkerInventoryActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    ContainerSlotReservations.MOD_ID, "carried_shulker_inventory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CarriedShulkerInventoryActionPayload> CODEC =
            CustomPacketPayload.codec(
                    CarriedShulkerInventoryActionPayload::write,
                    CarriedShulkerInventoryActionPayload::new);

    public CarriedShulkerInventoryActionPayload {
        creativeCursor = creativeCursor == null ? ItemStack.EMPTY : creativeCursor.copy();
    }

    /** Existing Survival callers deliberately carry no Creative cursor state. */
    public CarriedShulkerInventoryActionPayload(
            int menuId, int menuSlot, int physicalPlayerSlot,
            String carriedFingerprint, String sourceFingerprint
    ) {
        this(menuId, menuSlot, physicalPlayerSlot, carriedFingerprint, sourceFingerprint, ItemStack.EMPTY);
    }

    private CarriedShulkerInventoryActionPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(64), buffer.readUtf(64), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        buffer.writeVarInt(menuSlot);
        buffer.writeVarInt(physicalPlayerSlot);
        buffer.writeUtf(carriedFingerprint, 64);
        buffer.writeUtf(sourceFingerprint, 64);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, creativeCursor);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
