package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class GrassStepBlock extends StepBlock implements SpreadableGeometry {
    public GrassStepBlock(BlockBehaviour.Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return NibaruProviderAdapter.useComposedCapabilities(this, stack, state, level, pos, player, hand)
                .orElseGet(() -> super.useItemOn(stack, state, level, pos, player, hand, hit));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SpreadableSemantics.randomTick(state, level, pos, random);
    }

    @Override
    public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(SLAB_TYPE) != SlabType.BOTTOM) return Exposure.DEFAULT;
        return state.getValue(WATERLOGGED) ? Exposure.BLOCKED : Exposure.EXPOSED;
    }
}
