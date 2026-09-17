package dev.resivore.slotreservations;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contracts only the upstream fact C24 needs: incompatible sources never reach its helper click. */
final class MouseTweaksCompatibilityContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String MAIN = "yalter/mousetweaks/Main";
    private static final String HELPER = "rmbTweakMaybeClickSlot";
    private static final String HELPER_DESCRIPTOR =
            "(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String TARGET_SHA256 =
            "4592eff38a2e7af3487688e888a637a8cbd4767b46662df94d993a889102effe";

    @Test
    void exactMouseTweaks231RejectsIncompatibleOccupiedSourcesBeforeClickingThem() throws Exception {
        String configured = System.getProperty("mouseTweaksReferenceJar");
        Assumptions.assumeTrue(configured != null && Files.isRegularFile(Path.of(configured)),
                "The local optional Mouse Tweaks reference is unavailable on this host");
        Path jar = Path.of(configured);
        assertEquals(TARGET_SHA256, sha256(jar));
        assertEquals(75_872L, Files.size(jar));

        try (JarFile archive = new JarFile(jar.toFile())) {
            List<Instruction> code = instructions(archive, MAIN + ".class", HELPER, HELPER_DESCRIPTOR);
            int compatible = methodIndex(code, MAIN, "areStacksCompatible",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", 0);
            int branch = nextExecutable(code, compatible + 1);
            int rejected = nextExecutable(code, branch + 1);
            int click = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler", "clickSlot",
                    "(Lnet/minecraft/world/inventory/Slot;Lyalter/mousetweaks/MouseButton;Z)V", rejected + 1);
            assertTrue(compatible >= 0 && click > rejected);
            assertEquals(Opcodes.IFNE, code.get(branch).opcode());
            assertEquals(Opcodes.RETURN, code.get(rejected).opcode(),
                    "A shulker cursor and unrelated occupied source return before a Mouse Tweaks click");
        }
    }

    @Test
    void canary24UsesTheNativeScreenDragAndHasNoMouseTweaksTraversalBridge() throws IOException {
        String collector = source("client/CarriedShulkerRmbCollector.java");
        String gesture = source("client/CarriedShulkerRmbGesture.java");
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String mixins = Files.readString(ROOT.resolve("src/main/resources/container_slot_reservations.client.mixins.json"));
        String allMainJava = allMainJava();

        assertTrue(screen.contains("method = \"mouseDragged\""));
        assertTrue(screen.contains("CarriedShulkerRmbCollector.drag("));
        assertTrue(collector.contains("containerSlotReservations$slotAt(mouseX, mouseY)"));
        assertTrue(collector.contains("if (!GESTURE.enter(key)) return true"));
        assertTrue(collector.contains("if (target.hasItem()) dispatchInventoryToShulker"));
        assertTrue(collector.contains("GESTURE.leaveSlotSurface()"));
        assertTrue(collector.contains("!target.hasItem()"));
        assertFalse(collector.contains("MouseTweaksCompatibility"));
        assertFalse(collector.contains("rmbTweakMaybeClickSlot"));
        assertFalse(gesture.contains("SHULKER_TO_INVENTORY"));
        assertFalse(mixins.contains("MouseTweaksMainMixin"));
        for (String retired : List.of("CarriedShulkerMouseTweaks", "MouseTweaksMainMixin",
                "MouseTweaksCompatibility", "rmbTweakMaybeClickSlot", "SECONDARY_DEPOSIT",
                "MouseTweaksContainerScreenMixin", "IMTModGuiContainer3Ex")) {
            assertFalse(allMainJava.contains(retired), "C24 retained retired traversal machinery: " + retired);
        }
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }

    private static String allMainJava() throws IOException {
        try (var paths = Files.walk(ROOT.resolve("src/main/java"))) {
            StringBuilder all = new StringBuilder();
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                all.append(Files.readString(path));
            }
            return all.toString();
        }
    }

    private static List<Instruction> instructions(JarFile archive, String entryName, String method,
                                                   String descriptor) throws IOException {
        List<Instruction> result = new ArrayList<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String actualDescriptor,
                                                           String signature, String[] exceptions) {
                    if (!method.equals(name) || !descriptor.equals(actualDescriptor)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitInsn(int opcode) { result.add(new Instruction(opcode, null, null, null)); }
                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                             String descriptor, boolean isInterface) {
                            result.add(new Instruction(opcode, owner, name, descriptor));
                        }
                        @Override public void visitJumpInsn(int opcode, Label label) { result.add(new Instruction(opcode, null, null, null)); }
                        @Override public void visitLabel(Label label) { result.add(new Instruction(-1, null, null, null)); }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return List.copyOf(result);
    }

    private static int methodIndex(List<Instruction> code, String owner, String name,
                                   String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int nextExecutable(List<Instruction> code, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            if (code.get(index).opcode() != -1) return index;
        }
        throw new AssertionError("No executable instruction");
    }

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private record Instruction(int opcode, String owner, String name, String descriptor) {}
}
