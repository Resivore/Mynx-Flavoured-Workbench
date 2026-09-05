package com.yungnickyoung.minecraft.ribbits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.chute.ChuteEquipment;
import com.yungnickyoung.minecraft.ribbits.client.chute.ChuteClientController;
import com.yungnickyoung.minecraft.ribbits.module.ItemModule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The sole Chute renderer: code-rendered closed pack or open canopy, never Elytra wings. */
public final class ChuteLeafRenderer implements TrinketRenderer {
    private static final ChuteLeafRenderer INSTANCE = new ChuteLeafRenderer();
    private static final net.minecraft.resources.Identifier OPEN_MODEL =
            RibbitsCommon.id("chute_leaf_open");

    private static final net.minecraft.resources.Identifier CLOSED_MODEL =
            RibbitsCommon.id("chute_leaf_closed");

    private ChuteLeafRenderer() {
    }

    public static void register() {
        if (!TrinketRendererRegistry.hasRenderer(ItemModule.CHUTE_LEAF.get())) {
            TrinketRendererRegistry.registerRenderer(ItemModule.CHUTE_LEAF.get(), INSTANCE);
        }
    }

    @Override
    public void submit(
            ItemStack stack,
            TrinketSlotAccess slot,
            EntityModel<? extends LivingEntityRenderState> contextModel,
            PoseStack poseStack,
            SubmitNodeCollector submit,
            int light,
            LivingEntityRenderState renderState,
            float limbAngle,
            float limbDistance
    ) {
        if (!ChuteEquipment.isExactSlot(slot)
                || renderState.isInvisible
                || renderState.isInvisibleToPlayer
                || !(renderState instanceof AvatarRenderState avatar)
                || !(renderState instanceof HumanoidRenderState humanoidState)
                || !(contextModel instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        Entity renderedEntity = Minecraft.getInstance().level == null
                ? null
                : Minecraft.getInstance().level.getEntity(avatar.id);
        if (!(renderedEntity instanceof Player player)) {
            return;
        }

        boolean deployed = ChuteClientController.isRenderedDeployed(player);
        poseStack.pushPose();
        if (deployed) {
            applyDeployedPose(poseStack);
            submitModel(openStack(stack), ItemDisplayContext.NONE, player, avatar.id,
                    poseStack, submit, light, renderState.outlineColor);
        } else {
            TrinketRenderer.translateToChest(poseStack, humanoidModel, humanoidState);
            // Keep the closed leaf just outside armor/cape depth on the player's back.
            poseStack.translate(0.0D, 0.0D, 0.38D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(0.65F, 0.65F, 0.65F);
            submitModel(closedStack(stack), ItemDisplayContext.FIXED, player, avatar.id,
                    poseStack, submit, light, renderState.outlineColor);
        }
        poseStack.popPose();
    }

    @Override
    public void submitFirstPersonRightArm(
            ItemStack stack,
            TrinketSlotAccess slot,
            EntityModel<? extends LivingEntityRenderState> contextModel,
            ModelPart arm,
            PoseStack poseStack,
            SubmitNodeCollector submit,
            int light,
            LocalPlayer player,
            boolean isMainHand
    ) {
        submitFirstPerson(stack, slot, arm, poseStack, submit, light, player,
                isMainHand, HumanoidArm.RIGHT, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
    }

    @Override
    public void submitFirstPersonLeftArm(
            ItemStack stack,
            TrinketSlotAccess slot,
            EntityModel<? extends LivingEntityRenderState> contextModel,
            ModelPart arm,
            PoseStack poseStack,
            SubmitNodeCollector submit,
            int light,
            LocalPlayer player,
            boolean isMainHand
    ) {
        submitFirstPerson(stack, slot, arm, poseStack, submit, light, player,
                isMainHand, HumanoidArm.LEFT, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
    }

    private static void submitFirstPerson(
            ItemStack stack,
            TrinketSlotAccess slot,
            ModelPart arm,
            PoseStack poseStack,
            SubmitNodeCollector submit,
            int light,
            LocalPlayer player,
            boolean isMainHand,
            HumanoidArm armSide,
            ItemDisplayContext displayContext
    ) {
        // Exactly one callback renders: whichever physical arm is the player's main arm.
        if (!isMainHand
                || !ChuteEquipment.isExactSlot(slot)
                || player.isInvisible()
                || !ChuteClientController.isRenderedDeployed(player)) {
            return;
        }

        poseStack.pushPose();
        TrinketRenderer.translateToFirstPersonArm(poseStack, arm, armSide);
        poseStack.translate(armSide == HumanoidArm.RIGHT ? -0.30D : 0.30D, -1.15D, 0.35D);
        poseStack.scale(0.55F, 0.55F, 0.55F);
        submitModel(openStack(stack), displayContext, player, player.getId(),
                poseStack, submit, light, 0);
        poseStack.popPose();
    }

    static void applyDeployedPose(PoseStack poseStack) {
        // Do not inherit animated chest pitch or the donor item-frame Z tilt.
        // Entity model space has Y down; turn item Y up so the canopy stays above its grip.
        poseStack.translate(0.0D, -0.10D, 0.18D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    private static ItemStack closedStack(ItemStack equipped) {
        ItemStack closed = equipped.copy();
        closed.set(DataComponents.ITEM_MODEL, CLOSED_MODEL);
        return closed;
    }

    private static ItemStack openStack(ItemStack equipped) {
        ItemStack open = equipped.copy();
        open.set(DataComponents.ITEM_MODEL, OPEN_MODEL);
        return open;
    }

    private static void submitModel(
            ItemStack stack,
            ItemDisplayContext displayContext,
            Player owner,
            int seed,
            PoseStack poseStack,
            SubmitNodeCollector submit,
            int light,
            int outlineColor
    ) {
        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                itemState,
                stack,
                displayContext,
                owner.level(),
                owner,
                seed
        );
        itemState.submit(poseStack, submit, light, OverlayTexture.NO_OVERLAY, outlineColor);
    }
}
