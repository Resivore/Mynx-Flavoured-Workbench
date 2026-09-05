package dev.resivore.mynxregions;

import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Four-count ground cover; vanilla 26.2 FlowerBedBlock preserves stacking and full-cell bonemeal ejection. */
public final class CloverBlock extends FlowerBedBlock {
    public CloverBlock(BlockBehaviour.Properties properties) { super(properties); }
}
