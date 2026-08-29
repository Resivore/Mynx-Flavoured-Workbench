package dev.resivore.matchajei.network;

import dev.resivore.matchajei.data.MatchaDisplayData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record MatchaJeiDataPayload(
        String revision,
        List<MatchaDisplayData.Trade> trades,
        List<MatchaDisplayData.Acquisition> acquisitions,
        List<ItemStack> ingredients
) implements CustomPacketPayload {
    public static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ENTRIES = 4096;
    public static final Type<MatchaJeiDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("matcha_jei_integration", "display_data")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MatchaJeiDataPayload> STREAM_CODEC =
            StreamCodec.of(MatchaJeiDataPayload::encode, MatchaJeiDataPayload::decode);
    public static final MatchaJeiDataPayload EMPTY = new MatchaJeiDataPayload("", List.of(), List.of(), List.of());

    public MatchaJeiDataPayload {
        revision = revision == null ? "" : revision;
        trades = List.copyOf(trades);
        acquisitions = List.copyOf(acquisitions);
        ingredients = ingredients.stream().map(ItemStack::copy).toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MatchaJeiDataPayload payload) {
        buffer.writeUtf(payload.revision, 128);
        writeList(buffer, payload.trades, MatchaJeiDataPayload::encodeTrade);
        writeList(buffer, payload.acquisitions, MatchaJeiDataPayload::encodeAcquisition);
        writeList(buffer, payload.ingredients, ItemStack.STREAM_CODEC::encode);
    }

    private static MatchaJeiDataPayload decode(RegistryFriendlyByteBuf buffer) {
        String revision = buffer.readUtf(128);
        List<MatchaDisplayData.Trade> trades = readList(buffer, MatchaJeiDataPayload::decodeTrade);
        List<MatchaDisplayData.Acquisition> acquisitions = readList(buffer, MatchaJeiDataPayload::decodeAcquisition);
        List<ItemStack> ingredients = readList(buffer, ItemStack.STREAM_CODEC::decode);
        return new MatchaJeiDataPayload(revision, trades, acquisitions, ingredients);
    }

    private static void encodeTrade(RegistryFriendlyByteBuf buffer, MatchaDisplayData.Trade trade) {
        buffer.writeIdentifier(trade.sourceId());
        buffer.writeUtf(trade.displayKey(), 128);
        buffer.writeUtf(trade.profession(), 64);
        buffer.writeVarInt(trade.level());
        ItemStack.STREAM_CODEC.encode(buffer, trade.firstInput());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, trade.secondInput());
        ItemStack.STREAM_CODEC.encode(buffer, trade.output());
        buffer.writeBoolean(trade.conditional());
    }

    private static MatchaDisplayData.Trade decodeTrade(RegistryFriendlyByteBuf buffer) {
        return new MatchaDisplayData.Trade(
                buffer.readIdentifier(),
                buffer.readUtf(128),
                buffer.readUtf(64),
                buffer.readVarInt(),
                ItemStack.STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readBoolean()
        );
    }

    private static void encodeAcquisition(RegistryFriendlyByteBuf buffer, MatchaDisplayData.Acquisition acquisition) {
        buffer.writeIdentifier(acquisition.sourceId());
        buffer.writeUtf(acquisition.displayKey(), 128);
        buffer.writeUtf(acquisition.description(), 256);
        ItemStack.STREAM_CODEC.encode(buffer, acquisition.output());
    }

    private static MatchaDisplayData.Acquisition decodeAcquisition(RegistryFriendlyByteBuf buffer) {
        return new MatchaDisplayData.Acquisition(
                buffer.readIdentifier(),
                buffer.readUtf(128),
                buffer.readUtf(256),
                ItemStack.STREAM_CODEC.decode(buffer)
        );
    }

    private static <T> void writeList(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            EntryEncoder<T> encoder
    ) {
        if (values.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many Matcha JEI display entries: " + values.size());
        }
        buffer.writeVarInt(values.size());
        for (T value : values) {
            encoder.encode(buffer, value);
        }
    }

    private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, EntryDecoder<T> decoder) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid Matcha JEI display entry count: " + size);
        }
        List<T> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(decoder.decode(buffer));
        }
        return List.copyOf(values);
    }

    @FunctionalInterface
    private interface EntryEncoder<T> {
        void encode(RegistryFriendlyByteBuf buffer, T value);
    }

    @FunctionalInterface
    private interface EntryDecoder<T> {
        T decode(RegistryFriendlyByteBuf buffer);
    }
}
