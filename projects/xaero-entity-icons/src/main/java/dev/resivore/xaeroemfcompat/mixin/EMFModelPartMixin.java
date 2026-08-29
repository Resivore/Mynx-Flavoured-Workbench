package dev.resivore.xaeroemfcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.xaeroemfcompat.ModelPartDetectionBridge;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
abstract class EMFModelPartMixin {
    @Inject(
            method = "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            require = 1,
            remap = false
    )
    private void xaeroEmfEntityIconCompat$detectModelPart(
            PoseStack.Pose pose,
            VertexConsumer vertexConsumer,
            int light,
            int overlay,
            int color,
            CallbackInfo callbackInfo
    ) {
        ModelPartDetectionBridge.forwardToXaero((ModelPart) (Object) this, color);
    }
}
