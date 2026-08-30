package dev.resivore.notebook.model;

import java.util.Objects;
import java.util.UUID;

/**
 * An immutable snapshot of one note.
 *
 * <p>The UUID is the durable identity. The title and file name are deliberately
 * separate so a display rename does not become the note's identity.</p>
 */
public record NotebookNote(UUID id, String title, String fileName, String body) {
    public NotebookNote {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(body, "body");

        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
    }
}
