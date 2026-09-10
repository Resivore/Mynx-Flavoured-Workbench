package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnInventoryButtonPresentationTest {
    @Test
    void iconButtonKeepsTheUpstreamActionsButUsesTheSharedEighteenPixelBounds() throws Exception {
        String source = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackIconButtonMixin.java");

        assertTrue(source.contains("QuickStackIconButton.class"));
        assertTrue(source.contains("Button.OnPress secondaryOnPress"));
        assertTrue(source.contains("setSize(18, 18)"));
        assertFalse(source.contains("setTooltip("));
        assertFalse(source.contains("onPress("));
    }

    @Test
    void iconRendererUsesAuditedMinecraftButtonChromeWithoutABrittleQsnShadow() throws Exception {
        String source = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackCustomButtonChromeMixin.java");

        assertTrue(source.contains("@Mixin(targets = \"tempeststudios.quickstacknearby.QuickStackCustomButtonBase\", remap = false)"));
        assertTrue(source.contains("extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"));
        assertTrue(source.contains("if (!((Object) this instanceof QuickStackIconButton))"));
        assertTrue(source.contains("RenderPipelines.GUI_TEXTURED"));
        assertTrue(source.contains("NORMAL_BUTTON_SPRITES.get(button.active, button.isHoveredOrFocused())"));
        assertTrue(source.contains("ARGB.white(button.getAlpha())"));
        assertTrue(source.contains("36x36 (2x GUI-scale) user reference"));
        assertTrue(source.contains("0xFFFFFFFF"));
        assertTrue(source.contains("0xFF3F3F3F"));
        assertTrue(source.contains("callback.cancel()"));
        assertFalse(source.contains("@Shadow"));
        assertFalse(source.contains("extractDefaultSprite"));
        assertFalse(source.contains("blit("));
    }

    @Test
    void exactQsnBytecodeResolvesEveryC14PresentationSeamAndRejectsTheC13FailureMode()
            throws Exception {
        try (JarFile jar = new JarFile(referenceJar().toFile())) {
            ClassNode base = readClass(jar, "tempeststudios/quickstacknearby/QuickStackCustomButtonBase.class");
            ClassNode icon = readClass(jar, "tempeststudios/quickstacknearby/QuickStackIconButton.class");
            ClassNode modal = readClass(jar, "tempeststudios/quickstacknearby/QuickStackModalIconButton.class");
            ClassNode text = readClass(jar, "tempeststudios/quickstacknearby/QuickStackTextButton.class");

            assertEquals("net/minecraft/client/gui/components/Button", base.superName);
            assertTrue(hasMethod(base,
                    "extractContents",
                    "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"));
            assertFalse(hasMethod(base,
                    "extractDefaultSprite",
                    "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"),
                    "C13's invalid shadow must never return");
            assertEquals("tempeststudios/quickstacknearby/QuickStackCustomButtonBase", icon.superName);
            assertTrue(hasMethod(icon,
                    "paintButton",
                    "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"),
                    "C14 injects only into the exact production-declared icon rendering seam");
            assertTrue(hasMethod(icon,
                    "mouseClicked",
                    "(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"));
            assertTrue(hasMethod(modal,
                    "paintButton",
                    "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"));
            assertTrue(hasMethod(text,
                    "paintButton",
                    "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"));
        }
    }

    @Test
    void survivalPlacementIsGuardedAndTheClientMixinsArePackaged() throws Exception {
        String placement = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackButtonSlotBridgeMixin.java");
        String mixins = read("src/main/resources/quick_stack_nearby_compat.client.mixins.json");

        assertTrue(placement.contains("screen instanceof InventoryScreen"));
        assertTrue(placement.contains("firstBottomUpFreePosition"));
        assertTrue(placement.contains("Screens.getWidgets(screen)"));
        assertTrue(mixins.contains("QuickStackIconButtonMixin"));
        assertTrue(mixins.contains("QuickStackCustomButtonChromeMixin"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(System.getProperty("projectRoot")).resolve(relative), StandardCharsets.UTF_8);
    }

    private static Path referenceJar() {
        return Path.of(System.getProperty("qsnReferenceJar"));
    }

    private static ClassNode readClass(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertTrue(entry != null, "Missing exact upstream class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static boolean hasMethod(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }
}
