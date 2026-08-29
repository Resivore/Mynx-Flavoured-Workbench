package com.crispytwig.naturalist.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.crispytwig.naturalist.client.model.HippoBabyModel;
import com.crispytwig.naturalist.client.model.HippoModel;
import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.server.entity.mob.Hippo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.BlockItem;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class HippoRenderer extends NaturalistMobRenderer<Hippo> {
    public HippoRenderer(EntityRendererProvider.Context context) {
        super(context, new HippoModel(context.bakeLayer(HippoModel.LAYER_LOCATION)), new HippoBabyModel(context.bakeLayer(HippoBabyModel.LAYER_LOCATION)), 1.1F);
        this.addLayer(new HippoJawBlockLayer(this, context.getBlockModelResolver()));
    }

    private static class HippoJawBlockLayer extends RenderLayer<NaturalistRenderState<Hippo>, NaturalistEntityModel<Hippo>> {
        private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
        private final BlockModelResolver blockModelResolver;

        HippoJawBlockLayer(HippoRenderer parent, BlockModelResolver blockModelResolver) {
            super(parent);
            this.blockModelResolver = blockModelResolver;
        }

        @Override
        public void submit(@NotNull PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, int packedLight, @NotNull NaturalistRenderState<Hippo> state, float yRot, float xRot) {
            Hippo entity = state.entity;
            if (!(entity.getMainHandItem().getItem() instanceof BlockItem blockItem)) {
                return;
            }
            if (!(this.getParentModel() instanceof HippoModel hippoModel)) {
                return;
            }
            poseStack.pushPose();
            hippoModel.translateToBotJaw(poseStack);
            poseStack.translate(-0.4D, 0.76D, -1.8D);
            poseStack.scale(0.675F, 0.675F, 0.675F);
            BlockModelRenderState blockModel = new BlockModelRenderState();
            this.blockModelResolver.update(blockModel, blockItem.getBlock().defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
            blockModel.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
