package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.crispytwig.naturalist.client.model.LizardTailModel;
import com.crispytwig.naturalist.server.entity.mob.LizardTail;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class LizardTailRenderer extends NaturalistMobRenderer<LizardTail> {
    public LizardTailRenderer(EntityRendererProvider.Context context) {
        super(context, new LizardTailModel(context.bakeLayer(LizardTailModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull LizardTail entity) {
        return entity.getVariantTexture();
    }

    @Override
    public void submit(@NotNull NaturalistRenderState<LizardTail> state, @NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, -0.3, 0);
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }
}
