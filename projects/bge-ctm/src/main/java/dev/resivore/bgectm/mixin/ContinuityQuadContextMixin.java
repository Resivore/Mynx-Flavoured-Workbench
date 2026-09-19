package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import dev.resivore.bgectm.continuity.OverlayRenderCoordinator;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/** Captures exact quad bounds around Continuity's sole processor invocation. */
@Mixin(targets = "me.pepperbell.continuity.client.model.CtmBlockStateModel$CtmQuadTransform",
        remap = false)
abstract class ContinuityQuadContextMixin {
    @Shadow protected BlockAndTintGetter level;
    @Shadow protected BlockPos pos;
    @Shadow protected BlockState appearanceState;
    @Shadow protected BlockState state;
    @Shadow protected ProcessingContextImpl processingContext;

    @Unique private ContinuityQuadContext.Scope bgeCtm$quadScope;

    @Inject(method = "transform", at = @At("HEAD"), require = 1)
    private void bgeCtm$beginQuad(MutableQuadView quad, CallbackInfoReturnable<Boolean> callback) {
        if (bgeCtm$quadScope != null) bgeCtm$quadScope.close();
        bgeCtm$quadScope = ContinuityQuadContext.push(quad, state, pos);
    }

    @Inject(method = "transform", at = @At("RETURN"), require = 1)
    private void bgeCtm$finishQuad(MutableQuadView quad, CallbackInfoReturnable<Boolean> callback) {
        try {
            OverlayRenderCoordinator.finishQuad(quad, processingContext.getExtraQuadEmitter(),
                    callback.getReturnValue(), ContinuityQuadContext.current());
        } finally {
            bgeCtm$quadScope.close();
            bgeCtm$quadScope = null;
        }
    }

    /**
     * The exact 3.0.1 slice lookup precedes every processor call. Recording its selected arrays
     * distinguishes a managed quad with no matching processor from one that reaches a processor
     * but later fails its regular connection predicate.
     */
    @Redirect(method = "transformOnce",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply("
                    + "Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object bgeCtm$recordRuleSelection(Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc,
            Object sprite) {
        QuadProcessors.Slice slice = sliceFunc.apply((TextureAtlasSprite) sprite);
        if (BgeCtmDiagnostics.enabled()) {
            BgeCtmDiagnostics.ruleSelection(state, pos, appearanceState, sprite,
                    slice.processors().length, slice.multipassProcessors().length);
        }
        return slice;
    }

    @Redirect(
            method = "transformOnce",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/pepperbell/continuity/api/client/QuadProcessor;processQuad("
                            + "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;"
                            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
                            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/util/RandomSource;I"
                            + "Lme/pepperbell/continuity/api/client/QuadProcessor$ProcessingContext;)"
                            + "Lme/pepperbell/continuity/api/client/QuadProcessor$ProcessingResult;"),
            require = 1)
    private QuadProcessor.ProcessingResult bgeCtm$captureQuad(
            QuadProcessor processor, MutableQuadView quad, TextureAtlasSprite sprite,
            BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state,
            RandomSource random, int pass, QuadProcessor.ProcessingContext context) {
        return processor.processQuad(quad, sprite, level, pos, appearanceState, state,
                random, pass, context);
    }
}
