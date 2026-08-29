package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.crispytwig.naturalist.client.model.RhinoModel;
import com.crispytwig.naturalist.server.entity.mob.Rhino;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class RhinoRenderer extends NaturalistMobRenderer<Rhino> {
    public RhinoRenderer(EntityRendererProvider.Context context) {
        super(context, new RhinoModel(context.bakeLayer(RhinoModel.LAYER_LOCATION)), 1.1F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Rhino entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected void scale(@NotNull NaturalistRenderState<Rhino> state, @NotNull PoseStack poseStack) {
        float scale = state.isBaby ? 0.5F : 0.9F;
        poseStack.scale(scale, scale, scale);
    }
}
