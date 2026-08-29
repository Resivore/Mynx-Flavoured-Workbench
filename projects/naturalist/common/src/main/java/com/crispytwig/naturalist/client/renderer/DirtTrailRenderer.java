package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.model.DirtTrailModel;
import com.crispytwig.naturalist.server.entity.misc.DirtTrail;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class DirtTrailRenderer extends EntityRenderer<DirtTrail, NaturalistRenderState<DirtTrail>> {
    private static final Identifier TEXTURE = Naturalist.location("textures/entity/mole.png");

    private final DirtTrailModel model;

    public DirtTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DirtTrailModel(context.bakeLayer(DirtTrailModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public NaturalistRenderState<DirtTrail> createRenderState() {
        return new NaturalistRenderState<>();
    }

    @Override
    public void extractRenderState(DirtTrail entity, NaturalistRenderState<DirtTrail> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
        state.ageInTicks = entity.tickCount + partialTick;
        state.texture = TEXTURE;
    }

    @Override
    public void submit(@NotNull NaturalistRenderState<DirtTrail> state, @NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -(state.entity.getId() % 3) * 0.0625D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((state.entity.getId() * 61) % 360));
        if (state.entity.isSmall()) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.01F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
