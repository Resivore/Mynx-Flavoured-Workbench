package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MossyCarpetBlock overrides collision instead of deriving it from the shifted outline. */
@Mixin(MossyCarpetBlock.class)
public abstract class MossyCarpetCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
    private void slabDecorations$alignPaleMossCollision(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!state.is(Blocks.PALE_MOSS_CARPET)) return;
        double offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        VoxelShape shape = cir.getReturnValue();
        if (offset != 0.0D && !shape.isEmpty()) {
            cir.setReturnValue(shape.move(0.0D, offset, 0.0D));
        }
    }
}
