package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.crispytwig.naturalist.client.model.VultureBabyModel;
import com.crispytwig.naturalist.client.model.VultureModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.server.entity.mob.Vulture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class VultureRenderer extends NaturalistMobRenderer<Vulture> {
    public VultureRenderer(EntityRendererProvider.Context context) {
        super(context, new VultureModel(context.bakeLayer(VultureModel.LAYER_LOCATION)), new VultureBabyModel(context.bakeLayer(VultureBabyModel.LAYER_LOCATION)), 0.65F, 0.3F);
        this.addLayer(new VultureHeldItemLayer(this, context.getEntityRenderDispatcher().getItemInHandRenderer()));
    }

    private static class VultureHeldItemLayer extends RenderLayer<NaturalistRenderState<Vulture>, NaturalistEntityModel<Vulture>> {
        private final ItemInHandRenderer itemInHandRenderer;

        VultureHeldItemLayer(VultureRenderer parent, ItemInHandRenderer itemInHandRenderer) {
            super(parent);
            this.itemInHandRenderer = itemInHandRenderer;
        }

        @Override
        public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, int packedLight, @NotNull NaturalistRenderState<Vulture> state, float yRot, float xRot) {
            Vulture entity = state.entity;
            ItemStack stack = entity.getItemBySlot(EquipmentSlot.MAINHAND);
            if (stack.isEmpty() || !(this.getParentModel() instanceof VultureModel vultureModel)) {
                return;
            }
            poseStack.pushPose();
            vultureModel.translateToHeldItem(poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.GROUND, poseStack, submitNodeCollector, packedLight);
            poseStack.popPose();
        }
    }
}
