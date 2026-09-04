package dev.resivore.slotreservations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(renderer.contains("case UNRESERVED -> {"));
        assertTrue(renderer.contains("case OCCUPIED_RESERVED -> graphics.blitSprite("));
        assertTrue(renderer.contains("RenderPipelines.GUI_TEXTURED,"));
        assertTrue(renderer.contains("\"occupied_reservation_marker\""));
        assertTrue(renderer.contains("itemX + 13,"));
        assertTrue(renderer.contains("itemY,"));
        assertTrue(renderer.contains("private static final int OCCUPIED_MARKER_SIZE = 3"));
        assertEquals(3, occurrences(renderer, "OCCUPIED_MARKER_SIZE"),
                "The exact 3x3 sprite must be drawn one-to-one without scaling");
        assertFalse(renderer.contains("graphics.fill("));
        assertFalse(renderer.contains("0xFF24C7B8"));
        assertFalse(renderer.contains("copyWithCount(0)"));
        assertTrue(screen.contains("slot.x,"));
        assertTrue(screen.contains("slot.y,"));
        assertTrue(scope.contains("try {"));
        assertTrue(scope.contains("finally {"));
        assertTrue(scope.contains("ACTIVE_ALPHA.remove()"));
    }

    @Test
    void atlasAndPipGhostsShareAnIsolatedNeutralWhiteAlphaPipeline() throws IOException {
        String extractor = source("mixin/client/GuiGraphicsExtractorGhostMixin.java");
        String state = source("mixin/client/GuiItemRenderStateGhostMixin.java");
        String guiRenderer = source("mixin/client/GuiRendererGhostMixin.java");
        String pipRenderer = source("mixin/client/PictureInPictureRendererGhostMixin.java");
        String pipeline = source("client/GhostItemRenderPipeline.java");
        String scope = source("client/GhostItemRenderScope.java");
        String shader = Files.readString(ROOT.resolve(
                "src/main/resources/assets/container_slot_reservations/shaders/core/ghost_item_alpha.fsh"
        ));

        assertTrue(extractor.contains("GuiRenderState;addItem"));
        assertTrue(extractor.contains("GhostItemRenderScope.activeAlpha()"));
        assertTrue(state.contains("GhostItemRenderScope.OPAQUE_ALPHA"));
        assertTrue(scope.contains("return ARGB.white(alpha)"));
        assertFalse(scope.contains("alpha << 16"));
        assertTrue(pipeline.contains("RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)"));
        assertTrue(pipeline.contains("BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA"));
        assertTrue(pipeline.contains("GhostItemRenderScope.alphaOnlyWhite(alpha)"));
        assertTrue(shader.contains("vec4 ghost = vec4(item.rgb * opacity, item.a * opacity);"));
        assertTrue(shader.contains("if (ghost.a == 0.0)"));
        assertFalse(shader.contains("vertexColor.rgb"),
                "The packed RGB channels must never tint the cached item texture");
        assertFalse(shader.contains("0.2627") || shader.contains("0.2118")
                        || shader.contains("0.1647"),
                "The Matcha #43362A background must never be baked into the sprite shader");
        assertFalse(shader.contains("vec4(0.0") || shader.contains("vec3(0.0"),
                "The final item blit must not introduce a black fill or matte");
        assertFalse(pipeline.contains("fill(") || guiRenderer.contains("fill(")
                        || pipRenderer.contains("fill("),
                "The ghost seam must remain a texture-only blit");
        assertEquals(1, occurrences(guiRenderer, "GhostItemRenderPipeline.blit("));
        assertEquals(1, occurrences(pipRenderer, "GhostItemRenderPipeline.blit("));
        assertTrue(guiRenderer.contains("at = @At(\"HEAD\")"));
        assertTrue(guiRenderer.contains("cancellable = true"));
        assertTrue(guiRenderer.contains("require = 1"));
        assertTrue(guiRenderer.contains("expect = 1"));
        assertTrue(guiRenderer.contains("allow = 1"));
        assertTrue(guiRenderer.contains("callback.cancel()"));
        assertTrue(guiRenderer.contains("if (alpha == GhostItemRenderScope.OPAQUE_ALPHA)"));
        assertTrue(pipRenderer.contains("state instanceof OversizedItemRenderState"));
        assertTrue(pipRenderer.contains("at = @At(\"HEAD\")"));
        assertTrue(pipRenderer.contains("cancellable = true"));
        assertTrue(pipRenderer.contains("require = 1"));
        assertTrue(pipRenderer.contains("expect = 1"));
        assertTrue(pipRenderer.contains("allow = 1"));
        assertTrue(pipRenderer.contains("callback.cancel()"));
        assertTrue(pipRenderer.contains("if (alpha == GhostItemRenderScope.OPAQUE_ALPHA)"));
        assertFalse(guiRenderer.contains("ModifyConstant"));
        assertFalse(pipRenderer.contains("ModifyConstant"));
        assertFalse(guiRenderer.contains("0x59595959"));
        assertFalse(pipRenderer.contains("0x59595959"));
    }

    @Test
    void optionalTooltipMixinsHaveNoFuzsLinkage() throws IOException {
        String itemTooltip = source("mixin/client/ItemContentsTooltipSourceMixin.java");
        String clientTooltip = source("mixin/client/ClientItemContentsTooltipMixin.java");
        String storageTooltip = source("mixin/client/ContainerStorageTooltipMixin.java");
        String fabricMetadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));

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

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(needle, index)) >= 0;
             index += needle.length()) {
            count++;
        }
        return count;
    }
}
