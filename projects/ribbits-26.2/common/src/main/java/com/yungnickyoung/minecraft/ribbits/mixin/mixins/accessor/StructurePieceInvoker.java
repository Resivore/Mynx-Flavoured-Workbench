package com.yungnickyoung.minecraft.ribbits.mixin.mixins.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the two inherited StructurePiece operations used by the swamp-hut mixin. */
@Mixin(StructurePiece.class)
public interface StructurePieceInvoker {
    @Invoker("getWorldPos")
    BlockPos.MutableBlockPos ribbits$invokeGetWorldPos(int x, int y, int z);

    @Invoker("placeBlock")
    void ribbits$invokePlaceBlock(
            WorldGenLevel level,
            BlockState state,
            int x,
            int y,
            int z,
            BoundingBox bounds
    );
}
