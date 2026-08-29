package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;

import java.util.Objects;
import java.util.function.Consumer;

public final class MatchaClientData {
    private static MatchaJeiDataPayload current = MatchaJeiDataPayload.EMPTY;
    private static Consumer<MatchaJeiDataPayload> listener = payload -> {
    };

    private MatchaClientData() {
    }

    public static MatchaJeiDataPayload current() {
        return current;
    }

    public static void publish(MatchaJeiDataPayload payload) {
        current = Objects.requireNonNull(payload, "payload");
        listener.accept(current);
    }

    public static void setListener(Consumer<MatchaJeiDataPayload> newListener) {
        listener = newListener == null ? payload -> {
        } : newListener;
    }
}
