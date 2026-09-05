package dev.resivore.mynxregions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Reference bush outline plus native FlowerBlock suspicious-stew semantics. */
final class RuFlowerBlock extends FlowerBlock {
    private static final VoxelShape BUSH_SHAPE = Block.column(12.0, 0.0, 13.0);
    RuFlowerBlock(Holder<MobEffect> effect, float seconds, BlockBehaviour.Properties properties) { super(effect, seconds, properties); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BUSH_SHAPE.move(state.getOffset(pos));
    }
}
