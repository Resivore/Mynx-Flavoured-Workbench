package dev.resivore.villagerwork.client;

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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * VWR's direct C27 consumer for the installed Ribbits Fisherman geometry and texture.
 *
 * <p>The raw {@code fishing_rod} shaft/reel/detail is rendered beneath the caller's Frog
 * Villager folded arms. The source bone's Ribbit-body-specific -50 degree pose is intentionally
 * neutralized: the user-authoritative CEM supplies the visible angle through its real
 * {@code arms_rotation} hierarchy, not through a new guessed PoseStack correction.</p>
 */
final class RibbitsFishermanRodRenderer {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("ribbits", "fisherman_ribbit");
    private static final Identifier MODEL_FILE = Identifier.fromNamespaceAndPath("ribbits",
            "geckolib/models/fisherman_ribbit.geo.json");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ribbits",
            "textures/entity/ribbit.png");
    private static final RodVisual VISUAL = new RodVisual();
    private static final RodRenderer RENDERER = new RodRenderer();

    private RibbitsFishermanRodRenderer() {
    }

    static boolean submit(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!hasInstalledC27Resources()) return false;
        try {
            RENDERER.renderAtReferenceGrip(poseStack, collector, light);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    static boolean hasInstalledC27Resources() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResource(MODEL_FILE).isPresent()
                && minecraft.getResourceManager().getResource(TEXTURE).isPresent();
    }

    private static final class RodVisual implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // This presentation is static; VWR owns the cast lifecycle and line.
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
            return Identifier.fromNamespaceAndPath("ribbits", "ribbit");
        }
    }

    private static final class RodRenderer extends GeoObjectRenderer<RodVisual, Void, GeoRenderState> {
        private RodRenderer() {
            super(new RodModel());
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderInfo) {
            applyRibbitsGeometryRoot(renderInfo.poseStack());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderInfo,
                                              BoneSnapshots bones) {
            bones.ifPresent("main", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("body", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("fishing_rod", snapshot -> {
                // The reference has no rod-group rotation: preserve the source cube/detail data,
                // while leaving its Ribbit-body-specific bone pose out of the Frog Villager pose.
                snapshot.setRotation(0.0F, 0.0F, 0.0F);
            });
            bones.ifPresent("fishing_rod_2", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
            bones.ifPresent("fishing_rod_3", snapshot -> snapshot.skipRender(true));
        }

        private void renderAtReferenceGrip(PoseStack poseStack, SubmitNodeCollector collector, int light) {
            StandaloneRodBasis basis = standaloneBasis();
            poseStack.pushPose();
            try {
                basis.attachGripAtOrigin(poseStack);
                performRenderPass(VISUAL, null, poseStack, collector,
                        Minecraft.getInstance().gameRenderer.gameRenderState()
                                .levelRenderState.cameraRenderState, light, 0.0F);
            } finally {
                poseStack.popPose();
            }
        }

        /**
         * Detaches the Ribbits body/accessories origin while retaining raw fishing_rod cubes.
         * The physical outer tip is then exactly the C27 shaft's -9.5-pixel vector from the
         * shared reference grip.
         */
        private StandaloneRodBasis standaloneBasis() {
            GeoBone rod = getGeoModel().getBakedModel(MODEL).getBone("fishing_rod")
                    .orElseThrow(() -> new IllegalStateException("Ribbits C27 Fisherman rod is missing"));
            PoseStack modelPose = new PoseStack();
            applyRibbitsGeometryRoot(modelPose);
            RenderUtil.transformToBone(modelPose, rod);
            Vec3 grip = transform(modelPose, 0.0F, 0.0F, 0.0F);
            return new StandaloneRodBasis(grip);
        }

        private static Vec3 transform(PoseStack poseStack, float x, float y, float z) {
            org.joml.Vector4f transformed = poseStack.last().pose().transform(
                    new org.joml.Vector4f(x, y, z, 1.0F));
            return new Vec3(transformed.x(), transformed.y(), transformed.z());
        }

        private static void applyRibbitsGeometryRoot(PoseStack poseStack) {
            // The stock GeoObjectRenderer root, shared by rendered geometry and grip rebasing.
            poseStack.translate(0.5F, 0.51F, 0.5F);
        }
    }

    /** The Ribbits body origin is cancelled before the reference's folded-arm grip is applied. */
    private record StandaloneRodBasis(Vec3 rawGripPosition) {
        private void attachGripAtOrigin(PoseStack poseStack) {
            poseStack.translate(-rawGripPosition.x, -rawGripPosition.y, -rawGripPosition.z);
        }
    }
}
