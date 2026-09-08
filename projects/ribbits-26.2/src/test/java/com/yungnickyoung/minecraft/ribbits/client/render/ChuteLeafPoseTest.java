package com.yungnickyoung.minecraft.ribbits.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChuteLeafPoseTest {
    @Test
    void canopyStaysHorizontalAndAboveGripAtEveryBodyYaw() {
        for (int yaw = 0; yaw < 360; yaw += 15) {
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            // Vanilla living renderer converts entity coordinates to model coordinates.
            pose.scale(-1, -1, 1);
            ChuteLeafRenderer.applyDeployedPose(pose);
            Vector3f a = pose.last().pose().transformPosition(new Vector3f(0.015625F, 1.375F, -0.15625F));
            Vector3f b = pose.last().pose().transformPosition(new Vector3f(0.578125F, 1.375F, -0.15625F));
            Vector3f c = pose.last().pose().transformPosition(new Vector3f(0.015625F, 1.375F, 0.53125F));
            Vector3f grip = pose.last().pose().transformPosition(new Vector3f(0.296875F, 0.0F, 0.5F));
            assertEquals(a.y, b.y, 0.00001F);
            assertEquals(a.y, c.y, 0.00001F);
            assertTrue(a.y > grip.y);
        }
    }
}
