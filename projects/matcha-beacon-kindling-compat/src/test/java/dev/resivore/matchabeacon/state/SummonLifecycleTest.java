package dev.resivore.matchabeacon.state;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SummonLifecycleTest {
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MARKER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRADER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final BlockPos BEACON = new BlockPos(17, 72, -9);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void normalUninterruptedLifecycleUsesExactTenAndFiveMinuteActiveTimers() {
        SummonRecord record = approach();

        for (int tick = 1; tick < SummonRecord.APPROACH_TICKS; tick++) {
            LifecycleStep step = SummonLifecycle.advance(record, LifecycleObservation.activeApproach());
            assertEquals(LifecycleDecision.ADVANCE, step.decision());
            record = step.nextRecord().orElseThrow();
        }
        LifecycleStep arrival = SummonLifecycle.advance(record, LifecycleObservation.activeApproach());
        assertEquals(LifecycleDecision.ARRIVE, arrival.decision());
        assertEquals(0, arrival.nextRecord().orElseThrow().approachTicksRemaining());

        record = SummonLifecycle.beginVisit(arrival.nextRecord().orElseThrow(), TRADER);
        assertEquals(SummonPhase.VISIT, record.phase());
        assertEquals(Optional.of(TRADER), record.traderId());
        assertEquals(SummonRecord.VISIT_TICKS, record.visitTicksRemaining());

        for (int tick = 1; tick < SummonRecord.VISIT_TICKS; tick++) {
            LifecycleStep step = SummonLifecycle.advance(record, LifecycleObservation.activeVisit());
            assertEquals(LifecycleDecision.ADVANCE, step.decision());
            record = step.nextRecord().orElseThrow();
        }
        LifecycleStep departure = SummonLifecycle.advance(record, LifecycleObservation.activeVisit());
        assertEquals(LifecycleDecision.DEPART, departure.decision());
        assertTrue(departure.terminal());
    }

    @Test
    void logoutDuringApproachPausesAndRelogResumesWithoutLosingTime() {
        SummonRecord beforeLogout = consumeApproachTicks(approach(), 317);
        LifecycleObservation offline = new LifecycleObservation(false, true, true, true, false);

        for (int tick = 0; tick < 200; tick++) {
            LifecycleStep paused = SummonLifecycle.advance(beforeLogout, offline);
            assertEquals(LifecycleDecision.PAUSE, paused.decision());
            assertEquals(beforeLogout, paused.nextRecord().orElseThrow());
        }

        LifecycleStep resumed = SummonLifecycle.advance(beforeLogout, LifecycleObservation.activeApproach());
        assertEquals(LifecycleDecision.ADVANCE, resumed.decision());
        assertEquals(beforeLogout.approachTicksRemaining() - 1,
                resumed.nextRecord().orElseThrow().approachTicksRemaining());
    }

    @Test
    void unloadedBeaconChunkPausesUntilTheExactLocationCanBeProcessedAgain() {
        SummonRecord beforeUnload = consumeApproachTicks(approach(), 911);
        LifecycleObservation unloaded = new LifecycleObservation(true, false, false, false, false);

        LifecycleStep paused = SummonLifecycle.advance(beforeUnload, unloaded);
        assertEquals(LifecycleDecision.PAUSE, paused.decision());
        assertEquals(beforeUnload, paused.nextRecord().orElseThrow());

        LifecycleStep resumed = SummonLifecycle.advance(beforeUnload, LifecycleObservation.activeApproach());
        assertEquals(beforeUnload.approachTicksRemaining() - 1,
                resumed.nextRecord().orElseThrow().approachTicksRemaining());
    }

    @Test
    void logoutDuringVisitPausesDepartureTimerAndRelogResumes() {
        SummonRecord visit = visitReady();
        visit = consumeVisitTicks(visit, 421);
        LifecycleObservation offline = new LifecycleObservation(false, true, true, true, true);

        LifecycleStep paused = SummonLifecycle.advance(visit, offline);
        assertEquals(LifecycleDecision.PAUSE, paused.decision());
        assertEquals(visit, paused.nextRecord().orElseThrow());

        LifecycleStep resumed = SummonLifecycle.advance(visit, LifecycleObservation.activeVisit());
        assertEquals(LifecycleDecision.ADVANCE, resumed.decision());
        assertEquals(visit.visitTicksRemaining() - 1,
                resumed.nextRecord().orElseThrow().visitTicksRemaining());
    }

    @Test
    void brokenOrExtinguishedBeaconAndMissingExactMarkerCancelCleanly() {
        SummonRecord approach = approach();

        assertEquals(LifecycleDecision.CANCEL, SummonLifecycle.advance(
                approach,
                new LifecycleObservation(true, true, false, true, false)).decision());
        assertEquals(LifecycleDecision.CANCEL, SummonLifecycle.advance(
                approach,
                new LifecycleObservation(true, true, true, false, false)).decision());

    }

    @Test
    void unloadedVisitTraderPausesUntilTheExactEntityLoadsAgain() {
        SummonRecord visit = consumeVisitTicks(visitReady(), 73);
        LifecycleStep missingTrader = SummonLifecycle.advance(
                visit,
                new LifecycleObservation(true, true, true, true, false));
        assertEquals(LifecycleDecision.PAUSE, missingTrader.decision());
        assertEquals(visit, missingTrader.nextRecord().orElseThrow());

        LifecycleStep resumed = SummonLifecycle.advance(visit, LifecycleObservation.activeVisit());
        assertEquals(LifecycleDecision.ADVANCE, resumed.decision());
        assertEquals(visit.visitTicksRemaining() - 1,
                resumed.nextRecord().orElseThrow().visitTicksRemaining());
    }

    @Test
    void arrivalIsEmittedOnlyOnTheCrossingAndCannotDuplicateAfterBeginVisit() {
        SummonRecord oneTickAway = approach().withApproachTicksRemaining(1);
        LifecycleStep arrival = SummonLifecycle.advance(oneTickAway, LifecycleObservation.activeApproach());

        assertEquals(LifecycleDecision.ARRIVE, arrival.decision());
        SummonRecord arrived = arrival.nextRecord().orElseThrow();
        LifecycleStep noSecondArrival = SummonLifecycle.advance(arrived, LifecycleObservation.activeApproach());
        assertEquals(LifecycleDecision.WAITING_FOR_VISIT, noSecondArrival.decision());

        SummonRecord visit = SummonLifecycle.beginVisit(arrived, TRADER);
        LifecycleStep visiting = SummonLifecycle.advance(visit, LifecycleObservation.activeVisit());
        assertEquals(LifecycleDecision.ADVANCE, visiting.decision());
        assertEquals(Optional.of(TRADER), visiting.nextRecord().orElseThrow().traderId());
        assertThrows(IllegalStateException.class, () -> SummonLifecycle.beginVisit(visit, UUID.randomUUID()));
    }

    private static SummonRecord approach() {
        return SummonRecord.approach(PLAYER, MARKER, Level.OVERWORLD, BEACON);
    }

    private static SummonRecord visitReady() {
        return SummonLifecycle.beginVisit(approach().withApproachTicksRemaining(0), TRADER);
    }

    private static SummonRecord consumeApproachTicks(SummonRecord record, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            record = SummonLifecycle.advance(record, LifecycleObservation.activeApproach())
                    .nextRecord().orElseThrow();
        }
        return record;
    }

    private static SummonRecord consumeVisitTicks(SummonRecord record, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            record = SummonLifecycle.advance(record, LifecycleObservation.activeVisit())
                    .nextRecord().orElseThrow();
        }
        return record;
    }
}
