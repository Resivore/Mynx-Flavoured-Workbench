package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class MatchaClientData {
    private static MatchaJeiDataPayload current = MatchaJeiDataPayload.EMPTY;
    private static final Set<Consumer<MatchaJeiDataPayload>> listeners = new LinkedHashSet<>();

    private MatchaClientData() {
    }

    public static MatchaJeiDataPayload current() {
        return current;
    }

    public static void publish(MatchaJeiDataPayload payload) {
        current = Objects.requireNonNull(payload, "payload");
        listeners.forEach(listener -> listener.accept(current));
    }

    public static void addListener(Consumer<MatchaJeiDataPayload> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeListener(Consumer<MatchaJeiDataPayload> listener) {
        listeners.remove(listener);
    }
}
