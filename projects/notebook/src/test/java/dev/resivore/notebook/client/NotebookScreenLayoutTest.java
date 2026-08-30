package dev.resivore.notebook.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    }
}
