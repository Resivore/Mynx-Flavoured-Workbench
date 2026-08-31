package games.twinhead.moreslabsstairsandwalls.block.slime;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Typed Honey/Slime classification for native Nibaru and vanilla piston behavior. */
public final class StickyMaterialSemantics {
    public enum Family { NONE, HONEY, SLIME }

    private StickyMaterialSemantics() {}

    public static Family family(BlockState state) {
        if (state.is(Blocks.HONEY_BLOCK)) return Family.HONEY;
        if (state.is(Blocks.SLIME_BLOCK)) return Family.SLIME;
        return NibaruMaterialProfiles.fromBlock(state.getBlock()).map(profile -> {
            if (profile.family() == ModBlocks.HONEY_BLOCK) return Family.HONEY;
            if (profile.family() == ModBlocks.SLIME_BLOCK) return Family.SLIME;
            return Family.NONE;
        }).orElse(Family.NONE);
    }

    public static boolean isSticky(BlockState state) {
        return family(state) != Family.NONE;
    }

    public static boolean isOpposedPair(BlockState first, BlockState second) {
        Family a = family(first);
        Family b = family(second);
        return a != Family.NONE && b != Family.NONE && a != b;
    }
}
