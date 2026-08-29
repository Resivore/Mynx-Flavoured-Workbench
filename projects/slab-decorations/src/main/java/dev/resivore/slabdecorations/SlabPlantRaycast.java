package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Finds only the negative-Y portion that vanilla's cell traversal cannot visit. */
public final class SlabPlantRaycast {
    private SlabPlantRaycast() {
    }

    public static HitResult preferShiftedPlant(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            HitResult vanillaResult) {
        BlockHitResult shifted = BlockGetter.traverseBlocks(from, to, level, (blockView, traversedPos) -> {
            BlockPos plantPos = traversedPos.above();
            BlockState plantState = blockView.getBlockState(plantPos);
            if (NibaruHorizontalSurface.visibleOffset(plantState, blockView, plantPos)
                    != NibaruHorizontalSurface.BOTTOM_OFFSET) {
                return null;
            }
            return plantState.getShape(blockView, plantPos).clip(from, to, plantPos);
        }, ignored -> null);

        if (shifted == null) return vanillaResult;
        if (vanillaResult == null || vanillaResult.getType() == HitResult.Type.MISS) return shifted;

        double shiftedDistance = from.distanceToSqr(shifted.getLocation());
        double vanillaDistance = from.distanceToSqr(vanillaResult.getLocation());
        return shiftedDistance <= vanillaDistance ? shifted : vanillaResult;
    }
}
