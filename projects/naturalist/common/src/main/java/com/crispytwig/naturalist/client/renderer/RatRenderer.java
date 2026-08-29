package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.RatModel;
import com.crispytwig.naturalist.server.entity.mob.Rat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class RatRenderer extends NaturalistMobRenderer<Rat> {
    public RatRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel(context.bakeLayer(RatModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Rat entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void scale(@NotNull NaturalistRenderState<Rat> state, @NotNull PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.75F, 0.75F, 0.75F);
        }
    }

    @Override
    protected float getShadowRadius(NaturalistRenderState<Rat> state) {
        return (state.isBaby ? 0.2F : 0.3F) * state.scale * state.ageScale;
    }
}
