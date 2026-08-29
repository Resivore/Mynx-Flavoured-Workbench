package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.KomodoDragonModel;
import com.crispytwig.naturalist.server.entity.mob.KomodoDragon;
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
public class KomodoDragonRenderer extends NaturalistMobRenderer<KomodoDragon> {
    public KomodoDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new KomodoDragonModel(context.bakeLayer(KomodoDragonModel.LAYER_LOCATION)), 0.65F);
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull KomodoDragon entity) {
        return entity.getVariantTexture();
    }

    @Override
    public void submit(@NotNull NaturalistRenderState<KomodoDragon> state, @NotNull PoseStack poseStack,
                       @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.isBaby) {
            poseStack.scale(0.45F, 0.45F, 0.45F);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }

    @Override
    protected float getShadowRadius(NaturalistRenderState<KomodoDragon> state) {
        return (state.isBaby ? 0.3F : 0.65F) * state.scale * state.ageScale;
    }
}
