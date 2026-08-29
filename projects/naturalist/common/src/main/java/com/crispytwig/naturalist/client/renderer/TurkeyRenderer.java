package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.TurkeyModel;
import com.crispytwig.naturalist.server.entity.mob.Turkey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class TurkeyRenderer extends NaturalistMobRenderer<Turkey> {
    public TurkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new TurkeyModel(context.bakeLayer(TurkeyModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Turkey entity) {
        return entity.getVariantTexture();
    }

    @Override
    public void submit(@NotNull NaturalistRenderState<Turkey> state, @NotNull PoseStack poseStack,
                       @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }

    @Override
    protected float getShadowRadius(NaturalistRenderState<Turkey> state) {
        return (state.isBaby ? 0.15F : 0.3F) * state.scale * state.ageScale;
    }
}
