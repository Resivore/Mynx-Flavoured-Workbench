package dev.resivore.villagerwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.mixin.client.VillagerModelArmsAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * The live CEM folded-arm attachment used by the supplied Frog Villager resource.
 *
 * <p>The resource replaces the normal {@code arms} part, then puts the visible folded hands
 * beneath its {@code arms_rotation} child. {@link VillagerModel#translateToArms} reaches only
 * the replacement root; C21 follows that actual child {@link ModelPart} instead of pretending
 * that the vanilla crossed-arms transform contains the child hierarchy.</p>
 */
public final class FrogVillagerCemRodPose {
    /** The actual child id in the inspected active Frog Villager CEM/JEM resource. */
    public static final String FOLDED_ARMS_CHILD = "arms_rotation";

    /** The unrotated, user-authored Blockbench group translation in model pixels. */
    public static final float AUTHORED_GRIP_X_PIXELS = 0.0F;
    public static final float AUTHORED_GRIP_Y_PIXELS = -7.0F;
    public static final float AUTHORED_GRIP_Z_PIXELS = -6.0F;
    /** The C27 shaft's physical outer tip relative to that shared group origin. */
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F;

    private static final float MODEL_PIXELS_PER_BLOCK = 16.0F;

    private FrogVillagerCemRodPose() {
    }

    /**
     * Appends the exact live CEM folded-hands transform, then the fixture's authored group.
     * Nothing here supplies a replacement rod rotation.
     *
     * @return {@code false} when the active model is not the inspected Frog Villager hierarchy
     */
    public static boolean apply(VillagerModel model, VillagerRenderState state, PoseStack poseStack) {
        ModelPart arms = ((VillagerModelArmsAccessor) model).villagerWork$getArms();
        if (!hasFoldedArmsChild(arms)) return false;

        model.translateToArms(state, poseStack);
        applyFoldedArmsAndReferenceGrip(arms.getChild(FOLDED_ARMS_CHILD), poseStack);
        return true;
    }

    static boolean hasFoldedArmsChild(ModelPart arms) {
        return arms.hasChild(FOLDED_ARMS_CHILD);
    }

    static void applyFoldedArmsAndReferenceGrip(ModelPart foldedArms, PoseStack poseStack) {
        // This executes the CEM-loaded ModelPart's effective pivot/rotation/scale exactly as the
        // visible Frog Villager hands do. The reference group deliberately has no rotation.
        foldedArms.translateAndRotate(poseStack);
        poseStack.translate(AUTHORED_GRIP_X_PIXELS / MODEL_PIXELS_PER_BLOCK,
                AUTHORED_GRIP_Y_PIXELS / MODEL_PIXELS_PER_BLOCK,
                AUTHORED_GRIP_Z_PIXELS / MODEL_PIXELS_PER_BLOCK);
    }

    /**
     * Transforms the physical C27 outer shaft tip through the already-applied live rod matrix.
     * The returned coordinate is in the renderer's camera-relative world space, not a new
     * world-space hand approximation.
     */
    static Vec3 outerShaftTipInRenderSpace(PoseStack rodPose) {
        Vector4f tip = rodPose.last().pose().transform(new Vector4f(
                0.0F, 0.0F,
                OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS / MODEL_PIXELS_PER_BLOCK,
                1.0F));
        return new Vec3(tip.x(), tip.y(), tip.z());
    }
}
