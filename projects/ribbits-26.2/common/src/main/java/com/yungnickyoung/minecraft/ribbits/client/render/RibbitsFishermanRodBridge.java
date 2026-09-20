package com.yungnickyoung.minecraft.ribbits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Optional client visual provider for integrations that need Ribbits' authored Fisherman rod.
 *
 * <p>The provider deliberately renders only the {@code fishing_rod} bone from the private
 * Fisherman model. {@code fishing_rod_2} and {@code fishing_rod_3} are Ribbits' decorative
 * hanging line and terminal bobber, not part of the reusable rod visual. The bridge deliberately
 * rebases the authored shaft on its own grip instead of inheriting the Ribbit body's accessories
 * placement.</p>
 */
public final class RibbitsFishermanRodBridge {
    private static final Identifier MODEL = RibbitsCommon.id("fisherman_ribbit");
    private static final Identifier MODEL_FILE = RibbitsCommon.id("geckolib/models/fisherman_ribbit.geo.json");
    private static final Identifier TEXTURE = RibbitsCommon.id("textures/entity/ribbit.png");
    private static final RodVisual VISUAL = new RodVisual();
    private static final RodRenderer RENDERER = new RodRenderer();

    /*
     * Actual authored shaft endpoint: the fishing_rod bone pivots at (0, 5.5, -5.5), while its
     * outer face is at z=-15. The bone pivot is the reusable logical grip; the shaft outer face
     * is therefore -9.5 pixels on its local Z axis. Both coordinates are derived from the
     * Fisherman geometry, not from its body/accessories hierarchy or an item-display transform.
     */
    private static final RodLocalPoint GRIP_FROM_ROD_PIVOT = new RodLocalPoint(0.0F, 0.0F, 0.0F);
    private static final RodLocalPoint OUTER_TIP_FROM_ROD_PIVOT =
            new RodLocalPoint(0.0F, 0.0F, -9.5F / 16.0F);
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
     * Submits only the authored rod at the caller's crossed-arm grip and returns its explicit
     * arm-local outer-tip offset. The return value is never world or camera-relative space.
     * A {@code null} result means the provider is unavailable and the caller must use its own
     * fallback visual.
     */
    public static ArmLocalRodTip submit(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!isAvailable()) return null;

        try {
            StandaloneRodBasis basis = RENDERER.standaloneBasis();
            poseStack.pushPose();
            try {
                basis.attachGripAtOrigin(poseStack);
                RENDERER.performRenderPass(VISUAL, null, poseStack, collector,
                        Minecraft.getInstance().gameRenderer.gameRenderState()
                                .levelRenderState.cameraRenderState, light, 0.0F);
            } finally {
                poseStack.popPose();
            }
            return basis.tipFromArmGrip();
        } catch (RuntimeException | LinkageError exception) {
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                RibbitsCommon.LOGGER.warn("Ribbits Fisherman rod visual provider is unavailable; integrations may fall back", exception);
            }
            return null;
        }
    }

    /**
     * Returns the physical shaft tip from the same standalone grip-relative basis used by
     * {@link #submit(PoseStack, SubmitNodeCollector, int)}. Integrators convert this explicit
     * arm-local coordinate through their own entity transform; no camera position is involved.
     */
    public static ArmLocalRodTip armLocalTip() {
        if (!isAvailable()) return null;
        try {
            return RENDERER.standaloneBasis().tipFromArmGrip();
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    /** A physical outer shaft point relative to the crossed-arms grip, in model/arm-local space. */
    public record ArmLocalRodTip(float x, float y, float z) {
        public boolean isFinite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
        }
    }

    /** A point in the authored {@code fishing_rod} bone's own pivot-relative coordinate system. */
    private record RodLocalPoint(float x, float y, float z) {
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
            bones.ifPresent("main", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("body", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("fishing_rod_2", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
            bones.ifPresent("fishing_rod_3", snapshot -> snapshot.skipRender(true));
        }

        /**
         * Establishes the reusable rod basis directly from the loaded geometry. The grip is the
         * fishing_rod pivot; shifting by its rendered position detaches the shaft from the
         * Ribbit-only body/accessories origin before it is attached to another entity's hands.
         */
        private StandaloneRodBasis standaloneBasis() {
            GeoBone rod = getGeoModel().getBakedModel(MODEL).getBone("fishing_rod")
                    .orElseThrow(() -> new IllegalStateException("Ribbits Fisherman rod bone is missing"));
            PoseStack modelPose = new PoseStack();
            applyGeometryRoot(modelPose);
            RenderUtil.transformToBone(modelPose, rod);
            Vec3 grip = transform(modelPose, GRIP_FROM_ROD_PIVOT);
            Vec3 tip = transform(modelPose, OUTER_TIP_FROM_ROD_PIVOT);
            return new StandaloneRodBasis(grip, new ArmLocalRodTip(
                    (float)(tip.x - grip.x), (float)(tip.y - grip.y), (float)(tip.z - grip.z)));
        }

        private static Vec3 transform(PoseStack poseStack, RodLocalPoint point) {
            Vector4f transformed = poseStack.last().pose().transform(
                    new Vector4f(point.x(), point.y(), point.z(), 1.0F));
            return new Vec3(transformed.x(), transformed.y(), transformed.z());
        }

        private static void applyGeometryRoot(PoseStack poseStack) {
            // GeoObjectRenderer's normal object root; shared by rendering and outer-tip capture.
            poseStack.translate(0.5F, 0.51F, 0.5F);
        }
    }

    /** The complete standalone basis shared by visible rod submission and line-origin extraction. */
    private record StandaloneRodBasis(Vec3 rawGripPosition, ArmLocalRodTip tipFromArmGrip) {
        private void attachGripAtOrigin(PoseStack poseStack) {
            poseStack.translate(-rawGripPosition.x, -rawGripPosition.y, -rawGripPosition.z);
        }
    }
}
