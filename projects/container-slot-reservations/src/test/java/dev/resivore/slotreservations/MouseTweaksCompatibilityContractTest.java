package dev.resivore.slotreservations;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact upstream and narrow pre-screen compatibility contracts for Canary 25. */
final class MouseTweaksCompatibilityContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String MAIN = "yalter/mousetweaks/Main";
    private static final String HELPER = "rmbTweakMaybeClickSlot";
    private static final String HELPER_DESCRIPTOR =
            "(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)V";
    private static final String DRAG_DESCRIPTOR =
            "(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z";
    private static final String TARGET_SHA256 =
            "4592eff38a2e7af3487688e888a637a8cbd4767b46662df94d993a889102effe";

    @Test
    void exactMouseTweaks231CanClickAnEmptyEnteredSlotBeforeScreenDrag() throws Exception {
        String configured = System.getProperty("mouseTweaksReferenceJar");
        Assumptions.assumeTrue(configured != null && Files.isRegularFile(Path.of(configured)),
                "The local optional Mouse Tweaks reference is unavailable on this host");
        Path jar = Path.of(configured);
        assertEquals(TARGET_SHA256, sha256(jar));
        assertEquals(75_872L, Files.size(jar));

        try (JarFile archive = new JarFile(jar.toFile())) {
            String metadata = readUtf8(archive, "fabric.mod.json");
            assertTrue(metadata.contains("\"id\": \"mousetweaks\""));
            assertTrue(metadata.contains("\"version\": \"2.31\""));
            assertTrue(metadata.contains("\"minecraft\": \"~26.2\""));

            List<Instruction> helper = instructions(
                    archive, MAIN + ".class", HELPER, HELPER_DESCRIPTOR);
            int cursorEmpty = methodIndex(helper, "net/minecraft/world/item/ItemStack",
                    "isEmpty", "()Z", 0);
            int targetStack = methodIndex(helper, "net/minecraft/world/inventory/Slot",
                    "getItem", "()Lnet/minecraft/world/item/ItemStack;", cursorEmpty + 1);
            int compatible = methodIndex(helper, MAIN, "areStacksCompatible",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                    targetStack + 1);
            int compatibilityBranch = nextExecutable(helper, compatible + 1);
            int incompatibleReturn = nextExecutable(helper, compatibilityBranch + 1);
            int click = methodIndex(helper, "yalter/mousetweaks/IGuiScreenHandler", "clickSlot",
                    "(Lnet/minecraft/world/inventory/Slot;Lyalter/mousetweaks/MouseButton;Z)V",
                    incompatibleReturn + 1);
            assertTrue(cursorEmpty >= 0 && targetStack > cursorEmpty && compatible > targetStack
                    && click > incompatibleReturn);
            assertEquals(-1, methodIndex(helper, "net/minecraft/world/item/ItemStack",
                            "isEmpty", "()Z", targetStack + 1, compatible),
                    "The helper does not reject an empty target before its compatibility check");
            assertEquals(Opcodes.IFNE, helper.get(compatibilityBranch).opcode());
            assertEquals(Opcodes.RETURN, helper.get(incompatibleReturn).opcode(),
                    "An unrelated occupied target still returns before Mouse Tweaks clicks it");

            List<Instruction> compatibility = instructions(archive, MAIN + ".class",
                    "areStacksCompatible",
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z");
            List<Integer> emptyChecks = methodIndices(compatibility,
                    "net/minecraft/world/item/ItemStack", "isEmpty", "()Z");
            assertEquals(2, emptyChecks.size());
            int firstEmptyBranch = nextExecutable(compatibility, emptyChecks.get(0) + 1);
            int secondEmptyBranch = nextExecutable(compatibility, emptyChecks.get(1) + 1);
            assertEquals(Opcodes.IFNE, compatibility.get(firstEmptyBranch).opcode());
            assertEquals(Opcodes.IFNE, compatibility.get(secondEmptyBranch).opcode());
            assertEquals(jumpTargetIndex(compatibility, firstEmptyBranch),
                    jumpTargetIndex(compatibility, secondEmptyBranch),
                    "Either empty stack takes the exact compatibility-accepted branch");
            assertEquals(Opcodes.ICONST_1, compatibility.get(nextExecutable(
                    compatibility, jumpTargetIndex(compatibility, firstEmptyBranch))).opcode());

            List<Instruction> drag = instructions(
                    archive, MAIN + ".class", "onMouseDrag", DRAG_DESCRIPTOR);
            List<Integer> helperCalls = methodIndices(drag, MAIN, HELPER, HELPER_DESCRIPTOR);
            assertEquals(2, helperCalls.size(),
                    "Mouse Tweaks replays the origin and then visits the newly entered slot");
            int enteredSlotStore = fieldIndex(drag, Opcodes.PUTSTATIC, MAIN, "oldSelectedSlot",
                    "Lnet/minecraft/world/inventory/Slot;", helperCalls.get(0) + 1);
            assertTrue(enteredSlotStore > helperCalls.get(0)
                            && enteredSlotStore < helperCalls.get(1),
                    "The second helper call receives the newly entered slot, including an empty one");

            List<Instruction> fabricDrag = instructions(archive,
                    "yalter/mousetweaks/fabric/mixin/MixinMouseHandler.class",
                    "onMouseDragged",
                    "(Lnet/minecraft/client/gui/screens/Screen;"
                            + "Lnet/minecraft/client/input/MouseButtonEvent;DD"
                            + "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)Z");
            int mouseTweaks = methodIndex(fabricDrag, MAIN, "onMouseDrag", DRAG_DESCRIPTOR, 0);
            int wrappedScreen = methodIndex(fabricDrag,
                    "com/llamalad7/mixinextras/injector/wrapoperation/Operation", "call",
                    "([Ljava/lang/Object;)Ljava/lang/Object;", mouseTweaks + 1);
            assertTrue(mouseTweaks >= 0 && wrappedScreen > mouseTweaks,
                    "Mouse Tweaks mutates drag state before the wrapped Screen.mouseDragged call");
        }
    }

    @Test
    void canary25OwnsEligibleRmbInFabricAllowEventsBeforePuzzlesAndDefault() throws IOException {
        String initializer = source("client/ContainerSlotReservationsClient.java");
        String collector = source("client/CarriedShulkerRmbCollector.java");
        String guard = source("mixin/client/MouseTweaksInboundGuardMixin.java");
        String plugin = source("mixin/client/OptionalClientMixinPlugin.java");
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String mixins = Files.readString(
                ROOT.resolve("src/main/resources/container_slot_reservations.client.mixins.json"));
        String allMainJava = allMainJava();

        assertTrue(initializer.contains("ScreenMouseEvents.allowMouseClick(screen)"));
        assertTrue(initializer.contains("ScreenMouseEvents.allowMouseDrag(screen)"));
        assertTrue(initializer.contains("ScreenMouseEvents.allowMouseRelease(screen)"));
        assertTrue(initializer.contains("\"puzzleslib\", \"before\""));
        assertTrue(initializer.contains(
                "event.addPhaseOrdering(CARRIED_SHULKER_INPUT_PHASE, PUZZLES_BEFORE_PHASE)"));
        assertTrue(initializer.contains(
                "event.addPhaseOrdering(CARRIED_SHULKER_INPUT_PHASE, Event.DEFAULT_PHASE)"));
        assertTrue(initializer.contains("CarriedShulkerRmbCollector.begin(containerScreen, target)"));
        assertTrue(initializer.contains("CarriedShulkerRmbCollector.drag("));
        assertTrue(initializer.contains("CarriedShulkerRmbCollector.release()"));
        assertTrue(initializer.contains("ClientPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(initializer.contains("CarriedShulkerRmbCollector.reset()"));

        assertFalse(screen.contains("CarriedShulkerRmbCollector.begin("),
                "The C24 screen-method click hook is unreachable after puzzleslib interrupts input");
        assertFalse(screen.contains("CarriedShulkerRmbCollector.drag("),
                "Canary 25 traversal belongs to the earlier per-screen Fabric allow event");
        assertTrue(collector.contains("private static boolean ownsPhysicalRmb"));
        assertTrue(collector.contains("public static boolean ownsInboundRmb()"));
        assertTrue(collector.contains("return ownsPhysicalRmb;"));
        assertTrue(collector.contains(
                "dispatchInventoryToShulker(client.player, target, source.key())"),
                "The occupied origin must dispatch immediately instead of waiting for drag");
        assertTrue(collector.contains("if (source == null || !GESTURE.enter(source.hovered()))"));
        assertTrue(collector.contains("if (target.hasItem()) dispatchInventoryToShulker"),
                "An entered empty Slot must be visited without sending a transaction");
        assertTrue(collector.contains("if (client.player == null || candidateScreen != screen"
                + " || !validContext(client)"),
                "A screen or menu change must invalidate projected traversal");
        assertTrue(collector.contains("public static boolean release()"));
        assertTrue(screen.contains("method = \"removed\""));
        assertTrue(screen.contains("CarriedShulkerRmbCollector.reset()"));

        assertTrue(guard.contains("@Pseudo"));
        assertTrue(guard.contains("@Mixin(targets = \"yalter.mousetweaks.Main\", remap = false)"));
        assertTrue(guard.contains("method = \"onMouseDrag\""));
        assertTrue(guard.contains("at = @At(\"HEAD\")"));
        assertTrue(guard.contains("cancellable = true"));
        assertTrue(guard.contains("require = 1"));
        assertTrue(guard.contains("CarriedShulkerRmbCollector.ownsInboundRmb()"));
        assertTrue(guard.contains("callbackInfo.setReturnValue(false)"));
        assertTrue(plugin.contains("MouseTweaksInboundGuardMixin"));
        assertTrue(plugin.contains("isModLoaded(\"mousetweaks\")"));
        assertTrue(mixins.contains(
                "\"plugin\": \"dev.resivore.slotreservations.mixin.client.OptionalClientMixinPlugin\""));
        assertTrue(mixins.contains("\"MouseTweaksInboundGuardMixin\""));

        for (String retired : List.of(
                "CarriedShulkerMouseTweaks",
                "MouseTweaksMainMixin",
                "MouseTweaksMixinPlugin",
                "MouseTweaksCompatibility",
                "MouseTweaksContainerScreenMixin",
                "rmbTweakMaybeClickSlot",
                "oldSelectedSlot",
                "beforeMouseTweaksSlot",
                "afterMouseTweaksSlot",
                "observeMouseTweaksDrag",
                "nativeOutboundSlot",
                "afterInitialNativePress",
                "projectedSource",
                "IMTModGuiContainer3Ex",
                "MOUSE_TWEAKS_VIRTUAL_CONTAINER",
                "mouseTweaksShadowFingerprint",
                "SECONDARY_DEPOSIT")) {
            assertFalse(allMainJava.contains(retired),
                    "Canary 25 retained retired C20-C23 bridge machinery: " + retired);
        }
    }

    @Test
    void packagedGuardIsPseudoNarrowAndDoesNotBundleMouseTweaks() throws IOException {
        Path artifact = Path.of(System.getProperty("canary12Artifact"));
        try (JarFile archive = new JarFile(artifact.toFile())) {
            String entry = "dev/resivore/slotreservations/mixin/client/"
                    + "MouseTweaksInboundGuardMixin.class";
            ClassShape shape = classShape(archive, entry);
            assertTrue(shape.annotations().contains("Lorg/spongepowered/asm/mixin/Pseudo;"));
            assertTrue(shape.annotations().contains("Lorg/spongepowered/asm/mixin/Mixin;"));
            assertEquals(Set.of(MAIN), shape.mixinTargets());
            assertTrue(shape.methods().contains(
                    "containerSlotReservations$keepOwnedInboundDragServerAuthoritative"
                            + "(Lorg/spongepowered/asm/mixin/injection/callback/"
                            + "CallbackInfoReturnable;)V"));

            List<Instruction> code = instructions(archive, entry,
                    "containerSlotReservations$keepOwnedInboundDragServerAuthoritative",
                    "(Lorg/spongepowered/asm/mixin/injection/callback/"
                            + "CallbackInfoReturnable;)V");
            int owned = methodIndex(code,
                    "dev/resivore/slotreservations/client/CarriedShulkerRmbCollector",
                    "ownsInboundRmb", "()Z", 0);
            int block = methodIndex(code,
                    "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
                    "setReturnValue", "(Ljava/lang/Object;)V", owned + 1);
            assertTrue(owned >= 0 && block > owned);

            assertFalse(archive.stream().anyMatch(candidate ->
                            candidate.getName().startsWith("yalter/mousetweaks/")),
                    "CSR must not package Mouse Tweaks classes or an API stub");
            assertFalse(archive.stream().anyMatch(candidate -> candidate.getName().endsWith(
                            "MouseTweaksMainMixin.class")),
                    "The retired helper-level Mouse Tweaks bridge must not ship");
        }
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations")
                .resolve(relative));
    }

    private static String allMainJava() throws IOException {
        StringBuilder all = new StringBuilder();
        try (var paths = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                all.append(Files.readString(path)).append('\n');
            }
        }
        return all.toString();
    }

    private static String readUtf8(JarFile archive, String entryName) throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertNotNull(entry, "Missing JAR entry " + entryName);
        try (var stream = archive.getInputStream(entry)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ClassShape classShape(JarFile archive, String entryName) throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertNotNull(entry, "Missing packaged class " + entryName);
        Set<String> annotations = new HashSet<>();
        Set<String> mixinTargets = new HashSet<>();
        Set<String> methods = new HashSet<>();
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    annotations.add(descriptor);
                    if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override public AnnotationVisitor visitArray(String name) {
                            if (!"value".equals(name) && !"targets".equals(name)) return null;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override public void visit(String ignored, Object value) {
                                    if (value instanceof Type type) {
                                        mixinTargets.add(type.getInternalName());
                                    } else if (value instanceof String target) {
                                        mixinTargets.add(target.replace('.', '/'));
                                    }
                                }
                            };
                        }
                    };
                }

                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    methods.add(name + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return new ClassShape(Set.copyOf(annotations), Set.copyOf(mixinTargets), Set.copyOf(methods));
    }

    private static List<Instruction> instructions(JarFile archive, String entryName, String method,
                                                   String descriptor) throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertNotNull(entry, "Missing class " + entryName);
        List<Instruction> result = new ArrayList<>();
        boolean[] found = {false};
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String actualDescriptor,
                                                           String signature, String[] exceptions) {
                    if (!method.equals(name) || !descriptor.equals(actualDescriptor)) return null;
                    found[0] = true;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitInsn(int opcode) {
                            result.add(new Instruction(opcode, null, null, null, null));
                        }

                        @Override public void visitVarInsn(int opcode, int varIndex) {
                            result.add(new Instruction(opcode, null, null, null, varIndex));
                        }

                        @Override public void visitFieldInsn(int opcode, String owner, String name,
                                                             String descriptor) {
                            result.add(new Instruction(opcode, owner, name, descriptor, null));
                        }

                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                             String descriptor, boolean isInterface) {
                            result.add(new Instruction(opcode, owner, name, descriptor, null));
                        }

                        @Override public void visitJumpInsn(int opcode, Label label) {
                            result.add(new Instruction(opcode, null, null, null, label));
                        }

                        @Override public void visitLabel(Label label) {
                            result.add(new Instruction(-1, null, null, null, label));
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0], "Missing method " + method + descriptor + " in " + entryName);
        return List.copyOf(result);
    }

    private static List<Integer> methodIndices(List<Instruction> code, String owner, String name,
                                               String descriptor) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) result.add(index);
        }
        return List.copyOf(result);
    }

    private static int methodIndex(List<Instruction> code, String owner, String name,
                                   String descriptor, int start) {
        return methodIndex(code, owner, name, descriptor, start, code.size());
    }

    private static int methodIndex(List<Instruction> code, String owner, String name,
                                   String descriptor, int start, int endExclusive) {
        for (int index = Math.max(0, start); index < Math.min(code.size(), endExclusive); index++) {
            Instruction instruction = code.get(index);
            if (owner.equals(instruction.owner()) && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int fieldIndex(List<Instruction> code, int opcode, String owner, String name,
                                  String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.opcode() == opcode && owner.equals(instruction.owner())
                    && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int nextExecutable(List<Instruction> code, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            if (code.get(index).opcode() != -1) return index;
        }
        throw new AssertionError("No executable instruction at or after " + start);
    }

    private static int jumpTargetIndex(List<Instruction> code, int jumpIndex) {
        Object target = code.get(jumpIndex).operand();
        assertTrue(target instanceof Label, "Expected a jump at " + jumpIndex);
        for (int index = 0; index < code.size(); index++) {
            if (code.get(index).opcode() == -1 && code.get(index).operand() == target) return index;
        }
        throw new AssertionError("Missing jump target for " + jumpIndex);
    }

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private record Instruction(int opcode, String owner, String name, String descriptor,
                               Object operand) {}

    private record ClassShape(Set<String> annotations, Set<String> mixinTargets,
                              Set<String> methods) {}
}
