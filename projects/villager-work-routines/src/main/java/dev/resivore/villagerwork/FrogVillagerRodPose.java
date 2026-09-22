package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The user-authored Frog Villager CEM reference expressed in Minecraft 26.2 model units.
 *
 * <p>The reference places {@code EDIT_THIS__VWR_RIBBITS_ROD_POSE} under the real folded-arm
 * drawing part. C24 reaches that part through EMF's authored-part metadata, then applies the
 * reference group's authored local translation through EMF's documented {@code invertAxis="xy"}
 * conversion. No corrective rotation or scale is introduced.</p>
 */
public final class FrogVillagerRodPose {
    public static final float JEM_ARMS_ROTATION_DEGREES = 43.0F;
    public static final float AUTHORED_GRIP_X_PIXELS = 0.0F;
    public static final float AUTHORED_GRIP_Y_PIXELS = -7.0F;
    public static final float AUTHORED_GRIP_Z_PIXELS = -6.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F;

    public static final String AUTHORED_INVERT_AXIS = "xy";
    public static final float RUNTIME_GRIP_X_PIXELS = -AUTHORED_GRIP_X_PIXELS;
    public static final float RUNTIME_GRIP_Y_PIXELS = -AUTHORED_GRIP_Y_PIXELS;
    public static final float RUNTIME_GRIP_Z_PIXELS = AUTHORED_GRIP_Z_PIXELS;

    /* Minecraft model units are one sixteenth of a Blockbench/JEM model pixel. */
    public static final float AUTHORED_GRIP_X = AUTHORED_GRIP_X_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Y = AUTHORED_GRIP_Y_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Z = AUTHORED_GRIP_Z_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_X = RUNTIME_GRIP_X_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_Y = RUNTIME_GRIP_Y_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_Z = RUNTIME_GRIP_Z_PIXELS / 16.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z = OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS / 16.0F;

    private FrogVillagerRodPose() {
    }

    /** Applies exactly the live ModelPart pivot produced from the immutable authored group. */
    public static void applyReferenceGrip(PoseStack poseStack) {
        poseStack.translate(RUNTIME_GRIP_X, RUNTIME_GRIP_Y, RUNTIME_GRIP_Z);
    }
}
