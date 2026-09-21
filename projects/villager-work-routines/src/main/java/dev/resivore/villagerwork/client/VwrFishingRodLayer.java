package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.FishingFloat;
import dev.resivore.villagerwork.FrogVillagerRodPose;
import dev.resivore.villagerwork.ShearingToolMarker;
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
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

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
        FishingFloat fishingFloat = liveFloat(villager);
        boolean fishing = fishingFloat != null;
        boolean shearing = hasLiveShears(villager);
        if (!fishing && !shearing) return;

        if (fishing) {
            poseStack.pushPose();
            try {
                Matrix4f incomingEntityLayer = new Matrix4f(poseStack.last().pose());
                getParentModel().translateToArms(state, poseStack);
                Matrix4f afterTranslateToArms = new Matrix4f(poseStack.last().pose());
                FrogVillagerRodPose.applyReferenceGrip(poseStack);
                Matrix4f afterAuthoredGrip = new Matrix4f(poseStack.last().pose());

                // These observations only copy the matrices used by the restored C20 path. They
                // never select, gate, or alter rod submission or C20's independent line endpoint.
                RibbitsFishermanRodRenderer.Inspection inspection =
                        RibbitsFishermanRodRenderer.submit(poseStack, collector, light);
                try {
                    VwrRodDiagnostics.observeRenderPath(villager, fishingFloat.getId(), getParentModel(),
                            inspection.submitted(), incomingEntityLayer, afterTranslateToArms,
                            afterAuthoredGrip, inspection);
                    RodDiagnosticMarkers.submit(collector, incomingEntityLayer, afterTranslateToArms,
                            afterAuthoredGrip, inspection);
                } catch (RuntimeException | LinkageError ignored) {
                    // Diagnostic geometry/logging is never allowed to suppress the C20 rod.
                }
            } finally {
                poseStack.popPose();
            }
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

    private static FishingFloat liveFloat(Villager villager) {
        return villager.level().getEntitiesOfClass(FishingFloat.class,
                villager.getBoundingBox().inflate(18.0), floatEntity -> floatEntity.owner() == villager)
                .stream().findFirst().orElse(null);
    }

    private static boolean hasLiveShears(Villager villager) {
        return !villager.level().getEntitiesOfClass(ShearingToolMarker.class,
                villager.getBoundingBox().inflate(18.0), marker -> marker.owner() == villager).isEmpty();
    }

    /**
     * Restores C20's analytical outer-tip calculation unchanged. It intentionally does not use
     * the live layer/GeoLib matrices; C22 logs both endpoints so a later canary can correct the
     * mismatch from runtime evidence rather than hiding it here.
     */
    static Vec3 ribbitsRodTip(Villager villager, float partialTick) {
        Vec3 position = villager.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, villager.yBodyRotO, villager.yBodyRot);
        FrogVillagerRodPose.Point worldTip = FrogVillagerRodPose.outerShaftTip(position.x, position.y,
                position.z, bodyYaw);
        return worldTip.isFinite() ? new Vec3(worldTip.x(), worldTip.y(), worldTip.z()) : null;
    }

}
