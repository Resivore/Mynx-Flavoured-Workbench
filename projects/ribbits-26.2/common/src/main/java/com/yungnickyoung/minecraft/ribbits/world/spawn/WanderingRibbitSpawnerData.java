package com.yungnickyoung.minecraft.ribbits.world.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-global persistent schedule and exact one-entity lease for Wandering Ribbits. */
public final class WanderingRibbitSpawnerData extends SavedData {
    public static final long UNINITIALIZED_TIME = -1L;

    public static final Codec<WanderingRibbitSpawnerData> CODEC =
            RecordCodecBuilder.<WanderingRibbitSpawnerData>create(instance -> instance.group(
                    Codec.LONG.optionalFieldOf("next_attempt_time", UNINITIALIZED_TIME)
                            .forGetter(WanderingRibbitSpawnerData::nextAttemptTime),
                    Codec.LONG.optionalFieldOf("active_lease_generation", 0L)
                            .forGetter(WanderingRibbitSpawnerData::activeLeaseGeneration),
                    UUIDUtil.CODEC.optionalFieldOf("active_entity_uuid")
                            .forGetter(WanderingRibbitSpawnerData::activeEntityUuid),
                    Level.RESOURCE_KEY_CODEC.optionalFieldOf("active_dimension")
                            .forGetter(WanderingRibbitSpawnerData::activeDimension),
                    Codec.LONG.optionalFieldOf("visit_expiry", UNINITIALIZED_TIME)
                            .forGetter(WanderingRibbitSpawnerData::visitExpiry),
                    Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("fair_player_cursor", 0)
                            .forGetter(WanderingRibbitSpawnerData::fairPlayerCursor),
                    Codec.LONG.optionalFieldOf("last_observed_time", UNINITIALIZED_TIME)
                            .forGetter(WanderingRibbitSpawnerData::lastObservedTime)
            ).apply(instance, WanderingRibbitSpawnerData::decoded))
                    .validate(WanderingRibbitSpawnerData::validateDecoded);

    // Fabric accepts null for mod-owned SavedData without a vanilla DFU schema.
    @SuppressWarnings("DataFlowIssue")
    public static final SavedDataType<WanderingRibbitSpawnerData> TYPE = new SavedDataType<>(
            RibbitsCommon.id("wandering_ribbit_spawner"),
            WanderingRibbitSpawnerData::new,
            CODEC,
            null);

    private long nextAttemptTime;
    private long activeLeaseGeneration;
    private Optional<UUID> activeEntityUuid;
    private Optional<ResourceKey<Level>> activeDimension;
    private long visitExpiry;
    private int fairPlayerCursor;
    private long lastObservedTime;
    private long lastPersistenceMarkTime;

    public WanderingRibbitSpawnerData() {
        this(UNINITIALIZED_TIME, 0L, Optional.empty(), Optional.empty(),
                UNINITIALIZED_TIME, 0, UNINITIALIZED_TIME);
    }

    private WanderingRibbitSpawnerData(
            long nextAttemptTime,
            long activeLeaseGeneration,
            Optional<UUID> activeEntityUuid,
            Optional<ResourceKey<Level>> activeDimension,
            long visitExpiry,
            int fairPlayerCursor,
            long lastObservedTime
    ) {
        this.nextAttemptTime = nextAttemptTime;
        this.activeLeaseGeneration = activeLeaseGeneration;
        this.activeEntityUuid = Objects.requireNonNull(activeEntityUuid, "activeEntityUuid");
        this.activeDimension = Objects.requireNonNull(activeDimension, "activeDimension");
        this.visitExpiry = visitExpiry;
        this.fairPlayerCursor = fairPlayerCursor;
        this.lastObservedTime = lastObservedTime;
        this.lastPersistenceMarkTime = lastObservedTime;
    }

    private static WanderingRibbitSpawnerData decoded(
            long nextAttemptTime,
            long activeLeaseGeneration,
            Optional<UUID> activeEntityUuid,
            Optional<ResourceKey<Level>> activeDimension,
            long visitExpiry,
            int fairPlayerCursor,
            long lastObservedTime
    ) {
        return new WanderingRibbitSpawnerData(nextAttemptTime, activeLeaseGeneration,
                activeEntityUuid, activeDimension, visitExpiry, fairPlayerCursor, lastObservedTime);
    }

    private static DataResult<WanderingRibbitSpawnerData> validateDecoded(WanderingRibbitSpawnerData data) {
        if (data.activeLeaseGeneration < 0L) {
            return DataResult.error(() -> "Wandering Ribbit lease generation cannot be negative");
        }
        if (data.activeEntityUuid.isPresent() != data.activeDimension.isPresent()) {
            return DataResult.error(() -> "Wandering Ribbit lease UUID and dimension must be present together");
        }
        if (data.activeEntityUuid.isPresent() != (data.visitExpiry >= 0L)) {
            return DataResult.error(() -> "Wandering Ribbit active lease must have exactly one expiry");
        }
        return DataResult.success(data);
    }

    public boolean initializeIfNeeded(long now, long delay) {
        if (nextAttemptTime != UNINITIALIZED_TIME) {
            observeClock(now);
            return false;
        }
        nextAttemptTime = saturatedAdd(now, Math.max(0L, delay));
        lastObservedTime = now;
        lastPersistenceMarkTime = now;
        setDirty();
        return true;
    }

    /** Re-bases absolute deadlines if persistent game time moved backwards. */
    public void observeClock(long now) {
        boolean refreshPersistence = lastPersistenceMarkTime == UNINITIALIZED_TIME
                || now < lastPersistenceMarkTime
                || now - lastPersistenceMarkTime >= 1_200L;
        if (lastObservedTime != UNINITIALIZED_TIME && now < lastObservedTime) {
            long rollback = lastObservedTime - now;
            nextAttemptTime = rebaseDeadline(nextAttemptTime, rollback, now);
            visitExpiry = rebaseDeadline(visitExpiry, rollback, now);
            refreshPersistence = true;
        }
        if (refreshPersistence) {
            setDirty();
            lastPersistenceMarkTime = now;
        }
        lastObservedTime = now;
    }

    public void scheduleNextAttempt(long now, long delay) {
        nextAttemptTime = saturatedAdd(now, Math.max(0L, delay));
        lastObservedTime = now;
        lastPersistenceMarkTime = now;
        setDirty();
    }

    public long nextLeaseGeneration() {
        if (activeLeaseGeneration == Long.MAX_VALUE) {
            throw new IllegalStateException("Wandering Ribbit lease generation exhausted");
        }
        return activeLeaseGeneration + 1L;
    }

    /** Commits the cap lease only after the exact entity has been inserted into its level. */
    public void commitLease(UUID entityUuid, ResourceKey<Level> dimension, long generation, long expiry) {
        Objects.requireNonNull(entityUuid, "entityUuid");
        Objects.requireNonNull(dimension, "dimension");
        if (hasActiveLease()) {
            throw new IllegalStateException("A Wandering Ribbit lease is already active");
        }
        if (generation != nextLeaseGeneration() || expiry < 0L) {
            throw new IllegalArgumentException("Invalid Wandering Ribbit lease generation or expiry");
        }
        activeLeaseGeneration = generation;
        activeEntityUuid = Optional.of(entityUuid);
        activeDimension = Optional.of(dimension);
        visitExpiry = expiry;
        setDirty();
    }

    public boolean ownsLease(UUID entityUuid, ResourceKey<Level> dimension, long generation) {
        return generation == activeLeaseGeneration
                && activeEntityUuid.filter(entityUuid::equals).isPresent()
                && activeDimension.filter(dimension::equals).isPresent();
    }

    public boolean clearLeaseIfOwned(UUID entityUuid, ResourceKey<Level> dimension, long generation) {
        if (!ownsLease(entityUuid, dimension, generation)) {
            return false;
        }
        clearLease();
        return true;
    }

    public void clearLease() {
        if (!hasActiveLease()) {
            return;
        }
        activeEntityUuid = Optional.empty();
        activeDimension = Optional.empty();
        visitExpiry = UNINITIALIZED_TIME;
        setDirty();
    }

    /** Returns a stable starting index and advances one position for the next attempt. */
    public int takeFairPlayerStart(int playerCount) {
        if (playerCount <= 0) {
            throw new IllegalArgumentException("playerCount must be positive");
        }
        int selected = Math.floorMod(fairPlayerCursor, playerCount);
        fairPlayerCursor = selected == playerCount - 1 ? 0 : selected + 1;
        setDirty();
        return selected;
    }

    public boolean hasActiveLease() {
        return activeEntityUuid.isPresent();
    }

    public long nextAttemptTime() {
        return nextAttemptTime;
    }

    public long activeLeaseGeneration() {
        return activeLeaseGeneration;
    }

    public Optional<UUID> activeEntityUuid() {
        return activeEntityUuid;
    }

    public Optional<ResourceKey<Level>> activeDimension() {
        return activeDimension;
    }

    public long visitExpiry() {
        return visitExpiry;
    }

    public int fairPlayerCursor() {
        return fairPlayerCursor;
    }

    public long lastObservedTime() {
        return lastObservedTime;
    }

    private static long rebaseDeadline(long deadline, long rollback, long now) {
        if (deadline == UNINITIALIZED_TIME) {
            return deadline;
        }
        return Math.max(now, deadline - rollback);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
