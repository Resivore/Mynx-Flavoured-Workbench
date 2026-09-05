package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Itemless body companion. Clone/pick behavior resolves through the associated head item. */
public final class DropleafPlantBlock extends GrowingPlantBodyBlock {
    public static final MapCodec<DropleafPlantBlock> CODEC = simpleCodec(DropleafPlantBlock::new);
    public DropleafPlantBlock(BlockBehaviour.Properties properties) {
        super(properties, Direction.DOWN, Block.column(8.0, 0.0, 16.0), false);
    }
    @Override protected MapCodec<? extends DropleafPlantBlock> codec() { return CODEC; }
    @Override protected GrowingPlantHeadBlock getHeadBlock() { return MynxRegionsUnexplored.DROPLEAF; }
}
