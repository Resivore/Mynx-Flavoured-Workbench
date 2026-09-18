package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.SurfaceContactResolver;
import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import dev.resivore.bgectm.continuity.OverlayContactFilter;
import dev.resivore.bgectm.continuity.OverlayAttemptContext;
import dev.resivore.bgectm.continuity.OverlaySourceEligibility;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Vetoes only an already-positive exact Continuity Standard Overlay application. */
@Mixin(StandardOverlayQuadProcessor.class)
abstract class ContinuityOverlayContactMixin {
    private static final String APPLIES_OVERLAY = "appliesOverlay("
            + "Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;"
            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)Z";

    @Inject(method = APPLIES_OVERLAY, at = @At("HEAD"), require = 1)
    private void bgeCtm$beginOverlayDiagnostic(BlockPos otherPos,
            BlockState otherAppearanceState, BlockState otherState,
            BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
            BlockState state, Direction face, TextureAtlasSprite quadSprite,
            CallbackInfoReturnable<Boolean> callback) {
        OverlayAttemptContext.begin(otherPos, otherAppearanceState, otherState,
                pos, appearanceState, state, face);
    }

    @Redirect(method = APPLIES_OVERLAY,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;"
                    + "Lnet/minecraft/core/BlockPos;)Z"), require = 1)
    private boolean bgeCtm$allowTypedPartialSource(BlockState source, BlockGetter view, BlockPos sourcePos) {
        boolean nativeFull = source.isCollisionShapeFullBlock(view, sourcePos);
        boolean promoted = !nativeFull && OverlaySourceEligibility.mayReachCanonicalSemantics(source);
        OverlayAttemptContext.gate(nativeFull, promoted);
        return nativeFull || promoted;
    }

    @Redirect(method = APPLIES_OVERLAY,
            at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"),
            require = 1)
    private boolean bgeCtm$recordConnectBlocks(Predicate<Object> predicate, Object appearance) {
        boolean result = predicate.test(appearance);
        OverlayAttemptContext.connectBlocks(result);
        return result;
    }

    @Inject(
            method = APPLIES_OVERLAY,
            at = @At("RETURN"), cancellable = true, require = 1)
    private void bgeCtm$filterPositiveOverlay(BlockPos otherPos,
            BlockState otherAppearanceState, BlockState otherState,
            BlockAndTintGetter level, BlockPos pos, BlockState appearanceState,
            BlockState state, Direction face, TextureAtlasSprite quadSprite,
            CallbackInfoReturnable<Boolean> callback) {
        // Recover physical states from the render view. Canonical appearance arguments remain
        // Continuity's semantic authority and are deliberately absent from the geometry policy.
        BlockState realReceiverState = level.getBlockState(pos);
        BlockState realInducingState = level.getBlockState(otherPos);
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        boolean semantic = Boolean.TRUE.equals(callback.getReturnValue());
        boolean retained = OverlayContactFilter.retainAfterUpstream(semantic,
                realReceiverState, pos, realInducingState, otherPos, face, capture);
        if (!retained) callback.setReturnValue(false);
        OverlayAttemptContext.Attempt attempt = OverlayAttemptContext.end();
        if (attempt != null) {
            attempt.semantic = semantic;
            attempt.result = retained;
            attempt.quad = capture == null ? null : capture.surface();
            attempt.geometry = SurfaceContactResolver.inspect(realReceiverState, pos,
                    realInducingState, otherPos, face);
            if (semantic) {
                attempt.reason = retained ? "FINAL_OVERLAY"
                        : attempt.geometry == SurfaceContactResolver.Decision.NON_COPLANAR
                                ? "NON_COPLANAR"
                                : attempt.geometry == SurfaceContactResolver.Decision.NO_BOUNDARY_CONTACT
                                        ? "NO_BOUNDARY_CONTACT" : "FINAL_VETO";
            }
            BgeCtmDiagnostics.overlay(realReceiverState, pos, appearanceState,
                    realInducingState, otherPos, otherAppearanceState, attempt.nativeFull,
                    attempt.promoted, attempt.quad, attempt.geometry, semantic, retained,
                    attempt.reason);
        }
    }
}
