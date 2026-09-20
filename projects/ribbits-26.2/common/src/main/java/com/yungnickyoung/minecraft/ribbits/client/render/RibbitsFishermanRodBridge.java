package com.yungnickyoung.minecraft.ribbits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.geckolib.util.RenderUtil;
import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Optional client visual provider for integrations that need Ribbits' authored Fisherman rod.
 *
 * <p>The provider deliberately renders only the {@code fishing_rod} bone from the private
 * Fisherman model. {@code fishing_rod_2} and {@code fishing_rod_3} are Ribbits' decorative
 * hanging line and terminal bobber, not part of the reusable rod visual.</p>
 */
public final class RibbitsFishermanRodBridge {
    private static final Identifier MODEL = RibbitsCommon.id("fisherman_ribbit");
    private static final Identifier MODEL_FILE = RibbitsCommon.id("geckolib/models/fisherman_ribbit.geo.json");
    private static final Identifier TEXTURE = RibbitsCommon.id("textures/entity/ribbit.png");
    private static final RodVisual VISUAL = new RodVisual();
    private static final RodRenderer RENDERER = new RodRenderer();

    /*
     * These are VWR C17's already-vetted crossed-arms presentation transforms. Keeping them in
     * this render-only bridge lets an optional caller substitute the authored geometry without
     * taking ownership of it or resetting that presentation.
     */
    private static final float ARM_LOCAL_VERTICAL_TRANSLATION = 0.35F;
    private static final float ARM_LOCAL_DEPTH_TRANSLATION = -0.54F;
    private static final float ROTATE_X_DEGREES = -58.0F;
    private static final float ROTATE_Y_DEGREES = 12.0F;
    private static final float ROTATE_Z_DEGREES = -12.0F;
    private static final float SCALE = 1.28F;

    /*
     * Actual authored shaft endpoint: the fishing_rod bone pivots at (0, 5.5, -5.5), while the
     * shaft's outer face is at z=-15. The bridge resolves the bone from the loaded model and
     * transforms this local point through the same PoseStack used to render it.
     */
    private static final float OUTER_TIP_Z_FROM_ROD_PIVOT = -9.5F / 16.0F;
    private static boolean warnedUnavailable;

    private RibbitsFishermanRodBridge() {
    }

    /**
     * Returns whether the current client resource set can supply the exact authored model and
     * texture. Integrations use this to retain their independent fallback when Ribbits or its
     * private visual payload is unavailable.
     */
    public static boolean isAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResource(MODEL_FILE).isPresent()
                && minecraft.getResourceManager().getResource(TEXTURE).isPresent();
    }

    /**
     * Submits only the authored rod and returns its transformed outer tip in camera-relative
     * coordinates. A {@code null} result means the provider is unavailable and the caller must
     * use its own fallback visual.
     */
    public static Vec3 submit(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!isAvailable()) return null;

        try {
            poseStack.pushPose();
            applyVwrC17Placement(poseStack);
            Vec3 tip = RENDERER.captureOuterTip(poseStack);
            CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState()
                    .levelRenderState.cameraRenderState;
            RENDERER.performRenderPass(VISUAL, null, poseStack, collector, camera, light, 0.0F);
            poseStack.popPose();
            return tip;
        } catch (RuntimeException | LinkageError exception) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                RibbitsCommon.LOGGER.warn("Ribbits Fisherman rod visual provider is unavailable; integrations may fall back", exception);
            }
            return null;
        }
    }

    private static void applyVwrC17Placement(PoseStack poseStack) {
        poseStack.translate(0.0F, ARM_LOCAL_VERTICAL_TRANSLATION, ARM_LOCAL_DEPTH_TRANSLATION);
        poseStack.mulPose(Axis.XP.rotationDegrees(ROTATE_X_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(ROTATE_Y_DEGREES));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATE_Z_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    private static final class RodVisual implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // The bridge intentionally exposes stable geometry, not a second Ribbits animation.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    private static final class RodModel extends GeoModel<RodVisual> {
        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            return TEXTURE;
        }

        @Override
        public Identifier getAnimationResource(RodVisual visual) {
            return RibbitsCommon.id("ribbit");
        }
    }

    private static final class RodRenderer extends GeoObjectRenderer<RodVisual, Void, GeoRenderState> {
        private RodRenderer() {
            super(new RodModel());
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderInfo) {
            applyGeometryRoot(renderInfo.poseStack());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderInfo, BoneSnapshots bones) {
            bones.ifPresent("body", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("fishing_rod_2", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("fishing_rod_3", snapshot -> snapshot.skipRender(true));
        }

        private Vec3 captureOuterTip(PoseStack poseStack) {
            GeoBone rod = getGeoModel().getBakedModel(MODEL).getBone("fishing_rod")
                    .orElseThrow(() -> new IllegalStateException("Ribbits Fisherman rod bone is missing"));
            poseStack.pushPose();
            applyGeometryRoot(poseStack);
            RenderUtil.transformToBone(poseStack, rod);
            Vector4f tip = poseStack.last().pose().transform(
                    new Vector4f(0.0F, 0.0F, OUTER_TIP_Z_FROM_ROD_PIVOT, 1.0F));
            poseStack.popPose();
            return new Vec3(tip.x(), tip.y(), tip.z());
        }

        private static void applyGeometryRoot(PoseStack poseStack) {
            // GeoObjectRenderer's normal object root; shared by rendering and outer-tip capture.
            poseStack.translate(0.5F, 0.51F, 0.5F);
        }
    }
}
