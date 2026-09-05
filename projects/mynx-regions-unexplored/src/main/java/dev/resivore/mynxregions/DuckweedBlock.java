package dev.resivore.mynxregions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Surface foliage with water/ice support and explicitly no Lily Pad boat-destruction callback. */
public final class DuckweedBlock extends LilyPadBlock {
    public DuckweedBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                           InsideBlockEffectApplier effects, boolean intersects) { }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(this.asItem());
    }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return (level.getFluidState(pos).is(Fluids.WATER) || state.getBlock() instanceof IceBlock)
                && level.getFluidState(pos.above()).isEmpty();
    }
}
