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
                FoldedArmRenderPath.Attachment attachment = FoldedArmRenderPath.apply(
                        getParentModel(), state, poseStack);
                if (attachment.applied()) {
                    FrogVillagerRodPose.applyReferenceGrip(poseStack);
                    Matrix4f afterAuthoredGrip = new Matrix4f(poseStack.last().pose());

                    RibbitsFishermanRodRenderer.Inspection inspection =
                            RibbitsFishermanRodRenderer.submit(poseStack, collector, light);
                    FishingFloatRenderer.LineSubmission line = null;
                    if (inspection.submitted() && inspection.exactLiveCapture()
                            && inspection.physicalOuterTip() != null) {
                        try {
                            line = FishingFloatRenderer.submitLineFromPhysicalRod(
                                    inspection.physicalOuterTip(), collector,
                                    fishingFloat.getPosition(presentation.villagerWork$partialTick()));
                        } catch (RuntimeException | LinkageError ignored) {
                            // Never substitute an analytical or duplicate line after a live-tip failure.
                        }
                    }
                    try {
                        VwrRodDiagnostics.observeRenderPath(villager, fishingFloat.getId(),
                                getParentModel(), inspection.submitted(), incomingEntityLayer,
                                attachment, afterAuthoredGrip, inspection, line);
                        RodDiagnosticMarkers.submit(collector, incomingEntityLayer,
                                attachment.afterTranslateToArms(),
                                attachment.afterEffectiveFoldedArms(), afterAuthoredGrip, inspection);
                    } catch (RuntimeException | LinkageError ignored) {
                        // Diagnostic geometry/logging is never allowed to suppress the C24 rod/line.
                    }
                } else {
                    try {
                        VwrRodDiagnostics.observeAttachmentPath(getParentModel(), attachment);
                    } catch (RuntimeException | LinkageError ignored) {
                        // A diagnostic failure must not affect any other render layer.
                    }
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

}
