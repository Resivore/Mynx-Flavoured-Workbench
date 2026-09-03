package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientRenderingContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path JAVA = ROOT.resolve("src/main/java/dev/resivore/slotreservations");

    @Test
    void emptyReservationUsesOneScopedGhostDrawAndLiteralVanillaPositionedZero() throws IOException {
        String screen = source("mixin/client/AbstractContainerScreenMixin.java");
        String renderer = source("client/ReservationVisualRenderer.java");
        String scope = source("client/GhostItemRenderScope.java");

        assertTrue(screen.contains("ReservationVisualRenderer.extract("));
        assertFalse(screen.contains("GHOST_WASH"));
        assertFalse(screen.contains("graphics.outline("));
        assertFalse(renderer.contains("graphics.outline("));
        assertFalse(renderer.contains("slot.x, slot.y, slot.x + 16"));
        assertTrue(renderer.contains("public static final float GHOST_ALPHA = 0.35F"));
        assertTrue(renderer.contains("public static final int GHOST_ALPHA_8 = 0x59"));
        assertTrue(renderer.contains("GhostItemRenderScope.extract("));
        assertTrue(renderer.contains("private static final String EMPTY_COUNT = \"0\""));
        assertTrue(renderer.contains("return itemX + 17 - glyphWidth"));
        assertTrue(renderer.contains("return itemY + 9"));
        assertTrue(renderer.contains("-1,"));
        assertTrue(renderer.contains("true"));
        assertFalse(renderer.contains("copyWithCount(0)"));
        assertTrue(scope.contains("try {"));
        assertTrue(scope.contains("finally {"));
        assertTrue(scope.contains("ACTIVE_ALPHA.remove()"));
    }

    @Test
    void atlasBlitAlphaIsPerItemStateAndOptionalTooltipMixinsHaveNoFuzsLinkage() throws IOException {
        String extractor = source("mixin/client/GuiGraphicsExtractorGhostMixin.java");
        String state = source("mixin/client/GuiItemRenderStateGhostMixin.java");
        String guiRenderer = source("mixin/client/GuiRendererGhostMixin.java");
        String pipRenderer = source("mixin/client/PictureInPictureRendererGhostMixin.java");
        String itemTooltip = source("mixin/client/ItemContentsTooltipSourceMixin.java");
        String clientTooltip = source("mixin/client/ClientItemContentsTooltipMixin.java");
        String storageTooltip = source("mixin/client/ContainerStorageTooltipMixin.java");
        String fabricMetadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));

        assertTrue(extractor.contains("GuiRenderState;addItem"));
        assertTrue(extractor.contains("GhostItemRenderScope.activeAlpha()"));
        assertTrue(state.contains("GhostItemRenderScope.OPAQUE_ALPHA"));
        assertTrue(guiRenderer.contains(
                "method = \"submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;"
        ));
        assertTrue(guiRenderer.contains("GhostItemRenderScope.premultipliedWhite(alpha)"));
        assertTrue(pipRenderer.contains("state instanceof OversizedItemRenderState"));
        assertTrue(pipRenderer.contains(
                "method = \"blitTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;"
        ));
        assertTrue(pipRenderer.contains("GhostItemRenderScope.premultipliedWhite(alpha)"));
        assertTrue(itemTooltip.contains("@Pseudo"));
        assertTrue(clientTooltip.contains("@Pseudo"));
        assertTrue(clientTooltip.contains(
                "method = \"extractSlot(Lnet/minecraft/client/gui/Font;"
        ));
        assertTrue(clientTooltip.contains("slotX + 1"));
        assertTrue(clientTooltip.contains("slotY + 1"));
        assertFalse(itemTooltip.contains("import fuzs."));
        assertFalse(clientTooltip.contains("import fuzs."));
        assertTrue(storageTooltip.contains("@Pseudo"));
        assertTrue(storageTooltip.contains(
                "method = \"createTooltipImageComponent(Lnet/minecraft/world/item/ItemStack;"
        ));
        assertTrue(storageTooltip.contains("EasyShulkerTooltipCompat.captureSource("));
        assertFalse(storageTooltip.contains("import fuzs."));
        assertTrue(fabricMetadata.contains("\"easyshulkerboxes\": \"*\""));
        assertTrue(fabricMetadata.contains("\"iteminteractions\": \"*\""));
    }

    @Test
    void ordinaryTestRuntimeDoesNotCarryOptionalMods() {
        assertFalse(classExists(
                "fuzs.iteminteractions.common.api.v2.world.inventory.tooltip.ItemContentsTooltip"));
        assertFalse(classExists("fuzs.easyshulkerboxes.common.EasyShulkerBoxes"));
        assertTrue(classExists("dev.resivore.slotreservations.client.EasyShulkerTooltipCompat"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ClientRenderingContractTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
