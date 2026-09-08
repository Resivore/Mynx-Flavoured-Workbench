package dev.resivore.naturalistxaeroicons;

import net.minecraft.resources.Identifier;
import xaero.common.icon.XaeroIcon;

/**
 * A Canary replacement can leave Xaero's entity/variant cache populated with a raster made
 * before this companion's native-pose revision. Xaero's key has no presentation revision, so
 * retry that one stale Brown Bear entry only at a call where it can be rebuilt.
 */
public final class BrownBearIconCacheFreshness {
    private static boolean retryPending = true;

    private BrownBearIconCacheFreshness() {}

    public static boolean retryCachedNativeBear(Identifier id, XaeroIcon cached, boolean canPrerender) {
        if (!retryPending || !canPrerender || cached == null || id == null) return false;
        if (!id.getNamespace().equals("naturalist") || !id.getPath().equals("bear")) return false;
        retryPending = false;
        return true;
    }

    /** A resource reload already removes the whole owned entry, so it needs no stale-cache retry. */
    public static void resourceReloadEvicted() {
        retryPending = false;
    }

    static void resetForTest() {
        retryPending = true;
    }
}
