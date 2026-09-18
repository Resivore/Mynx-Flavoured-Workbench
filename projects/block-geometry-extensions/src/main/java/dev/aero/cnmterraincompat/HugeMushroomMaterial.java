package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.world.level.block.Block;

/** Profile predicate kept provider-neutral for any future HugeMushroom-style source. */
final class HugeMushroomMaterial {
    private HugeMushroomMaterial() {}
    static boolean isHugeMushroom(Block source) {
        return NibaruProviderAdapter.profile(source)
                .map(HugeMushroomMaterial::isHugeMushroom).orElse(false);
    }
    static boolean isHugeMushroom(NibaruMaterialProfile profile) {
        return profile.visualProfile() == VisualProfile.HUGE_MUSHROOM;
    }
}
