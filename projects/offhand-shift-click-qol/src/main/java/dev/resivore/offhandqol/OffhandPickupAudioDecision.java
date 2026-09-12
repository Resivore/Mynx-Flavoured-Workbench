package dev.resivore.offhandqol;

/** Sound-only accounting for one standalone ItemEntity.playerTouch invocation. */
public final class OffhandPickupAudioDecision {
    private OffhandPickupAudioDecision() {}

    public static boolean shouldSendFallback(
            int customItemsMoved,
            boolean vanillaTakeReached,
            boolean carriedRoutingPresent
    ) {
        return customItemsMoved > 0 && !vanillaTakeReached && !carriedRoutingPresent;
    }
}
