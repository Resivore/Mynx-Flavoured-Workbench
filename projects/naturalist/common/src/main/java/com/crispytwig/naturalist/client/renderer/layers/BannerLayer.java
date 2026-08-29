package com.crispytwig.naturalist.client.renderer.layers;

import com.crispytwig.naturalist.client.model.NaturalistEntityModel;
import com.crispytwig.naturalist.client.renderer.NaturalistRenderState;
import com.crispytwig.naturalist.server.entity.mob.Elephant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

@Environment(EnvType.CLIENT)
public class BannerLayer extends RenderLayer<NaturalistRenderState<Elephant>, NaturalistEntityModel<Elephant>> {
    private final SpriteGetter sprites;
    private final BannerFlagModel flag;
    private final Model.Simple bar;
    private final BannerFlagModel mirroredFlag;
    private final Model.Simple mirroredBar;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float scale;

    public BannerLayer(
            RenderLayerParent<NaturalistRenderState<Elephant>, NaturalistEntityModel<Elephant>> parent,
            EntityRendererProvider.Context context,
            float offsetX,
            float offsetY,
            float offsetZ,
            float scale
    ) {
        super(parent);
        this.sprites = context.getSprites();
        BannerParts normal = bakeBanner(false);
        BannerParts mirrored = bakeBanner(true);
        this.flag = normal.flag();
        this.bar = normal.bar();
        this.mirroredFlag = mirrored.flag();
        this.mirroredBar = mirrored.bar();
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.scale = scale;
    }

    private static BannerParts bakeBanner(boolean mirrored) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder flagBuilder = CubeListBuilder.create().texOffs(0, 0);
        CubeListBuilder barBuilder = CubeListBuilder.create().texOffs(0, 42);
        if (mirrored) {
            flagBuilder.mirror();
            barBuilder.mirror();
        }
        root.addOrReplaceChild(
                "flag", flagBuilder.addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild(
                "bar", barBuilder.addBox(-10.0F, -32.0F, -1.0F, 20.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 32.0F, 0.0F));

        ModelPart bakedRoot = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        return new BannerParts(
                new BannerFlagModel(bakedRoot.getChild("flag")),
                new Model.Simple(bakedRoot.getChild("bar"), RenderTypes::entitySolid));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       NaturalistRenderState<Elephant> state, float yRot, float xRot) {
        Elephant entity = state.entity;
        if (state.isBaby || state.isInvisible) {
            return;
        }
        ItemStack stack = entity.getBanner();
        if (!(stack.getItem() instanceof BannerItem bannerItem)) {
            return;
        }

        DyeColor color = bannerItem.getColor();
        BannerPatternLayers patterns = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        float phase = (Mth.positiveModulo(entity.tickCount + entity.getId() * 13, 100) + state.partialTick) / 100.0F;
        float sway = (-0.0125F + 0.01F * Mth.cos(Mth.TWO_PI * phase)) * Mth.PI;
        float tilt = Mth.lerp(state.partialTick, entity.bannerSwingO, entity.bannerSwing) * Mth.DEG_TO_RAD + entity.getRenderRoll();
        float lift = Mth.lerp(state.partialTick, entity.bannerLiftO, entity.bannerLift) * Mth.DEG_TO_RAD;

        ModelPart root = this.getParentModel().root();
        poseStack.pushPose();
        root.translateAndRotate(poseStack);
        root.getChild("body").translateAndRotate(poseStack);
        for (int side = 1; side >= -1; side -= 2) {
            poseStack.pushPose();
            poseStack.translate(side * this.offsetX / 16.0F, this.offsetY / 16.0F, this.offsetZ / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * side));
            poseStack.scale(this.scale, this.scale, this.scale);

            BannerFlagModel sideFlag = side > 0 ? this.flag : this.mirroredFlag;
            Model.Simple sideBar = side > 0 ? this.bar : this.mirroredBar;
            BannerPose sidePose = new BannerPose(Math.min(sway - side * tilt, 0.0F), side * lift);
            submitNodeCollector.submitModel(
                    sideBar, Unit.INSTANCE, poseStack, packedLight, OverlayTexture.NO_OVERLAY, -1,
                    Sheets.BANNER_BASE, this.sprites, state.outlineColor, null);
            submitNodeCollector.submitModel(
                    sideFlag, sidePose, poseStack, packedLight, OverlayTexture.NO_OVERLAY, -1,
                    Sheets.BANNER_BASE, this.sprites, state.outlineColor, null);
            BannerRenderer.submitPatterns(
                    this.sprites, poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY,
                    sideFlag, sidePose, true, color, patterns, null);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private record BannerParts(BannerFlagModel flag, Model.Simple bar) {
    }

    private record BannerPose(float xRot, float zRot) {
    }

    private static final class BannerFlagModel extends Model<BannerPose> {
        private BannerFlagModel(ModelPart flag) {
            super(flag, RenderTypes::entitySolid);
        }

        @Override
        public void setupAnim(BannerPose pose) {
            super.setupAnim(pose);
            this.root().xRot = pose.xRot();
            this.root().zRot = pose.zRot();
        }
    }
}
