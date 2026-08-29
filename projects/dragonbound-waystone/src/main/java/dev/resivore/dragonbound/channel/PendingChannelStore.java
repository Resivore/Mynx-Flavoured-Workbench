package dev.resivore.dragonbound.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class PendingChannelStore {
    private final Map<UUID, PendingChannel> pending = new HashMap<>();

    boolean add(PendingChannel channel) {
        return pending.putIfAbsent(channel.playerId(), channel) == null;
    }

    Optional<PendingChannel> get(UUID playerId) {
        return Optional.ofNullable(pending.get(playerId));
    }

    boolean isCurrent(UUID playerId, long token) {
        PendingChannel channel = pending.get(playerId);
        return channel != null && channel.token() == token;
    }

    Optional<PendingChannel> removeExact(UUID playerId, long token) {
        PendingChannel channel = pending.get(playerId);
        if (channel == null || channel.token() != token || !pending.remove(playerId, channel)) {
            return Optional.empty();
        }
        return Optional.of(channel);
    }

    List<PendingChannel> snapshot() {
        return new ArrayList<>(pending.values());
    }

    int size() {
        return pending.size();
    }
}
