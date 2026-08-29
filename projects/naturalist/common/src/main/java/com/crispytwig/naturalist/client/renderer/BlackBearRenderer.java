package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.client.model.BlackBearBabyModel;
import com.crispytwig.naturalist.client.model.BlackBearModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.layers.DyeLayer;
import com.crispytwig.naturalist.server.entity.mob.BlackBear;
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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BlackBearRenderer extends NaturalistMobRenderer<BlackBear> {
    public BlackBearRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackBearModel(context.bakeLayer(BlackBearModel.LAYER_LOCATION)), new BlackBearBabyModel(context.bakeLayer(BlackBearBabyModel.LAYER_LOCATION)), 0.9F);
        this.addLayer(new BlackBearHeldItemLayer(this, context.getEntityRenderDispatcher().getItemInHandRenderer()));
        this.addLayer(new DyeLayer<>(this, "black_bear"));
    }

    private static class BlackBearHeldItemLayer extends RenderLayer<NaturalistRenderState<BlackBear>, NaturalistEntityModel<BlackBear>> {
        private final ItemInHandRenderer itemInHandRenderer;

        BlackBearHeldItemLayer(BlackBearRenderer parent, ItemInHandRenderer itemInHandRenderer) {
            super(parent);
            this.itemInHandRenderer = itemInHandRenderer;
        }

        @Override
        public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, int packedLight, @NotNull NaturalistRenderState<BlackBear> state, float yRot, float xRot) {
            BlackBear entity = state.entity;
            if (entity.getEatCounter() < 8) {
                return;
            }
            ItemStack stack = entity.getMainHandItem();
            if (stack.isEmpty() || stack.is(Items.SWEET_BERRIES) || stack.is(Items.HONEYCOMB)) {
                return;
            }
            if (!(this.getParentModel() instanceof BlackBearModel bearModel)) {
                return;
            }
            poseStack.pushPose();
            bearModel.translateToRightArm(poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
            poseStack.translate(0.0F, -7 / 16F, 0.0F);
            this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, submitNodeCollector, packedLight);
            poseStack.popPose();
        }
    }
}
