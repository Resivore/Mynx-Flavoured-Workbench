package dev.resivore.notebook.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recognizes the intentionally small Notebook checklist syntax without
 * changing any surrounding user-authored text or line endings.
 */
public final class ChecklistParser {
    private ChecklistParser() {
    }

    /**
     * Finds lines whose first non-indentation characters are {@code [ ]},
     * {@code [x]}, or {@code [X]}.
     */
    public static List<Entry> parse(String source) {
        Objects.requireNonNull(source, "source");

        List<Entry> entries = new ArrayList<>();
        int lineStart = 0;
        int lineIndex = 0;

        while (lineStart <= source.length()) {
            int newline = source.indexOf('\n', lineStart);
            int lineEnd = newline >= 0 ? newline : source.length();
            int contentEnd = lineEnd;
            if (contentEnd > lineStart && source.charAt(contentEnd - 1) == '\r') {
                contentEnd--;
            }

            int marker = lineStart;
            while (marker < contentEnd) {
                char character = source.charAt(marker);
                if (character != ' ' && character != '\t') {
                    break;
                }
                marker++;
            }

            if (marker + 2 < contentEnd
                    && source.charAt(marker) == '['
                    && source.charAt(marker + 2) == ']') {
                char state = source.charAt(marker + 1);
                if (state == ' ' || state == 'x' || state == 'X') {
                    int textStart = marker + 3;
                    while (textStart < contentEnd) {
                        char character = source.charAt(textStart);
                        if (character != ' ' && character != '\t') {
                            break;
                        }
                        textStart++;
                    }
                    entries.add(new Entry(
                            lineIndex,
                            lineStart,
                            contentEnd,
                            marker,
                            state == 'x' || state == 'X',
                            source.substring(textStart, contentEnd)
                    ));
                }
            }

            if (newline < 0) {
                break;
            }
            lineStart = newline + 1;
            lineIndex++;
        }

        return List.copyOf(entries);
    }

    /** Toggles exactly the marker represented by a previously parsed entry. */
    public static String toggle(String source, Entry entry) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(entry, "entry");

        int marker = entry.markerOffset();
        if (marker < 0
                || marker + 2 >= source.length()
                || source.charAt(marker) != '['
                || source.charAt(marker + 2) != ']') {
            throw new IllegalArgumentException("entry no longer identifies a checklist marker");
        }

        char state = source.charAt(marker + 1);
        if (state != ' ' && state != 'x' && state != 'X') {
            throw new IllegalArgumentException("entry no longer identifies a checklist marker");
        }

        StringBuilder toggled = new StringBuilder(source);
        toggled.setCharAt(marker + 1, state == ' ' ? 'x' : ' ');
        return toggled.toString();
    }

    /** Toggles a checklist marker on a source line, or returns the source unchanged. */
    public static String toggleAtLine(String source, int lineIndex) {
        if (lineIndex < 0) {
            return Objects.requireNonNull(source, "source");
        }
        return parse(source).stream()
                .filter(entry -> entry.lineIndex() == lineIndex)
                .findFirst()
                .map(entry -> toggle(source, entry))
                .orElse(source);
    }

    public record Entry(
            int lineIndex,
            int lineStart,
            int lineEnd,
            int markerOffset,
            boolean checked,
            String text
    ) {
        public Entry {
            Objects.requireNonNull(text, "text");
        }
    }
}
