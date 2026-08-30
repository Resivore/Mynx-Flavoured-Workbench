package dev.aero.shulkertrowel.contract;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Canary4CnmConsumptionContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void trowelDebitsOnlyWhenCanonicalDelegatedPlacementConsumes() throws IOException {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/item/ShulkerTrowelItem.java"
        )).replace("\r\n", "\n");

        assertTrue(source.contains("new ItemStack(selected.placementItem(), 1)"));
        assertTrue(source.contains("result = selected.placementItem().place(placementContext);"));
        assertTrue(source.contains("result.consumesAction()\n"
                + "                && placementStack.isEmpty()\n"
                + "                && !player.getAbilities().instabuild"));
        assertTrue(source.indexOf("selected.placementItem().place(placementContext)")
                < source.indexOf("placementStack.isEmpty()"));
        assertFalse(source.contains("SlabType.DOUBLE"));
        assertFalse(source.contains("VerticalSlabBlock.DOUBLE"));
        assertFalse(source.contains("BgeLayerBlock"));
        assertFalse(source.contains("TargetGeometry.LAYER"));
        assertFalse(source.contains("LAYERS"));
    }

    @Test
    void exactCnmRedirectOwnsHorizontalVerticalAndStepDoubleConsumption() throws IOException {
        Path cnmJar = Path.of(System.getProperty(
                "cnmJar",
                PROJECT_ROOT.resolve("../../originals/mods/clutternomore-2.0.7+26.2-fabric.jar")
                        .normalize()
                        .toString()
        ));
        try (JarFile jar = new JarFile(cnmJar.toFile())) {
            MethodNode redirect = method(
                    jar,
                    "dev/tazer/clutternomore/common/mixin/block/BlockItemMixin.class",
                    "cnm$place"
            );

            assertEquals(1, calls(redirect,
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap",
                    "isShape"));
            assertEquals(1, calls(redirect, "net/minecraft/world/item/ItemStack", "consume"));
            assertTrue(readsField(redirect,
                    "net/minecraft/world/level/block/SlabBlock",
                    "TYPE"));
            assertTrue(readsField(redirect,
                    "dev/tazer/clutternomore/common/blocks/VerticalSlabBlock",
                    "DOUBLE"));

            MethodNode stepInitializer = method(
                    jar,
                    "dev/tazer/clutternomore/common/blocks/StepBlock.class",
                    "<clinit>"
            );
            assertTrue(readsField(stepInitializer,
                    "net/minecraft/world/level/block/state/properties/BlockStateProperties",
                    "SLAB_TYPE"));
            assertTrue(writesField(stepInitializer,
                    "dev/tazer/clutternomore/common/blocks/StepBlock",
                    "SLAB_TYPE"));
        }
    }

    private static MethodNode method(JarFile jar, String entryName, String name) throws IOException {
        var entry = jar.getJarEntry(entryName);
        assertTrue(entry != null, "Missing CNM class " + entryName);
        var owner = new org.objectweb.asm.tree.ClassNode();
        try (var input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(owner, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method " + entryName + "." + name));
    }

    private static long calls(MethodNode method, String owner, String name) {
        return StreamSupport.stream(method.instructions.spliterator(), false)
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(owner)
                        && call.name.equals(name))
                .count();
    }

    private static boolean readsField(MethodNode method, String owner, String name) {
        return StreamSupport.stream(method.instructions.spliterator(), false)
                .anyMatch(instruction -> instruction instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals(owner)
                        && field.name.equals(name));
    }

    private static boolean writesField(MethodNode method, String owner, String name) {
        return StreamSupport.stream(method.instructions.spliterator(), false)
                .anyMatch(instruction -> instruction instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.PUTSTATIC
                        && field.owner.equals(owner)
                        && field.name.equals(name));
    }
}
