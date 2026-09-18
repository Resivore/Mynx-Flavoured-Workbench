package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

/** Captures exact quad bounds around Continuity's sole processor invocation. */
@Mixin(targets = "me.pepperbell.continuity.client.model.CtmBlockStateModel$CtmQuadTransform",
        remap = false)
abstract class ContinuityQuadContextMixin {
    @Shadow protected BlockAndTintGetter level;
    @Shadow protected BlockPos pos;
    @Shadow protected BlockState appearanceState;
    @Shadow protected BlockState state;

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
        BgeCtmDiagnostics.ruleSelection(state, pos, appearanceState, sprite,
                slice.processors().length, slice.multipassProcessors().length);
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
        try (ContinuityQuadContext.Scope ignored = ContinuityQuadContext.push(quad)) {
            return processor.processQuad(quad, sprite, level, pos, appearanceState, state,
                    random, pass, context);
        }
    }
}
