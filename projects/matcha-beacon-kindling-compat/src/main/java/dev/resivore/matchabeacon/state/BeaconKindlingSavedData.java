package dev.resivore.matchabeacon.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-global persistent authority for all exact-player Beacon Kindling summons. */
public final class BeaconKindlingSavedData extends SavedData {
    private static final String MOD_ID = "matcha_beacon_kindling_compat";
    private static final Codec<List<SummonRecord>> RECORD_LIST_CODEC = SummonRecord.CODEC.listOf()
            .optionalFieldOf("summons", List.of())
            .codec();

    public static final Codec<BeaconKindlingSavedData> CODEC = RECORD_LIST_CODEC.comapFlatMap(
            BeaconKindlingSavedData::decode,
            BeaconKindlingSavedData::orderedRecords);

    // Fabric accepts null for mod-owned SavedData without a vanilla DFU schema.
    @SuppressWarnings("DataFlowIssue")
    public static final SavedDataType<BeaconKindlingSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "summons"),
            BeaconKindlingSavedData::new,
            CODEC,
            null);

    private final Map<UUID, SummonRecord> byPlayer;

    public BeaconKindlingSavedData() {
        this(new HashMap<>());
    }

    private BeaconKindlingSavedData(Map<UUID, SummonRecord> byPlayer) {
        this.byPlayer = byPlayer;
    }

    private static DataResult<BeaconKindlingSavedData> decode(List<SummonRecord> records) {
        Map<UUID, SummonRecord> byPlayer = new HashMap<>();
        for (SummonRecord record : records) {
            SummonRecord previous = byPlayer.putIfAbsent(record.playerId(), record);
            if (previous != null) {
                return DataResult.error(() ->
                        "Duplicate Beacon Kindling summon for player " + record.playerId());
            }
            for (SummonRecord other : byPlayer.values()) {
                if (other.playerId().equals(record.playerId())) {
                    continue;
                }
                if (other.markerId().equals(record.markerId())) {
                    return DataResult.error(() ->
                            "Beacon Kindling marker " + record.markerId() + " has multiple player owners");
                }
                if (record.traderId().isPresent()
                        && other.traderId().equals(record.traderId())) {
                    return DataResult.error(() ->
                            "Beacon Kindling trader " + record.traderId().orElseThrow()
                                    + " has multiple player owners");
                }
            }
        }
        return DataResult.success(new BeaconKindlingSavedData(byPlayer));
    }

    private List<SummonRecord> orderedRecords() {
        ArrayList<SummonRecord> records = new ArrayList<>(byPlayer.values());
        records.sort(Comparator.comparing(record -> record.playerId().toString()));
        return List.copyOf(records);
    }

    public Optional<SummonRecord> get(UUID playerId) {
        return Optional.ofNullable(byPlayer.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public boolean hasActiveSummon(UUID playerId) {
        return byPlayer.containsKey(Objects.requireNonNull(playerId, "playerId"));
    }

    public Map<UUID, SummonRecord> snapshot() {
        return Map.copyOf(byPlayer);
    }

    public Optional<SummonRecord> findByMarker(UUID markerId) {
        Objects.requireNonNull(markerId, "markerId");
        return byPlayer.values().stream().filter(record -> record.ownsMarker(markerId)).findFirst();
    }

    public Optional<SummonRecord> findByTrader(UUID traderId) {
        Objects.requireNonNull(traderId, "traderId");
        return byPlayer.values().stream().filter(record -> record.ownsTrader(traderId)).findFirst();
    }

    public Optional<SummonRecord> put(SummonRecord record) {
        Objects.requireNonNull(record, "record");
        for (SummonRecord other : byPlayer.values()) {
            if (other.playerId().equals(record.playerId())) {
                continue;
            }
            if (other.markerId().equals(record.markerId())) {
                throw new IllegalArgumentException(
                        "Marker " + record.markerId() + " is already owned by " + other.playerId());
            }
            if (record.traderId().isPresent() && other.traderId().equals(record.traderId())) {
                throw new IllegalArgumentException(
                        "Trader " + record.traderId().orElseThrow() + " is already owned by " + other.playerId());
            }
        }
        SummonRecord previous = byPlayer.put(record.playerId(), record);
        if (!record.equals(previous)) {
            setDirty();
        }
        return Optional.ofNullable(previous);
    }

    public Optional<SummonRecord> remove(UUID playerId) {
        SummonRecord removed = byPlayer.remove(Objects.requireNonNull(playerId, "playerId"));
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    /** Applies a pure lifecycle result without allowing it to affect another player's record. */
    public boolean apply(LifecycleStep step) {
        Objects.requireNonNull(step, "step");
        if (step.decision() == LifecycleDecision.ARRIVE) {
            throw new IllegalArgumentException(
                    "ARRIVE must be resolved by storing beginVisit's exact trader UUID or removing the summon");
        }
        SummonRecord before = byPlayer.get(step.playerId());
        if (before == null || !before.markerId().equals(step.markerId())) {
            return false;
        }
        if (step.nextRecord().isPresent()) {
            put(step.nextRecord().orElseThrow());
        } else {
            remove(step.playerId());
        }
        return !Objects.equals(before, byPlayer.get(step.playerId()));
    }
}
