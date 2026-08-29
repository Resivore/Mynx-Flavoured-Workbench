package dev.aero.cnmterraincompat.client;

import dev.aero.cnmterraincompat.InsetModelContract;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Narrow extension of CNM's normal resource pass for provider-declared Honey/Slime inset visuals. */
public final class InsetGeneratedResources {
    private InsetGeneratedResources() {}

    public static void extendVertical(Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = inset(parent);
        if (profile == null) return;
        AssetGenerator.write("models/block/%s.json".formatted(shape.getPath()),
                InsetModelContract.verticalModel(profile, Direction.NORTH, false));
        for (Direction facing : InsetModelContract.horizontalDirections()) {
            AssetGenerator.write("models/block/%s%s.json".formatted(shape.getPath(),
                    InsetModelContract.suffix(facing, SlabType.BOTTOM)),
                    InsetModelContract.verticalModel(profile, facing, false));
        }
        AssetGenerator.write("models/block/%s_inset_double.json".formatted(shape.getPath()),
                InsetModelContract.verticalModel(profile, Direction.NORTH, true));
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                InsetModelContract.verticalBlockState(shape));
    }

    public static void extendStep(Identifier parent, Identifier shape) {
        NibaruMaterialProfile profile = inset(parent);
        if (profile == null) return;
        AssetGenerator.write("models/block/%s.json".formatted(shape.getPath()),
                InsetModelContract.stepModel(profile, Direction.NORTH, SlabType.BOTTOM));
        for (Direction facing : InsetModelContract.horizontalDirections()) for (SlabType type : SlabType.values()) {
            AssetGenerator.write("models/block/%s%s.json".formatted(shape.getPath(),
                    InsetModelContract.suffix(facing, type)),
                    InsetModelContract.stepModel(profile, facing, type));
        }
        AssetGenerator.write("blockstates/%s.json".formatted(shape.getPath()),
                InsetModelContract.stepBlockState(shape));
    }

    private static NibaruMaterialProfile inset(Identifier parent) {
        return NibaruMaterialProfiles.fromId(parent).filter(profile ->
                profile.visualProfile() == VisualProfile.HONEY_INSET
                        || profile.visualProfile() == VisualProfile.SLIME_INSET).orElse(null);
    }
}
