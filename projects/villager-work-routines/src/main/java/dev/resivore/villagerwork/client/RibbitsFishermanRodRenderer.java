package dev.resivore.villagerwork.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.FrogVillagerRodPose;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * VWR's asset-contract consumer for the installed Ribbits Fisherman geometry and texture.
 *
 * <p>The raw {@code fishing_rod} shaft/reel/detail is rendered beneath the caller's Frog
 * Villager folded arms. The source bone's Ribbit-body pose is neutralized from its own live baked
 * values so the unrotated authored rod group inherits only the effective folded-arm transform.
 * No guessed PoseStack correction is added. The captured physical tip from the exact submitted
 * rod transform is also VWR's sole fishing-line endpoint authority.</p>
 */
final class RibbitsFishermanRodRenderer {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("ribbits", "fisherman_ribbit");
    private static final Identifier MODEL_FILE = Identifier.fromNamespaceAndPath("ribbits",
            "geckolib/models/fisherman_ribbit.geo.json");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ribbits",
            "textures/entity/ribbit.png");
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("villager_work_routines/ribbits");
    private static String lastFailure;
    private static final RodVisual VISUAL = new RodVisual();
    private static final RodRenderer RENDERER = new RodRenderer();

    private RibbitsFishermanRodRenderer() {
    }

    /** Submits C33's solid rod: C31's retained pose plus C32's exact shaft-axis step twice. */
    static Inspection submitC33ShaftTranslated(PoseStack poseStack, SubmitNodeCollector collector,
                                                int light) {
        Inspection inspection;
        try {
            inspection = RENDERER.inspect();
        } catch (RuntimeException | LinkageError error) {
            inspection = Inspection.probeFailure(error);
        }
        if (inspection.failure() != null) return diagnose(inspection);
        try {
            return diagnose(RENDERER.renderAtReferenceGrip(poseStack, collector, light, inspection, VISUAL,
                    FrogVillagerRodPose.C33_SHAFT_TRANSLATION_APPLICATIONS));
        } catch (RuntimeException | LinkageError error) {
            return diagnose(inspection.withSubmission(false,
                    error.getClass().getName() + ": " + String.valueOf(error.getMessage())));
        }
    }

    private static Inspection diagnose(Inspection inspection) {
        if (inspection.failure() != null && !inspection.failure().equals(lastFailure)) {
            lastFailure = inspection.failure();
            LOGGER.warn("VWR Ribbits rod/line presentation unavailable: {}. {}. Restore compatible "
                    + "Fisherman assets or remove the overriding resource pack, then reload resources; "
                    + "if intentional, update VWR's asset contract. Server work routines and Ribbits "
                    + "entities are unaffected.", inspection.failure(), inspection.resourceReport());
        } else if (inspection.failure() == null) {
            lastFailure = null;
        }
        return inspection;
    }

    static void invalidateResources() {
        RENDERER.cachedInspection = null;
        RENDERER.cachedLiveBoneChain = null;
        lastFailure = null;
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

    private static class RodRenderer extends GeoObjectRenderer<RodVisual, Void, GeoRenderState> {
        private LiveCapture liveCapture;
        private Object inspectedResourceManager;
        private BakedGeoModel inspectedBakedModel;
        private Inspection cachedInspection;
        private String cachedLiveBoneChain;

        protected RodRenderer() {
            super(new RodModel());
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderInfo) {
            applyRibbitsGeometryRoot(renderInfo.poseStack());
            if (liveCapture != null) {
                try {
                    liveCapture.captureGeometryRoot(renderInfo.poseStack());
                } catch (RuntimeException | LinkageError error) {
                    liveCapture.captureFailure(error);
                }
            }
        }

        @Override
        public void preRenderPass(RenderPassInfo<GeoRenderState> renderInfo,
                                  SubmitNodeCollector collector) {
            if (liveCapture == null) return;
            try {
                renderInfo.model().getBone("fishing_rod").ifPresent(rod ->
                        renderInfo.addPerBoneRender(rod, (info, bone, nodes) -> {
                            try {
                                liveCapture.capturePhysicalRod(info.poseStack(), bone,
                                        cachedLiveBoneChain);
                                if (cachedLiveBoneChain == null) {
                                    cachedLiveBoneChain = liveCapture.liveBoneChain;
                                }
                            } catch (RuntimeException | LinkageError error) {
                                liveCapture.captureFailure(error);
                            }
                        }));
            } catch (RuntimeException | LinkageError error) {
                liveCapture.captureFailure(error);
            }
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderInfo,
                                              BoneSnapshots bones) {
            applyReferenceBoneAdjustments(bones);
        }

        private static void applyReferenceBoneAdjustments(BoneSnapshots bones) {
            bones.ifPresent("main", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("body", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_arm", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("right_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("left_leg", snapshot -> snapshot.skipRender(true));
            bones.ifPresent("fishing_rod", snapshot -> {
                // GeckoLib adds the snapshot delta to these live baked values. Cancel that
                // Ribbit-body-specific pose without inventing a literal replacement rotation;
                // the reference rod group itself has no authored rotation.
                GeoBone bone = snapshot.getBone();
                snapshot.setRotation(-bone.baseRotX(), -bone.baseRotY(), -bone.baseRotZ());
            });
            bones.ifPresent("fishing_rod_2", snapshot -> {
                snapshot.skipRender(true);
                snapshot.skipChildrenRender(true);
            });
            bones.ifPresent("fishing_rod_3", snapshot -> snapshot.skipRender(true));
        }

        protected Inspection renderAtReferenceGrip(PoseStack poseStack, SubmitNodeCollector collector,
                                                    int light, Inspection inspection) {
            return renderAtReferenceGrip(poseStack, collector, light, inspection, VISUAL, 0);
        }

        protected Inspection renderAtReferenceGrip(PoseStack poseStack, SubmitNodeCollector collector,
                                                    int light, Inspection inspection, RodVisual visual,
                                                    int c32ShaftTranslationApplications) {
            StandaloneRodBasis basis = standaloneBasis();
            liveCapture = new LiveCapture(inspection, basis.rawGripPosition());
            poseStack.pushPose();
            try {
                basis.attachGripAtOrigin(poseStack);
                for (int application = 0; application < c32ShaftTranslationApplications; application++) {
                    // This is after the immutable C31 attachment pose and grip rebase, but before
                    // every C27 cube and the authoritative physical-tip capture share the pass.
                    FrogVillagerRodPose.applyC32ShaftAxisTranslation(poseStack);
                }
                performRenderPass(visual, null, poseStack, collector,
                        Minecraft.getInstance().gameRenderer.gameRenderState()
                                .levelRenderState.cameraRenderState, light, 0.0F);
                return liveCapture.finish();
            } finally {
                poseStack.popPose();
                liveCapture = null;
            }
        }

        protected Inspection inspect() {
            Minecraft minecraft = Minecraft.getInstance();
            Object resourceManager = minecraft.getResourceManager();
            String ribbitsVersion = FabricLoader.getInstance().getModContainer("ribbits")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("<missing>");
            BakedGeoModel bakedModel = null;
            try {
                bakedModel = getGeoModel().getBakedModel(MODEL);
                if (resourceManager == inspectedResourceManager && bakedModel == inspectedBakedModel
                        && cachedInspection != null) return cachedInspection;
                cachedLiveBoneChain = null;
                Optional<Resource> modelResource = minecraft.getResourceManager().getResource(MODEL_FILE);
                Optional<Resource> textureResource = minecraft.getResourceManager().getResource(TEXTURE);
                RibbitsRodContract.Result contract = RibbitsRodContract.check(
                        readResource(modelResource), readResource(textureResource));
                String report = "installed_version=" + ribbitsVersion + " (informational only)"
                        + " model=" + MODEL_FILE + " source_pack="
                        + modelResource.map(Resource::sourcePackId).orElse("<missing>")
                        + " texture=" + TEXTURE + " source_pack="
                        + textureResource.map(Resource::sourcePackId).orElse("<missing>")
                        + " contract=" + contract.detail();
                String signature = report;
                if (!contract.compatible()) {
                    return cacheInspection(resourceManager, bakedModel,
                            Inspection.unavailable(signature, report, contract.detail()));
                }
                Optional<GeoBone> rodResult = bakedModel.getBone("fishing_rod");
                if (bakedModel.isMissingno() || rodResult.isEmpty()
                        || bakedModel.getBone("fishing_rod_2").isEmpty()
                        || bakedModel.getBone("fishing_rod_3").isEmpty()) {
                    return cacheInspection(resourceManager, bakedModel, Inspection.unavailable(
                            signature, report, "GeckoLib did not bake the contracted Fisherman bones"));
                }
                StandaloneRodBasis basis = standaloneBasis();
                return cacheInspection(resourceManager, bakedModel,
                        new Inspection(signature, report + "\n" + describeBoneChain(rodResult.get(), false),
                                null, null, null, null, null,
                                basis.rawGripPosition(), false, false, null));
            } catch (IOException | RuntimeException | LinkageError error) {
                String failure = error.getClass().getName() + ": " + String.valueOf(error.getMessage());
                return cacheInspection(resourceManager, bakedModel, Inspection.unavailable(
                        failure, "installed_version=" + ribbitsVersion + " model=" + MODEL_FILE
                                + " texture=" + TEXTURE, failure));
            }
        }

        private static byte[] readResource(Optional<Resource> resource) throws IOException {
            if (resource.isEmpty()) return null;
            try (InputStream input = resource.get().open()) {
                return input.readAllBytes();
            }
        }

        private Inspection cacheInspection(Object resourceManager, BakedGeoModel bakedModel,
                                             Inspection inspection) {
            inspectedResourceManager = resourceManager;
            inspectedBakedModel = bakedModel;
            cachedInspection = inspection;
            return inspection;
        }

        private static String describeBoneChain(GeoBone rod, boolean useLiveSnapshots) {
            List<GeoBone> chain = new ArrayList<>();
            for (GeoBone current = rod; current != null; current = current.parent()) chain.add(current);
            Collections.reverse(chain);

            StringJoiner names = new StringJoiner(" -> ");
            StringBuilder details = new StringBuilder();
            for (GeoBone bone : chain) {
                names.add(bone.name());
                BoneSnapshot snapshot = useLiveSnapshots ? bone.frameSnapshot : null;
                float translateX = snapshot == null ? 0.0F : snapshot.getTranslateX();
                float translateY = snapshot == null ? 0.0F : snapshot.getTranslateY();
                float translateZ = snapshot == null ? 0.0F : snapshot.getTranslateZ();
                float deltaRotX = snapshot == null ? 0.0F : snapshot.getRotX();
                float deltaRotY = snapshot == null ? 0.0F : snapshot.getRotY();
                float deltaRotZ = snapshot == null ? 0.0F : snapshot.getRotZ();
                float scaleX = snapshot == null ? 1.0F : snapshot.getScaleX();
                float scaleY = snapshot == null ? 1.0F : snapshot.getScaleY();
                float scaleZ = snapshot == null ? 1.0F : snapshot.getScaleZ();
                String snapshotSource = useLiveSnapshots
                        ? (snapshot == null ? "absent_identity" : "live") : "static_identity_preview";
                details.append(String.format(Locale.ROOT,
                        "\n    %s pivot_px=(%.6f, %.6f, %.6f) base_rotation_rad=(%.6f, %.6f, %.6f) "
                                + "base_rotation_deg=(%.3f, %.3f, %.3f) snapshot=%s "
                                + "render_delta_translation_px=(%.6f, %.6f, %.6f) "
                                + "render_delta_rotation_rad=(%.6f, %.6f, %.6f) scale=(%.6f, %.6f, %.6f) "
                                + "effective_rotation_rad=(%.6f, %.6f, %.6f)",
                        bone.name(), bone.pivotX(), bone.pivotY(), bone.pivotZ(),
                        bone.baseRotX(), bone.baseRotY(), bone.baseRotZ(),
                        Math.toDegrees(bone.baseRotX()), Math.toDegrees(bone.baseRotY()),
                        Math.toDegrees(bone.baseRotZ()), snapshotSource,
                        translateX, translateY, translateZ,
                        deltaRotX, deltaRotY, deltaRotZ,
                        scaleX, scaleY, scaleZ,
                        bone.baseRotX() + deltaRotX,
                        bone.baseRotY() + deltaRotY,
                        bone.baseRotZ() + deltaRotZ));
            }
            return "  root_to_fishing_rod=" + names + details;
        }

        /**
         * Detaches the Ribbits body/accessories origin while retaining raw fishing_rod cubes.
         * The physical outer tip is then exactly the C27 shaft's -9.5-pixel vector from the
         * shared reference grip.
         */
        private StandaloneRodBasis standaloneBasis() {
            GeoBone rod = getGeoModel().getBakedModel(MODEL).getBone("fishing_rod")
                    .orElseThrow(() -> new IllegalStateException("Ribbits Fisherman rod is missing"));
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

        private static final class LiveCapture {
            private final Inspection preliminary;
            private final Vec3 rawGripPosition;
            private Matrix4f geometryRootMatrix;
            private Matrix4f rodPivotMatrix;
            private Vec3 geometryRootOrigin;
            private Vec3 physicalGrip;
            private Vec3 physicalOuterTip;
            private String liveBoneChain;
            private String captureFailure;

            private LiveCapture(Inspection preliminary, Vec3 rawGripPosition) {
                this.preliminary = preliminary;
                this.rawGripPosition = rawGripPosition;
            }

            private void captureGeometryRoot(PoseStack poseStack) {
                geometryRootMatrix = new Matrix4f(poseStack.last().pose());
                geometryRootOrigin = transform(poseStack, 0.0F, 0.0F, 0.0F);
            }

            private void capturePhysicalRod(PoseStack poseStack, GeoBone rod,
                                            String cachedBoneChain) {
                rodPivotMatrix = new Matrix4f(poseStack.last().pose());
                physicalGrip = transform(poseStack, 0.0F, 0.0F, 0.0F);
                physicalOuterTip = transform(poseStack, 0.0F, 0.0F,
                        FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z);
                liveBoneChain = cachedBoneChain == null
                        ? describeBoneChain(rod, true) : cachedBoneChain;
            }

            private void captureFailure(Throwable error) {
                captureFailure = error.getClass().getName() + ": " + String.valueOf(error.getMessage());
            }

            private Inspection finish() {
                boolean exactLiveCapture = geometryRootMatrix != null && rodPivotMatrix != null
                        && physicalGrip != null && physicalOuterTip != null;
                String report = preliminary.resourceReport();
                if (liveBoneChain != null) {
                    report += "\n  live_render_snapshot (snapshot deltas are actual for this submission):\n"
                            + liveBoneChain;
                }
                String failure = preliminary.failure();
                if (captureFailure != null) {
                    failure = failure == null ? "diagnostic live capture: " + captureFailure
                            : failure + "; diagnostic live capture: " + captureFailure;
                }
                return new Inspection(preliminary.resourceSignature(), report,
                        geometryRootMatrix != null ? geometryRootMatrix : preliminary.geometryRootMatrix(),
                        rodPivotMatrix != null ? rodPivotMatrix : preliminary.rodPivotMatrix(),
                        geometryRootOrigin != null ? geometryRootOrigin : preliminary.geometryRootOrigin(),
                        physicalGrip != null ? physicalGrip : preliminary.physicalGrip(),
                        physicalOuterTip != null ? physicalOuterTip : preliminary.physicalOuterTip(),
                        rawGripPosition, true, exactLiveCapture, failure);
            }
        }

    }

    /** The Ribbits body origin is cancelled before the reference's folded-arm grip is applied. */
    private record StandaloneRodBasis(Vec3 rawGripPosition) {
        private void attachGripAtOrigin(PoseStack poseStack) {
            poseStack.translate(-rawGripPosition.x, -rawGripPosition.y, -rawGripPosition.z);
        }
    }

    /** Immutable copies of the live stages used by C27's rod, line, markers, and diagnostics. */
    record Inspection(String resourceSignature, String resourceReport,
                      Matrix4f geometryRootMatrix, Matrix4f rodPivotMatrix,
                      Vec3 geometryRootOrigin, Vec3 physicalGrip,
                      Vec3 physicalOuterTip, Vec3 rawGripPosition,
                      boolean submitted, boolean exactLiveCapture, String failure) {
        private static Inspection unavailable(String signature, String report, String failure) {
            return new Inspection(signature, report, null, null, null, null, null, null,
                    false, false, failure);
        }

        private static Inspection probeFailure(Throwable error) {
            String failure = error.getClass().getName() + ": " + String.valueOf(error.getMessage());
            return unavailable("diagnostic-probe-failure/" + failure,
                    "Ribbits asset contract state\n  diagnostic_probe_failure=" + failure,
                    failure);
        }

        private Inspection withSubmission(boolean submitted, String submissionFailure) {
            String combinedFailure = failure == null ? submissionFailure
                    : failure + "; submission: " + submissionFailure;
            return new Inspection(resourceSignature, resourceReport, geometryRootMatrix,
                    rodPivotMatrix, geometryRootOrigin, physicalGrip, physicalOuterTip,
                    rawGripPosition, submitted, false, combinedFailure);
        }

        boolean hasPhysicalRodPoints() {
            return physicalGrip != null && physicalOuterTip != null;
        }
    }
}
