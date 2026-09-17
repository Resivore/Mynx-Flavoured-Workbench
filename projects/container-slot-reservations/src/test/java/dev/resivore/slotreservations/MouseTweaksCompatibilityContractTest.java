package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bytecode/source contract for the exact Mouse Tweaks 2.31 handoff used through C22. */
final class MouseTweaksCompatibilityContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String TARGET_SHA256 =
            "4592eff38a2e7af3487688e888a637a8cbd4767b46662df94d993a889102effe";

    @Test
    void exactMouseTweaks231ExposesTheCustomContainerSeamUsedByTheBridge() throws Exception {
        String configured = System.getProperty("mouseTweaksReferenceJar");
        Assumptions.assumeTrue(configured != null && Files.isRegularFile(Path.of(configured)),
                "The local optional Mouse Tweaks reference is unavailable on this host");
        Path jar = Path.of(configured);
        assertEquals(TARGET_SHA256, sha256(jar));
        assertEquals(75_872L, Files.size(jar),
                "The canonical Mouse Tweaks identity must include the verified file length");

        try (JarFile archive = new JarFile(jar.toFile())) {
            assertTrue(archive.stream().anyMatch(entry -> entry.getName().equals(
                    "yalter/mousetweaks/api/IMTModGuiContainer3Ex.class")));
            Set<String> apiMethods = methods(archive, "yalter/mousetweaks/api/IMTModGuiContainer3Ex.class");
            assertTrue(apiMethods.contains("MT_getSlots()Ljava/util/List;"));
            assertTrue(apiMethods.contains("MT_getSlotUnderMouse(DD)Lnet/minecraft/world/inventory/Slot;"));
            assertTrue(apiMethods.contains("MT_clickSlot(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/world/inventory/ContainerInput;)V"));
            assertTrue(apiMethods.contains("MT_disableRMBDraggingFunctionality()Z"));
            Set<String> mainMethods = methods(archive, "yalter/mousetweaks/Main.class");
            Set<String> mainFields = fields(archive, "yalter/mousetweaks/Main.class");
            assertTrue(mainMethods.contains("onMouseClicked(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z"));
            assertTrue(mainMethods.contains("onMouseDrag(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z"));
            assertTrue(mainMethods.contains("onMouseReleased(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z"));
            assertTrue(mainMethods.contains("onMouseScrolled(Lnet/minecraft/client/gui/screens/Screen;DDD)Z"));
            assertTrue(mainFields.contains("oldSelectedSlot:Lnet/minecraft/world/inventory/Slot;"));
            assertTrue(mainFields.contains("canDoRMBDrag:Z"));
            assertTrue(mainFields.contains("rmbTweakLeftOriginalSlot:Z"));

            List<String> dragCalls = methodCalls(archive, "yalter/mousetweaks/Main.class", "onMouseDrag");
            assertTrue(dragCalls.contains(
                    "yalter/mousetweaks/IGuiScreenHandler.getSlotUnderMouse(DD)Lnet/minecraft/world/inventory/Slot;"));
            assertTrue(dragCalls.contains(
                    "yalter/mousetweaks/IGuiScreenHandler.disableRMBDraggingFunctionality()Z"));
            assertTrue(dragCalls.contains(
                    "yalter/mousetweaks/Main.rmbTweakMaybeClickSlot(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/item/ItemStack;)V"));
            assertEquals(Set.of(0), directBooleanReturns(
                            archive, "yalter/mousetweaks/Main.class", "onMouseDrag"),
                    "The post-upstream Screen.mouseDragged fallback relies on exact 2.31 always returning false");
            List<String> helperCalls = methodCalls(
                    archive, "yalter/mousetweaks/Main.class", "rmbTweakMaybeClickSlot");
            assertTrue(helperCalls.contains(
                    "yalter/mousetweaks/IGuiScreenHandler.clickSlot(Lnet/minecraft/world/inventory/Slot;Lyalter/mousetweaks/MouseButton;Z)V"));

            List<String> fabricDragCalls = methodCalls(archive,
                    "yalter/mousetweaks/fabric/mixin/MixinMouseHandler.class", "onMouseDragged");
            int upstream = fabricDragCalls.indexOf(
                    "yalter/mousetweaks/Main.onMouseDrag(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z");
            int screen = fabricDragCalls.indexOf(
                    "com/llamalad7/mixinextras/injector/wrapoperation/Operation.call([Ljava/lang/Object;)Ljava/lang/Object;");
            assertTrue(upstream >= 0 && screen > upstream,
                    "Exact 2.31 must run Mouse Tweaks before the wrapped Screen.mouseDragged fallback");
        }
    }

    @Test
    void bridgeIsOptionalTransientAndRoutesMutationsThroughExistingAuthority() throws IOException {
        String panel = source("client/ShulkerPanel.java");
        String mixin = source("mixin/client/MouseTweaksContainerScreenMixin.java");
        String plugin = source("mixin/client/MouseTweaksMixinPlugin.java");
        String wheel = source("client/MouseTweaksCompatibility.java");
        String client = source("client/ContainerSlotReservationsClient.java");
        String gesture = source("client/MouseTweaksRmbGesture.java");
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String actions = source("ShulkerPanelActions.java");
        String networking = source("ReservationNetworking.java");
        String trace = source("MouseTweaksTrace.java");
        String content = source("network/ShulkerPanelContentActionPayload.java");
        String payload = source("network/ShulkerPanelMenuQuickMovePayload.java");

        assertTrue(panel.contains("MOUSE_TWEAKS_VIRTUAL_CONTAINER"));
        assertTrue(panel.contains("Transient read-only view"));
        assertTrue(panel.contains("beginMouseTweaksRightGesture"));
        assertTrue(panel.contains("MOUSE_TWEAKS_RMB_GESTURE"));
        assertTrue(panel.contains("mouseTweaksShadowFingerprint"));
        assertTrue(panel.contains("ShulkerTransferPlanner.planExactInsertion"));
        assertTrue(panel.contains("SECONDARY_DEPOSIT"));
        assertFalse(panel.contains("if (MOUSE_TWEAKS_RMB_GESTURE.upstreamArmed()) return true"),
                "Configuration eligibility must not suppress fallback without an observed provider action");
        assertTrue(panel.contains("post-upstream fallback offered"));
        assertTrue(panel.contains("mouseTweaksDepositBridge"),
                "An already-open panel must remain available for ordinary-slot -> panel deposit gestures");
        assertTrue(panel.contains("mouseTweaksPreGestureRetention"));
        assertFalse(panel.contains("!binding.menu().getCarried().isEmpty() || MOUSE_TWEAKS_RMB_GESTURE.isActive()"),
                "A remaining cursor stack must not keep the panel sticky after release");
        assertTrue(panel.contains("ItemStack.matches(mouseTweaksShadowCarried, binding.menu().getCarried())"),
                "A native boundary may resume a projected panel chain only after cursor convergence");
        assertTrue(panel.contains("mouseTweaksQuickMoveFromMenuSlot"));
        assertTrue(panel.contains("MouseTweaksCompatibility.consumeWheel"));
        assertTrue(mixin.contains("implements IMTModGuiContainer3Ex"));
        assertTrue(mixin.contains("Mouse Tweaks extended provider selected"));
        assertTrue(mixin.contains("Mouse Tweaks requested slot under pointer"));
        assertTrue(mixin.contains("Mouse Tweaks invoked slot action"));
        assertTrue(mixin.contains("ContainerInput.QUICK_MOVE"));
        assertTrue(mixin.contains("mouseTweaksNativeClick"));
        assertTrue(screen.contains("beginMouseTweaksRightGesture"));
        assertTrue(client.contains("ScreenMouseEvents.allowMouseClick(screen)"));
        assertTrue(client.contains("ScreenMouseEvents.allowMouseRelease(screen)"));
        assertTrue(client.contains("Fabric RMB press observed"));
        assertTrue(client.contains("ShulkerPanel.endMouseTweaksRightGesture()"));
        assertTrue(gesture.contains("COLLECTION_SOURCE"));
        assertTrue(gesture.contains("DEPOSIT"));
        assertTrue(gesture.contains("enterPanelCell"));
        assertTrue(gesture.contains("reset()"));
        assertFalse(gesture.contains("upstreamArmed"));
        assertTrue(plugin.contains("isModLoaded(\"mousetweaks\")"));
        assertTrue(wheel.contains("Class.forName(\"yalter.mousetweaks.Main\", false"));
        assertTrue(actions.contains("handleMenuQuickMove"));
        assertTrue(actions.contains("SECONDARY_DEPOSIT"));
        assertTrue(content.contains("PRIMARY, SECONDARY, QUICK_MOVE, SECONDARY_DEPOSIT"),
                "C21 must append its wire action without renumbering accepted C20 actions");
        assertTrue(actions.contains("ShulkerTransferPlanner.planInsertion(host.stack(), before)"));
        assertTrue(actions.contains("ShulkerHostResolver.removableSource(player, source)"));
        assertTrue(trace.contains("container_slot_reservations.debugMouseTweaks"));
        assertTrue(trace.contains("if (enabled())"), "Finished Canary tracing must be disabled by default");
        assertTrue(networking.contains("Server received content payload"));
        assertTrue(actions.contains("Host and fingerprint accepted"));
        assertTrue(actions.contains("Reservation/native insertion result"));
        assertTrue(actions.contains("Insertion planner result"));
        assertTrue(actions.contains("Mutation committed"));
        assertTrue(actions.contains("Authoritative host/cursor sync returned"));
        assertTrue(payload.contains("String hostFingerprint"));
        assertFalse(payload.contains("ItemStack"));
        assertFalse(payload.contains("ItemStackTemplate"));
    }

    @Test
    void releaseArtifactDoesNotShipAMouseTweaksApiStub() throws IOException {
        Path artifact = Path.of(System.getProperty("canary12Artifact"));
        try (JarFile archive = new JarFile(artifact.toFile())) {
            assertFalse(archive.stream().anyMatch(entry -> entry.getName().startsWith("yalter/mousetweaks/")));
        }
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/dev/resivore/slotreservations").resolve(relative));
    }

    private static Set<String> methods(JarFile archive, String entryName) throws IOException {
        Set<String> methods = new HashSet<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    methods.add(name + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        }
        return methods;
    }

    private static Set<String> fields(JarFile archive, String entryName) throws IOException {
        Set<String> fields = new HashSet<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public org.objectweb.asm.FieldVisitor visitField(int access, String name,
                                                                            String descriptor, String signature,
                                                                            Object value) {
                    fields.add(name + ":" + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        }
        return fields;
    }

    private static List<String> methodCalls(JarFile archive, String entryName,
                                            String selectedMethod) throws IOException {
        List<String> calls = new ArrayList<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    if (!name.equals(selectedMethod)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                              String descriptor, boolean isInterface) {
                            calls.add(owner + "." + name + descriptor);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return List.copyOf(calls);
    }

    private static Set<Integer> directBooleanReturns(JarFile archive, String entryName,
                                                     String selectedMethod) throws IOException {
        Set<Integer> values = new HashSet<>();
        try (var stream = archive.getInputStream(archive.getJarEntry(entryName))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    if (!name.equals(selectedMethod)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        private int previousOpcode = -1;

                        @Override public void visitInsn(int opcode) {
                            if (opcode == Opcodes.IRETURN) {
                                assertTrue(previousOpcode == Opcodes.ICONST_0
                                                || previousOpcode == Opcodes.ICONST_1,
                                        "Expected an exact direct boolean return in " + selectedMethod);
                                values.add(previousOpcode == Opcodes.ICONST_1 ? 1 : 0);
                            }
                            previousOpcode = opcode;
                        }

                        @Override public void visitVarInsn(int opcode, int varIndex) { previousOpcode = -1; }
                        @Override public void visitFieldInsn(int opcode, String owner, String name,
                                                             String descriptor) { previousOpcode = -1; }
                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                              String descriptor, boolean isInterface) {
                            previousOpcode = -1;
                        }
                        @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
                            previousOpcode = -1;
                        }
                        @Override public void visitLdcInsn(Object value) { previousOpcode = -1; }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return Set.copyOf(values);
    }

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
