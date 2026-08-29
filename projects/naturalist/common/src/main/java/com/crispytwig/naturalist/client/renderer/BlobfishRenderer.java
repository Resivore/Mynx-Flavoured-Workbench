package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.model.BlobfishGrayModel;
import com.crispytwig.naturalist.client.model.BlobfishPinkModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.server.entity.mob.Blobfish;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class BlobfishRenderer extends NaturalistMobRenderer<Blobfish> {
    private static final Identifier GRAY = Naturalist.location("textures/entity/blobfish/gray.png");
    private static final Identifier PINK = Naturalist.location("textures/entity/blobfish/pink.png");

    private final NaturalistEntityModel<Blobfish> pinkModel;
    private final NaturalistEntityModel<Blobfish> grayModel;

    public BlobfishRenderer(EntityRendererProvider.Context context) {
        super(context, new BlobfishPinkModel(context.bakeLayer(BlobfishPinkModel.LAYER_LOCATION)), 0.0F);
        this.pinkModel = this.model;
        this.grayModel = new BlobfishGrayModel(context.bakeLayer(BlobfishGrayModel.LAYER_LOCATION));
    }

    @Override
    public @NotNull Identifier getTextureLocation(@NotNull Blobfish entity) {
        if (entity.hasNonDefaultVariant()) {
            return entity.getVariantTexture();
        }
        return entity.isGray() ? GRAY : PINK;
    }

    @Override
    protected NaturalistEntityModel<Blobfish> selectModel(NaturalistRenderState<Blobfish> state) {
        return state.entity.isGray() ? this.grayModel : this.pinkModel;
    }

    @Override
    protected void setupRotations(@NotNull NaturalistRenderState<Blobfish> state, @NotNull PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.entity.swimTilt.getTilt(state.partialTick)));
    }
}
