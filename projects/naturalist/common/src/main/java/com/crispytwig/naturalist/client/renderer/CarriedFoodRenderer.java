package com.crispytwig.naturalist.client.renderer;

import com.crispytwig.naturalist.server.entity.misc.CarriedFoodEntity;
import com.crispytwig.naturalist.server.entity.mob.Ant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class CarriedFoodRenderer extends ItemEntityRenderer {
    public CarriedFoodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public CarriedFoodRenderState createRenderState() {
        return new CarriedFoodRenderState();
    }

    @Override
    public void extractRenderState(ItemEntity entity, ItemEntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        CarriedFoodRenderState state = (CarriedFoodRenderState) renderState;
        state.hasCarrier = false;
        if (entity instanceof CarriedFoodEntity food) {
            Ant ant = food.resolveAnt();
            if (ant != null) {
                state.hasCarrier = true;
                state.carrierOffsetX = Mth.lerp(partialTick, ant.xOld, ant.getX()) - Mth.lerp(partialTick, entity.xOld, entity.getX());
                state.carrierOffsetY = Mth.lerp(partialTick, ant.yOld, ant.getY()) + ant.getBbHeight() + CarriedFoodEntity.BACK_GAP - Mth.lerp(partialTick, entity.yOld, entity.getY());
                state.carrierOffsetZ = Mth.lerp(partialTick, ant.zOld, ant.getZ()) - Mth.lerp(partialTick, entity.zOld, entity.getZ());
            }
        }
    }

    @Override
    public void submit(ItemEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        CarriedFoodRenderState state = (CarriedFoodRenderState) renderState;
        if (state.hasCarrier) {
            poseStack.pushPose();
            poseStack.translate(state.carrierOffsetX, state.carrierOffsetY, state.carrierOffsetZ);
            super.submit(state, poseStack, submitNodeCollector, camera);
            poseStack.popPose();
        } else {
            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }

    public static class CarriedFoodRenderState extends ItemEntityRenderState {
        private boolean hasCarrier;
        private double carrierOffsetX;
        private double carrierOffsetY;
        private double carrierOffsetZ;
    }
}
