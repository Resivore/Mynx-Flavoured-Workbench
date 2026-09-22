package dev.resivore.villagerwork;

import java.util.function.IntUnaryOperator;

/** Small server-side phase contract for the single Fisherman rod transaction. */
public final class FishingRodLifecycle {
    public static final int DWELL_BASE_TICKS = 180;
    public static final int DWELL_RANDOM_BOUND = 240;
    public static final int POST_RETRIEVE_COOLDOWN_TICKS = 100;
    public static final int PRODUCTION_GATE_BOUND = 4;

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

    /** Returns the exact 180..419 tick dwell using the supplied server-authoritative RNG. */
    public static int nextDwellTicks(IntUnaryOperator nextInt) {
        int offset = nextInt.applyAsInt(DWELL_RANDOM_BOUND);
        if (offset < 0 || offset >= DWELL_RANDOM_BOUND)
            throw new IllegalArgumentException("RNG result outside requested bound: " + offset);
        return DWELL_BASE_TICKS + offset;
    }

    /** Exactly one of the four possible bounded RNG outcomes proceeds to the loot table. */
    public static boolean productiveRetrieve(int gateRoll) {
        if (gateRoll < 0 || gateRoll >= PRODUCTION_GATE_BOUND)
            throw new IllegalArgumentException("production gate roll outside 0..3: " + gateRoll);
        return gateRoll == 0;
    }
}
