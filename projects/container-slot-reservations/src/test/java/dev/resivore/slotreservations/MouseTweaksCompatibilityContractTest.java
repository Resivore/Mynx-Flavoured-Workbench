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
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bytecode/source contract for the exact optional Mouse Tweaks provider inspected by C20. */
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
        }
    }

    @Test
    void bridgeIsOptionalTransientAndRoutesMutationsThroughExistingAuthority() throws IOException {
        String panel = source("client/ShulkerPanel.java");
        String mixin = source("mixin/client/MouseTweaksContainerScreenMixin.java");
        String plugin = source("mixin/client/MouseTweaksMixinPlugin.java");
        String wheel = source("client/MouseTweaksCompatibility.java");
        String gesture = source("client/MouseTweaksRmbGesture.java");
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String actions = source("ShulkerPanelActions.java");
        String content = source("network/ShulkerPanelContentActionPayload.java");
        String payload = source("network/ShulkerPanelMenuQuickMovePayload.java");

        assertTrue(panel.contains("MOUSE_TWEAKS_VIRTUAL_CONTAINER"));
        assertTrue(panel.contains("Transient read-only view"));
        assertTrue(panel.contains("beginMouseTweaksRightGesture"));
        assertTrue(panel.contains("MOUSE_TWEAKS_RMB_GESTURE"));
        assertTrue(panel.contains("mouseTweaksShadowFingerprint"));
        assertTrue(panel.contains("ShulkerTransferPlanner.planExactInsertion"));
        assertTrue(panel.contains("SECONDARY_DEPOSIT"));
        assertTrue(panel.contains("mouseTweaksDepositBridge"),
                "An already-open panel must remain available for ordinary-slot -> panel deposit gestures");
        assertTrue(panel.contains("ItemStack.matches(mouseTweaksShadowCarried, binding.menu().getCarried())"),
                "A native boundary may resume a projected panel chain only after cursor convergence");
        assertTrue(panel.contains("mouseTweaksQuickMoveFromMenuSlot"));
        assertTrue(panel.contains("MouseTweaksCompatibility.consumeWheel"));
        assertTrue(mixin.contains("implements IMTModGuiContainer3Ex"));
        assertTrue(mixin.contains("ContainerInput.QUICK_MOVE"));
        assertTrue(mixin.contains("mouseTweaksNativeClick"));
        assertTrue(screen.contains("beginMouseTweaksRightGesture"));
        assertTrue(gesture.contains("COLLECTION_SOURCE"));
        assertTrue(gesture.contains("DEPOSIT"));
        assertTrue(gesture.contains("enterPanelCell"));
        assertTrue(gesture.contains("reset()"));
        assertTrue(plugin.contains("isModLoaded(\"mousetweaks\")"));
        assertTrue(wheel.contains("Class.forName(\"yalter.mousetweaks.Main\", false"));
        assertTrue(actions.contains("handleMenuQuickMove"));
        assertTrue(actions.contains("SECONDARY_DEPOSIT"));
        assertTrue(content.contains("PRIMARY, SECONDARY, QUICK_MOVE, SECONDARY_DEPOSIT"),
                "C21 must append its wire action without renumbering accepted C20 actions");
        assertTrue(actions.contains("ShulkerTransferPlanner.planInsertion(host.stack(), before)"));
        assertTrue(actions.contains("ShulkerHostResolver.removableSource(player, source)"));
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

    private static String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
