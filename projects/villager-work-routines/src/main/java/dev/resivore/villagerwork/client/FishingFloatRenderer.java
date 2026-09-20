package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.villagerwork.FishingFloat;
import dev.resivore.villagerwork.FishingLineGeometry;
import dev.resivore.villagerwork.FishingRodPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/** Isolated bobber and line drawing; no villager renderer, model part, or texture hook. */
public final class FishingFloatRenderer extends EntityRenderer<FishingFloat, FishingFloatRenderer.FloatState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");

    public FishingFloatRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }

    @Override public FloatState createRenderState() { return new FloatState(); }

    @Override public void extractRenderState(FishingFloat entity, FloatState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Entity owner = entity.owner();
        if (owner instanceof Villager villager) {
            Vec3 villagerPosition = villager.getPosition(partialTick);
            float yaw = Mth.rotLerp(partialTick, villager.yBodyRotO, villager.yBodyRot);
            Vec3 ribbitsTip = VwrFishingRodLayer.ribbitsRodTip(villager);
            if (ribbitsTip != null) {
                // The provider captures this endpoint from the same PoseStack that submitted the
                // authored rod, rather than retaining an independent VWR rod-tip approximation.
                state.line = ribbitsTip.subtract(entity.getPosition(partialTick));
            } else if (!VwrFishingRodLayer.ribbitsRodProviderAvailable()) {
                // Ribbits is optional. Retain C17's proven stick/line presentation only when its
                // visual provider or resources are unavailable.
                FishingRodPose.Point tip = FishingRodPose.tip(villagerPosition.x, villagerPosition.y,
                        villagerPosition.z, yaw);
                state.line = tip.isFinite()
                        ? new Vec3(tip.x(), tip.y(), tip.z()).subtract(entity.getPosition(partialTick))
                        : null;
            } else {
                // Await the matching rod-layer submission instead of displaying a line from an
                // arbitrary fallback point beside an available authored rod.
                state.line = null;
            }
        } else state.line = null;
    }

    @Override public void submit(FloatState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.line != null) {
            var segments = FishingLineGeometry.segments((float) state.line.x, (float) state.line.y, (float) state.line.z);
            if (!segments.isEmpty()) {
                // 26.2's LINES vertex format is position, color, normal, and line width. Match
                // the vanilla FishingHookRenderer contract rather than relying on default elements.
                float lineWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                        .windowRenderState.appropriateLineWidth;
                collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vertices) -> {
                    for (FishingLineGeometry.Segment segment : segments) {
                        lineVertex(vertices, pose, segment.start(), lineWidth);
                        lineVertex(vertices, pose, segment.end(), lineWidth);
                    }
                });
            }
        }
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

    public static final class FloatState extends EntityRenderState { Vec3 line; }
}
