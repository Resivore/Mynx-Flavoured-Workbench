package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateRouteRulesTest {
    private static final BlockPos SHEEP_INTERACTION = new BlockPos(8, 64, 0);

    private static GateRouteRules.Candidate route(int gateX, double approachCost, double sheepCost,
                                                   boolean nearReachable, boolean reachesSelectedSheep) {
        BlockPos gate = new BlockPos(gateX, 64, 0);
        return new GateRouteRules.Candidate(gate, gate.north(), gate.south(), SHEEP_INTERACTION,
                approachCost, sheepCost, nearReachable, reachesSelectedSheep);
    }

    @Test void directRouteNeverFallsBackToGate() {
        assertTrue(GateRouteRules.select(true, List.of(route(2, 1, 1, true, true))).isEmpty());
    }

    @Test void blockedDirectRouteSelectsGateConnectingToKnownSheep() {
        var relevant = route(4, 5, 6, true, true);
        assertEquals(relevant, GateRouteRules.select(false, List.of(relevant)).orElseThrow());
    }

    @Test void nearerUnrelatedGateDoesNotDisplaceSheepRoute() {
        var unrelated = route(1, 1, 1, true, false);
        var relevant = route(5, 5, 4, true, true);
        assertEquals(relevant, GateRouteRules.select(false, List.of(unrelated, relevant)).orElseThrow());
    }

    @Test void multipleValidGatesUsePathCostThenStablePositionTieBreak() {
        var expensive = route(1, 10, 10, true, true);
        var higherCoordinate = route(7, 4, 5, true, true);
        var lowerCoordinate = route(3, 5, 4, true, true);
        assertEquals(lowerCoordinate, GateRouteRules.select(false,
                List.of(expensive, higherCoordinate, lowerCoordinate)).orElseThrow());
        assertEquals(lowerCoordinate, GateRouteRules.select(false,
                List.of(lowerCoordinate, expensive, higherCoordinate)).orElseThrow());
    }

    @Test void noGateOrNoReachableNearSideRemainsUnreachable() {
        assertTrue(GateRouteRules.select(false, List.of()).isEmpty());
        assertTrue(GateRouteRules.select(false, List.of(route(4, 2, 2, false, true))).isEmpty());
        assertTrue(GateRouteRules.select(false, List.of(route(4, 2, 2, true, false))).isEmpty());
    }

    @Test void invalidOrUnboundedPathCostsCannotWin() {
        assertTrue(GateRouteRules.select(false, List.of(route(4, Double.NaN, 0, true, true))).isEmpty());
        assertTrue(GateRouteRules.select(false, List.of(route(4, Double.POSITIVE_INFINITY, 0, true, true))).isEmpty());
        assertTrue(GateRouteRules.select(false, List.of(route(4, -1, 0, true, true))).isEmpty());
    }

    @Test void gateOwnershipAndSafeRestorationRespectOriginalAndExternalState() {
        assertFalse(GateRouteRules.ownsOpenTransition(true, true));
        assertFalse(GateRouteRules.ownsOpenTransition(false, false));
        boolean owned = GateRouteRules.ownsOpenTransition(false, true);
        assertTrue(owned);
        assertTrue(GateRouteRules.mayClose(owned, true, true, false, false, true, true));
        assertFalse(GateRouteRules.mayClose(false, true, true, false, false, true, true));
        assertFalse(GateRouteRules.mayClose(owned, true, true, true, false, true, true));
        assertFalse(GateRouteRules.mayClose(owned, true, true, false, true, true, true));
        assertFalse(GateRouteRules.mayClose(owned, true, true, false, false, false, true));
        assertFalse(GateRouteRules.mayClose(owned, true, true, false, false, true, false));
        assertFalse(GateRouteRules.mayClose(owned, false, true, false, false, true, true));
    }

    @Test void entryAndExitRequireMilestonesAndCancellationStopsProgress() {
        var stage = GateRouteRules.Stage.APPROACH_ENTRY;
        assertEquals(stage, GateRouteRules.advance(stage, false));
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.OPEN_ENTRY, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.CROSS_ENTRY, stage);
        assertEquals(stage, GateRouteRules.advance(stage, false)); // gate opened, but no physical crossing
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.CLOSE_ENTRY, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.APPROACH_SHEEP, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.SHEAR, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.APPROACH_EXIT, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.OPEN_EXIT, stage);
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.CROSS_EXIT, stage);
        assertEquals(stage, GateRouteRules.advance(stage, false));
        stage = GateRouteRules.advance(stage, true);
        assertEquals(GateRouteRules.Stage.CLOSE_EXIT, stage);
        assertEquals(GateRouteRules.Stage.RETURN_TO_LOOM, GateRouteRules.advance(stage, true));
        assertEquals(GateRouteRules.Stage.CANCELLED, GateRouteRules.cancel(stage));
        assertEquals(GateRouteRules.Stage.CANCELLED,
                GateRouteRules.advance(GateRouteRules.Stage.CANCELLED, true));
    }
}
