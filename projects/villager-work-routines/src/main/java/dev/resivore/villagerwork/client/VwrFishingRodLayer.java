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
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import dev.resivore.villagerwork.FishingFloat;
import dev.resivore.villagerwork.FishingRodPose;
import dev.resivore.villagerwork.ShearingToolMarker;
import net.minecraft.world.item.Items;

/**
 * A single client-only Fisherman prop posed at crossed villager arms while this villager owns a
 * live VWR float. The optional Ribbits provider owns its protected rod material; VWR retains its
 * plain-stick fallback when that provider is unavailable. Both paths stay independent of
 * equipment synchronization, avoiding vanilla's duplicate fishing line and hook.
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
        boolean fishing = hasLiveFloat(villager);
        boolean shearing = hasLiveShears(villager);
        if (!fishing && !shearing) return;

        if (fishing) {
            poseStack.pushPose();
            getParentModel().translateToArms(state, poseStack);
            boolean submittedRibbitsRod = RibbitsFishermanRodProvider.submit(poseStack, collector, light);
            poseStack.popPose();
            if (!submittedRibbitsRod) submitFishingStick(villager, state, poseStack, collector, light);
        }
        if (shearing) {
            poseStack.pushPose();
            getParentModel().translateToArms(state, poseStack);
            submitShears(villager, poseStack, collector, light);
            poseStack.popPose();
        }
    }

    private void submitFishingStick(Villager villager, VillagerRenderState state, PoseStack poseStack,
                                    SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        // C17 stays solely on the item-renderer fallback path. The standalone Ribbits rod is
        // attached directly to the crossed-arms grip and never inherits these display transforms.
        FishingRodPose.BodySpaceOffset inward = FishingRodPose.C17_INWARD_BODY_OFFSET;
        poseStack.translate(inward.x(), inward.y(), inward.z());
        getParentModel().translateToArms(state, poseStack);
        poseStack.translate(0.0F, FishingRodPose.STICK_VERTICAL_TRANSLATION,
                FishingRodPose.STICK_ARM_LOCAL_DEPTH_TRANSLATION);
        poseStack.mulPose(Axis.XP.rotationDegrees(-58.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F));
        poseStack.scale(1.28F, 1.28F, 1.28F);
        itemRenderer.renderItem(villager, new ItemStack(Items.STICK),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, collector, light);
        poseStack.popPose();
    }

    private void submitShears(Villager villager, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.02F, -0.30F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-58.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
        poseStack.scale(1.12F, 1.12F, 1.12F);
        itemRenderer.renderItem(villager, new ItemStack(Items.SHEARS),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, poseStack, collector, light);
        poseStack.popPose();
    }

    private static boolean hasLiveFloat(Villager villager) {
        return !villager.level().getEntitiesOfClass(FishingFloat.class,
                villager.getBoundingBox().inflate(18.0), floatEntity -> floatEntity.owner() == villager).isEmpty();
    }

    private static boolean hasLiveShears(Villager villager) {
        return !villager.level().getEntitiesOfClass(ShearingToolMarker.class,
                villager.getBoundingBox().inflate(18.0), marker -> marker.owner() == villager).isEmpty();
    }

    /**
     * Converts the provider's explicit crossed-arms-local shaft tip to world space using the
     * interpolated villager location and actual body yaw. Entity layers receive a camera-relative
     * PoseStack in 26.2, so the camera is intentionally never used for this physical endpoint.
     */
    static Vec3 ribbitsRodTip(Villager villager, float partialTick) {
        RibbitsFishermanRodProvider.ArmLocalRodTip armLocalTip = RibbitsFishermanRodProvider.armLocalTip();
        if (armLocalTip == null) return null;
        Vec3 position = villager.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, villager.yBodyRotO, villager.yBodyRot);
        FishingRodPose.Point worldTip = FishingRodPose.tipFromCrossedArms(position.x, position.y,
                position.z, bodyYaw, new FishingRodPose.ArmLocalPoint(armLocalTip.x(),
                        armLocalTip.y(), armLocalTip.z()));
        return worldTip.isFinite() ? new Vec3(worldTip.x(), worldTip.y(), worldTip.z()) : null;
    }

    static boolean ribbitsRodProviderAvailable() {
        return RibbitsFishermanRodProvider.isAvailable();
    }
}
