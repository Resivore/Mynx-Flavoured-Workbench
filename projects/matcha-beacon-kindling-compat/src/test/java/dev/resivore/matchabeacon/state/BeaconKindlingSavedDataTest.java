package dev.resivore.matchabeacon.state;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BeaconKindlingSavedDataTest {
    private static final UUID PLAYER_ONE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_TWO = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MARKER_ONE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MARKER_TWO = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID REPLACEMENT_MARKER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID TRADER_ONE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecRestartRoundTripPreservesExactPendingOwnerLocationAndTimer() {
        SummonRecord pending = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.NETHER,
                new BlockPos(-37, 61, 92));
        for (int tick = 0; tick < 1_237; tick++) {
            pending = SummonLifecycle.advance(pending, LifecycleObservation.activeApproach())
                    .nextRecord().orElseThrow();
        }

        BeaconKindlingSavedData original = new BeaconKindlingSavedData();
        original.put(pending);
        JsonElement encoded = BeaconKindlingSavedData.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        BeaconKindlingSavedData restored = BeaconKindlingSavedData.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        SummonRecord decoded = restored.get(PLAYER_ONE).orElseThrow();
        assertEquals(pending, decoded);
        assertEquals(Level.NETHER, decoded.dimension());
        assertEquals(new BlockPos(-37, 61, 92), decoded.beaconPos());
        assertTrue(decoded.ownsMarker(MARKER_ONE));
        assertEquals(SummonRecord.APPROACH_TICKS - 1_237, decoded.approachTicksRemaining());

        LifecycleStep resumed = SummonLifecycle.advance(decoded, LifecycleObservation.activeApproach());
        assertEquals(decoded.approachTicksRemaining() - 1,
                resumed.nextRecord().orElseThrow().approachTicksRemaining());
    }

    @Test
    void twoPlayersAndMarkersAdvanceIndependently() {
        SummonRecord first = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.OVERWORLD,
                new BlockPos(1, 70, 1));
        SummonRecord second = SummonRecord.approach(
                PLAYER_TWO,
                MARKER_TWO,
                Level.END,
                new BlockPos(-9, 80, 4));
        BeaconKindlingSavedData data = new BeaconKindlingSavedData();
        data.put(first);
        data.put(second);

        LifecycleStep firstStep = SummonLifecycle.advance(first, LifecycleObservation.activeApproach());
        assertTrue(data.apply(firstStep));

        assertEquals(first.approachTicksRemaining() - 1,
                data.get(PLAYER_ONE).orElseThrow().approachTicksRemaining());
        assertEquals(second, data.get(PLAYER_TWO).orElseThrow());
        assertTrue(data.get(PLAYER_ONE).orElseThrow().ownsMarker(MARKER_ONE));
        assertFalse(data.get(PLAYER_ONE).orElseThrow().ownsMarker(MARKER_TWO));
        assertEquals(Map.of(PLAYER_ONE, data.get(PLAYER_ONE).orElseThrow(), PLAYER_TWO, second), data.snapshot());
    }

    @Test
    void exactMarkerCannotBeOwnedByTwoPlayers() {
        BeaconKindlingSavedData data = new BeaconKindlingSavedData();
        SummonRecord first = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.OVERWORLD,
                BlockPos.ZERO);
        SummonRecord conflicting = SummonRecord.approach(
                PLAYER_TWO,
                MARKER_ONE,
                Level.OVERWORLD,
                new BlockPos(10, 70, 10));

        data.put(first);
        assertThrows(IllegalArgumentException.class, () -> data.put(conflicting));
        assertEquals(Optional.of(first), data.findByMarker(MARKER_ONE));
        assertTrue(data.get(PLAYER_TWO).isEmpty());
    }

    @Test
    void exactTraderCannotBeOwnedByTwoPlayers() {
        BeaconKindlingSavedData data = new BeaconKindlingSavedData();
        SummonRecord first = SummonLifecycle.beginVisit(
                SummonRecord.approach(PLAYER_ONE, MARKER_ONE, Level.OVERWORLD, BlockPos.ZERO)
                        .withApproachTicksRemaining(0),
                TRADER_ONE);
        SummonRecord conflicting = SummonLifecycle.beginVisit(
                SummonRecord.approach(
                                PLAYER_TWO,
                                MARKER_TWO,
                                Level.END,
                                new BlockPos(10, 70, 10))
                        .withApproachTicksRemaining(0),
                TRADER_ONE);

        data.put(first);
        assertThrows(IllegalArgumentException.class, () -> data.put(conflicting));
        assertEquals(Optional.of(first), data.findByTrader(TRADER_ONE));
        assertTrue(data.get(PLAYER_TWO).isEmpty());
    }

    @Test
    void stalePrePatchLockIsClearedOnlyWithoutAValidTrackedSummon() {
        SummonRecord tracked = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.OVERWORLD,
                BlockPos.ZERO);

        assertTrue(StaleLockPolicy.shouldClearMatchaLock(true, Optional.empty()));
        assertFalse(StaleLockPolicy.shouldClearMatchaLock(true, Optional.of(tracked)));
        assertFalse(StaleLockPolicy.shouldClearMatchaLock(false, Optional.empty()));
    }

    @Test
    void terminalDecisionRemovesOnlyItsExactPlayerRecord() {
        SummonRecord first = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.OVERWORLD,
                BlockPos.ZERO);
        SummonRecord second = SummonRecord.approach(
                PLAYER_TWO,
                MARKER_TWO,
                Level.END,
                new BlockPos(4, 75, 4));
        BeaconKindlingSavedData data = new BeaconKindlingSavedData();
        data.put(first);
        data.put(second);

        LifecycleStep cancelled = SummonLifecycle.advance(
                first,
                new LifecycleObservation(true, true, false, true, false));
        assertTrue(data.apply(cancelled));

        assertTrue(data.get(PLAYER_ONE).isEmpty());
        assertEquals(second, data.get(PLAYER_TWO).orElseThrow());
    }

    @Test
    void staleTerminalDecisionCannotRemoveAReplacementSummon() {
        SummonRecord original = SummonRecord.approach(
                PLAYER_ONE,
                MARKER_ONE,
                Level.OVERWORLD,
                BlockPos.ZERO);
        LifecycleStep staleCancellation = SummonLifecycle.advance(
                original,
                new LifecycleObservation(true, true, false, true, false));
        SummonRecord replacement = SummonRecord.approach(
                PLAYER_ONE,
                REPLACEMENT_MARKER,
                Level.NETHER,
                new BlockPos(8, 64, 8));
        BeaconKindlingSavedData data = new BeaconKindlingSavedData();
        data.put(original);
        data.put(replacement);

        assertFalse(data.apply(staleCancellation));
        assertEquals(replacement, data.get(PLAYER_ONE).orElseThrow());
    }
}
