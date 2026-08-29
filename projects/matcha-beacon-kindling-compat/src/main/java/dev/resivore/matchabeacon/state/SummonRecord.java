package dev.resivore.matchabeacon.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable exact identity and active-time counters for one player's summon.
 *
 * <p>The record deliberately stores both entity UUIDs and the immutable beacon location. Runtime
 * discovery therefore never has to substitute a nearest player, marker, beacon, or trader.</p>
 */
public final class SummonRecord {
    public static final int APPROACH_TICKS = 12_000;
    public static final int VISIT_TICKS = 6_000;

    public static final Codec<SummonRecord> CODEC = RecordCodecBuilder.<SummonRecord>create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(SummonRecord::playerId),
            UUIDUtil.CODEC.fieldOf("marker_uuid").forGetter(SummonRecord::markerId),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(SummonRecord::dimension),
            BlockPos.CODEC.fieldOf("beacon_pos").forGetter(SummonRecord::beaconPos),
            SummonPhase.CODEC.fieldOf("phase").forGetter(SummonRecord::phase),
            Codec.intRange(0, APPROACH_TICKS).fieldOf("approach_ticks_remaining")
                    .forGetter(SummonRecord::approachTicksRemaining),
            Codec.intRange(1, VISIT_TICKS).fieldOf("visit_ticks_remaining")
                    .forGetter(SummonRecord::visitTicksRemaining),
            UUIDUtil.CODEC.optionalFieldOf("trader_uuid")
                    .forGetter((SummonRecord record) -> record.traderId())
    ).apply(instance, SummonRecord::decoded)).validate(SummonRecord::validateDecoded);

    private final UUID playerId;
    private final UUID markerId;
    private final ResourceKey<Level> dimension;
    private final BlockPos beaconPos;
    private final SummonPhase phase;
    private final int approachTicksRemaining;
    private final int visitTicksRemaining;
    private final Optional<UUID> traderId;

    private SummonRecord(
            UUID playerId,
            UUID markerId,
            ResourceKey<Level> dimension,
            BlockPos beaconPos,
            SummonPhase phase,
            int approachTicksRemaining,
            int visitTicksRemaining,
            Optional<UUID> traderId
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.markerId = Objects.requireNonNull(markerId, "markerId");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.beaconPos = Objects.requireNonNull(beaconPos, "beaconPos").immutable();
        this.phase = Objects.requireNonNull(phase, "phase");
        this.approachTicksRemaining = approachTicksRemaining;
        this.visitTicksRemaining = visitTicksRemaining;
        this.traderId = Objects.requireNonNull(traderId, "traderId");
    }

    /** Starts the exact ten minutes of valid active approach time. */
    public static SummonRecord approach(
            UUID playerId,
            UUID markerId,
            ResourceKey<Level> dimension,
            BlockPos beaconPos
    ) {
        return checked(new SummonRecord(
                playerId,
                markerId,
                dimension,
                beaconPos,
                SummonPhase.APPROACH,
                APPROACH_TICKS,
                VISIT_TICKS,
                Optional.empty()));
    }

    private static SummonRecord decoded(
            UUID playerId,
            UUID markerId,
            ResourceKey<Level> dimension,
            BlockPos beaconPos,
            SummonPhase phase,
            int approachTicksRemaining,
            int visitTicksRemaining,
            Optional<UUID> traderId
    ) {
        return new SummonRecord(
                playerId,
                markerId,
                dimension,
                beaconPos,
                phase,
                approachTicksRemaining,
                visitTicksRemaining,
                traderId);
    }

    private static DataResult<SummonRecord> validateDecoded(SummonRecord record) {
        String error = validationError(record);
        return error == null ? DataResult.success(record) : DataResult.error(() -> error);
    }

    private static SummonRecord checked(SummonRecord record) {
        String error = validationError(record);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return record;
    }

    private static String validationError(SummonRecord record) {
        if (record.approachTicksRemaining < 0 || record.approachTicksRemaining > APPROACH_TICKS) {
            return "Approach ticks must be between 0 and " + APPROACH_TICKS;
        }
        if (record.visitTicksRemaining < 1 || record.visitTicksRemaining > VISIT_TICKS) {
            return "Visit ticks must be between 1 and " + VISIT_TICKS;
        }
        if (record.phase == SummonPhase.APPROACH) {
            if (record.traderId.isPresent()) {
                return "An approaching summon cannot own a trader UUID";
            }
            if (record.visitTicksRemaining != VISIT_TICKS) {
                return "An approaching summon must retain the full visit duration";
            }
        } else {
            if (record.approachTicksRemaining != 0) {
                return "A visiting summon must have completed its approach";
            }
            if (record.traderId.isEmpty()) {
                return "A visiting summon must own an exact trader UUID";
            }
        }
        return null;
    }

    SummonRecord withApproachTicksRemaining(int remaining) {
        if (phase != SummonPhase.APPROACH) {
            throw new IllegalStateException("Only an approaching summon has an approach timer");
        }
        return checked(new SummonRecord(
                playerId,
                markerId,
                dimension,
                beaconPos,
                phase,
                remaining,
                visitTicksRemaining,
                traderId));
    }

    SummonRecord beginVisit(UUID exactTraderId) {
        if (phase != SummonPhase.APPROACH || approachTicksRemaining != 0) {
            throw new IllegalStateException("A visit can begin only after the approach reaches zero");
        }
        return checked(new SummonRecord(
                playerId,
                markerId,
                dimension,
                beaconPos,
                SummonPhase.VISIT,
                0,
                VISIT_TICKS,
                Optional.of(Objects.requireNonNull(exactTraderId, "exactTraderId"))));
    }

    SummonRecord withVisitTicksRemaining(int remaining) {
        if (phase != SummonPhase.VISIT) {
            throw new IllegalStateException("Only a visiting summon has an active visit timer");
        }
        return checked(new SummonRecord(
                playerId,
                markerId,
                dimension,
                beaconPos,
                phase,
                approachTicksRemaining,
                remaining,
                traderId));
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID markerId() {
        return markerId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos beaconPos() {
        return beaconPos;
    }

    public SummonPhase phase() {
        return phase;
    }

    public int approachTicksRemaining() {
        return approachTicksRemaining;
    }

    public int visitTicksRemaining() {
        return visitTicksRemaining;
    }

    public Optional<UUID> traderId() {
        return traderId;
    }

    public boolean ownsMarker(UUID candidate) {
        return markerId.equals(Objects.requireNonNull(candidate, "candidate"));
    }

    public boolean ownsTrader(UUID candidate) {
        UUID exactCandidate = Objects.requireNonNull(candidate, "candidate");
        return traderId.filter(exactCandidate::equals).isPresent();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SummonRecord that)) {
            return false;
        }
        return approachTicksRemaining == that.approachTicksRemaining
                && visitTicksRemaining == that.visitTicksRemaining
                && playerId.equals(that.playerId)
                && markerId.equals(that.markerId)
                && dimension.equals(that.dimension)
                && beaconPos.equals(that.beaconPos)
                && phase == that.phase
                && traderId.equals(that.traderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                playerId,
                markerId,
                dimension,
                beaconPos,
                phase,
                approachTicksRemaining,
                visitTicksRemaining,
                traderId);
    }

    @Override
    public String toString() {
        return "SummonRecord["
                + "playerId=" + playerId
                + ", markerId=" + markerId
                + ", dimension=" + dimension.identifier()
                + ", beaconPos=" + beaconPos
                + ", phase=" + phase
                + ", approachTicksRemaining=" + approachTicksRemaining
                + ", visitTicksRemaining=" + visitTicksRemaining
                + ", traderId=" + traderId
                + ']';
    }
}
