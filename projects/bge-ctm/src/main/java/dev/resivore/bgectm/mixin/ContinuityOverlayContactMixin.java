package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import dev.resivore.bgectm.continuity.OverlayContactFilter;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vetoes only an already-positive exact Continuity Standard Overlay application. */
@Mixin(StandardOverlayQuadProcessor.class)
abstract class ContinuityOverlayContactMixin {
    @Inject(
            method = "appliesOverlay("
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/core/Direction;"
                    + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)Z",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void bgeCtm$filterPositiveOverlay(BlockPos otherPos,
            BlockState otherAppearanceState, BlockState otherState,
            BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
            BlockState state, Direction face, TextureAtlasSprite quadSprite,
            CallbackInfoReturnable<Boolean> callback) {
        if (!Boolean.TRUE.equals(callback.getReturnValue())) return;

        // Recover physical states from the render view. Canonical appearance arguments remain
        // Continuity's semantic authority and are deliberately absent from the geometry policy.
        BlockState realReceiverState = level.getBlockState(pos);
        BlockState realInducingState = level.getBlockState(otherPos);
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        boolean retained = OverlayContactFilter.retainAfterUpstream(true,
                realReceiverState, pos, realInducingState, otherPos, face, capture);
        if (!retained) callback.setReturnValue(false);
    }
}
