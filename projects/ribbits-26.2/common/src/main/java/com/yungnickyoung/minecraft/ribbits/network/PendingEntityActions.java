package com.yungnickyoung.minecraft.ribbits.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Retains entity-bound client actions until the target entity is available.
 */
final class PendingEntityActions<T> {
    private final Map<UUID, List<Consumer<T>>> pendingActions = new HashMap<>();

    void executeOrQueue(UUID entityId, T entity, Consumer<T> action) {
        if (entity == null) {
            this.pendingActions.computeIfAbsent(entityId, ignored -> new ArrayList<>()).add(action);
            return;
        }

        action.accept(entity);
    }

    void onEntityAvailable(UUID entityId, T entity) {
        List<Consumer<T>> actions = this.pendingActions.remove(entityId);
        if (actions == null) {
            return;
        }

        actions.forEach(action -> action.accept(entity));
    }

    void clear() {
        this.pendingActions.clear();
    }

    int pendingActionCount(UUID entityId) {
        List<Consumer<T>> actions = this.pendingActions.get(entityId);
        return actions == null ? 0 : actions.size();
    }
}
