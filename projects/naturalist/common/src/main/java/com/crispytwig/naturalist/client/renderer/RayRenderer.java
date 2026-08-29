package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.RayModel;
import com.crispytwig.naturalist.server.entity.mob.Ray;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class RayRenderer extends NaturalistMobRenderer<Ray> {
    public RayRenderer(EntityRendererProvider.Context context) {
        super(context, new RayModel(context.bakeLayer(RayModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Ray entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Ray> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.entity.swimTilt.getTilt(state.partialTick)));
    }
}
