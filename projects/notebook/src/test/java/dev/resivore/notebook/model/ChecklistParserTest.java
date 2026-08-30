package dev.resivore.notebook.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistParserTest {
    @Test
    void parsesOnlySupportedPlainTextMarkers() {
        String source = """
                # Heading
                [ ] unfinished task
                  [x] completed task
                [X]\tuppercase works
                - [ ] list syntax is deliberately outside the subset
                ordinary [x] text
                [] malformed
                """;

        List<ChecklistParser.Entry> entries = ChecklistParser.parse(source);
        assertEquals(3, entries.size());
        assertEquals("unfinished task", entries.get(0).text());
        assertFalse(entries.get(0).checked());
        assertEquals("completed task", entries.get(1).text());
        assertTrue(entries.get(1).checked());
        assertEquals("uppercase works", entries.get(2).text());
        assertTrue(entries.get(2).checked());
    }

    @Test
    void togglingChangesOnlyMarkerAndPreservesCrLfAndUnicode() {
        String source = "intro\r\n[ ] 雪 task\r\n\t[x] done\r\nending";
        List<ChecklistParser.Entry> entries = ChecklistParser.parse(source);

        String checked = ChecklistParser.toggle(source, entries.getFirst());
        assertEquals("intro\r\n[x] 雪 task\r\n\t[x] done\r\nending", checked);
        String unchecked = ChecklistParser.toggleAtLine(checked, 2);
        assertEquals("intro\r\n[x] 雪 task\r\n\t[ ] done\r\nending", unchecked);
        assertEquals(unchecked, ChecklistParser.toggleAtLine(unchecked, 0));
    }

    @Test
    void staleParsedEntryCannotMutateUnrelatedText() {
        ChecklistParser.Entry entry = ChecklistParser.parse("[ ] task").getFirst();
        assertThrows(IllegalArgumentException.class, () -> ChecklistParser.toggle("not a task", entry));
    }
}
