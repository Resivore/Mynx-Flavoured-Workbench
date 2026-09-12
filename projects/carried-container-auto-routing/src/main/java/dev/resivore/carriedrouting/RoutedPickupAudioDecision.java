package dev.resivore.carriedrouting;

/** Sound-only accounting for one ItemEntity.playerTouch invocation. */
public final class RoutedPickupAudioDecision {
    private RoutedPickupAudioDecision() {}

    public static FallbackCue fallbackCue(
            int customItemsMoved,
            boolean routedToCarriedContainer,
            boolean vanillaTakeReached
    ) {
        if (customItemsMoved <= 0 || vanillaTakeReached) return FallbackCue.NONE;
        return routedToCarriedContainer ? FallbackCue.LOWER_PITCH : FallbackCue.NORMAL_PITCH;
    }

    public enum FallbackCue { NONE, NORMAL_PITCH, LOWER_PITCH }
}
