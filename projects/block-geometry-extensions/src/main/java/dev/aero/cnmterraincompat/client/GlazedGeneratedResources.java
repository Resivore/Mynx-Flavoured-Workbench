package dev.aero.cnmterraincompat.client;

import dev.aero.cnmterraincompat.GlazedModelContract;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Writes only the extra ordinary JSON states/models required by GLAZED_ORIENTED. */
public final class GlazedGeneratedResources {
    private GlazedGeneratedResources() {}

    public static void extendVertical(Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = glazed(parent);
        if (profile == null) return;
        AssetGenerator.write("models/block/%s.json".formatted(shape.getPath()),
                GlazedModelContract.verticalModel(profile, Direction.NORTH));
        for (Direction relative : GlazedModelContract.horizontalDirections()) {
            AssetGenerator.write("models/block/%s%s.json".formatted(shape.getPath(),
                    GlazedModelContract.relativeSuffix(relative, SlabType.BOTTOM)),
                    GlazedModelContract.verticalModel(profile, relative));
        }
        AssetGenerator.write("models/block/%s_glazed_double.json".formatted(shape.getPath()),
                GlazedModelContract.verticalDoubleModel(profile));
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                GlazedModelContract.verticalBlockState(shape));
    }

    public static void extendStep(Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = glazed(parent);
        if (profile == null) return;
        AssetGenerator.write("models/block/%s.json".formatted(shape.getPath()),
                GlazedModelContract.stepModel(profile, Direction.NORTH, SlabType.BOTTOM));
        for (Direction relative : GlazedModelContract.horizontalDirections()) {
            for (SlabType type : SlabType.values()) {
                AssetGenerator.write("models/block/%s%s.json".formatted(shape.getPath(),
                        GlazedModelContract.relativeSuffix(relative, type)),
                        GlazedModelContract.stepModel(profile, relative, type));
            }
        }
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                GlazedModelContract.stepBlockState(shape));
    }

    private static NibaruMaterialProfile glazed(Identifier parent) {
        return NibaruMaterialProfiles.fromId(parent)
                .filter(profile -> profile.visualProfile() == VisualProfile.GLAZED_ORIENTED)
                .orElse(null);
    }
}
