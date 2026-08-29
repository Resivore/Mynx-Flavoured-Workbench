package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.MoleModel;
import com.crispytwig.naturalist.server.entity.mob.Mole;
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
public class MoleRenderer extends NaturalistMobRenderer<Mole> {
    public MoleRenderer(EntityRendererProvider.Context context) {
        super(context, new MoleModel(context.bakeLayer(MoleModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Mole entity) {
        return entity.getVariantTexture();
    }

    @Override
    public void submit(@NotNull NaturalistRenderState<Mole> state, @NotNull PoseStack poseStack,
                       @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.isBaby) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }

    @Override
    protected float getShadowRadius(NaturalistRenderState<Mole> state) {
        float shadowRadius = state.entity.isRolledUp() ? 0.0F : state.isBaby ? 0.25F : 0.4F;
        return shadowRadius * state.scale * state.ageScale;
    }
}
