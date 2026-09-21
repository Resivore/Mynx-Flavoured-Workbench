package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins C21's live-CEM attachment contract independently of arbitrary world or camera endpoints. */
class FrogVillagerCemRodPoseTest {
    @Test
    void preservesOnlyTheUserAuthoredReferenceGroupAndTheActualCemChildName() {
        assertEquals("arms_rotation", FrogVillagerCemRodPose.FOLDED_ARMS_CHILD);
        assertEquals(0.0F, FrogVillagerCemRodPose.AUTHORED_GRIP_X_PIXELS, 0.000001F);
        assertEquals(-7.0F, FrogVillagerCemRodPose.AUTHORED_GRIP_Y_PIXELS, 0.000001F);
        assertEquals(-6.0F, FrogVillagerCemRodPose.AUTHORED_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(-9.5F, FrogVillagerCemRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS, 0.000001F);

        ModelPart foldedArms = new ModelPart(List.of(), Map.of());
        ModelPart arms = new ModelPart(List.of(), Map.of("arms_rotation", foldedArms));
        assertTrue(FrogVillagerCemRodPose.hasFoldedArmsChild(arms));
        assertFalse(FrogVillagerCemRodPose.hasFoldedArmsChild(foldedArms));
    }

    @Test
    void lineOriginIsTransformedByTheSameFinalRodPoseStack() {
        PoseStack rodPose = new PoseStack();
        rodPose.translate(2.0F, 3.0F, 4.0F);

        Vec3 tip = FrogVillagerCemRodPose.outerShaftTipInRenderSpace(rodPose);
        assertEquals(2.0D, tip.x, 0.000001D);
        assertEquals(3.0D, tip.y, 0.000001D);
        assertEquals(4.0D - 9.5D / 16.0D, tip.z, 0.000001D);
    }
}
