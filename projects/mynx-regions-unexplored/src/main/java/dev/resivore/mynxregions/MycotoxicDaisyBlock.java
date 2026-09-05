package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The name is visual only: no collision effect, poison, or invented propagation. */
public final class MycotoxicDaisyBlock extends DoublePlantBlock {
    public static final MapCodec<MycotoxicDaisyBlock> CODEC = simpleCodec(MycotoxicDaisyBlock::new);
    public MycotoxicDaisyBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<MycotoxicDaisyBlock> codec() { return CODEC; }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.NYLIUM) || state.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(state, level, pos);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            MycotoxicDaisyParticles.trySpawn(level, pos, random, 0.45, 0.95);
        }
    }
}
