package dev.resivore.matchavillagerestoration;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;

public final class VillagePackPrecedence {
    private VillagePackPrecedence() {
    }

    /**
     * Moves only the C2 built-in pack to Minecraft's final, highest-priority pack position.
     * All other packs retain their exact relative order.
     */
    public static List<PackResources> prioritize(List<PackResources> packs) {
        int restorationIndex = -1;

        for (int index = 0; index < packs.size(); index++) {
            if (!MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID.equals(
                    packs.get(index).packId())) {
                continue;
            }
            if (restorationIndex >= 0) {
                throw new IllegalStateException(
                        "Duplicate built-in pack id: "
                                + MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID);
            }
            restorationIndex = index;
        }

        if (restorationIndex < 0 || restorationIndex == packs.size() - 1) {
            return packs;
        }

        ArrayList<PackResources> ordered = new ArrayList<>(packs);
        PackResources restoration = ordered.remove(restorationIndex);
        ordered.add(restoration);
        return List.copyOf(ordered);
    }
}
