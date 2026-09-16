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
import dev.resivore.villagerwork.FishingFloat;
import net.minecraft.world.item.Items;

/**
 * A single plain stick model posed at crossed villager arms while this villager owns a live VWR
 * float. It is intentionally independent of equipment synchronization: the float is already
 * the authoritative, synchronized visual state for a cast, whereas a temporary fishing-rod item
 * fights client serialization and also carries a duplicate vanilla line and hook.
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
                || !(state instanceof VwrFishingRodPresentation presentation)) return;
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(presentation.villagerWork$entityId());
        if (!(entity instanceof Villager villager)) return;
        if (!hasLiveFloat(villager)) return;

        poseStack.pushPose();
        // This follows the active villager model's arms, so resource-pack geometry remains in
        // charge of its body. The forward/upward tilt agrees with FishingRodPose.tip.
        getParentModel().translateToArms(state, poseStack);
        poseStack.translate(0.0F, 0.08F, -0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-58.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F));
        poseStack.scale(1.28F, 1.28F, 1.28F);
        itemRenderer.renderItem(villager, new ItemStack(Items.STICK),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, collector, light);
        poseStack.popPose();
    }

    private static boolean hasLiveFloat(Villager villager) {
        return !villager.level().getEntitiesOfClass(FishingFloat.class,
                villager.getBoundingBox().inflate(18.0), floatEntity -> floatEntity.owner() == villager).isEmpty();
    }
}
