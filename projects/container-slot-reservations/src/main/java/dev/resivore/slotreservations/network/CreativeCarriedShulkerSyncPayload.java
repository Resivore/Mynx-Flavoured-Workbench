package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Returns an accepted Creative cursor mutation to its client-only inventory facade. */
public record CreativeCarriedShulkerSyncPayload(
        int menuId, String predecessorFingerprint, ItemStack carried
) implements CustomPacketPayload {
    public static final Type<CreativeCarriedShulkerSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "creative_carried_shulker_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeCarriedShulkerSyncPayload> CODEC =
            CustomPacketPayload.codec(CreativeCarriedShulkerSyncPayload::write,
                    CreativeCarriedShulkerSyncPayload::new);

    public CreativeCarriedShulkerSyncPayload {
        carried = carried == null ? ItemStack.EMPTY : carried.copy();
    }

    private CreativeCarriedShulkerSyncPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUtf(64), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        buffer.writeUtf(predecessorFingerprint, 64);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, carried);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
