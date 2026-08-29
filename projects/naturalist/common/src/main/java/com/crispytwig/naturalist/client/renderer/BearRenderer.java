package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.BearBabyModel;
import com.crispytwig.naturalist.client.model.BearModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.layers.DyeLayer;
import com.crispytwig.naturalist.server.entity.mob.Bear;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BearRenderer extends NaturalistMobRenderer<Bear> {
    public BearRenderer(EntityRendererProvider.Context context) {
        super(context, new BearModel(context.bakeLayer(BearModel.LAYER_LOCATION)), new BearBabyModel(context.bakeLayer(BearBabyModel.LAYER_LOCATION)), 0.9F);
        this.addLayer(new BearHeldItemLayer(this, context.getEntityRenderDispatcher().getItemInHandRenderer()));
        this.addLayer(new DyeLayer<>(this, "bear"));
    }

    private static class BearHeldItemLayer extends RenderLayer<NaturalistRenderState<Bear>, NaturalistEntityModel<Bear>> {
        private final ItemInHandRenderer itemInHandRenderer;

        BearHeldItemLayer(BearRenderer parent, ItemInHandRenderer itemInHandRenderer) {
            super(parent);
            this.itemInHandRenderer = itemInHandRenderer;
        }

        @Override
        public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, int packedLight, @NotNull NaturalistRenderState<Bear> state, float yRot, float xRot) {
            Bear entity = state.entity;
            if (entity.getEatCounter() < 8) {
                return;
            }
            ItemStack stack = entity.getMainHandItem();
            if (stack.isEmpty() || !(this.getParentModel() instanceof BearModel bearModel)) {
                return;
            }
            poseStack.pushPose();
            bearModel.translateToRightHand(poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
            poseStack.translate(1 / 16F, -8 / 16F, 2 / 16F);
            this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, submitNodeCollector, packedLight);
            poseStack.popPose();
        }
    }
}
