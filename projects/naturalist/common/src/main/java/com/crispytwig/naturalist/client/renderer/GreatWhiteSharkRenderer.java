package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.GreatWhiteSharkModel;
import com.crispytwig.naturalist.server.entity.mob.GreatWhiteShark;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class GreatWhiteSharkRenderer extends NaturalistMobRenderer<GreatWhiteShark> {
    public GreatWhiteSharkRenderer(EntityRendererProvider.Context context) {
        super(context, new GreatWhiteSharkModel(context.bakeLayer(GreatWhiteSharkModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull GreatWhiteShark entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<GreatWhiteShark> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, state.entity.getRenderYaw(state.partialTick), entityScale);
    }
}
