package dev.resivore.carryonpatch;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import net.minecraft.world.entity.Entity;

/** Allocates process-local IDs from a domain disjoint from normal server entity IDs. */
public final class RenderOnlyEntityIds {
    private static final AtomicInteger NEXT_RENDER_ID = new AtomicInteger(-1);

    private RenderOnlyEntityIds() {
    }

    /**
     * Preserves an assigned ID, or supplies one negative render-only ID for the unassigned
     * Minecraft 26.2 sentinel. The selected ID is stored on the synthetic entity by the caller.
     */
    public static int selectId(int currentId) {
        return selectId(currentId, ignored -> false);
    }

    public static int selectId(int currentId, IntPredicate idInUse) {
        return selectId(currentId, NEXT_RENDER_ID, idInUse);
    }

    static int selectId(int currentId, AtomicInteger sequence, IntPredicate idInUse) {
        if (currentId != Entity.INVALID_ENTITY_ID) {
            return currentId;
        }

        while (true) {
            int candidate = sequence.getAndUpdate(current ->
                    current < Entity.INVALID_ENTITY_ID && current != Integer.MIN_VALUE
                            ? current - 1
                            : Entity.INVALID_ENTITY_ID);
            if (candidate >= Entity.INVALID_ENTITY_ID) {
                throw new IllegalStateException("Carry On Patch exhausted render-only entity IDs");
            }
            if (!idInUse.test(candidate)) {
                return candidate;
            }
        }
    }
}
