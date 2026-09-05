package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Historical RU Stone Bud: stone-supported, shears-harvested, and deliberately non-growing. */
public final class StoneBudBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<StoneBudBlock> CODEC = simpleCodec(StoneBudBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 5.0);

    public StoneBudBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends StoneBudBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return state.is(MynxRegionsUnexplored.STONE_BUD_SUPPORTS); }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return false; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return false; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { }
}
