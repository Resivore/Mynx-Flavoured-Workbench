package com.yungnickyoung.minecraft.ribbits.wandering;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.yungnickyoung.minecraft.ribbits.world.spawn.WanderingRibbitSpawnerData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitSpawnerDataTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void initializationAndClockRollbackPreserveRemainingDelay() {
        WanderingRibbitSpawnerData data = new WanderingRibbitSpawnerData();
        assertTrue(data.initializeIfNeeded(1_000L, 500L));
        assertFalse(data.initializeIfNeeded(1_001L, 999L));
        assertEquals(1_500L, data.nextAttemptTime());

        data.observeClock(1_200L);
        data.observeClock(200L);
        assertEquals(500L, data.nextAttemptTime());
        assertEquals(200L, data.lastObservedTime());
    }

    @Test
    void continuouslyObservedClockIsMarkedForPersistenceEveryTwelveHundredTicks() {
        WanderingRibbitSpawnerData data = new WanderingRibbitSpawnerData();
        data.initializeIfNeeded(1_000L, 500L);
        data.setDirty(false);

        for (long now = 1_001L; now < 2_200L; now++) {
            data.observeClock(now);
        }
        assertFalse(data.isDirty());

        data.observeClock(2_200L);
        assertTrue(data.isDirty());
    }

    @Test
    void leaseUsesExactUuidDimensionAndMonotonicGeneration() {
        WanderingRibbitSpawnerData data = new WanderingRibbitSpawnerData();
        UUID entity = UUID.randomUUID();
        long generation = data.nextLeaseGeneration();
        data.commitLease(entity, Level.OVERWORLD, generation, 48_000L);

        assertTrue(data.hasActiveLease());
        assertTrue(data.ownsLease(entity, Level.OVERWORLD, generation));
        assertFalse(data.ownsLease(UUID.randomUUID(), Level.OVERWORLD, generation));
        assertFalse(data.ownsLease(entity, Level.NETHER, generation));
        assertFalse(data.clearLeaseIfOwned(entity, Level.OVERWORLD, generation + 1));
        assertTrue(data.clearLeaseIfOwned(entity, Level.OVERWORLD, generation));
        assertFalse(data.hasActiveLease());
        assertEquals(generation + 1, data.nextLeaseGeneration());
    }

    @Test
    void capLeaseCannotBeOverwritten() {
        WanderingRibbitSpawnerData data = new WanderingRibbitSpawnerData();
        data.commitLease(UUID.randomUUID(), Level.OVERWORLD, 1L, 48_000L);
        assertThrows(IllegalStateException.class, () ->
                data.commitLease(UUID.randomUUID(), Level.OVERWORLD, 2L, 96_000L));
    }

    @Test
    void fairCursorRotatesAndSurvivesChangingPlayerCounts() {
        WanderingRibbitSpawnerData data = new WanderingRibbitSpawnerData();
        assertEquals(0, data.takeFairPlayerStart(3));
        assertEquals(1, data.takeFairPlayerStart(3));
        assertEquals(0, data.takeFairPlayerStart(1));
        assertEquals(0, data.takeFairPlayerStart(2));
        assertEquals(1, data.takeFairPlayerStart(2));
        assertThrows(IllegalArgumentException.class, () -> data.takeFairPlayerStart(0));
    }

    @Test
    void codecRoundTripRetainsScheduleLeaseAndFairnessIdentity() {
        WanderingRibbitSpawnerData original = new WanderingRibbitSpawnerData();
        original.initializeIfNeeded(5_000L, 60_000L);
        original.takeFairPlayerStart(7);
        UUID entity = UUID.randomUUID();
        original.commitLease(entity, Level.OVERWORLD, 1L, 53_000L);

        JsonElement encoded = WanderingRibbitSpawnerData.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        WanderingRibbitSpawnerData decoded = WanderingRibbitSpawnerData.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(65_000L, decoded.nextAttemptTime());
        assertEquals(1, decoded.fairPlayerCursor());
        assertEquals(53_000L, decoded.visitExpiry());
        assertTrue(decoded.ownsLease(entity, Level.OVERWORLD, 1L));
    }
}
