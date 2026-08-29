package dev.resivore.matchabeacon.runtime;

import net.minecraft.world.entity.Entity;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class FunctionExecutionGuard {
    private static final ThreadLocal<UUID> PERMITTED_SUMMON_MARKER = new ThreadLocal<>();

    private FunctionExecutionGuard() {
    }

    public static boolean allows(String functionId, Entity sourceEntity) {
        UUID permitted = PERMITTED_SUMMON_MARKER.get();
        return permitted != null
                && MatchaContract.SUMMON_FUNCTION_ID.equals(functionId)
                && sourceEntity != null
                && permitted.equals(sourceEntity.getUUID());
    }

    public static <T> T withPermittedSummon(UUID markerId, Supplier<T> action) {
        Objects.requireNonNull(markerId, "markerId");
        Objects.requireNonNull(action, "action");
        if (PERMITTED_SUMMON_MARKER.get() != null) {
            throw new IllegalStateException("Nested Matcha summon permission is not supported");
        }
        PERMITTED_SUMMON_MARKER.set(markerId);
        try {
            return action.get();
        } finally {
            PERMITTED_SUMMON_MARKER.remove();
        }
    }
}
