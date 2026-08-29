package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.crispytwig.naturalist.client.model.AnglerfishModel;
import com.crispytwig.naturalist.server.entity.mob.Anglerfish;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class AnglerfishRenderer extends NaturalistMobRenderer<Anglerfish> {
    private static final int FULL_BRIGHT = 0x00F000F0;

    public AnglerfishRenderer(EntityRendererProvider.Context context) {
        super(context, new AnglerfishModel(context.bakeLayer(AnglerfishModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Anglerfish entity) {
        return entity.getVariantTexture();
    }

    @Override
    public void extractRenderState(Anglerfish entity, NaturalistRenderState<Anglerfish> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity.isGlowing()) {
            state.lightCoords = FULL_BRIGHT;
        }
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Anglerfish> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.entity.swimTilt.getTilt(state.partialTick)));
    }
}
