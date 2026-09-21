package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The user-authored Frog Villager CEM reference expressed in Minecraft 26.2 model units.
 *
 * <p>The reference places {@code EDIT_THIS__VWR_RIBBITS_ROD_POSE} under the real folded-arm
 * drawing part. C23 reaches that live part through the renderer's effective structural path,
 * then this class supplies the reference group's authored local translation and no corrective
 * rotation or scale.</p>
 */
public final class FrogVillagerRodPose {
    public static final float JEM_ARMS_ROTATION_DEGREES = 43.0F;
    public static final float AUTHORED_GRIP_X_PIXELS = 0.0F;
    public static final float AUTHORED_GRIP_Y_PIXELS = -7.0F;
    public static final float AUTHORED_GRIP_Z_PIXELS = -6.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F;

    /* Minecraft model units are one sixteenth of a Blockbench/JEM model pixel. */
    public static final float AUTHORED_GRIP_X = AUTHORED_GRIP_X_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Y = AUTHORED_GRIP_Y_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Z = AUTHORED_GRIP_Z_PIXELS / 16.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z = OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS / 16.0F;

    private FrogVillagerRodPose() {
    }

    /** Applies exactly the reference group's local translation after the visible folded arms. */
    public static void applyReferenceGrip(PoseStack poseStack) {
        poseStack.translate(AUTHORED_GRIP_X, AUTHORED_GRIP_Y, AUTHORED_GRIP_Z);
    }
}
