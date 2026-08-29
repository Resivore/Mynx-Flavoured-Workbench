package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import dev.resivore.slabdecorations.PlantFamilyEligibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseShapeMixin {
    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true)
    private void slabDecorations$alignOutlineToVisiblePlant(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState) (Object) this;
        double offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        VoxelShape shape = cir.getReturnValue();
        if (offset != 0.0D && !shape.isEmpty() && shape.min(Direction.Axis.Y) >= 0.0D) {
            cir.setReturnValue(shape.move(0.0D, offset, 0.0D));
        }
    }

    /** Covers the cached collision overload used by static-shape feature outputs. */
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true)
    private void slabDecorations$alignGeneratedOutputCollision(
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState) (Object) this;
        if (!PlantFamilyEligibility.isSubstrateFeatureOutput(state.getBlock())) return;
        double offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        VoxelShape shape = cir.getReturnValue();
        if (offset != 0.0D && !shape.isEmpty()) {
            cir.setReturnValue(shape.move(0.0D, offset, 0.0D));
        }
    }
}
