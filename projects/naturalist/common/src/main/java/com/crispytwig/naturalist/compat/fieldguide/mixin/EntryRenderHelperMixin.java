package com.crispytwig.naturalist.compat.fieldguide.mixin;

import com.crispytwig.naturalist.client.NaturalistPortraitRenderState;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryRenderHelper.class)
public class EntryRenderHelperMixin {

    @Unique
    private static final String naturalist$RENDER_ENTITY =
            "renderEntity(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;ZFIIIIF" +
            "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;" +
            "Lcom/evandev/fieldguide/api/variant/VariantDef;" +
            "Lcom/evandev/fieldguide/api/variant/VariantProvider;)V";

    @Redirect(
        method = naturalist$RENDER_ENTITY,
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
    )
    private static void naturalist$applyAutoFitScale(PoseStack pose, float x, float y, float z) {
        float m = NaturalistPortraitRenderState.SCALE;
        pose.scale(x * m, y * m, z * m);
    }

    @Inject(method = naturalist$RENDER_ENTITY, at = @At("HEAD"))
    private static void naturalist$portraitStart(Entity entity, Object entrySource, boolean isPage, float yRotation,
                                                 int x, int y, int maxWidth, int maxHeight, float bounceScale,
                                                 PoseStack poseStack, SubmitNodeCollector collector,
                                                 VariantDef variantDef, VariantProvider<Mob> provider, CallbackInfo ci) {
        NaturalistPortraitRenderState.ACTIVE = true;
    }

    @Inject(method = naturalist$RENDER_ENTITY, at = @At("RETURN"))
    private static void naturalist$portraitEnd(CallbackInfo ci) {
        NaturalistPortraitRenderState.ACTIVE = false;
    }

    @Redirect(
        method = naturalist$RENDER_ENTITY,
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Quaternionf;rotationY(F)Lorg/joml/Quaternionf;"
        )
    )
    private static Quaternionf naturalist$rotatePortrait(Quaternionf quaternion, float radians,
                                                         Entity entity, Object entrySource, boolean isPage, float yRotation,
                                                         int x, int y, int maxWidth, int maxHeight, float bounceScale,
                                                         PoseStack poseStack, SubmitNodeCollector collector,
                                                         VariantDef variantDef, VariantProvider<Mob> provider) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id.getNamespace().equals("naturalist") && id.getPath().equals("crab")) {
            radians += (float) Math.toRadians(90.0D);
        }
        return quaternion.rotationY(radians);
    }
}
