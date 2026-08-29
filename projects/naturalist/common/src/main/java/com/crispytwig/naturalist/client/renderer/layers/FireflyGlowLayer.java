package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.crispytwig.naturalist.server.entity.mob.Firefly;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class FireflyGlowLayer extends RenderLayer<NaturalistRenderState<Firefly>, NaturalistEntityModel<Firefly>> {
    private static final Identifier GLOW = Naturalist.location("textures/entity/firefly_glow.png");
    private static final Identifier GLOW_E = Naturalist.location("textures/entity/firefly_glow_e.png");
    private static final int TOTAL_FRAMES = 30;
    private static final int TICKS_PER_FRAME = 1;

    public FireflyGlowLayer(RenderLayerParent<NaturalistRenderState<Firefly>, NaturalistEntityModel<Firefly>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<Firefly> state, float yRot, float xRot) {
        Firefly entity = state.entity;
        int frame;
        if (entity.isGlowing()) {
            frame = Math.min((entity.tickCount - entity.getGlowStartTick()) / TICKS_PER_FRAME, TOTAL_FRAMES - 1);
        } else {
            frame = 0;
        }

        this.submitAnimatedModel(
                poseStack, submitNodeCollector, packedLight, state, RenderTypes.entityCutout(GLOW), frame);

        if (entity.isGlowing()) {
            this.submitAnimatedModel(
                    poseStack, submitNodeCollector, packedLight, state,
                    RenderTypes.entityTranslucentEmissive(GLOW_E), frame);
        }
    }

    private void submitAnimatedModel(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                                     NaturalistRenderState<Firefly> state, RenderType renderType, int frame) {
        NaturalistEntityModel<Firefly> model = this.getParentModel();
        submitNodeCollector.order(1).submitCustomGeometry(poseStack, renderType, (renderPose, buffer) -> {
            PoseStack modelPose = new PoseStack();
            modelPose.last().set(renderPose);
            model.setupAnim(state);
            model.renderToBuffer(
                    modelPose, new AnimatedUVVertexConsumer(buffer, TOTAL_FRAMES, frame),
                    packedLight, OverlayTexture.NO_OVERLAY, -1);
        });
    }
}
