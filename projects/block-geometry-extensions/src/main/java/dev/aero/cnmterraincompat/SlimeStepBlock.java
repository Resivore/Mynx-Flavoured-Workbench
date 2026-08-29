package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.slime.SlimeSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
final class SlimeStepBlock extends ProviderStepBlock {
    SlimeStepBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
        if (!level.isClientSide()) {
            if (SlimeSemantics.handlesFall(entity)) SlimeSemantics.suppressFallDamage(level, entity, distance);
            else super.fallOn(level, state, pos, entity, distance);
        }
    }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        SlimeSemantics.modifyHorizontalMovement(entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return NibaruProviderAdapter.cullsBoundTranslucent(this, state, neighbor)
                || super.skipRendering(state, neighbor, direction);
    }
}
