package dev.resivore.mynxregions;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class MycotoxicDaisyPotBlock extends FlowerPotBlock {
    public MycotoxicDaisyPotBlock(Block content, BlockBehaviour.Properties properties) {
        super(content, properties);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MycotoxicDaisyParticles.trySpawn(level, pos, random, 0.35, 0.8);
    }
}
