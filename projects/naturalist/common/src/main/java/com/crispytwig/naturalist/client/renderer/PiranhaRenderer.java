package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.PiranhaModel;
import com.crispytwig.naturalist.server.entity.mob.Piranha;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class PiranhaRenderer extends NaturalistMobRenderer<Piranha> {
    public PiranhaRenderer(EntityRendererProvider.Context context) {
        super(context, new PiranhaModel(context.bakeLayer(PiranhaModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Piranha entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Piranha> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.entity.swimTilt.getTilt(state.partialTick)));
    }
}
