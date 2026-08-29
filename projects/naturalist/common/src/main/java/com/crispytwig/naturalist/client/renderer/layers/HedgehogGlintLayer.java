package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.crispytwig.naturalist.server.entity.mob.Hedgehog;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class HedgehogGlintLayer extends RenderLayer<NaturalistRenderState<Hedgehog>, NaturalistEntityModel<Hedgehog>> {
    public HedgehogGlintLayer(RenderLayerParent<NaturalistRenderState<Hedgehog>, NaturalistEntityModel<Hedgehog>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<Hedgehog> state, float yRot, float xRot) {
        if (!state.entity.hasThrowEnchantments() || state.isInvisible) {
            return;
        }
        submitNodeCollector.order(1).submitModel(
                this.getParentModel(), state, poseStack, RenderTypes.entityGlint(),
                packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
    }
}
