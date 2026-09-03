package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseShapeMixin {
    @Unique
    private static final ThreadLocal<ArrayDeque<Object>> slabDecorations$geometryStates =
            new ThreadLocal<>();

    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true)
    private void slabDecorations$alignOutlineToVisiblePlant(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!slabDecorations$isGeometryEvaluationActive()) {
            cir.setReturnValue(slabDecorations$shiftShape(cir.getReturnValue(), level, pos));
        }
    }

    /** Shifts cached and uncached collision overloads exactly once. */
    @WrapMethod(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape slabDecorations$alignCachedCollision(
            BlockGetter level,
            BlockPos pos,
            Operation<VoxelShape> original) {
        if (slabDecorations$isGeometryEvaluationActive()) {
            return original.call(level, pos);
        }
        slabDecorations$pushGeometryEvaluation();
        try {
            return slabDecorations$shiftShape(original.call(level, pos), level, pos);
        } finally {
            slabDecorations$popGeometryEvaluation();
        }
    }

    @WrapMethod(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape slabDecorations$alignContextCollision(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            Operation<VoxelShape> original) {
        if (slabDecorations$isGeometryEvaluationActive()) {
            return original.call(level, pos, context);
        }
        slabDecorations$pushGeometryEvaluation();
        try {
            return slabDecorations$shiftShape(original.call(level, pos, context), level, pos);
        } finally {
            slabDecorations$popGeometryEvaluation();
        }
    }

    @WrapMethod(
            method = "getVisualShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape slabDecorations$alignVisualShape(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            Operation<VoxelShape> original) {
        if (slabDecorations$isGeometryEvaluationActive()) {
            return original.call(level, pos, context);
        }
        slabDecorations$pushGeometryEvaluation();
        try {
            return slabDecorations$shiftShape(original.call(level, pos, context), level, pos);
        } finally {
            slabDecorations$popGeometryEvaluation();
        }
    }

    @WrapMethod(
            method = "getInteractionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape slabDecorations$alignInteractionShape(
            BlockGetter level,
            BlockPos pos,
            Operation<VoxelShape> original) {
        if (slabDecorations$isGeometryEvaluationActive()) {
            return original.call(level, pos);
        }
        slabDecorations$pushGeometryEvaluation();
        try {
            return slabDecorations$shiftShape(original.call(level, pos), level, pos);
        } finally {
            slabDecorations$popGeometryEvaluation();
        }
    }

    @WrapMethod(
            method = "getEntityInsideCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    private VoxelShape slabDecorations$alignEntityInsideShape(
            BlockGetter level,
            BlockPos pos,
            Entity entity,
            Operation<VoxelShape> original) {
        if (slabDecorations$isGeometryEvaluationActive()) {
            return original.call(level, pos, entity);
        }
        slabDecorations$pushGeometryEvaluation();
        try {
            return slabDecorations$shiftShape(original.call(level, pos, entity), level, pos);
        } finally {
            slabDecorations$popGeometryEvaluation();
        }
    }

    @Unique
    private VoxelShape slabDecorations$shiftShape(
            VoxelShape shape,
            BlockGetter level,
            BlockPos pos) {
        BlockState state = (BlockState) (Object) this;
        double offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        return offset != 0.0D && !shape.isEmpty()
                ? shape.move(0.0D, offset, 0.0D)
                : shape;
    }

    @Unique
    private boolean slabDecorations$isGeometryEvaluationActive() {
        ArrayDeque<Object> states = slabDecorations$geometryStates.get();
        if (states == null) return false;
        for (Object state : states) {
            if (state == this) return true;
        }
        return false;
    }

    @Unique
    private void slabDecorations$pushGeometryEvaluation() {
        ArrayDeque<Object> states = slabDecorations$geometryStates.get();
        if (states == null) {
            states = new ArrayDeque<>();
            slabDecorations$geometryStates.set(states);
        }
        states.push(this);
    }

    @Unique
    private void slabDecorations$popGeometryEvaluation() {
        ArrayDeque<Object> states = slabDecorations$geometryStates.get();
        if (states == null || states.isEmpty()) {
            slabDecorations$geometryStates.remove();
            throw new IllegalStateException("geometry evaluation scope underflow");
        }
        Object removed = states.pop();
        if (removed != this) {
            states.clear();
            slabDecorations$geometryStates.remove();
            throw new IllegalStateException("geometry evaluation scope mismatch");
        }
        if (states.isEmpty()) slabDecorations$geometryStates.remove();
    }
}
