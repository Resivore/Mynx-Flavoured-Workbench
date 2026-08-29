package com.crispytwig.naturalist.compat.fieldguide.mixin;

import com.crispytwig.naturalist.client.NaturalistPortraitRenderState;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryRenderHelper.class)
public class EntryRenderHelperMixin {

    @Redirect(
        method = "renderEntity(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;ZF)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private static void naturalist$applyAutoFitScale(PoseStack pose, float x, float y, float z) {
        float m = NaturalistPortraitRenderState.SCALE;
        pose.scale(x * m, y * m, z * m);
    }

    @Inject(method = "renderEntity(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;ZF)V", at = @At("HEAD"))
    private static void naturalist$portraitStart(Entity entity, Object entrySource, boolean isPage, float yRotation, CallbackInfo ci) {
        NaturalistPortraitRenderState.ACTIVE = true;
    }

    @Inject(method = "renderEntity(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;ZF)V", at = @At("RETURN"))
    private static void naturalist$portraitEnd(CallbackInfo ci) {
        NaturalistPortraitRenderState.ACTIVE = false;
    }

    @Redirect(
        method = "renderEntity(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;ZF)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
            ordinal = 1
        )
    )
    private static Quaternionf naturalist$rotatePortrait(Axis axis, float f, Entity entity, Object entrySource, boolean isPage, float yRotation) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id.getNamespace().equals("naturalist") && id.getPath().equals("crab")) {
            return axis.rotationDegrees(f + 90.0F);
        }
        return axis.rotationDegrees(f);
    }
}
