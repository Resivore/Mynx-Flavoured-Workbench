package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Spreadable lifecycle that respects a geometry-owned top-surface exposure decision. */
public final class GeometrySpreadableBehavior {
    private GeometrySpreadableBehavior() {}

    public static void randomTick(BlockState state, ServerLevel level,
            BlockPos pos, RandomSource random) {
        if (!SpreadableSemantics.canSurvive(state, level, pos)) {
            level.setBlockAndUpdate(pos, SpreadableSemantics.toBase(state));
            return;
        }
        spreadFrom(state, level, pos, random, true);
    }

    public static void spreadFrom(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random, boolean includeVanillaBase) {
        if (!SpreadableSemantics.isSpreadableSource(state)
                || level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        Block material = SpreadableSemantics.materialFor(state);
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (includeVanillaBase || !level.getBlockState(target).is(Blocks.DIRT)) {
                trySpread(level, material, target);
            }
        }
    }

    public static boolean trySpread(ServerLevel level, Block material, BlockPos targetPos) {
        BlockState oldState = level.getBlockState(targetPos);
        BlockState newState = SpreadableSemantics.toSpreadable(oldState, material);
        if (newState == oldState || !SpreadableSemantics.canSurvive(newState, level, targetPos)) {
            return false;
        }

        // Exact geometry-aware canSurvive already inspected the relevant top footprint. Keep
        // Nibaru's coarse water-above guard for accepted legacy geometries whose exposure result
        // does not promise that stronger contract (notably Step and Layer).
        if (!(newState.getBlock() instanceof GeometryAwareSpreadable)
                && level.getFluidState(targetPos.above()).is(FluidTags.WATER)) {
            return false;
        }
        level.setBlockAndUpdate(targetPos, withSnowy(newState,
                level.getBlockState(targetPos.above()).is(BlockTags.SNOW)));
        return true;
    }

    private static BlockState withSnowy(BlockState state, boolean snowy) {
        return state.hasProperty(BlockStateProperties.SNOWY)
                ? state.setValue(BlockStateProperties.SNOWY, snowy) : state;
    }
}
