package dev.resivore.notebook.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookClientWiringTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void keybindAndBothPlayerInventoryScreensUseCurrentFabricApis() throws IOException {
        String client = read("src/main/java/dev/resivore/notebook/NotebookClient.java");

        assertTrue(client.contains("KeyMappingHelper.registerKeyMapping"));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register"));
        assertTrue(client.contains("openKey.consumeClick()"));
        assertTrue(client.contains("client.gui.screen()"));
        assertTrue(client.contains("ScreenEvents.AFTER_INIT.register"));
        assertTrue(client.contains("Screens.getWidgets(screen)"));
        assertTrue(client.contains("screen instanceof InventoryScreen"));
        assertTrue(client.contains("screen instanceof CreativeModeInventoryScreen"));
        assertTrue(client.contains("inventoryButtons.remove(screen)"));
        assertTrue(client.contains("widgets.remove(previous)"));
        assertTrue(client.contains("new WeakReference<>(button)"));
        assertTrue(client.contains("getConfigDir().resolve(\"notebook\")"));
        assertTrue(client.contains("setScreenAndShow(new NotebookScreen"));
    }

    @Test
    void actualContainerBoundsDriveCompactCollisionAwareButtonPlacement() throws IOException {
        String accessor = read(
                "src/main/java/dev/resivore/notebook/mixin/client/ContainerScreenAccess.java");
        String client = read("src/main/java/dev/resivore/notebook/NotebookClient.java");

        assertTrue(accessor.contains("@Mixin(AbstractContainerScreen.class)"));
        assertTrue(accessor.contains("@Accessor(\"leftPos\")"));
        assertTrue(accessor.contains("@Accessor(\"topPos\")"));
        assertTrue(accessor.contains("@Accessor(\"imageWidth\")"));
        assertTrue(client.contains("widget.getRight()"));
        assertTrue(client.contains("widget.getBottom()"));
        assertTrue(client.contains("INVENTORY_BUTTON_SIZE = 18"));
    }

    @Test
    void readingEditingAndFilesystemWrappingContractsRemainSeparated() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");

        assertTrue(screen.contains("public void added()"));
        assertTrue(screen.contains("store.rescan()"));
        assertTrue(screen.contains("public void removed()"));
        assertTrue(screen.contains("MultiLineEditBox.builder()"));
        assertFalse(screen.contains("setCharacterLimit("));
        assertFalse(screen.contains("setLineLimit("));
        assertTrue(screen.contains("font.split(component"));
        assertTrue(screen.contains("ChecklistParser.toggleAtLine"));
        assertTrue(screen.contains("store.move(draggedId, dragTargetIndex)"));
    }

    @Test
    void bookArtAndRulesUseTheCurrentGuiAndTextLayoutContracts() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");

        assertTrue(screen.contains("renderBookArtwork(graphics, x, y)"));
        assertTrue(screen.contains("RenderPipelines.GUI_TEXTURED"));
        assertTrue(screen.contains("BOOK_TEXTURE_WIDTH"));
        assertTrue(screen.contains("BOOK_TEXTURE_HEIGHT"));
        assertTrue(screen.contains("PageTextLayout.forCurrentEditor(layout, font.lineHeight)"));
        assertTrue(screen.contains("AbstractTextAreaWidget.DEFAULT_TOTAL_PADDING"));
        assertTrue(screen.contains("bodyEditor.scrollAmount()"));
        assertFalse(screen.contains("TEXT_LINE_HEIGHT"));
    }

    @Test
    void clickToEditAutosaveAndNoShadowContractsRemainExplicit() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");
        String store = read("src/main/java/dev/resivore/notebook/storage/NotebookStore.java");

        assertFalse(screen.contains("editButton"));
        assertFalse(screen.contains("screen.notebook.edit"));
        assertFalse(screen.contains("screen.notebook.done"));
        assertTrue(screen.contains("beginEditingAt(titleEditor, event, doubleClick)"));
        assertTrue(screen.contains("beginEditingAt(bodyEditor, event, doubleClick)"));
        assertTrue(screen.contains("editor.onClick(event, doubleClick)"));
        assertTrue(screen.indexOf("toggleChecklist(hit.logicalLine())")
                < screen.indexOf("beginEditingAt(bodyEditor, event, doubleClick)"));
        assertTrue(screen.contains("AUTOSAVE_DELAY_TICKS"));
        assertTrue(screen.contains("flushEdits(false)"));
        assertTrue(screen.contains("if (!editorDirty)"));
        assertTrue(screen.contains("store.updateBody(selectedId, bodyEditor.getValue(), !sessionBodyBackedUp)"));
        assertTrue(store.contains("updateBody(UUID id, String body, boolean retainBackup)"));
        assertFalse(screen.contains("COLOR_DIM"));
        assertFalse(screen.contains("drawEditPaper"));
        assertFalse(screen.contains("centeredText("));
        assertTrue(screen.contains("titleEditor.setTextShadow(false)"));
        assertTrue(screen.contains(".setTextShadow(false)"));
        assertTrue(screen.contains("graphics.text(font, text, centerX - font.width(text) / 2, y, color, false)"));
    }

    @Test
    void checklistUsesExactCheckedSpriteAndLiveInkUncheckedBoxWithoutChangingInteraction() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");

        assertTrue(screen.contains("CHECKBOX_SIZE = 8"));
        assertTrue(screen.contains("pageText.textY(noteScroll, 0)"));
        assertTrue(screen.contains("pageText.checkboxY(y, CHECKBOX_SIZE)"));
        assertTrue(screen.contains("CHECKBOX_DRAW_OFFSET + CHECKBOX_SIZE + CHECKBOX_TEXT_GAP"));
        assertTrue(screen.contains("CHECKBOX_SIZE + CHECKBOX_HIT_PADDING * 2"));
        assertTrue(screen.contains("CHECKED_CHECKBOX_TEXTURE"));
        assertTrue(screen.contains("textures/gui/notebook_checkbox_checked.png"));
        assertTrue(screen.contains("graphics.blit("));
        assertTrue(screen.contains("RenderPipelines.GUI_TEXTURED"));
        assertTrue(screen.contains("COLOR_INK = 0xFF2A2119"));
        assertTrue(screen.contains("graphics.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_INK)"));
        assertFalse(screen.contains("COLOR_CHECKBOX_CHECKED"));
        assertFalse(screen.contains("COLOR_CHECK_MARK"));
        assertFalse(screen.contains("COLOR_CHECKBOX_INTERIOR"));
        assertFalse(screen.contains("COLOR_CHECKBOX_OUTLINE"));
        assertFalse(screen.contains("drawCheckMark"));
        assertTrue(screen.indexOf("toggleChecklist(hit.logicalLine())")
                < screen.indexOf("beginEditingAt(bodyEditor, event, doubleClick)"));
    }

    @Test
    void checkedCheckboxSpriteIsTheExactSuppliedEightPixelGreenMatrix() throws IOException {
        BufferedImage sprite = ImageIO.read(ROOT.resolve(
                "src/main/resources/assets/notebook/textures/gui/notebook_checkbox_checked.png").toFile());
        String[] expected = {
                "GGGGGGGG",
                "G......G",
                "G....G.G",
                "G...G..G",
                "G.G.G..G",
                "G..G...G",
                "G......G",
                "GGGGGGGG"
        };

        assertTrue(sprite != null);
        assertTrue(sprite.getWidth() == 8 && sprite.getHeight() == 8);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int expectedPixel = expected[y].charAt(x) == 'G' ? 0xFF43693F : 0x00000000;
                assertTrue(sprite.getRGB(x, y) == expectedPixel,
                        "unexpected checked-checkbox pixel at " + x + "," + y);
            }
        }
    }

    @Test
    void uncheckedCheckboxIsTheEightPixelTransparentLiveInkHollowBox() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");
        int methodStart = screen.indexOf("private void drawCheckbox");
        int methodEnd = screen.indexOf("private void drawReadingScrollbar", methodStart);
        String checkboxMethod = screen.substring(methodStart, methodEnd);
        int uncheckedStart = checkboxMethod.indexOf(
                "graphics.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_INK)");
        String uncheckedPath = checkboxMethod.substring(uncheckedStart);

        assertTrue(methodStart >= 0 && methodEnd > methodStart && uncheckedStart >= 0);
        assertTrue(checkboxMethod.contains("CHECKBOX_SIZE = 8") || screen.contains("CHECKBOX_SIZE = 8"));
        assertFalse(uncheckedPath.contains("graphics.fill("));
        assertFalse(uncheckedPath.contains("0xFF43693F"));
    }

    @Test
    void escapeExplicitlyExitsEditingBeforeItCanUseTheScreenClosePath() throws IOException {
        String screen = read("src/main/java/dev/resivore/notebook/client/NotebookScreen.java");

        int escape = screen.indexOf("if (event.isEscape())");
        int editingEscape = screen.indexOf("if (editing)", escape);
        int flushAndExit = screen.indexOf("flushEdits(true);", editingEscape);
        int consumedFirstEscape = screen.indexOf("return true;", flushAndExit);
        int close = screen.indexOf("onClose();", consumedFirstEscape);

        assertTrue(escape >= 0);
        assertTrue(editingEscape > escape);
        assertTrue(flushAndExit > editingEscape);
        assertTrue(consumedFirstEscape > flushAndExit);
        assertTrue(close > consumedFirstEscape);
    }

    @Test
    void modMetadataIsClientOnlyAndUsesTheCurrentTargetStack() throws IOException {
        String metadata = read("src/main/resources/fabric.mod.json");
        String mixins = read("src/main/resources/notebook.client.mixins.json");

        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertTrue(metadata.contains("dev.resivore.notebook.NotebookClient"));
        assertTrue(metadata.contains("\"fabric-api\": \">=0.157.0+26.2\""));
        assertTrue(metadata.contains("\"minecraft\": \"=26.2\""));
        assertTrue(metadata.contains("\"java\": \">=25\""));
        assertTrue(mixins.contains("\"compatibilityLevel\": \"JAVA_25\""));
        assertTrue(mixins.contains("\"ContainerScreenAccess\""));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
