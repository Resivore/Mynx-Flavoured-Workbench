package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import dev.resivore.villagerwork.FrogVillagerRodPose;
import dev.resivore.villagerwork.ShearingToolMarker;
import net.minecraft.world.item.Items;

/**
 * A single client-only Fisherman prop posed at crossed villager arms while this villager owns a
 * live VWR float. VWR directly consumes the installed private Ribbits C27 model/resources but
 * owns its Frog Villager reference pose and sole live line/float. It does not synchronize an
 * equipment item, avoiding vanilla's duplicate fishing line and hook.
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
            FrogVillagerRodPose.applyReferenceGrip(poseStack);
            RibbitsFishermanRodRenderer.submit(poseStack, collector, light);
            poseStack.popPose();
        }
        if (shearing) {
            poseStack.pushPose();
            getParentModel().translateToArms(state, poseStack);
            submitShears(villager, poseStack, collector, light);
            poseStack.popPose();
        }
    }

    private void submitShears(Villager villager, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.02F, -0.30F);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-58.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(10.0F));
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
     * Converts the reference's physical outer shaft tip to world space through the exact same
     * folded-arms/reference chain that the visible rod receives. Entity layers get a
     * camera-relative PoseStack in 26.2, so camera state is intentionally never an input.
     */
    static Vec3 ribbitsRodTip(Villager villager, float partialTick) {
        Vec3 position = villager.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, villager.yBodyRotO, villager.yBodyRot);
        FrogVillagerRodPose.Point worldTip = FrogVillagerRodPose.outerShaftTip(position.x, position.y,
                position.z, bodyYaw);
        return worldTip.isFinite() ? new Vec3(worldTip.x(), worldTip.y(), worldTip.z()) : null;
    }
}
