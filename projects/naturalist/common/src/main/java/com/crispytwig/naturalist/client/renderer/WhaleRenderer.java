package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.WhaleBabyModel;
import com.crispytwig.naturalist.client.model.WhaleModel;
import com.crispytwig.naturalist.server.entity.mob.Whale;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class WhaleRenderer extends NaturalistMobRenderer<Whale> {
    public WhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new WhaleModel(context.bakeLayer(WhaleModel.LAYER_LOCATION)), new WhaleBabyModel(context.bakeLayer(WhaleBabyModel.LAYER_LOCATION)), 0.0F, 0.0F);
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Whale> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, state.entity.getRenderYaw(state.partialTick), entityScale);
    }
}
