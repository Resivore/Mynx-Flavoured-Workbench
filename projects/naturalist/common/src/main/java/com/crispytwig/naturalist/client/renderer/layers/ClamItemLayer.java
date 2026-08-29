package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.client.model.ClamModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.crispytwig.naturalist.server.entity.mob.Clam;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class ClamItemLayer extends RenderLayer<NaturalistRenderState<Clam>, NaturalistEntityModel<Clam>> {
    private final ItemInHandRenderer itemInHandRenderer;
    private final Quaternionf scratchRotation = new Quaternionf();

    public ClamItemLayer(RenderLayerParent<NaturalistRenderState<Clam>, NaturalistEntityModel<Clam>> parent,
                         ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<Clam> state, float yRot, float xRot) {
        Clam clam = state.entity;
        ItemStack held = clam.getMainHandItem();
        if (held.isEmpty() || !(this.getParentModel() instanceof ClamModel model)) {
            return;
        }

        poseStack.pushPose();
        model.translateToItem(poseStack);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.8F, 0.8F, 0.8F);

        poseStack.translate(0.0F, 0.6F + Mth.sin(state.ageInTicks * 0.1F) * 0.2F, 0.0F);

        poseStack.mulPose(poseStack.last().pose().getNormalizedRotation(this.scratchRotation).conjugate());
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        this.itemInHandRenderer.renderItem(clam, held, ItemDisplayContext.FIXED, poseStack, submitNodeCollector, packedLight);
        poseStack.popPose();
    }
}
