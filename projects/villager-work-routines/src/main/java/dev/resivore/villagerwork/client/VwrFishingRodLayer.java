package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final Map<Integer, RodTip> RIBBITS_ROD_TIPS = new ConcurrentHashMap<>();
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
        if (!fishing && !shearing) {
            RIBBITS_ROD_TIPS.remove(villager.getId());
            return;
        }

        if (fishing) {
            poseStack.pushPose();
            // Apply C17's physical inward correction in the unrotated body basis.  Applying it
            // after translateToArms would use the pitched arms basis and repeat C16's downward
            // local-Z result instead of moving the rod toward the torso.
            FishingRodPose.BodySpaceOffset inward = FishingRodPose.C17_INWARD_BODY_OFFSET;
            poseStack.translate(inward.x(), inward.y(), inward.z());
            getParentModel().translateToArms(state, poseStack);
            submitFishingRod(villager, poseStack, collector, light);
            poseStack.popPose();
        }
        if (shearing) {
            poseStack.pushPose();
            getParentModel().translateToArms(state, poseStack);
            submitShears(villager, poseStack, collector, light);
            poseStack.popPose();
        }
    }

    private void submitFishingRod(Villager villager, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Vec3 cameraRelativeTip = RibbitsFishermanRodProvider.submit(poseStack, collector, light);
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.gameRenderState()
                .levelRenderState.cameraRenderState.pos;
        if (cameraRelativeTip != null && cameraPosition != null) {
            RIBBITS_ROD_TIPS.put(villager.getId(), new RodTip(cameraRelativeTip.add(cameraPosition), villager.tickCount));
            return;
        }

        RIBBITS_ROD_TIPS.remove(villager.getId());
        submitFishingStick(villager, poseStack, collector, light);
    }

    private void submitFishingStick(Villager villager, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
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

    /** Returns this frame's exact camera-to-world rod tip when the optional provider rendered it. */
    static Vec3 ribbitsRodTip(Villager villager) {
        RodTip tip = RIBBITS_ROD_TIPS.get(villager.getId());
        if (tip == null || Math.abs(villager.tickCount - tip.ownerTick()) > 1) return null;
        return tip.worldPosition();
    }

    static boolean ribbitsRodProviderAvailable() {
        return RibbitsFishermanRodProvider.isAvailable();
    }

    private record RodTip(Vec3 worldPosition, int ownerTick) {
    }
}
