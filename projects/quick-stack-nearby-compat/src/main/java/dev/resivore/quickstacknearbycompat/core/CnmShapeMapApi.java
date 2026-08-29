package dev.resivore.quickstacknearbycompat.core;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.Item;

/** Direct CNM API references, isolated so this class is loaded only after the optional-mod gate passes. */
final class CnmShapeMapApi {
    private CnmShapeMapApi() {}

    static boolean inSameShapeSet(Item first, Item second) {
        return ShapeMap.inSameShapeSet(first, second);
    }
}
