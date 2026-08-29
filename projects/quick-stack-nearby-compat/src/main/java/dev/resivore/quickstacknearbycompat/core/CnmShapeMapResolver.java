package dev.resivore.quickstacknearbycompat.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;

/** Optional bridge to the exact shape-family equivalence owned by Clutter No More. */
public final class CnmShapeMapResolver {
    public static final String CNM_MOD_ID = "clutternomore";

    private CnmShapeMapResolver() {}

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(CNM_MOD_ID);
    }

    public static boolean inSameShapeSet(Item first, Item second) {
        if (!isAvailable()) {
            return false;
        }

        try {
            return CnmShapeMapApi.inSameShapeSet(first, second);
        } catch (LinkageError unavailableShapeMapApi) {
            throw new IllegalStateException(
                    "Loaded Clutter No More does not expose the audited ShapeMap API",
                    unavailableShapeMapApi
            );
        }
    }
}
