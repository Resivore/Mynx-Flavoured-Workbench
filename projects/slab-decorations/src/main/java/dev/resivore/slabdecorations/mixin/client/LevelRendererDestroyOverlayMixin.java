package dev.resivore.slabdecorations.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the destroy-crack pose with the lowered model when its emission has no world view. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererDestroyOverlayMixin {
    @WrapOperation(
            method = "submitBlockDestroyAnimation(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 slabDecorations$alignDestroyOverlay(
            BlockState state,
            BlockPos pos,
            Operation<Vec3> original) {
        Vec3 vanilla = original.call(state, pos);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return vanilla;
        double offset = NibaruHorizontalSurface.visibleOffset(state, level, pos);
        return offset == 0.0D ? vanilla : vanilla.add(0.0D, offset, 0.0D);
    }
}
