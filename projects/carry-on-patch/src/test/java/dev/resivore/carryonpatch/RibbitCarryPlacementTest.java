package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

class RibbitCarryPlacementTest {
    @Test
    void targetsOnlyExplicitRibbitTypesWithoutRibbitsOrGeckolibInstalled() {
        assertTrue(RibbitCarryPlacement.targets(Identifier.parse("ribbits:ribbit")));
        assertTrue(RibbitCarryPlacement.targets(Identifier.parse("ribbits:wandering_ribbit")));
        for (String id : new String[]{"minecraft:frog", "minecraft:pig", "animalgarden:crocodile",
                "other:ribbit", "ribbits:chute_leaf", "ribbits:ribbit_impostor"}) {
            assertFalse(RibbitCarryPlacement.targets(Identifier.parse(id)), id);
        }
        assertFalse(RibbitCarryPlacement.targets(null));
    }

    @Test
    void firstPersonTransformRaisesAndMovesAwayAfterScaleAndHalfTurn() {
        PoseStack poses = new PoseStack();
        poses.translate(0F, -.45F, -.55F);
        poses.scale(.3F, .3F, .3F);
        poses.mulPose(Axis.YP.rotationDegrees(180));
        assertTranslationAndUnchangedBasis(poses, true, new Vector3f(0F, -.30F, -.67F));
    }

    @Test
    void thirdPersonTransformRaisesInPlayerModelSpaceAndMovesForward() {
        PoseStack poses = new PoseStack();
        poses.translate(0F, .9F, -.45F);
        poses.scale(.5F, -.5F, -.5F);
        assertTranslationAndUnchangedBasis(poses, false, new Vector3f(0F, .60F, -.55F));
    }

    private static void assertTranslationAndUnchangedBasis(
            PoseStack poses, boolean first, Vector3f expectedOrigin) {
        Matrix4f before = new Matrix4f(poses.last().pose());
        poses.pushPose();
        RibbitCarryPlacement.translate(poses, first);
        Matrix4f after = poses.last().pose();
        Vector3f origin = after.transformPosition(new Vector3f());
        assertTrue(origin.distance(expectedOrigin) < .00001F, origin.toString());
        for (Vector3f axis : new Vector3f[]{new Vector3f(1,0,0), new Vector3f(0,1,0),
                new Vector3f(0,0,1)}) {
            assertEquals(before.transformDirection(new Vector3f(axis)),
                    after.transformDirection(new Vector3f(axis)));
        }
        poses.popPose();
        assertEquals(before, poses.last().pose());
        assertTrue(poses.isEmpty());
    }

    @Test
    void packagedDrawScopeRestoresPoseOnNormalAndExceptionalExitAndNeverSuppressesDraw()
            throws Exception {
        try (ZipFile zip = new ZipFile(Path.of(System.getProperty("patchJar")).toFile())) {
            ClassNode node = new ClassNode();
            new ClassReader(zip.getInputStream(zip.getEntry(
                    "dev/resivore/carryonpatch/RibbitCarryPlacement.class"))).accept(node, 0);
            MethodNode submit = node.methods.stream().filter(m -> m.name.equals("submit"))
                    .findFirst().orElseThrow();
            int pushes = 0, pops = 0, draws = 0, rethrows = 0;
            for (AbstractInsnNode instruction : submit.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    if (call.name.equals("pushPose")) pushes++;
                    if (call.name.equals("popPose")) pops++;
                    if (call.name.equals("submit")) draws++;
                }
                if (instruction.getOpcode() == Opcodes.ATHROW) rethrows++;
            }
            assertEquals(1, pushes);
            assertEquals(2, pops); // javac duplicates finally for success and exceptional exit
            assertEquals(2, draws); // unchanged non-target path and scoped Ribbit path
            assertEquals(1, rethrows);
            assertTrue(submit.tryCatchBlocks.stream().anyMatch(t -> t.type == null));
        }
    }
}
