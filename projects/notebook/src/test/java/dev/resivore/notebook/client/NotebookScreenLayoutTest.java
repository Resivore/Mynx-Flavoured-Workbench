package dev.resivore.notebook.client;

import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookScreenLayoutTest {
    @ParameterizedTest
    @CsvSource({
            "320,240",
            "427,240",
            "640,360",
            "854,480",
            "1920,1080"
    })
    void bookAndBothInteractivePagesFitCommonScaledResolutions(int width, int height) {
        NotebookScreen.BookLayout layout = NotebookScreen.BookLayout.fit(width, height);

        assertTrue(layout.bookX() >= 0);
        assertTrue(layout.bookY() >= 0);
        assertTrue(layout.bookX() + layout.bookWidth() <= width);
        assertTrue(layout.bookY() + layout.bookHeight() <= height);
        assertTrue(layout.leftContentWidth() >= 90);
        assertTrue(layout.rightContentWidth() >= 90);
        assertTrue(layout.indexHeight() >= 90);
        assertTrue(layout.bodyHeight() >= 90);
        assertTrue(Math.abs(layout.bookWidth() * 400 - layout.bookHeight() * 640) <= 640);
    }

    @ParameterizedTest
    @CsvSource({
            "320,240",
            "854,480",
            "1920,1080"
    })
    void ruledRowsShareTheEditorTextOriginAndScrollGrid(int width, int height) {
        NotebookScreen.BookLayout book = NotebookScreen.BookLayout.fit(width, height);
        int lineHeight = 9; // Minecraft 26.2's live default font row height.
        NotebookScreen.PageTextLayout text =
                NotebookScreen.PageTextLayout.forCurrentEditor(book, lineHeight);

        assertEquals(
                text.viewportTop() + AbstractTextAreaWidget.DEFAULT_TOTAL_PADDING / 2,
                text.textTop());
        assertEquals(text.textTop() + lineHeight - 1, text.firstRuleY(0));
        assertEquals(text.firstRuleY(0) - lineHeight, text.firstRuleY(lineHeight));
        assertTrue(text.firstRuleY(0) < text.viewportBottom());
    }
}
