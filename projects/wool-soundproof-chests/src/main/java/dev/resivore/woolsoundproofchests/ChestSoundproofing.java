package dev.resivore.woolsoundproofchests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ChestSoundproofing {
    private ChestSoundproofing() {
    }

    public static boolean shouldSuppressContainerEvent(Level level, BlockPos chestPos) {
        if (!level.getBlockState(chestPos).is(Blocks.CHEST)) {
            return false;
        }

        return SoundproofingEvaluator.qualifies(face -> {
            Direction direction = switch (face) {
                case NORTH -> Direction.NORTH;
                case SOUTH -> Direction.SOUTH;
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                case DOWN -> Direction.DOWN;
            };
            BlockPos adjacentPos = chestPos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            boolean fullBlock = adjacentState.isCollisionShapeFullBlock(level, adjacentPos);
            return new SoundproofingEvaluator.Seal(fullBlock, fullBlock && adjacentState.is(BlockTags.WOOL));
        });
    }
}
