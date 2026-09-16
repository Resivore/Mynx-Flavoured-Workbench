package dev.resivore.slabdecorations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Finds shifted plant portions that vanilla's logical-cell traversal cannot visit. */
public final class SlabPlantRaycast {
    private SlabPlantRaycast() {
    }

    public static HitResult preferShiftedPlant(
            BlockGetter level,
            Vec3 from,
            Vec3 to,
            HitResult vanillaResult) {
        BlockHitResult shifted = BlockGetter.traverseBlocks(from, to, level, (blockView, traversedPos) -> {
            BlockHitResult belowCell = shiftedHit(blockView, traversedPos.above(),
                    NibaruHorizontalSurface.BOTTOM_OFFSET, from, to);
            BlockHitResult aboveCell = shiftedHit(blockView, traversedPos.below(),
                    NibaruHorizontalSurface.CEILING_TOP_OFFSET, from, to);
            return nearer(from, belowCell, aboveCell);
        }, ignored -> null);

        if (shifted == null) return vanillaResult;
        if (vanillaResult == null || vanillaResult.getType() == HitResult.Type.MISS) return shifted;

        double shiftedDistance = from.distanceToSqr(shifted.getLocation());
        double vanillaDistance = from.distanceToSqr(vanillaResult.getLocation());
        return shiftedDistance <= vanillaDistance ? shifted : vanillaResult;
    }

    private static BlockHitResult shiftedHit(
            BlockGetter level,
            BlockPos plantPos,
            double expectedOffset,
            Vec3 from,
            Vec3 to) {
        BlockState plantState = level.getBlockState(plantPos);
        if (NibaruHorizontalSurface.visibleOffset(plantState, level, plantPos) != expectedOffset) {
            return null;
        }
        return plantState.getShape(level, plantPos).clip(from, to, plantPos);
    }

    private static BlockHitResult nearer(Vec3 from, BlockHitResult first, BlockHitResult second) {
        if (first == null) return second;
        if (second == null) return first;
        return from.distanceToSqr(first.getLocation()) <= from.distanceToSqr(second.getLocation())
                ? first
                : second;
    }
}
