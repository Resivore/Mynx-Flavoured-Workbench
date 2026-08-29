package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.honey.HoneySemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
final class HoneyStepBlock extends ProviderStepBlock {
    HoneyStepBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return HoneyDerivedGeometry.step(state);
    }

    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
        HoneySemantics.fallOn(level, entity, distance, this.soundType);
    }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean intersects) {
        HoneySemantics.applySlideIfEligible(state, level, pos, entity, HoneyDerivedGeometry.step(state));
        super.entityInside(state, level, pos, entity, effects, intersects);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return NibaruProviderAdapter.cullsBoundTranslucent(this, state, neighbor)
                || super.skipRendering(state, neighbor, direction);
    }
}
