package games.twinhead.moreslabsstairsandwalls.block.spreadable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.world.level.block.state.BlockState;

public final class PodzolGeometryConversion {
    private PodzolGeometryConversion() {
    }

    public static BlockState convert(BlockState source) {
        var derived = DerivedMaterialTraits.fromBlock(source.getBlock());
        if (derived.isPresent()) {
            var sourceProfile = NibaruMaterialProfiles.fromBlock(derived.get().canonicalParent()).orElse(null);
            if (sourceProfile == null) return null;
            var target = sourceProfile.transition(MaterialTransition.Type.PODZOL_GROWTH).orElse(null);
            if (target == null) return null;
            var targetProfile = NibaruMaterialProfiles.fromFamily(target.target()).orElseThrow();
            return DerivedMaterialTraits.equivalent(targetProfile.canonicalParent(), derived.get().geometry())
                    .map(block -> block.withPropertiesOf(source)).orElse(null);
        }
        ModBlocks[] eligible = {
                ModBlocks.GRASS_BLOCK, ModBlocks.PODZOL, ModBlocks.DIRT,
                ModBlocks.COARSE_DIRT, ModBlocks.MYCELIUM, ModBlocks.ROOTED_DIRT
        };
        for (ModBlocks.BlockType type : ModBlocks.BlockType.values()) {
            for (ModBlocks family : eligible) {
                if (source.is(family.getBlock(type))) {
                    return ModBlocks.PODZOL.getBlock(type).withPropertiesOf(source);
                }
            }
        }
        return null;
    }
}
