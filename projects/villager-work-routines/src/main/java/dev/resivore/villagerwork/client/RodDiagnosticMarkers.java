package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.villagerwork.FrogVillagerRodPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Small, bright VWR-owned axes and shapes retained for the focused C26 pose comparison. */
final class RodDiagnosticMarkers {
    private RodDiagnosticMarkers() {
    }

    static void submit(SubmitNodeCollector collector, Matrix4fc incomingEntityLayer,
                       Matrix4fc afterTranslateToArms, Matrix4fc afterEffectiveFoldedArms,
                       Matrix4fc c24Grip, Matrix4fc c25Grip,
                       RibbitsFishermanRodRenderer.Inspection rod) {
        submitMarker(collector, incomingEntityLayer, Shape.CROSS, 0.055F, 255, 255, 255);
        submitMarker(collector, afterTranslateToArms, Shape.PLUS, 0.085F, 64, 160, 255);
        submitMarker(collector, afterEffectiveFoldedArms, Shape.PLUS, 0.085F, 64, 160, 255);
        // Cyan square = exact C24 grip. Magenta X = current C25 grip. The long triad and
        // white arrow are both in C24 local space, exactly where C25's +Y/+Z is applied.
        submitMarker(collector, c24Grip, Shape.SQUARE, 0.095F, 0, 255, 255);
        submitMarker(collector, c25Grip, Shape.X, 0.095F, 255, 0, 255);
        submitCorrectionAxes(collector, c24Grip);
        if (rod.geometryRootMatrix() != null) {
            submitMarker(collector, rod.geometryRootMatrix(), Shape.TRIANGLE, 0.070F, 255, 112, 0);
        }
        if (rod.rodPivotMatrix() != null) {
            submitMarker(collector, rod.rodPivotMatrix(), Shape.DIAMOND, 0.060F, 255, 255, 0);
            Matrix4f tipMatrix = new Matrix4f(rod.rodPivotMatrix())
                    .translate(0.0F, 0.0F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z);
            submitMarker(collector, tipMatrix, Shape.STAR, 0.070F, 64, 255, 64);
        }
    }

    private static void submitCorrectionAxes(SubmitNodeCollector collector, Matrix4fc c24Grip) {
        PoseStack markerPose = new PoseStack();
        markerPose.mulPose(c24Grip);
        float vanillaWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        float width = Math.max(3.5F, vanillaWidth * 2.0F);
        collector.submitCustomGeometry(markerPose, RenderTypes.lines(), (pose, vertices) -> {
            float axis = 0.24F;
            line(vertices, pose, 0, 0, 0, axis, 0, 0, 255, 48, 48, width);
            line(vertices, pose, 0, 0, 0, 0, axis, 0, 48, 255, 48, width);
            line(vertices, pose, 0, 0, 0, 0, 0, axis, 48, 96, 255, width);
            line(vertices, pose, 0, 0, 0,
                    0, FrogVillagerRodPose.FINAL_ALIGNMENT_Y,
                    FrogVillagerRodPose.FINAL_ALIGNMENT_Z, 255, 255, 255, width);
        });
    }

    private static void submitMarker(SubmitNodeCollector collector, Matrix4fc matrix, Shape shape,
                                     float size, int red, int green, int blue) {
        PoseStack markerPose = new PoseStack();
        markerPose.mulPose(matrix);
        float vanillaWidth = Minecraft.getInstance().gameRenderer.gameRenderState()
                .windowRenderState.appropriateLineWidth;
        float width = Math.max(2.5F, vanillaWidth * 1.75F);
        collector.submitCustomGeometry(markerPose, RenderTypes.lines(), (pose, vertices) -> {
            // Local orientation axes: X red, Y green, Z blue at every checkpoint.
            line(vertices, pose, 0, 0, 0, size * 1.35F, 0, 0, 255, 48, 48, width);
            line(vertices, pose, 0, 0, 0, 0, size * 1.35F, 0, 48, 255, 48, width);
            line(vertices, pose, 0, 0, 0, 0, 0, size * 1.35F, 48, 96, 255, width);
            drawShape(vertices, pose, shape, size, red, green, blue, width);
        });
    }

    private static void drawShape(VertexConsumer vertices, PoseStack.Pose pose, Shape shape,
                                  float s, int red, int green, int blue, float width) {
        switch (shape) {
            case CROSS -> {
                line(vertices, pose, -s, 0, 0, s, 0, 0, red, green, blue, width);
                line(vertices, pose, 0, -s, 0, 0, s, 0, red, green, blue, width);
                line(vertices, pose, 0, 0, -s, 0, 0, s, red, green, blue, width);
            }
            case SQUARE -> {
                line(vertices, pose, -s, -s, 0, s, -s, 0, red, green, blue, width);
                line(vertices, pose, s, -s, 0, s, s, 0, red, green, blue, width);
                line(vertices, pose, s, s, 0, -s, s, 0, red, green, blue, width);
                line(vertices, pose, -s, s, 0, -s, -s, 0, red, green, blue, width);
            }
            case X -> {
                line(vertices, pose, -s, -s, 0, s, s, 0, red, green, blue, width);
                line(vertices, pose, -s, s, 0, s, -s, 0, red, green, blue, width);
                line(vertices, pose, 0, -s, -s, 0, s, s, red, green, blue, width);
            }
            case PLUS -> {
                line(vertices, pose, -s, 0, 0, s, 0, 0, red, green, blue, width);
                line(vertices, pose, 0, -s, 0, 0, s, 0, red, green, blue, width);
            }
            case TRIANGLE -> {
                line(vertices, pose, 0, s, 0, -s, -s, 0, red, green, blue, width);
                line(vertices, pose, -s, -s, 0, s, -s, 0, red, green, blue, width);
                line(vertices, pose, s, -s, 0, 0, s, 0, red, green, blue, width);
            }
            case DIAMOND -> {
                line(vertices, pose, 0, s, 0, s, 0, 0, red, green, blue, width);
                line(vertices, pose, s, 0, 0, 0, -s, 0, red, green, blue, width);
                line(vertices, pose, 0, -s, 0, -s, 0, 0, red, green, blue, width);
                line(vertices, pose, -s, 0, 0, 0, s, 0, red, green, blue, width);
            }
            case STAR -> {
                line(vertices, pose, -s, 0, 0, s, 0, 0, red, green, blue, width);
                line(vertices, pose, 0, -s, 0, 0, s, 0, red, green, blue, width);
                line(vertices, pose, 0, 0, -s, 0, 0, s, red, green, blue, width);
                line(vertices, pose, -s * 0.7F, -s * 0.7F, 0,
                        s * 0.7F, s * 0.7F, 0, red, green, blue, width);
                line(vertices, pose, -s * 0.7F, s * 0.7F, 0,
                        s * 0.7F, -s * 0.7F, 0, red, green, blue, width);
            }
        }
    }

    private static void line(VertexConsumer vertices, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int red, int green, int blue, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float inverseLength = 1.0F / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = dx * inverseLength;
        float ny = dy * inverseLength;
        float nz = dz * inverseLength;
        vertices.addVertex(pose, x1, y1, z1).setColor(red, green, blue, 255)
                .setNormal(pose, nx, ny, nz).setLineWidth(width);
        vertices.addVertex(pose, x2, y2, z2).setColor(red, green, blue, 255)
                .setNormal(pose, -nx, -ny, -nz).setLineWidth(width);
    }

    private enum Shape { CROSS, SQUARE, PLUS, X, TRIANGLE, DIAMOND, STAR }
}
