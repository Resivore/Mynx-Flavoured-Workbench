package dev.resivore.villagerwork;

/** Small server-side phase contract for the single Fisherman rod transaction. */
public final class FishingRodLifecycle {
    private FishingRodLifecycle() {}

    public enum Phase {
        IDLE,
        NAVIGATING_TO_BANK,
        CAST_TELEGRAPH,
        FLOAT_ACTIVE,
        RETRIEVING,
        RETURNING_TO_BARREL,
        CANCELLED
    }

    /** The prop exists only from the completed bank approach through retrieval. */
    public static boolean retainsRod(Phase phase) {
        return phase == Phase.CAST_TELEGRAPH || phase == Phase.FLOAT_ACTIVE || phase == Phase.RETRIEVING;
    }

    public static Phase afterRetrieve(boolean ownsFish) {
        return ownsFish ? Phase.RETURNING_TO_BARREL : Phase.IDLE;
    }
}
