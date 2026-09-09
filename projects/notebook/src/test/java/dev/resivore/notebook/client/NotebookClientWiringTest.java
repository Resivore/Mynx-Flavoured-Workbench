package dev.resivore.notebook.client;

import org.junit.jupiter.api.Test;

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
