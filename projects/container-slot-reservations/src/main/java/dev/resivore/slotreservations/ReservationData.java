package dev.resivore.slotreservations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable, sparse reservation state for one physical 27-slot container.
 *
 * <p>Templates are always normalized to count one. Their item and complete component patch are
 * retained, so identity comparisons can use {@link ItemStack#isSameItemSameComponents} without
 * ever representing a reservation as a physical item in the container.</p>
 */
public final class ReservationData {
    public static final int FORMAT_VERSION = 1;
    public static final int SLOT_COUNT = 27;
    public static final ReservationData EMPTY = new ReservationData(Map.of());

    private static final Codec<Integer> VERSION_CODEC = Codec.INT.validate(version ->
            version == FORMAT_VERSION
                    ? DataResult.success(version)
                    : DataResult.error(() -> "Unsupported reservation format version: " + version));
    private static final Codec<Integer> SLOT_CODEC = Codec.intRange(0, SLOT_COUNT - 1);

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SLOT_CODEC.fieldOf("slot").forGetter(Entry::slot),
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(Entry::template)
    ).apply(instance, Entry::new));

    private static final Codec<SerializedData> SERIALIZED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VERSION_CODEC.fieldOf("version").forGetter(SerializedData::version),
            ENTRY_CODEC.listOf().fieldOf("slots").forGetter(SerializedData::slots)
    ).apply(instance, SerializedData::new));

    public static final Codec<ReservationData> CODEC = SERIALIZED_CODEC.comapFlatMap(
            ReservationData::decode,
            ReservationData::encode
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ReservationData> STREAM_CODEC = StreamCodec.of(
            ReservationData::encodeToNetwork,
            ReservationData::decodeFromNetwork
    );

    private final NavigableMap<Integer, ItemStackTemplate> reservations;

    private ReservationData(Map<Integer, ItemStackTemplate> reservations) {
        TreeMap<Integer, ItemStackTemplate> normalized = new TreeMap<>();
        reservations.forEach((slot, template) -> normalized.put(
                validateSlot(slot),
                normalize(Objects.requireNonNull(template, "template"))
        ));
        this.reservations = Collections.unmodifiableNavigableMap(normalized);
    }

    public int version() {
        return FORMAT_VERSION;
    }

    public int size() {
        return reservations.size();
    }

    public boolean isEmpty() {
        return reservations.isEmpty();
    }

    /** Returns a new count-one stack, never the value retained by this data object. */
    public Optional<ItemStack> get(int slot) {
        validateSlot(slot);
        return getTemplate(slot).map(ItemStackTemplate::create);
    }

    public boolean matches(int slot, ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        if (incoming.isEmpty()) {
            return false;
        }
        return get(slot).filter(template -> ItemStack.isSameItemSameComponents(template, incoming)).isPresent();
    }

    public ReservationData with(int slot, ItemStack template) {
        validateSlot(slot);
        Objects.requireNonNull(template, "template");
        if (template.isEmpty()) {
            throw new IllegalArgumentException("A reservation template must be non-empty");
        }

        ItemStackTemplate normalized = normalize(ItemStackTemplate.fromNonEmptyStack(template));
        if (normalized.equals(reservations.get(slot))) {
            return this;
        }

        TreeMap<Integer, ItemStackTemplate> changed = new TreeMap<>(reservations);
        changed.put(slot, normalized);
        return new ReservationData(changed);
    }

    public ReservationData without(int slot) {
        validateSlot(slot);
        if (!reservations.containsKey(slot)) {
            return this;
        }

        TreeMap<Integer, ItemStackTemplate> changed = new TreeMap<>(reservations);
        changed.remove(slot);
        return changed.isEmpty() ? EMPTY : new ReservationData(changed);
    }

    /** Returns a sparse map whose stack values are fresh defensive copies. */
    public Map<Integer, ItemStack> reservations() {
        TreeMap<Integer, ItemStack> copies = new TreeMap<>();
        reservations.forEach((slot, template) -> copies.put(slot, template.create()));
        return Collections.unmodifiableMap(copies);
    }

    Optional<ItemStackTemplate> getTemplate(int slot) {
        validateSlot(slot);
        return Optional.ofNullable(reservations.get(slot));
    }

    private static DataResult<ReservationData> decode(SerializedData serialized) {
        if (serialized.slots().size() > SLOT_COUNT) {
            return DataResult.error(() -> "Too many reservation entries: " + serialized.slots().size());
        }

        TreeMap<Integer, ItemStackTemplate> decoded = new TreeMap<>();
        for (Entry entry : serialized.slots()) {
            if (decoded.putIfAbsent(entry.slot(), entry.template()) != null) {
                return DataResult.error(() -> "Duplicate reservation slot: " + entry.slot());
            }
        }
        return DataResult.success(decoded.isEmpty() ? EMPTY : new ReservationData(decoded));
    }

    private static SerializedData encode(ReservationData data) {
        List<Entry> entries = data.reservations.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList();
        return new SerializedData(FORMAT_VERSION, entries);
    }

    private static void encodeToNetwork(RegistryFriendlyByteBuf buffer, ReservationData data) {
        buffer.writeVarInt(FORMAT_VERSION);
        buffer.writeVarInt(data.reservations.size());
        data.reservations.forEach((slot, template) -> {
            buffer.writeVarInt(slot);
            ItemStackTemplate.STREAM_CODEC.encode(buffer, template);
        });
    }

    private static ReservationData decodeFromNetwork(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readVarInt();
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported reservation format version: " + version);
        }

        int size = buffer.readVarInt();
        if (size < 0 || size > SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid reservation entry count: " + size);
        }

        TreeMap<Integer, ItemStackTemplate> decoded = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            int slot = validateSlot(buffer.readVarInt());
            ItemStackTemplate template = normalize(ItemStackTemplate.STREAM_CODEC.decode(buffer));
            if (decoded.putIfAbsent(slot, template) != null) {
                throw new IllegalArgumentException("Duplicate reservation slot: " + slot);
            }
        }
        return decoded.isEmpty() ? EMPTY : new ReservationData(decoded);
    }

    private static ItemStackTemplate normalize(ItemStackTemplate template) {
        return template.withCount(1);
    }

    private static int validateSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException("Reservation slot must be between 0 and 26: " + slot);
        }
        return slot;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof ReservationData data && reservations.equals(data.reservations);
    }

    @Override
    public int hashCode() {
        return reservations.hashCode();
    }

    @Override
    public String toString() {
        return "ReservationData[version=" + FORMAT_VERSION + ", reservations=" + reservations + ']';
    }

    private record Entry(int slot, ItemStackTemplate template) {
        private Entry {
            validateSlot(slot);
            template = normalize(Objects.requireNonNull(template, "template"));
        }
    }

    private record SerializedData(int version, List<Entry> slots) {
        private SerializedData {
            slots = List.copyOf(new ArrayList<>(slots));
        }
    }
}
