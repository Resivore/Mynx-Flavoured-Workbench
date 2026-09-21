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
     * Emits VWR's sole 16-segment line from the physical shaft tip obtained from the same final
     * pose matrix that just rendered the rod. Camera position is used only to express the float
     * in that renderer's camera-relative coordinate system; it never supplies a line endpoint.
     */
    static void submitLineFromSharedRodPose(PoseStack rodPose, SubmitNodeCollector collector,
                                            Vec3 floatWorldPosition) {
        Vec3 rodTip = FrogVillagerCemRodPose.outerShaftTipInRenderSpace(rodPose);
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.gameRenderState()
                .levelRenderState.cameraRenderState.pos;
        Vec3 floatRenderPosition = floatWorldPosition.subtract(cameraPosition);
        var segments = FishingLineGeometry.segmentsBetween(
                (float) floatRenderPosition.x, (float) floatRenderPosition.y, (float) floatRenderPosition.z,
                (float) rodTip.x, (float) rodTip.y, (float) rodTip.z);
        if (segments.isEmpty()) return;

        // The endpoints are already camera-relative world coordinates, so this intentionally
        // begins from identity instead of inventing another arm/world transform chain.
        PoseStack linePose = new PoseStack();
        float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(linePose, RenderTypes.lines(), (pose, vertices) -> {
            for (FishingLineGeometry.Segment segment : segments) {
                lineVertex(vertices, pose, segment.start(), lineWidth);
                lineVertex(vertices, pose, segment.end(), lineWidth);
            }
        });
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

    public static final class FloatState extends EntityRenderState {}
}
