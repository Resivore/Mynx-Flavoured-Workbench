package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;


final class GrassVerticalSlab extends VerticalSlabBlock implements GeometryAwareSpreadable {
    GrassVerticalSlab(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return NibaruProviderAdapter.useComposedCapabilities(this, stack, state, level, pos, player, hand)
                .orElseGet(() -> super.useItemOn(stack, state, level, pos, player, hand, hit));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GeometrySpreadableBehavior.randomTick(state, level, pos, random);
    }

    static boolean canGrassSurvive(BlockState grassState, LevelReader level, BlockPos pos) {
        return SpreadableSemantics.canSurvive(grassState, level, pos);
    }

    @Override
    public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
        int footprint = state.getValue(DOUBLE)
                ? GeometrySurfaceExposure.FULL
                : switch (state.getValue(FACING)) {
                    case NORTH -> GeometrySurfaceExposure.NORTH_WEST
                            | GeometrySurfaceExposure.NORTH_EAST;
                    case EAST -> GeometrySurfaceExposure.NORTH_EAST
                            | GeometrySurfaceExposure.SOUTH_EAST;
                    case SOUTH -> GeometrySurfaceExposure.SOUTH_WEST
                            | GeometrySurfaceExposure.SOUTH_EAST;
                    case WEST -> GeometrySurfaceExposure.NORTH_WEST
                            | GeometrySurfaceExposure.SOUTH_WEST;
                    default -> throw new IllegalStateException("Vertical Slab facing must be horizontal");
                };
        return GeometrySurfaceExposure.topExposure(level, pos, footprint);
    }
}
