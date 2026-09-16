package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A single vanilla fishing-rod item model posed at crossed villager arms.  Vanilla's generic
 * crossed-arms layer uses GROUND transforms; this layer deliberately uses the hand transform so
 * the long rod reads as a tool rather than a small carried icon.
 */
public final class VwrFishingRodLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
    private final ItemInHandRenderer itemRenderer;

    public VwrFishingRodLayer(RenderLayerParent<VillagerRenderState, VillagerModel> parent,
                              ItemInHandRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                 VillagerRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isBaby
                || !(state instanceof VwrFishingRodPresentation presentation)
                || !presentation.villagerWork$renderFishingRod()) return;
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(presentation.villagerWork$entityId());
        if (!(entity instanceof Villager villager)) return;

        poseStack.pushPose();
        // This follows the active villager model's arms, so resource-pack geometry remains in
        // charge of its body.  The modest forward/upward tilt agrees with FishingRodPose.tip.
        getParentModel().translateToArms(state, poseStack);
        poseStack.translate(0.0F, 0.10F, -0.28F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-62.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(14.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-16.0F));
        poseStack.scale(1.18F, 1.18F, 1.18F);
        itemRenderer.renderItem(villager, new ItemStack(Items.FISHING_ROD),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, collector, light);
        poseStack.popPose();
    }
}
