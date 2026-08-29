package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class GlowLayer<T extends LivingEntity, M extends EntityModel<NaturalistRenderState<T>>>
        extends RenderLayer<NaturalistRenderState<T>, M> {
    private final Function<T, Identifier> glowmask;

    public GlowLayer(RenderLayerParent<NaturalistRenderState<T>, M> parent, Function<T, Identifier> glowmask) {
        super(parent);
        this.glowmask = glowmask;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<T> state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        Identifier texture = this.glowmask.apply(state.entity);
        if (texture == null) {
            return;
        }
        submitNodeCollector.order(1).submitModel(
                this.getParentModel(), state, poseStack, RenderTypes.entityTranslucentEmissive(texture),
                packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
    }
}
