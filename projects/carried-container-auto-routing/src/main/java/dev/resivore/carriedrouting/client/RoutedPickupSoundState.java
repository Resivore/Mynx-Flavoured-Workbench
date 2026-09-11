package dev.resivore.carriedrouting.client;

import java.util.HashSet;
import java.util.Set;

/** Client-thread-only, event-scoped marker consumed by the matching vanilla take-item packet. */
public final class RoutedPickupSoundState {
    public static final float PITCH_MULTIPLIER = 0.80F;
    private static final Set<Integer> ROUTED_ENTITY_IDS = new HashSet<>();

    private RoutedPickupSoundState() {}

    public static void mark(int itemEntityId) { ROUTED_ENTITY_IDS.add(itemEntityId); }

    public static boolean consume(int itemEntityId) { return ROUTED_ENTITY_IDS.remove(itemEntityId); }
}
