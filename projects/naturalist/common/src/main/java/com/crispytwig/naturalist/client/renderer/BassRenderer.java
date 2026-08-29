package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.BassModel;
import com.crispytwig.naturalist.client.model.LargeBassModel;
import com.crispytwig.naturalist.client.model.MediumBassModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.server.entity.mob.Bass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class BassRenderer extends NaturalistMobRenderer<Bass> {
    private final NaturalistEntityModel<Bass> normalModel;
    private final NaturalistEntityModel<Bass> mediumModel;
    private final NaturalistEntityModel<Bass> largeModel;

    public BassRenderer(EntityRendererProvider.Context context) {
        super(context, new BassModel(context.bakeLayer(BassModel.LAYER_LOCATION)), 0.0F);
        this.normalModel = this.model;
        this.mediumModel = new MediumBassModel(context.bakeLayer(MediumBassModel.LAYER_LOCATION));
        this.largeModel = new LargeBassModel(context.bakeLayer(LargeBassModel.LAYER_LOCATION));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Bass entity) {
        return entity.getVariantTexture();
    }

    @Override
    protected NaturalistEntityModel<Bass> selectModel(NaturalistRenderState<Bass> state) {
        if (state.entity.isLargeVariant()) {
            return this.largeModel;
        }
        if (state.entity.isMediumVariant()) {
            return this.mediumModel;
        }
        return this.normalModel;
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Bass> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.entity.swimTilt.getTilt(state.partialTick)));
    }
}
