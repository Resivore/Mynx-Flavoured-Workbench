package com.crispytwig.naturalist.server.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;

public interface ContainerBoundWorker {
    void tryAssignWorkstation(BlockPos pos);

    @Nullable
    static Container getContainer(Level level, BlockPos pos) {
        return HopperBlockEntity.getContainerAt(level, pos);
    }
}
