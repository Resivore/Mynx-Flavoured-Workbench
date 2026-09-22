package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrelInteractionRulesTest {
    private static final BlockPos BARREL = new BlockPos(0, 0, 0);

    @Test void exactHalfBlockBoundaryGapIsAllowedButAnyLargerGapIsNot() {
        assertEquals(0.5D, BarrelInteractionRules.MAX_BOUNDARY_GAP);
        assertEquals(0.25D, BarrelInteractionRules.MAX_BOUNDARY_GAP_SQUARED);
        assertTrue(BarrelInteractionRules.withinReach(
                new AABB(1.5, 0.0, 0.2, 2.1, 1.8, 0.8), BARREL));
        assertFalse(BarrelInteractionRules.withinReach(
                new AABB(1.500001, 0.0, 0.2, 2.100001, 1.8, 0.8), BARREL));
    }

    @Test void adjacentBodyUsesBlockBoundaryRatherThanImpossibleCenterDistance() {
        AABB centeredInAdjacentBlock = new AABB(1.2, 0.0, 0.2, 1.8, 1.8, 0.8);
        assertTrue(BarrelInteractionRules.withinReach(centeredInAdjacentBlock, BARREL));
        assertTrue(centeredInAdjacentBlock.getCenter().distanceTo(Vec3.atCenterOf(BARREL)) > 0.5D);
    }

    @Test void diagonalAndVerticalSeparationUseEuclideanAabbGap() {
        assertTrue(BarrelInteractionRules.withinReach(
                new AABB(1.3, 1.4, 0.2, 1.9, 3.2, 0.8), BARREL));
        assertFalse(BarrelInteractionRules.withinReach(
                new AABB(1.4, 1.4, 0.2, 2.0, 3.2, 0.8), BARREL));
    }

    @Test void predictedStandingBodyIsCenteredAtCandidateWithoutTeleportingLiveState() {
        AABB current = new AABB(8.2, 4.0, 9.2, 8.8, 5.8, 9.8);
        AABB predicted = BarrelInteractionRules.bodyAtFeet(current,
                new Vec3(8.5, 4.0, 9.5), new BlockPos(1, 0, 0));
        assertTrue(BarrelInteractionRules.withinReach(predicted, BARREL));
        assertFalse(BarrelInteractionRules.withinReach(current, BARREL));
    }

    @Test void facingUsesTheVisibleHorizontalLookDirection() {
        Vec3 eye = new Vec3(2.0, 1.6, 0.5);
        assertTrue(BarrelInteractionRules.facing(new Vec3(-1.0, 0.0, 0.0), eye, BARREL));
        assertFalse(BarrelInteractionRules.facing(new Vec3(1.0, 0.0, 0.0), eye, BARREL));
        assertFalse(BarrelInteractionRules.facing(Vec3.ZERO, eye, BARREL));
    }

    @Test void unreachableOutputStaysOwnedAndReceiverChangesRequireANewArrivalTick() {
        BarrelArrivalGate gate = new BarrelArrivalGate();
        BlockPos first = BARREL;
        BlockPos second = BARREL.east();
        int ownedCount = 7;

        if (gate.ready(first, 10, false, false)) ownedCount = 0;
        if (gate.ready(first, 11, false, true)) ownedCount = 0;
        assertEquals(7, ownedCount, "an unreachable receiver never authorizes mutation");

        assertFalse(gate.ready(first, 12, true, true), "arrival and transfer cannot share a tick");
        assertFalse(gate.ready(first, 13, true, false), "the villager must visibly face the barrel");
        assertTrue(gate.ready(first, 14, true, true));

        assertFalse(gate.ready(second, 15, true, true),
                "a different actual receiver requires its own later facing tick");
        assertTrue(gate.ready(second, 16, true, true));
        gate.clear();
        assertFalse(gate.ready(second, 17, true, true));
    }
}
