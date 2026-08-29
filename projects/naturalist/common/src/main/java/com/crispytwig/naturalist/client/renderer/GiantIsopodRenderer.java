package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.crispytwig.naturalist.client.model.GiantIsopodModel;
import com.crispytwig.naturalist.server.entity.mob.GiantIsopod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class GiantIsopodRenderer extends NaturalistMobRenderer<GiantIsopod> {
    public GiantIsopodRenderer(EntityRendererProvider.Context context) {
        super(context, new GiantIsopodModel(context.bakeLayer(GiantIsopodModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull GiantIsopod entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void scale(@NotNull NaturalistRenderState<GiantIsopod> state, @NotNull PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
