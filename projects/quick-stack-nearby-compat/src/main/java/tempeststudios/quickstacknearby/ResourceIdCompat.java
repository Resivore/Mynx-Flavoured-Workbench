package tempeststudios.quickstacknearby;

import net.minecraft.resources.Identifier;

final class ResourceIdCompat {
    private ResourceIdCompat() {
    }

    static Identifier quickStackId(String path) {
        return Identifier.fromNamespaceAndPath(QuickStackNearby.MOD_ID, path);
    }
}
