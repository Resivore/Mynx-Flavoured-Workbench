package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            assertRmbPressArmingContract(archive);
            assertExtendedHandlerPrecedesGenericHandler(archive);

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
    void packagedOptionalMixinCarriesTheExactExtendedProviderContract() throws IOException {
        Path artifact = Path.of(System.getProperty("canary12Artifact"));
        try (JarFile archive = new JarFile(artifact.toFile())) {
            String mixinEntry = "dev/resivore/slotreservations/mixin/client/"
                    + "MouseTweaksContainerScreenMixin.class";
            CompiledClassShape mixin = classShape(archive, mixinEntry);
            assertTrue(mixin.interfaces().contains("yalter/mousetweaks/api/IMTModGuiContainer3Ex"),
                    "The packaged screen mixin must add Mouse Tweaks' real extended-provider interface");
            assertTrue(mixin.annotations().contains("Lorg/spongepowered/asm/mixin/Pseudo;"));
            assertTrue(mixin.annotations().contains("Lorg/spongepowered/asm/mixin/Mixin;"));
            assertEquals(Set.of("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen"),
                    mixin.mixinTargets());

            Set<String> providerMethods = Set.of(
                    "MT_isMouseTweaksDisabled()Z",
                    "MT_isWheelTweakDisabled()Z",
                    "MT_getSlots()Ljava/util/List;",
                    "MT_getSlotUnderMouse(DD)Lnet/minecraft/world/inventory/Slot;",
                    "MT_isCraftingOutput(Lnet/minecraft/world/inventory/Slot;)Z",
                    "MT_isIgnored(Lnet/minecraft/world/inventory/Slot;)Z",
                    "MT_disableRMBDraggingFunctionality()Z",
                    "MT_clickSlot(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/world/inventory/ContainerInput;)V"
            );
            for (String method : providerMethods) {
                Integer access = mixin.methods().get(method);
                assertTrue(access != null && (access & Opcodes.ACC_PUBLIC) != 0,
                        "Packaged mixin is missing public provider method " + method);
            }

            List<Instruction> click = methodInstructions(archive, mixinEntry, "MT_clickSlot",
                    "(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/world/inventory/ContainerInput;)V");
            String panelOwner = "dev/resivore/slotreservations/client/ShulkerPanel";
            int before = methodIndex(click, panelOwner, "beforeMouseTweaksNativeClick",
                    "(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/world/inventory/ContainerInput;)"
                            + "Ldev/resivore/slotreservations/client/ShulkerPanel$NativeClickPlan;", 0);
            int invoke = methodIndex(click,
                    "dev/resivore/slotreservations/client/ShulkerPanel$NativeClickPlan",
                    "invoke", "()Z", before + 1);
            int allowedJump = nextExecutable(click, invoke + 1);
            assertEquals(Opcodes.IFNE, click.get(allowedJump).opcode(),
                    "A false native-click plan must return before the invoker");
            int suppressedReturn = nextExecutable(click, allowedJump + 1);
            assertEquals(Opcodes.RETURN, click.get(suppressedReturn).opcode());
            int allowedTarget = jumpTargetIndex(click, allowedJump);
            int nativeInvoker = methodIndex(click,
                    "dev/resivore/slotreservations/mixin/client/ContainerScreenMouseAccess",
                    "containerSlotReservations$clickSlot",
                    "(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
                    allowedTarget);
            int afterNormal = methodIndex(click, panelOwner, "afterMouseTweaksNativeClick",
                    "(Ldev/resivore/slotreservations/client/ShulkerPanel$NativeClickPlan;Z)V",
                    nativeInvoker + 1);
            int afterExceptional = methodIndex(click, panelOwner, "afterMouseTweaksNativeClick",
                    "(Ldev/resivore/slotreservations/client/ShulkerPanel$NativeClickPlan;Z)V",
                    afterNormal + 1);
            int rethrow = opcodeIndex(click, Opcodes.ATHROW, afterExceptional + 1);
            assertTrue(before >= 0 && invoke > before && allowedTarget > suppressedReturn
                            && nativeInvoker >= allowedTarget && afterNormal > nativeInvoker
                            && afterExceptional > afterNormal && rethrow > afterExceptional,
                    "Packaged provider must guard the native invoker and run its post hook on normal and exceptional exits");

            String pluginEntry = "dev/resivore/slotreservations/mixin/client/MouseTweaksMixinPlugin.class";
            List<Instruction> plugin = methodInstructions(archive, pluginEntry, "shouldApplyMixin",
                    "(Ljava/lang/String;Ljava/lang/String;)Z");
            int optionalName = ldcIndex(plugin,
                    "dev.resivore.slotreservations.mixin.client.MouseTweaksContainerScreenMixin", 0);
            int nameMatch = methodIndex(plugin, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",
                    optionalName + 1);
            int loader = methodIndex(plugin, "net/fabricmc/loader/api/FabricLoader", "getInstance",
                    "()Lnet/fabricmc/loader/api/FabricLoader;", nameMatch + 1);
            int modId = ldcIndex(plugin, "mousetweaks", loader + 1);
            int loaded = methodIndex(plugin, "net/fabricmc/loader/api/FabricLoader", "isModLoaded",
                    "(Ljava/lang/String;)Z", modId + 1);
            assertTrue(optionalName >= 0 && nameMatch > optionalName && loader > nameMatch
                            && modId > loader && loaded > modId,
                    "The packaged mixin plugin must gate exactly the optional provider on mod id mousetweaks");
            int nonOptionalJump = nextExecutable(plugin, nameMatch + 1);
            int missingProviderJump = nextExecutable(plugin, loaded + 1);
            assertEquals(Opcodes.IFEQ, plugin.get(nonOptionalJump).opcode(),
                    "A different mixin name must bypass the optional-mod check");
            assertEquals(Opcodes.IFEQ, plugin.get(missingProviderJump).opcode(),
                    "The optional provider must be rejected when Mouse Tweaks is not loaded");
            int accepted = jumpTargetIndex(plugin, nonOptionalJump);
            int rejected = jumpTargetIndex(plugin, missingProviderJump);
            assertEquals(Opcodes.ICONST_1, plugin.get(nextExecutable(plugin, accepted + 1)).opcode(),
                    "Non-optional mixins must remain applicable");
            assertEquals(Opcodes.ICONST_0, plugin.get(nextExecutable(plugin, rejected + 1)).opcode(),
                    "The Mouse Tweaks provider must be inapplicable when the mod is absent");
            assertEquals(Opcodes.ICONST_1,
                    plugin.get(nextExecutable(plugin, missingProviderJump + 1)).opcode(),
                    "A loaded Mouse Tweaks provider must make the optional mixin applicable");

            assertFalse(archive.stream().anyMatch(entry -> entry.getName().startsWith("yalter/mousetweaks/")),
                    "The applicability contract must use the provider's API rather than bundling its stub");
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
        assertTrue(panel.contains("dispatchMouseTweaksSecondary(slot, \"post-upstream-screen-fallback\")"));
        assertTrue(panel.contains("dispatchMouseTweaksSecondary(slot, \"mouse-tweaks-provider\")"));
        int sharedClaim = panel.indexOf("if (!MOUSE_TWEAKS_RMB_GESTURE.enterPanelCell(slot)");
        int claimedTrace = panel.indexOf("MouseTweaksTrace.event(8, \"CSR virtual cell claimed\"", sharedClaim);
        assertTrue(sharedClaim >= 0 && claimedTrace > sharedClaim,
                "Provider and post-upstream fallback must share one cell-identity claim before stage 8");
        assertTrue(gesture.contains("cell == lastPanelCell"),
                "The shared claim must deduplicate provider and fallback actions for the same cell");
        assertTrue(panel.contains("mouseTweaksDepositBridge"),
                "An already-open panel must remain available for ordinary-slot -> panel deposit gestures");
        assertTrue(panel.contains("mouseTweaksPreGestureRetention"));
        assertTrue(panel.contains("if (!carrying) mouseTweaksPreGestureRetention = false;"));
        assertTrue(panel.contains("retainPanel(mouseTweaksPreGestureRetention, carrying)"));
        assertTrue(panel.contains("GLFW.glfwGetMouseButton"));
        assertTrue(panel.contains("== GLFW.GLFW_RELEASE"),
                "Physical RMB state must backstop a short-circuited release callback");
        assertFalse(panel.contains("!binding.menu().getCarried().isEmpty() || MOUSE_TWEAKS_RMB_GESTURE.isActive()"),
                "A remaining cursor stack must not keep the panel sticky after release");
        int liveRebaseStart = panel.indexOf("if (MOUSE_TWEAKS_RMB_GESTURE.takeShadowNeedsLiveCarried())");
        int liveRebaseEnd = panel.indexOf("ItemStack changedHost;", liveRebaseStart);
        assertTrue(liveRebaseStart >= 0 && liveRebaseEnd > liveRebaseStart);
        String liveRebase = panel.substring(liveRebaseStart, liveRebaseEnd);
        assertEquals(1, occurrences(liveRebase, "mouseTweaksShadowHost ="),
                "Only the first menu-origin boundary may refresh the projected host");
        assertEquals(1, occurrences(liveRebase, "mouseTweaksShadowFingerprint ="),
                "Only the first menu-origin boundary may refresh the projected fingerprint");
        assertEquals(1, occurrences(liveRebase, "mouseTweaksShadowCarried ="),
                "No post-panel native action may replace the projection with a stale live cursor");
        assertTrue(panel.contains("beforeMouseTweaksNativeClick"));
        assertTrue(panel.contains("afterMouseTweaksNativeClick"));
        assertTrue(panel.contains("canProjectNativePlaceOne"));
        assertTrue(panel.contains("applyNativeCursorDelta"));
        assertTrue(panel.contains("projected cursor exhausted"));
        assertTrue(gesture.contains("blockedUntilRelease"));
        assertTrue(panel.contains("isBlockedUntilRelease()"));
        assertTrue(panel.contains("blockMouseTweaksRightGesture()"));
        int providerStart = panel.indexOf("public static boolean mouseTweaksClick");
        int providerEnd = panel.indexOf("public static final class NativeClickPlan", providerStart);
        String provider = panel.substring(providerStart, providerEnd);
        assertTrue(provider.contains("MOUSE_TWEAKS_RMB_GESTURE.isActive()"));
        assertTrue(provider.contains("dispatchMouseTweaksSecondary"));
        assertTrue(provider.indexOf("return true;", provider.indexOf("dispatchMouseTweaksSecondary")) >= 0,
                "A blocked active gesture must remain provider-owned instead of falling through");
        int dragStart = panel.indexOf("public static boolean drag");
        int dragEnd = panel.indexOf("public static boolean release", dragStart);
        String drag = panel.substring(dragStart, dragEnd);
        assertTrue(drag.contains("if (MOUSE_TWEAKS_RMB_GESTURE.isActive())"));
        assertTrue(drag.contains("dispatchMouseTweaksSecondary"));
        assertTrue(drag.lastIndexOf("return true;") > drag.indexOf("dispatchMouseTweaksSecondary"),
                "The screen fallback must consume a blocked held-RMB gesture");
        assertTrue(panel.contains("target.container == binding.slot().container"),
                "A physical alias of the bound host must not bypass the projected-host guard");
        int beforeNative = mixin.indexOf("beforeMouseTweaksNativeClick");
        int suppressNative = mixin.indexOf("if (!plan.invoke()) return;", beforeNative);
        int nativeInvoker = mixin.indexOf("containerSlotReservations$clickSlot", suppressNative);
        int afterNative = mixin.indexOf("afterMouseTweaksNativeClick", nativeInvoker);
        assertTrue(beforeNative >= 0 && suppressNative > beforeNative && nativeInvoker > suppressNative
                        && afterNative > nativeInvoker,
                "The guarded before/native/after sequence is part of the Mouse Tweaks compatibility contract");
        int beforeMethod = panel.indexOf("public static NativeClickPlan beforeMouseTweaksNativeClick");
        int afterMethod = panel.indexOf("public static void afterMouseTweaksNativeClick", beforeMethod);
        int nextMethod = panel.indexOf("public static boolean mouseTweaksQuickMoveFromMenuSlot", afterMethod);
        String nativeProjection = panel.substring(beforeMethod, nextMethod);
        assertTrue(nativeProjection.contains("markUnprojectedNativeBoundary()"));
        assertFalse(nativeProjection.contains("mouseTweaksShadowHost ="));
        assertFalse(nativeProjection.contains("mouseTweaksShadowFingerprint ="),
                "Native place-one projection must preserve the projected predecessor host/fingerprint");
        assertTrue(panel.contains("mouseTweaksQuickMoveFromMenuSlot"));
        assertTrue(panel.contains("MouseTweaksCompatibility.consumeWheel"));
        assertTrue(mixin.contains("implements IMTModGuiContainer3Ex"));
        assertTrue(mixin.contains("Mouse Tweaks extended provider selected"));
        assertTrue(mixin.contains("Mouse Tweaks requested slot under pointer"));
        assertTrue(mixin.contains("Mouse Tweaks invoked slot action"));
        assertTrue(mixin.contains("ContainerInput.QUICK_MOVE"));
        assertTrue(mixin.contains("beforeMouseTweaksNativeClick"));
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
        int authoritativeSync = actions.indexOf("synchronizeCommittedMenu(player, host.menu());");
        int committedTrace = actions.indexOf("MouseTweaksTrace.event(17, \"Mutation committed\"",
                authoritativeSync);
        assertTrue(authoritativeSync >= 0 && committedTrace > authoritativeSync,
                "The server must synchronize its host/cursor mutation before reporting commit");
        assertTrue(actions.contains("menu.broadcastFullState()"));
        assertTrue(actions.contains("menu.broadcastChanges()"));
        assertFalse(actions.contains("MouseTweaksTrace.event(18"),
                "Stage 18 is a client receipt, not a server send");
        assertTrue(panel.contains("MouseTweaksTrace.event(18, \"Authoritative host/cursor sync received\""));
        assertTrue(panel.contains("acceptedBinding="));
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

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int offset = 0; (offset = value.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
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

    private static void assertRmbPressArmingContract(JarFile archive) throws IOException {
        String main = "yalter/mousetweaks/Main";
        List<Instruction> code = methodInstructions(
                archive, main + ".class", "onMouseClicked",
                "(Lnet/minecraft/client/gui/screens/Screen;DDLyalter/mousetweaks/MouseButton;)Z");

        int slotLookup = methodIndex(code, "yalter/mousetweaks/IGuiScreenHandler",
                "getSlotUnderMouse", "(DD)Lnet/minecraft/world/inventory/Slot;", 0);
        int slotStore = fieldIndex(code, Opcodes.PUTSTATIC, main, "oldSelectedSlot",
                "Lnet/minecraft/world/inventory/Slot;", slotLookup + 1);
        int carried = methodIndex(code, "net/minecraft/world/inventory/AbstractContainerMenu",
                "getCarried", "()Lnet/minecraft/world/item/ItemStack;", slotStore + 1);
        int right = fieldIndex(code, Opcodes.GETSTATIC, "yalter/mousetweaks/MouseButton", "RIGHT",
                "Lyalter/mousetweaks/MouseButton;", carried + 1);
        int rightExit = opcodeIndex(code, Opcodes.IF_ACMPNE, right + 1);
        int carriedEmpty = methodIndex(code, "net/minecraft/world/item/ItemStack", "isEmpty", "()Z",
                rightExit + 1);
        assertTrue(slotLookup >= 0 && slotStore > slotLookup && carried > slotStore
                        && right > carried && rightExit > right && carriedEmpty > rightExit,
                "Exact 2.31 must snapshot the pointed slot, then gate RMB arming on its carried stack");

        int nonEmpty = assertFalseReturnFallthrough(code, carriedEmpty, Opcodes.IFEQ,
                "An empty carried stack must reject RMB-drag arming");
        int enabled = fieldIndex(code, Opcodes.GETFIELD, "yalter/mousetweaks/Config", "rmbTweak", "Z",
                nonEmpty + 1);
        assertTrue(enabled > nonEmpty,
                "The nonempty-carried path must then consult the RMB-tweak configuration");
        int enabledPath = assertFalseReturnFallthrough(code, enabled, Opcodes.IFNE,
                "Disabled RMB tweaks must reject RMB-drag arming");
        int arm = fieldIndex(code, Opcodes.PUTSTATIC, main, "canDoRMBDrag", "Z", enabledPath + 1);
        int leaveOriginal = fieldIndex(code, Opcodes.PUTSTATIC, main, "rmbTweakLeftOriginalSlot", "Z",
                arm + 1);
        assertTrue(arm > enabledPath && leaveOriginal > arm,
                "A nonempty carried stack with RMB tweaks enabled must arm the drag and reset its origin flag");
        assertEquals(Opcodes.ICONST_1, code.get(previousExecutable(code, arm - 1)).opcode(),
                "canDoRMBDrag must be set true");
        assertEquals(Opcodes.ICONST_0, code.get(previousExecutable(code, leaveOriginal - 1)).opcode(),
                "rmbTweakLeftOriginalSlot must be reset false");
        assertTrue(jumpTargetIndex(code, rightExit) > leaveOriginal,
                "Both arming writes must remain inside the RIGHT-button branch");

        assertFalse(code.stream().anyMatch(instruction ->
                        "net/minecraft/world/inventory/Slot".equals(instruction.owner())),
                "RMB press arming must not query the initially hovered Slot's contents or capacity");
        assertFalse(code.stream().anyMatch(instruction -> instruction.kind() == InstructionKind.FIELD
                        && instruction.opcode() == Opcodes.GETSTATIC
                        && main.equals(instruction.owner())
                        && "oldSelectedSlot".equals(instruction.name())),
                "The stored origin slot must not gate RMB press arming");
    }

    private static void assertExtendedHandlerPrecedesGenericHandler(JarFile archive) throws IOException {
        List<Instruction> code = methodInstructions(
                archive, "yalter/mousetweaks/Main.class", "findHandler",
                "(Lnet/minecraft/client/gui/screens/Screen;)Lyalter/mousetweaks/IGuiScreenHandler;");
        String api = "yalter/mousetweaks/api/IMTModGuiContainer3Ex";
        String extendedHandler = "yalter/mousetweaks/handlers/IMTModGuiContainer3ExHandler";
        String genericScreen = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
        String genericHandler = "yalter/mousetweaks/handlers/GuiContainerHandler";

        int extendedCheck = typeIndex(code, Opcodes.INSTANCEOF, api, 0);
        assertTrue(extendedCheck >= 0, "findHandler must test the extended provider interface");
        assertFalse(code.subList(0, extendedCheck).stream()
                        .anyMatch(instruction -> instruction.opcode() == Opcodes.INSTANCEOF),
                "The extended provider must be findHandler's first type test");
        int extendedMiss = nextExecutable(code, extendedCheck + 1);
        assertEquals(Opcodes.IFEQ, code.get(extendedMiss).opcode());
        int afterExtendedBranch = jumpTargetIndex(code, extendedMiss);
        int extendedNew = typeIndex(code, Opcodes.NEW, extendedHandler, extendedMiss + 1);
        int extendedCast = typeIndex(code, Opcodes.CHECKCAST, api, extendedNew + 1);
        int extendedConstructor = methodIndex(code, extendedHandler, "<init>",
                "(Lyalter/mousetweaks/api/IMTModGuiContainer3Ex;)V", extendedCast + 1);
        int extendedReturn = opcodeIndex(code, Opcodes.ARETURN, extendedConstructor + 1);
        assertTrue(extendedNew > extendedMiss && extendedCast > extendedNew
                        && extendedConstructor > extendedCast && extendedReturn > extendedConstructor
                        && extendedReturn < afterExtendedBranch,
                "The extended type test must construct and return IMTModGuiContainer3ExHandler");

        int genericCheck = typeIndex(code, Opcodes.INSTANCEOF, genericScreen, afterExtendedBranch + 1);
        int genericNew = typeIndex(code, Opcodes.NEW, genericHandler, genericCheck + 1);
        int genericCast = typeIndex(code, Opcodes.CHECKCAST, genericScreen, genericNew + 1);
        int genericConstructor = methodIndex(code, genericHandler, "<init>",
                "(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;)V", genericCast + 1);
        int genericReturn = opcodeIndex(code, Opcodes.ARETURN, genericConstructor + 1);
        assertTrue(genericCheck > extendedReturn && genericNew > genericCheck && genericCast > genericNew
                        && genericConstructor > genericCast && genericReturn > genericConstructor,
                "Generic AbstractContainerScreen handling must occur only after the extended-provider return");
    }

    private static CompiledClassShape classShape(JarFile archive, String entryName) throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertTrue(entry != null, "Missing packaged class " + entryName);
        Set<String> interfaces = new HashSet<>();
        Set<String> annotations = new HashSet<>();
        Set<String> mixinTargets = new HashSet<>();
        Map<String, Integer> methods = new LinkedHashMap<>();
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public void visit(int version, int access, String name, String signature,
                                            String superName, String[] implementedInterfaces) {
                    if (implementedInterfaces != null) {
                        interfaces.addAll(List.of(implementedInterfaces));
                    }
                }

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
                    methods.put(name + descriptor, access);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return new CompiledClassShape(Set.copyOf(interfaces), Set.copyOf(annotations),
                Set.copyOf(mixinTargets), Map.copyOf(methods));
    }

    private static List<Instruction> methodInstructions(JarFile archive, String entryName,
                                                        String selectedMethod,
                                                        String selectedDescriptor) throws IOException {
        var entry = archive.getJarEntry(entryName);
        assertTrue(entry != null, "Missing class " + entryName);
        List<Instruction> instructions = new ArrayList<>();
        boolean[] found = {false};
        try (var stream = archive.getInputStream(entry)) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                           String signature, String[] exceptions) {
                    if (!name.equals(selectedMethod) || !descriptor.equals(selectedDescriptor)) return null;
                    found[0] = true;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitInsn(int opcode) {
                            instructions.add(new Instruction(InstructionKind.INSN, opcode,
                                    null, null, null, null));
                        }

                        @Override public void visitIntInsn(int opcode, int operand) {
                            instructions.add(new Instruction(InstructionKind.INT, opcode,
                                    null, null, null, operand));
                        }

                        @Override public void visitVarInsn(int opcode, int varIndex) {
                            instructions.add(new Instruction(InstructionKind.VAR, opcode,
                                    null, null, null, varIndex));
                        }

                        @Override public void visitTypeInsn(int opcode, String type) {
                            instructions.add(new Instruction(InstructionKind.TYPE, opcode,
                                    type, null, null, null));
                        }

                        @Override public void visitFieldInsn(int opcode, String owner, String name,
                                                             String descriptor) {
                            instructions.add(new Instruction(InstructionKind.FIELD, opcode,
                                    owner, name, descriptor, null));
                        }

                        @Override public void visitMethodInsn(int opcode, String owner, String name,
                                                              String descriptor, boolean isInterface) {
                            instructions.add(new Instruction(InstructionKind.METHOD, opcode,
                                    owner, name, descriptor, isInterface));
                        }

                        @Override public void visitInvokeDynamicInsn(String name, String descriptor,
                                                                     org.objectweb.asm.Handle bootstrapMethodHandle,
                                                                     Object... bootstrapMethodArguments) {
                            instructions.add(new Instruction(InstructionKind.INVOKEDYNAMIC,
                                    Opcodes.INVOKEDYNAMIC, null, name, descriptor, null));
                        }

                        @Override public void visitJumpInsn(int opcode, Label label) {
                            instructions.add(new Instruction(InstructionKind.JUMP, opcode,
                                    null, null, null, label));
                        }

                        @Override public void visitLabel(Label label) {
                            instructions.add(new Instruction(InstructionKind.LABEL, -1,
                                    null, null, null, label));
                        }

                        @Override public void visitLdcInsn(Object value) {
                            instructions.add(new Instruction(InstructionKind.LDC, Opcodes.LDC,
                                    null, null, null, value));
                        }

                        @Override public void visitIincInsn(int varIndex, int increment) {
                            instructions.add(new Instruction(InstructionKind.IINC, Opcodes.IINC,
                                    null, null, null, List.of(varIndex, increment)));
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found[0], "Missing method " + selectedMethod + selectedDescriptor + " in " + entryName);
        return List.copyOf(instructions);
    }

    private static int assertFalseReturnFallthrough(List<Instruction> code, int predicate,
                                                    int expectedJump,
                                                    String message) {
        int jump = nextExecutable(code, predicate + 1);
        assertEquals(expectedJump, code.get(jump).opcode(), message);
        int falseConstant = nextExecutable(code, jump + 1);
        int falseReturn = nextExecutable(code, falseConstant + 1);
        assertEquals(Opcodes.ICONST_0, code.get(falseConstant).opcode(), message);
        assertEquals(Opcodes.IRETURN, code.get(falseReturn).opcode(), message);
        int target = jumpTargetIndex(code, jump);
        assertTrue(target > falseReturn, message);
        return target;
    }

    private static int methodIndex(List<Instruction> code, String owner, String name,
                                   String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.METHOD
                    && owner.equals(instruction.owner())
                    && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int fieldIndex(List<Instruction> code, int opcode, String owner, String name,
                                  String descriptor, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.FIELD && instruction.opcode() == opcode
                    && owner.equals(instruction.owner())
                    && name.equals(instruction.name())
                    && descriptor.equals(instruction.descriptor())) return index;
        }
        return -1;
    }

    private static int typeIndex(List<Instruction> code, int opcode, String type, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.TYPE && instruction.opcode() == opcode
                    && type.equals(instruction.owner())) return index;
        }
        return -1;
    }

    private static int ldcIndex(List<Instruction> code, Object value, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.LDC && value.equals(instruction.operand())) return index;
        }
        return -1;
    }

    private static int opcodeIndex(List<Instruction> code, int opcode, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            if (code.get(index).opcode() == opcode) return index;
        }
        return -1;
    }

    private static int nextExecutable(List<Instruction> code, int start) {
        for (int index = Math.max(0, start); index < code.size(); index++) {
            if (code.get(index).kind() != InstructionKind.LABEL) return index;
        }
        throw new AssertionError("No executable instruction at or after index " + start);
    }

    private static int previousExecutable(List<Instruction> code, int start) {
        for (int index = Math.min(start, code.size() - 1); index >= 0; index--) {
            if (code.get(index).kind() != InstructionKind.LABEL) return index;
        }
        throw new AssertionError("No executable instruction at or before index " + start);
    }

    private static int jumpTargetIndex(List<Instruction> code, int jumpIndex) {
        Object target = code.get(jumpIndex).operand();
        assertTrue(target instanceof Label, "Expected a jump at instruction " + jumpIndex);
        for (int index = 0; index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.kind() == InstructionKind.LABEL && instruction.operand() == target) return index;
        }
        throw new AssertionError("Missing jump target label for instruction " + jumpIndex);
    }

    private enum InstructionKind {
        INSN, INT, VAR, TYPE, FIELD, METHOD, INVOKEDYNAMIC, JUMP, LABEL, LDC, IINC
    }

    private record Instruction(InstructionKind kind, int opcode, String owner, String name,
                               String descriptor, Object operand) {}

    private record CompiledClassShape(Set<String> interfaces, Set<String> annotations,
                                      Set<String> mixinTargets, Map<String, Integer> methods) {}

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
