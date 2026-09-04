package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

class RenderOnlyEntityIdsTest {
    @Test
    void assignedIdsArePreservedWithoutConsumingTheRenderSequence() {
        AtomicInteger sequence = new AtomicInteger(-41);
        AtomicBoolean collisionProbeCalled = new AtomicBoolean();

        assertEquals(73, RenderOnlyEntityIds.selectId(73, sequence, ignored -> {
            collisionProbeCalled.set(true);
            return true;
        }));
        assertEquals(-41, sequence.get());
        assertEquals(false, collisionProbeCalled.get());
    }

    @Test
    void selectedIdIsNegativeNonSentinelAndStableWhenSelectedAgain() {
        AtomicInteger sequence = new AtomicInteger(-1);

        int selected = RenderOnlyEntityIds.selectId(
                Entity.INVALID_ENTITY_ID, sequence, ignored -> false);
        assertTrue(selected < Entity.INVALID_ENTITY_ID);
        assertNotEquals(Entity.INVALID_ENTITY_ID, selected);
        assertEquals(selected,
                RenderOnlyEntityIds.selectId(selected, sequence, ignored -> false));
        assertEquals(-2, sequence.get(), "reselecting an assigned ID must not consume another value");
    }

    @Test
    void simultaneousSyntheticEntitiesReceiveUniqueNegativeIds() {
        AtomicInteger sequence = new AtomicInteger(-1);
        Set<Integer> selected = new HashSet<>();

        for (int index = 0; index < 10_000; index++) {
            int id = RenderOnlyEntityIds.selectId(
                    Entity.INVALID_ENTITY_ID, sequence, ignored -> false);
            assertTrue(id < Entity.INVALID_ENTITY_ID);
            assertTrue(selected.add(id), () -> "duplicate render-only ID " + id);
        }
    }

    @Test
    void idsAlreadyTrackedByTheClientLevelAreSkipped() {
        AtomicInteger sequence = new AtomicInteger(-1);

        int selected = RenderOnlyEntityIds.selectId(
                Entity.INVALID_ENTITY_ID, sequence, id -> id == -1 || id == -2);

        assertEquals(-3, selected);
        assertEquals(-4, sequence.get());
    }

    @Test
    void exhaustionUsesIntegerMinimumOnceThenFailsClosedWithoutWrapping() {
        AtomicInteger sequence = new AtomicInteger(Integer.MIN_VALUE);

        assertEquals(Integer.MIN_VALUE,
                RenderOnlyEntityIds.selectId(
                        Entity.INVALID_ENTITY_ID, sequence, ignored -> false));
        assertEquals(Entity.INVALID_ENTITY_ID, sequence.get());
        assertThrows(IllegalStateException.class,
                () -> RenderOnlyEntityIds.selectId(
                        Entity.INVALID_ENTITY_ID, sequence, ignored -> false));
        assertEquals(Entity.INVALID_ENTITY_ID, sequence.get());
    }


    @Test
    void aCollisionAtIntegerMinimumFailsClosedInTheSameSelection() {
        AtomicInteger sequence = new AtomicInteger(Integer.MIN_VALUE);

        assertThrows(IllegalStateException.class,
                () -> RenderOnlyEntityIds.selectId(
                        Entity.INVALID_ENTITY_ID, sequence, ignored -> true));
        assertEquals(Entity.INVALID_ENTITY_ID, sequence.get());
    }
}
