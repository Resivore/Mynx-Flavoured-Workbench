package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ReservationSnapshotPayload(int menuId, List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 54;
    public static final Type<ReservationSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            ContainerSlotReservations.MOD_ID, "reservation_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReservationSnapshotPayload> CODEC =
            CustomPacketPayload.codec(ReservationSnapshotPayload::write, ReservationSnapshotPayload::new);

    public ReservationSnapshotPayload {
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Reservation snapshot has more than " + MAX_ENTRIES + " entries");
        }
        entries = List.copyOf(entries);
    }

    private ReservationSnapshotPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readVarInt(), readEntries(buffer));
    }

    private static List<Entry> readEntries(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException("Reservation snapshot entry count " + size + " is out of bounds");
        }
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            int menuSlotIndex = buffer.readVarInt();
            Optional<ItemStackTemplate> template = buffer.readBoolean()
                    ? Optional.of(ItemStackTemplate.STREAM_CODEC.decode(buffer).withCount(1))
                    : Optional.empty();
            entries.add(new Entry(menuSlotIndex, template));
        }
        return entries;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        if (entries.size() > MAX_ENTRIES) {
            throw new EncoderException("Reservation snapshot entry count exceeds " + MAX_ENTRIES);
        }
        buffer.writeVarInt(menuId);
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeVarInt(entry.menuSlotIndex());
            buffer.writeBoolean(entry.template().isPresent());
            entry.template().ifPresent(template -> ItemStackTemplate.STREAM_CODEC.encode(buffer, template.withCount(1)));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int menuSlotIndex, Optional<ItemStackTemplate> template) {
        public Entry {
            if (menuSlotIndex < 0) throw new IllegalArgumentException("Negative menu slot index");
            template = template.map(value -> value.withCount(1));
        }
    }
}
