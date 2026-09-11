package dev.resivore.slotreservations;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
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
import java.util.UUID;

/** Sparse reservations: format 1 templates plus format 2 specific portable-container identities. */
public final class ReservationData {
    public static final int FORMAT_VERSION = 2;
    private static final int LEGACY_FORMAT_VERSION = 1;
    public static final int SLOT_COUNT = 27;
    public static final ReservationData EMPTY = new ReservationData(Map.of());

    private static final Codec<Integer> SLOT_CODEC = Codec.intRange(0, SLOT_COUNT - 1);
    private static final Codec<PortableContainerIdentity.Family> FAMILY_CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try { return DataResult.success(PortableContainerIdentity.Family.valueOf(value)); }
                catch (IllegalArgumentException exception) { return DataResult.error(() -> "Unknown portable container family: " + value); }
            }, PortableContainerIdentity.Family::name);
    private static final Codec<LegacyEntry> LEGACY_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SLOT_CODEC.fieldOf("slot").forGetter(LegacyEntry::slot),
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(LegacyEntry::template)
    ).apply(instance, LegacyEntry::new));
    private static final Codec<LegacySerializedData> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.validate(version -> version == LEGACY_FORMAT_VERSION ? DataResult.success(version)
                    : DataResult.error(() -> "Unsupported legacy reservation format version: " + version))
                    .fieldOf("version").forGetter(LegacySerializedData::version),
            LEGACY_ENTRY_CODEC.listOf().fieldOf("slots").forGetter(LegacySerializedData::slots)
    ).apply(instance, LegacySerializedData::new));
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SLOT_CODEC.fieldOf("slot").forGetter(Entry::slot),
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(Entry::template),
            FAMILY_CODEC.optionalFieldOf("portable_family").forGetter(Entry::family),
            UUIDUtil.CODEC.optionalFieldOf("portable_id").forGetter(Entry::identity)
    ).apply(instance, Entry::new));
    private static final Codec<SerializedData> SERIALIZED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.validate(version -> version == FORMAT_VERSION ? DataResult.success(version)
                    : DataResult.error(() -> "Unsupported reservation format version: " + version))
                    .fieldOf("version").forGetter(SerializedData::version),
            ENTRY_CODEC.listOf().fieldOf("slots").forGetter(SerializedData::slots)
    ).apply(instance, SerializedData::new));
    public static final Codec<ReservationData> CODEC = Codec.either(SERIALIZED_CODEC, LEGACY_CODEC)
            .comapFlatMap(ReservationData::decode, ReservationData::encode);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReservationData> STREAM_CODEC = StreamCodec.of(
            ReservationData::encodeToNetwork, ReservationData::decodeFromNetwork);

    private final NavigableMap<Integer, Entry> reservations;
    private ReservationData(Map<Integer, Entry> reservations) {
        TreeMap<Integer, Entry> normalized = new TreeMap<>();
        reservations.forEach((slot, entry) -> normalized.put(validateSlot(slot), normalize(entry)));
        this.reservations = Collections.unmodifiableNavigableMap(normalized);
    }

    public int version() { return FORMAT_VERSION; }
    public int size() { return reservations.size(); }
    public boolean isEmpty() { return reservations.isEmpty(); }
    /** A display/template copy which never bears CSR's physical identity component. */
    public Optional<ItemStack> get(int slot) { validateSlot(slot); return getEntry(slot).map(entry -> entry.template().create()); }
    public boolean matches(int slot, ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        return !incoming.isEmpty() && getEntry(slot).map(entry -> entry.matches(incoming)).orElse(false);
    }
    public ReservationData with(int slot, ItemStack template) {
        validateSlot(slot); Objects.requireNonNull(template, "template");
        if (template.isEmpty()) throw new IllegalArgumentException("A reservation template must be non-empty");
        Entry entry = Entry.template(slot, template);
        if (entry.equals(reservations.get(slot))) return this;
        TreeMap<Integer, Entry> changed = new TreeMap<>(reservations); changed.put(slot, entry);
        return new ReservationData(changed);
    }
    public ReservationData withSpecific(int slot, ItemStack physical, UUID identity) {
        validateSlot(slot); Objects.requireNonNull(physical, "physical"); Objects.requireNonNull(identity, "identity");
        PortableContainerIdentity.Family family = PortableContainerIdentity.familyOf(physical)
                .orElseThrow(() -> new IllegalArgumentException("Specific reservations require a portable container"));
        if (physical.getCount() != 1 || PortableContainerIdentity.isEmpty(physical)) {
            throw new IllegalArgumentException("Specific reservations require a non-empty count-one portable container");
        }
        Entry entry = Entry.specific(slot, physical, family, identity);
        if (entry.equals(reservations.get(slot))) return this;
        TreeMap<Integer, Entry> changed = new TreeMap<>(reservations); changed.put(slot, entry);
        return new ReservationData(changed);
    }
    public ReservationData without(int slot) {
        validateSlot(slot); if (!reservations.containsKey(slot)) return this;
        TreeMap<Integer, Entry> changed = new TreeMap<>(reservations); changed.remove(slot);
        return changed.isEmpty() ? EMPTY : new ReservationData(changed);
    }
    public Map<Integer, ItemStack> reservations() {
        TreeMap<Integer, ItemStack> copies = new TreeMap<>();
        reservations.forEach((slot, entry) -> copies.put(slot, entry.template().create()));
        return Collections.unmodifiableMap(copies);
    }
    Optional<ItemStackTemplate> getTemplate(int slot) { return getEntry(slot).map(Entry::template); }
    public Optional<Entry> getEntry(int slot) { validateSlot(slot); return Optional.ofNullable(reservations.get(slot)); }

    private static DataResult<ReservationData> decode(Either<SerializedData, LegacySerializedData> encoded) {
        List<Entry> entries = encoded.map(SerializedData::slots,
                legacy -> legacy.slots().stream().map(entry -> Entry.template(entry.slot(), entry.template().create())).toList());
        if (entries.size() > SLOT_COUNT) return DataResult.error(() -> "Too many reservation entries: " + entries.size());
        TreeMap<Integer, Entry> decoded = new TreeMap<>();
        for (Entry entry : entries) if (decoded.putIfAbsent(entry.slot(), entry) != null) {
            return DataResult.error(() -> "Duplicate reservation slot: " + entry.slot());
        }
        return DataResult.success(decoded.isEmpty() ? EMPTY : new ReservationData(decoded));
    }
    private static Either<SerializedData, LegacySerializedData> encode(ReservationData data) {
        return Either.left(new SerializedData(FORMAT_VERSION, data.reservations.values().stream().toList()));
    }
    private static void encodeToNetwork(RegistryFriendlyByteBuf buffer, ReservationData data) {
        buffer.writeVarInt(FORMAT_VERSION); buffer.writeVarInt(data.reservations.size());
        data.reservations.forEach((slot, entry) -> {
            buffer.writeVarInt(slot); ItemStackTemplate.STREAM_CODEC.encode(buffer, entry.template()); buffer.writeBoolean(entry.specific());
            if (entry.specific()) { buffer.writeEnum(entry.family().orElseThrow()); UUIDUtil.STREAM_CODEC.encode(buffer, entry.identity().orElseThrow()); }
        });
    }
    private static ReservationData decodeFromNetwork(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readVarInt();
        if (version != LEGACY_FORMAT_VERSION && version != FORMAT_VERSION) throw new IllegalArgumentException("Unsupported reservation format version: " + version);
        int size = buffer.readVarInt();
        if (size < 0 || size > SLOT_COUNT) throw new IllegalArgumentException("Invalid reservation entry count: " + size);
        TreeMap<Integer, Entry> decoded = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            int slot = validateSlot(buffer.readVarInt()); ItemStackTemplate template = ItemStackTemplate.STREAM_CODEC.decode(buffer);
            Entry entry = version == FORMAT_VERSION && buffer.readBoolean()
                    ? Entry.specific(slot, template.create(), buffer.readEnum(PortableContainerIdentity.Family.class), UUIDUtil.STREAM_CODEC.decode(buffer))
                    : Entry.template(slot, template.create());
            if (decoded.putIfAbsent(slot, entry) != null) throw new IllegalArgumentException("Duplicate reservation slot: " + slot);
        }
        return decoded.isEmpty() ? EMPTY : new ReservationData(decoded);
    }
    private static Entry normalize(Entry entry) {
        return entry.specific() ? Entry.specific(entry.slot(), entry.template().create(), entry.family().orElseThrow(), entry.identity().orElseThrow())
                : Entry.template(entry.slot(), entry.template().create());
    }
    private static int validateSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IndexOutOfBoundsException("Reservation slot must be between 0 and 26: " + slot);
        return slot;
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof ReservationData data && reservations.equals(data.reservations); }
    @Override public int hashCode() { return reservations.hashCode(); }
    @Override public String toString() { return "ReservationData[version=" + FORMAT_VERSION + ", reservations=" + reservations + ']'; }

    public record Entry(int slot, ItemStackTemplate template, Optional<PortableContainerIdentity.Family> family, Optional<UUID> identity) {
        public Entry {
            validateSlot(slot); template = ItemStackTemplate.fromNonEmptyStack(PortableContainerIdentity.withoutIdentity(template.create())).withCount(1);
            family = family == null ? Optional.empty() : family; identity = identity == null ? Optional.empty() : identity;
            if (family.isPresent() != identity.isPresent()) throw new IllegalArgumentException("Specific reservation family and identity must appear together");
        }
        static Entry template(int slot, ItemStack source) { return new Entry(slot, ItemStackTemplate.fromNonEmptyStack(PortableContainerIdentity.withoutIdentity(source)), Optional.empty(), Optional.empty()); }
        static Entry specific(int slot, ItemStack display, PortableContainerIdentity.Family family, UUID identity) { return new Entry(slot, ItemStackTemplate.fromNonEmptyStack(PortableContainerIdentity.withoutIdentity(display)), Optional.of(family), Optional.of(identity)); }
        public boolean specific() { return family.isPresent(); }
        public boolean matches(ItemStack incoming) {
            if (specific()) return PortableContainerIdentity.matches(family.orElseThrow(), identity.orElseThrow(), incoming);
            ItemStack display = template.create();
            // Only a generic empty portable-container reservation ignores CSR's dormant marker.
            // Ordinary template matching remains fully component-exact.
            return PortableContainerIdentity.genericEmptyMatches(display, incoming)
                    || ItemStack.isSameItemSameComponents(display, incoming);
        }
    }
    private record LegacyEntry(int slot, ItemStackTemplate template) { private LegacyEntry { validateSlot(slot); template = template.withCount(1); } }
    private record LegacySerializedData(int version, List<LegacyEntry> slots) { private LegacySerializedData { slots = List.copyOf(new ArrayList<>(slots)); } }
    private record SerializedData(int version, List<Entry> slots) { private SerializedData { slots = List.copyOf(new ArrayList<>(slots)); } }
}
