package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.FishingFloat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
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
            // A semantic eye/held-rod approximation avoids assuming vanilla arm geometry.
            Vec3 hand = villager.getEyePosition(partialTick).add(0, -0.45, 0);
            state.line = hand.subtract(entity.getPosition(partialTick));
        } else state.line = null;
    }

    @Override public void submit(FloatState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.line != null) {
            Vec3 end = state.line;
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vertices) -> {
                vertices.addVertex(pose, 0, 0, 0).setColor(110, 98, 82, 255).setNormal(pose, 0, 1, 0);
                vertices.addVertex(pose, (float)end.x, (float)end.y, (float)end.z)
                        .setColor(110, 98, 82, 255).setNormal(pose, 0, 1, 0);
            });
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

    public static final class FloatState extends EntityRenderState { Vec3 line; }
}
