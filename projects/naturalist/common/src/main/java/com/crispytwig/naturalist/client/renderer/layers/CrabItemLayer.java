package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.client.model.CrabModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.crispytwig.naturalist.server.entity.mob.Crab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class CrabItemLayer extends RenderLayer<NaturalistRenderState<Crab>, NaturalistEntityModel<Crab>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CrabItemLayer(RenderLayerParent<NaturalistRenderState<Crab>, NaturalistEntityModel<Crab>> parent,
                         ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<Crab> state, float yRot, float xRot) {
        Crab crab = state.entity;
        ItemStack held = crab.getMainHandItem();
        if (held.isEmpty() || !(this.getParentModel() instanceof CrabModel model)) {
            return;
        }

        poseStack.pushPose();
        model.translateToItem(poseStack);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.itemInHandRenderer.renderItem(
                crab, held, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, submitNodeCollector, packedLight);
        poseStack.popPose();
    }
}
