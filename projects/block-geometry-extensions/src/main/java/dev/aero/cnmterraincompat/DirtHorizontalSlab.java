package dev.aero.cnmterraincompat;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

final class DirtHorizontalSlab extends SlabBlock {
    DirtHorizontalSlab(BlockBehaviour.Properties properties) {
        super(properties);
    }

    static BlockState copyGeometry(BlockState from, BlockState to) {
        return to.setValue(TYPE, from.getValue(TYPE))
                .setValue(WATERLOGGED, from.getValue(WATERLOGGED));
    }
}
