package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivestockGateOwnerIndexTest {
    @Test void worldsAndDimensionsWithTheSamePositionRemainIndependent() {
        var index = index();
        var owner = owner(1, 11);

        index.activate("overworld", 42L, "oak/north", owner);

        assertTrue(index.containsOwner("overworld", 42L, owner));
        assertFalse(index.containsPosition("nether", 42L));
    }

    @Test void releasingOneOwnerDoesNotReleaseACompanionOwner() {
        var index = index();
        var first = owner(1, 11);
        var second = owner(1, 22);
        index.activate("overworld", 42L, "oak/north", first);
        index.activate("overworld", 42L, "oak/north", second);

        assertTrue(index.release("overworld", 42L, first));
        assertFalse(index.containsOwner("overworld", 42L, first));
        assertTrue(index.containsOwner("overworld", 42L, second));
        assertEquals(1, index.ownerCount("overworld", 42L));

        assertTrue(index.release("overworld", 42L, second));
        assertFalse(index.containsPosition("overworld", 42L));
    }

    @Test void changedGateIdentityFailsClosedUntilExplicitInvalidation() {
        var index = index();
        var owner = owner(1, 11);
        index.activate("overworld", 42L, "oak/north", owner);

        assertEquals(LivestockGateBlocker.OwnerIndex.Activation.IDENTITY_CONFLICT,
                index.activate("overworld", 42L, "spruce/east", owner));
        assertEquals(Set.of(owner), index.invalidate("overworld", 42L));
        assertFalse(index.containsPosition("overworld", 42L));
    }

    @Test void ownerTokenIsIdempotentAndCanBeReconstructedAfterTransientClear() {
        var original = owner(1, 11);
        var reconstructed = owner(1, 11);
        var index = index();

        assertEquals(LivestockGateBlocker.OwnerIndex.Activation.ADDED,
                index.activate("overworld", 42L, "oak/north", original));
        assertEquals(LivestockGateBlocker.OwnerIndex.Activation.ALREADY_PRESENT,
                index.activate("overworld", 42L, "oak/north", reconstructed));
        assertEquals(1, index.ownerCount("overworld", 42L));

        index.clearWorld("overworld");
        assertEquals(LivestockGateBlocker.OwnerIndex.Activation.ADDED,
                index.activate("overworld", 42L, "oak/north", reconstructed));
        assertTrue(index.containsOwner("overworld", 42L, original));
    }

    @Test void releaseOwnerAndWorldUnloadCleanupAreScoped() {
        var index = index();
        var first = owner(1, 11);
        var second = owner(2, 22);
        index.activate("overworld", 42L, "oak/north", first);
        index.activate("overworld", 43L, "oak/north", first);
        index.activate("overworld", 43L, "oak/north", second);
        index.activate("nether", 42L, "oak/north", first);

        for (long position : index.positionsOwnedBy("overworld", first))
            index.release("overworld", position, first);
        assertFalse(index.containsPosition("overworld", 42L));
        assertTrue(index.containsOwner("overworld", 43L, second));
        assertTrue(index.containsOwner("nether", 42L, first));

        Map<Long, Set<LivestockGateBlocker.Owner>> removed = index.clearWorld("overworld");
        assertEquals(Set.of(second), removed.get(43L));
        assertTrue(index.containsOwner("nether", 42L, first));
    }

    private static LivestockGateBlocker.OwnerIndex<String, Long, String, LivestockGateBlocker.Owner> index() {
        return new LivestockGateBlocker.OwnerIndex<>(new HashMap<>());
    }

    private static LivestockGateBlocker.Owner owner(long villager, long route) {
        return new LivestockGateBlocker.Owner(new UUID(0, villager), new UUID(0, route));
    }
}
