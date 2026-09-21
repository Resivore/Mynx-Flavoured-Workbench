package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.villagerwork.FishingFloat;
import dev.resivore.villagerwork.FishingLineGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Isolated bobber and line drawing; no villager renderer, model part, or texture hook. */
public final class FishingFloatRenderer extends EntityRenderer<FishingFloat, FishingFloatRenderer.FloatState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");

    public FishingFloatRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }

    @Override public FloatState createRenderState() { return new FloatState(); }

    @Override public void extractRenderState(FishingFloat entity, FloatState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override public void submit(FloatState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, vertices) -> {
            vertex(vertices, pose, -0.125f, -0.125f, 0, 1, state.lightCoords);
            vertex(vertices, pose, 0.125f, -0.125f, 1, 1, state.lightCoords);
            vertex(vertices, pose, 0.125f, 0.125f, 1, 0, state.lightCoords);
            vertex(vertices, pose, -0.125f, 0.125f, 0, 0, state.lightCoords);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * Submits VWR's sole line from the physical tip captured by the exact rod render pass.
     * Camera position only expresses the existing float in that same camera-relative space.
     */
    static LineSubmission submitLineFromPhysicalRod(Vec3 physicalTipRender,
                                                     SubmitNodeCollector collector,
                                                     Vec3 floatWorld) {
        if (physicalTipRender == null || floatWorld == null) {
            return new LineSubmission(physicalTipRender, null, null, floatWorld, 0, false);
        }
        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.gameRenderState()
                .levelRenderState.cameraRenderState.pos;
        Vec3 floatRender = floatWorld.subtract(cameraWorld);
        Vec3 physicalTipWorld = physicalTipRender.add(cameraWorld);
        if (!finite(physicalTipRender) || !finite(floatRender)) {
            return new LineSubmission(physicalTipRender, physicalTipWorld, floatRender, floatWorld,
                    0, false);
        }

        var segments = FishingLineGeometry.segmentsFromRodTip(
                (float) physicalTipRender.x, (float) physicalTipRender.y,
                (float) physicalTipRender.z,
                (float) floatRender.x, (float) floatRender.y, (float) floatRender.z);
        if (segments.isEmpty()) {
            return new LineSubmission(physicalTipRender, physicalTipWorld, floatRender, floatWorld,
                    0, false);
        }

        // Both endpoints are already camera-relative, so reusing an entity/arm PoseStack would
        // apply the live transform twice. The identity stack submits the captured points exactly.
        PoseStack linePose = new PoseStack();
        float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(linePose, RenderTypes.lines(), (pose, vertices) -> {
            for (FishingLineGeometry.Segment segment : segments) {
                lineVertex(vertices, pose, segment.start(), lineWidth);
                lineVertex(vertices, pose, segment.end(), lineWidth);
            }
        });
        return new LineSubmission(physicalTipRender, physicalTipWorld, floatRender, floatWorld,
                segments.size(), true);
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float u, float v, int light) {
        vertices.addVertex(pose, x, y, 0).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void lineVertex(VertexConsumer vertices, PoseStack.Pose pose, FishingLineGeometry.Vertex vertex,
                                   float lineWidth) {
        FishingLineGeometry.Point position = vertex.position();
        FishingLineGeometry.Point normal = vertex.normal();
        vertices.addVertex(pose, position.x(), position.y(), position.z())
                .setColor(110, 98, 82, 255)
                .setNormal(pose, normal.x(), normal.y(), normal.z())
                .setLineWidth(lineWidth);
    }

    record LineSubmission(Vec3 physicalTipRender, Vec3 physicalTipWorld,
                          Vec3 floatRender, Vec3 floatWorld,
                          int segmentCount, boolean submitted) {
    }

    public static final class FloatState extends EntityRenderState {
    }
}
