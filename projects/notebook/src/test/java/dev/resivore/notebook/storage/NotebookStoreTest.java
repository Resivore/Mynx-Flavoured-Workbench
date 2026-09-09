package dev.resivore.notebook.storage;

import com.google.gson.JsonParser;
import dev.resivore.notebook.model.NotebookNote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void crudReloadNewlinesUnicodeLongAndEmptyNotesRemainPlainMarkdown() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();

        String longLine = "very long line ".repeat(2_000);
        String body = "# Ancient City\n\n[ ] Bring wool\nUnicode: 雪 🐈 café\n" + longLine;
        NotebookNote ancient = store.create("Ancient City", body);
        NotebookNote empty = store.create("Empty", "");

        assertEquals(body, Files.readString(
                root.resolve("notes").resolve(ancient.fileName()), StandardCharsets.UTF_8
        ));
        assertFalse(Files.readString(root.resolve("notebook.json"), StandardCharsets.UTF_8)
                .contains("Bring wool"));

        store.move(empty.id(), 0);
        store.select(ancient.id());
        NotebookNote renamed = store.rename(ancient.id(), "Ancient City: Plans?");
        String revised = body + "\n[x] Looted\n";
        store.updateBody(ancient.id(), revised);

        assertEquals(ancient.id(), renamed.id());
        assertFalse(renamed.fileName().contains(":"));
        assertFalse(renamed.fileName().contains("?"));

        NotebookStore reloaded = new NotebookStore(root);
        reloaded.rescan();
        assertEquals(List.of(empty.id(), ancient.id()), ids(reloaded.notes()));
        assertEquals(ancient.id(), reloaded.selectedId().orElseThrow());
        assertEquals("", reloaded.find(empty.id()).orElseThrow().body());
        assertEquals(revised, reloaded.find(ancient.id()).orElseThrow().body());
        assertEquals("Ancient City: Plans?", reloaded.find(ancient.id()).orElseThrow().title());

        assertTrue(reloaded.delete(empty.id()));
        assertFalse(reloaded.delete(empty.id()));
        NotebookStore afterDelete = new NotebookStore(root);
        afterDelete.rescan();
        assertEquals(List.of(ancient.id()), ids(afterDelete.notes()));
    }

    @Test
    void duplicateAndUnsafeTitlesProduceDistinctPortableFilesWithoutChangingDisplayTitle() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();

        NotebookNote first = store.create("CON", "first");
        NotebookNote second = store.create("CON", "second");
        NotebookNote untitled = store.create("  ", "third");

        assertEquals("CON", first.title());
        assertEquals("CON_.md", first.fileName());
        assertEquals("CON_ (2).md", second.fileName());
        assertEquals("Untitled", untitled.title());
        assertEquals("Untitled.md", untitled.fileName());
    }

    @Test
    void rescanLoadsExternalChangesCreationsAndDeletions() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();
        NotebookNote first = store.create("First", "inside");
        NotebookNote removed = store.create("Removed", "gone");

        Files.writeString(
                root.resolve("notes").resolve(first.fileName()),
                "externally edited\nwith real lines\n",
                StandardCharsets.UTF_8
        );
        Files.writeString(
                root.resolve("notes").resolve("Outside 雪.md"),
                "created outside",
                StandardCharsets.UTF_8
        );
        Files.delete(root.resolve("notes").resolve(removed.fileName()));

        store.rescan();
        assertEquals("externally edited\nwith real lines\n", store.find(first.id()).orElseThrow().body());
        assertTrue(store.find(removed.id()).isEmpty());
        NotebookNote external = store.notes().stream()
                .filter(note -> note.title().equals("Outside 雪"))
                .findFirst()
                .orElseThrow();
        assertEquals("created outside", external.body());
        assertTrue(store.selectedId().stream().anyMatch(id -> store.find(id).isPresent()));

        NotebookStore reloaded = new NotebookStore(root);
        reloaded.rescan();
        assertEquals(external.id(), reloaded.find(external.id()).orElseThrow().id());
    }

    @Test
    void externalRenameIsConservativelyADeleteAndNewIdentity() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();
        NotebookNote original = store.create("Original", "same body");

        Files.move(
                root.resolve("notes").resolve(original.fileName()),
                root.resolve("notes").resolve("Renamed Outside.md")
        );
        store.rescan();

        assertTrue(store.find(original.id()).isEmpty());
        NotebookNote externallyRenamed = store.notes().getFirst();
        assertNotEquals(original.id(), externallyRenamed.id());
        assertEquals("Renamed Outside", externallyRenamed.title());
        assertEquals("same body", externallyRenamed.body());
    }

    @Test
    void staleAndMalformedMetadataAreRepairedWithoutMakingNotesUnavailable() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        Path notes = root.resolve("notes");
        Files.createDirectories(notes);
        Files.writeString(notes.resolve("One.md"), "one", StandardCharsets.UTF_8);
        Files.writeString(notes.resolve("Two.md"), "two", StandardCharsets.UTF_8);

        UUID oneId = UUID.randomUUID();
        UUID staleId = UUID.randomUUID();
        String staleMetadata = """
                {
                  "format": 1,
                  "selectedId": "%s",
                  "order": ["%s", "%s", "not-a-uuid"],
                  "notes": {
                    "%s": {"title": "One Custom", "file": "One.md"},
                    "%s": {"title": "Missing", "file": "Missing.md"},
                    "also-not-a-uuid": {"title": "Unsafe", "file": "../Outside.md"}
                  }
                }
                """.formatted(staleId, staleId, oneId, oneId, staleId);
        Files.writeString(root.resolve("notebook.json"), staleMetadata, StandardCharsets.UTF_8);

        NotebookStore store = new NotebookStore(root);
        store.rescan();
        assertEquals(2, store.notes().size());
        assertEquals(oneId, store.notes().getFirst().id());
        assertEquals("One Custom", store.notes().getFirst().title());
        assertTrue(store.find(staleId).isEmpty());
        assertEquals(oneId, store.selectedId().orElseThrow());
        JsonParser.parseString(Files.readString(root.resolve("notebook.json"), StandardCharsets.UTF_8));

        Files.writeString(root.resolve("notebook.json"), "{ definitely malformed", StandardCharsets.UTF_8);
        store.rescan();
        assertEquals(2, store.notes().size());
        assertTrue(store.notes().stream().allMatch(note -> note.id() != null));
        JsonParser.parseString(Files.readString(root.resolve("notebook.json"), StandardCharsets.UTF_8));
    }

    @Test
    void explicitManualOrderSurvivesRepeatedReloads() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();
        NotebookNote alpha = store.create("Alpha", "a");
        NotebookNote beta = store.create("Beta", "b");
        NotebookNote gamma = store.create("Gamma", "c");

        store.move(gamma.id(), 0);
        store.move(alpha.id(), 2);
        List<UUID> expected = List.of(gamma.id(), beta.id(), alpha.id());
        assertEquals(expected, ids(store.notes()));

        NotebookStore firstReload = new NotebookStore(root);
        firstReload.rescan();
        assertEquals(expected, ids(firstReload.notes()));
        NotebookStore secondReload = new NotebookStore(root);
        secondReload.rescan();
        assertEquals(expected, ids(secondReload.notes()));

        String metadata = Files.readString(root.resolve("notebook.json"), StandardCharsets.UTF_8);
        assertTrue(metadata.contains("\"order\""));
        assertTrue(metadata.indexOf(gamma.id().toString()) < metadata.indexOf(beta.id().toString()));
    }

    @Test
    void bodyUpdatesAndDeleteKeepOnlyThreeRotatingPriorVersions() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();
        NotebookNote note = store.create("Versions", "v0");

        store.updateBody(note.id(), "v1");
        store.updateBody(note.id(), "v2");
        store.updateBody(note.id(), "v3");
        store.updateBody(note.id(), "v4");

        Path backups = root.resolve("backups").resolve(note.id().toString());
        assertEquals("v3", Files.readString(backups.resolve("previous-1.md"), StandardCharsets.UTF_8));
        assertEquals("v2", Files.readString(backups.resolve("previous-2.md"), StandardCharsets.UTF_8));
        assertEquals("v1", Files.readString(backups.resolve("previous-3.md"), StandardCharsets.UTF_8));
        try (var stream = Files.list(backups)) {
            assertEquals(3, stream.count());
        }

        store.delete(note.id());
        assertEquals("v4", Files.readString(backups.resolve("previous-1.md"), StandardCharsets.UTF_8));
        assertEquals("v3", Files.readString(backups.resolve("previous-2.md"), StandardCharsets.UTF_8));
        assertEquals("v2", Files.readString(backups.resolve("previous-3.md"), StandardCharsets.UTF_8));
    }

    @Test
    void continuousEditSessionAutosavesKeepOnePreSessionBackup() throws IOException {
        Path root = temporaryDirectory.resolve("notebook");
        NotebookStore store = new NotebookStore(root);
        store.rescan();
        NotebookNote note = store.create("Autosave", "before");

        store.updateBody(note.id(), "first autosave", true);
        store.updateBody(note.id(), "second autosave", false);
        store.updateBody(note.id(), "third autosave", false);
        store.updateBody(note.id(), "third autosave", false);

        Path backups = root.resolve("backups").resolve(note.id().toString());
        assertEquals("before", Files.readString(backups.resolve("previous-1.md"), StandardCharsets.UTF_8));
        try (var stream = Files.list(backups)) {
            assertEquals(1, stream.count());
        }
        assertEquals("third autosave", store.find(note.id()).orElseThrow().body());

        store.updateBody(note.id(), "next session", true);
        assertEquals("third autosave", Files.readString(backups.resolve("previous-1.md"), StandardCharsets.UTF_8));
        assertEquals("before", Files.readString(backups.resolve("previous-2.md"), StandardCharsets.UTF_8));
    }

    private static List<UUID> ids(List<NotebookNote> notes) {
        return notes.stream().map(NotebookNote::id).toList();
    }
}
