package dev.aero.cnmterraincompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

final class GrassHorizontalSlab extends SlabBlock {
    GrassHorizontalSlab(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GrassVerticalSlab.canGrassSurvive(state, level, pos)) {
            level.setBlockAndUpdate(pos, GrassFamilyBehavior.convertGrassGeometryToDirt(state));
            return;
        }
        GrassFamilyBehavior.spreadFrom(state, level, pos, random, true);
    }
}
