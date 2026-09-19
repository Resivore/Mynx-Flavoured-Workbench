package dev.resivore.bgectm.mixin;

import dev.resivore.bgectm.continuity.ContinuityQuadContext;
import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor.SpriteCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aligns exact successful probes with each non-null Continuity collector sprite. */
@Mixin(StandardOverlayQuadProcessor.class)
abstract class ContinuityOverlayContributionMixin {
    private static final String COLLECTOR =
            "Lme/pepperbell/continuity/client/processor/overlay/StandardOverlayQuadProcessor$SpriteCollector;";
    private static final String ADD = COLLECTOR + "add("
            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V";
    private static final String PREPARE_ONE = "prepareCollector(" + COLLECTOR + "I)" + COLLECTOR;
    private static final String PREPARE_TWO = "prepareCollector(" + COLLECTOR + "II)" + COLLECTOR;
    private static final String TWO_SIDES = "fromTwoSidesAdj(" + COLLECTOR
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;II"
            + "Lnet/minecraft/core/BlockPos$MutableBlockPos;"
            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;"
            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)" + COLLECTOR;
    private static final String ONE_SIDE = "fromOneSide(" + COLLECTOR
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;"
            + "Lnet/minecraft/core/Direction;III"
            + "Lnet/minecraft/core/BlockPos$MutableBlockPos;"
            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/Direction;"
            + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)" + COLLECTOR;

    @Inject(method = "getSprites", at = @At("HEAD"), require = 1)
    private void bgeCtm$beginContributionAssembly(BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, Direction lightFace,
            TextureAtlasSprite quadSprite, Direction[] directions,
            ProcessingDataProvider dataProvider, CallbackInfoReturnable<SpriteCollector> callback) {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture != null) capture.beginOverlayAssembly(directions);
    }

    @Inject(method = PREPARE_ONE, at = @At("HEAD"), require = 1)
    private void bgeCtm$beginPrepareOne(SpriteCollector collector, int sprite0,
            CallbackInfoReturnable<SpriteCollector> callback) {
        beginPending(sprite0);
    }

    @Redirect(method = PREPARE_ONE,
            at = @At(value = "INVOKE", target = ADD, ordinal = 0), require = 1)
    private void bgeCtm$addPrepareOne(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 0);
    }

    @Inject(method = PREPARE_ONE, at = @At("RETURN"), require = 1)
    private void bgeCtm$endPrepareOne(SpriteCollector collector, int sprite0,
            CallbackInfoReturnable<SpriteCollector> callback) {
        endPending();
    }

    @Inject(method = PREPARE_TWO, at = @At("HEAD"), require = 1)
    private void bgeCtm$beginPrepareTwo(SpriteCollector collector, int sprite0, int sprite1,
            CallbackInfoReturnable<SpriteCollector> callback) {
        beginPending(sprite0, sprite1);
    }

    @Redirect(method = PREPARE_TWO,
            at = @At(value = "INVOKE", target = ADD, ordinal = 0), require = 1)
    private void bgeCtm$addPrepareTwoFirst(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 0);
    }

    @Redirect(method = PREPARE_TWO,
            at = @At(value = "INVOKE", target = ADD, ordinal = 1), require = 1)
    private void bgeCtm$addPrepareTwoSecond(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 1);
    }

    @Inject(method = PREPARE_TWO, at = @At("RETURN"), require = 1)
    private void bgeCtm$endPrepareTwo(SpriteCollector collector, int sprite0, int sprite1,
            CallbackInfoReturnable<SpriteCollector> callback) {
        endPending();
    }

    @Inject(method = TWO_SIDES, at = @At("HEAD"), require = 1)
    private void bgeCtm$beginTwoSides(SpriteCollector collector, BlockState appearanceState0,
            BlockState appearanceState1, Direction dir0, Direction dir1, int sprite,
            int spriteC01, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level,
            BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace,
            TextureAtlasSprite quadSprite, CallbackInfoReturnable<SpriteCollector> callback) {
        beginPending(sprite, spriteC01);
    }

    @Redirect(method = TWO_SIDES,
            at = @At(value = "INVOKE", target = ADD, ordinal = 0), require = 1)
    private void bgeCtm$addTwoSidesBase(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 0);
    }

    @Redirect(method = TWO_SIDES,
            at = @At(value = "INVOKE", target = ADD, ordinal = 1), require = 1)
    private void bgeCtm$addTwoSidesCorner(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 1);
    }

    @Inject(method = TWO_SIDES, at = @At("RETURN"), require = 1)
    private void bgeCtm$endTwoSides(SpriteCollector collector, BlockState appearanceState0,
            BlockState appearanceState1, Direction dir0, Direction dir1, int sprite,
            int spriteC01, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level,
            BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace,
            TextureAtlasSprite quadSprite, CallbackInfoReturnable<SpriteCollector> callback) {
        endPending();
    }

    @Inject(method = ONE_SIDE, at = @At("HEAD"), require = 1)
    private void bgeCtm$beginOneSide(SpriteCollector collector, BlockState appearanceState0,
            BlockState appearanceState1, BlockState appearanceState2, Direction dir0,
            Direction dir1, Direction dir2, int sprite, int spriteC01, int spriteC12,
            BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, Direction lightFace,
            TextureAtlasSprite quadSprite, CallbackInfoReturnable<SpriteCollector> callback) {
        beginPending(sprite, spriteC01, spriteC12);
    }

    @Redirect(method = ONE_SIDE,
            at = @At(value = "INVOKE", target = ADD, ordinal = 0), require = 1)
    private void bgeCtm$addOneSideBase(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 0);
    }

    @Redirect(method = ONE_SIDE,
            at = @At(value = "INVOKE", target = ADD, ordinal = 1), require = 1)
    private void bgeCtm$addOneSideFirstCorner(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 1);
    }

    @Redirect(method = ONE_SIDE,
            at = @At(value = "INVOKE", target = ADD, ordinal = 2), require = 1)
    private void bgeCtm$addOneSideSecondCorner(SpriteCollector collector, TextureAtlasSprite sprite) {
        addPending(collector, sprite, 2);
    }

    @Inject(method = ONE_SIDE, at = @At("RETURN"), require = 1)
    private void bgeCtm$endOneSide(SpriteCollector collector, BlockState appearanceState0,
            BlockState appearanceState1, BlockState appearanceState2, Direction dir0,
            Direction dir1, Direction dir2, int sprite, int spriteC01, int spriteC12,
            BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos,
            BlockState appearanceState, BlockState state, Direction lightFace,
            TextureAtlasSprite quadSprite, CallbackInfoReturnable<SpriteCollector> callback) {
        endPending();
    }

    @Redirect(method = "getSprites",
            at = @At(value = "INVOKE", target = ADD, ordinal = 0), require = 1)
    private void bgeCtm$addCornerLeftDown(SpriteCollector collector, TextureAtlasSprite sprite) {
        addConstant(collector, sprite, 2);
    }

    @Redirect(method = "getSprites",
            at = @At(value = "INVOKE", target = ADD, ordinal = 1), require = 1)
    private void bgeCtm$addCornerDownRight(SpriteCollector collector, TextureAtlasSprite sprite) {
        addConstant(collector, sprite, 0);
    }

    @Redirect(method = "getSprites",
            at = @At(value = "INVOKE", target = ADD, ordinal = 2), require = 1)
    private void bgeCtm$addCornerRightUp(SpriteCollector collector, TextureAtlasSprite sprite) {
        addConstant(collector, sprite, 14);
    }

    @Redirect(method = "getSprites",
            at = @At(value = "INVOKE", target = ADD, ordinal = 3), require = 1)
    private void bgeCtm$addCornerLeftUp(SpriteCollector collector, TextureAtlasSprite sprite) {
        addConstant(collector, sprite, 16);
    }

    private static void beginPending(int... spriteIndices) {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture != null) capture.beginPendingSprites(spriteIndices);
    }

    private static void addPending(SpriteCollector collector, TextureAtlasSprite sprite,
            int argumentIndex) {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture != null) capture.addPendingSprite(argumentIndex, sprite != null);
        collector.add(sprite);
    }

    private static void endPending() {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture != null) capture.endPendingSprites();
    }

    private static void addConstant(SpriteCollector collector, TextureAtlasSprite sprite,
            int spriteIndex) {
        ContinuityQuadContext.Capture capture = ContinuityQuadContext.current();
        if (capture != null) capture.addOverlaySprite(spriteIndex, sprite != null);
        collector.add(sprite);
    }
}
