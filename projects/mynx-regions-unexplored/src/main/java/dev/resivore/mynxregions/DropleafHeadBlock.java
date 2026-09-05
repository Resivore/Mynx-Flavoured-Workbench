package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Downward-growing luminous head; unlike Big Dripleaf this has no tilt/platform state. */
public final class DropleafHeadBlock extends GrowingPlantHeadBlock {
    public static final MapCodec<DropleafHeadBlock> CODEC = simpleCodec(DropleafHeadBlock::new);
    public DropleafHeadBlock(BlockBehaviour.Properties properties) {
        super(properties, Direction.DOWN, Block.column(8.0, 0.0, 16.0), false, 0.1D);
    }
    @Override protected MapCodec<? extends DropleafHeadBlock> codec() { return CODEC; }
    @Override protected int getBlocksToGrowWhenBonemealed(RandomSource random) { return 1 + random.nextInt(2); }
    @Override protected Block getBodyBlock() { return MynxRegionsUnexplored.DROPLEAF_PLANT; }
    @Override protected boolean canGrowInto(BlockState state) { return state.isAir(); }
}
