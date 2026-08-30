package dev.resivore.notebook.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.notebook.model.NotebookNote;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Filesystem-backed Notebook model.
 *
 * <p>Note bodies remain ordinary UTF-8 Markdown files. The small JSON file is
 * machine-owned and contains only identity, ordering, selection, and file-name
 * associations. Calling {@link #rescan()} treats the files on disk as the
 * source of truth for which notes currently exist.</p>
 */
public final class NotebookStore {
    private static final int METADATA_FORMAT = 1;
    private static final int BACKUP_COUNT = 3;
    private static final int MAX_FILE_STEM_CODE_POINTS = 80;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final Path root;
    private final Path metadataFile;
    private final Path notesDirectory;
    private final Path backupsDirectory;
    private final ArrayList<NotebookNote> orderedNotes = new ArrayList<>();
    private UUID selectedId;

    /**
     * Creates an unloaded store rooted at the supplied notebook directory.
     * Call {@link #rescan()} at an appropriate client lifecycle/open boundary.
     */
    public NotebookStore(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.metadataFile = this.root.resolve("notebook.json");
        this.notesDirectory = this.root.resolve("notes");
        this.backupsDirectory = this.root.resolve("backups");
    }

    public synchronized Path root() {
        return root;
    }

    /**
     * Reloads note bodies and reconciles metadata with the current notes
     * directory. Missing references are discarded, and new Markdown files are
     * appended with new identities. File renames performed outside Minecraft
     * intentionally appear as delete-plus-create rather than being guessed.
     */
    public synchronized void rescan() throws IOException {
        ensureLayout();

        Metadata metadata = readMetadata();
        LinkedHashMap<String, Path> filesByKey = scanMarkdownFiles();
        ArrayList<NotebookNote> reconciled = new ArrayList<>();
        Set<UUID> addedIds = new HashSet<>();

        for (UUID id : metadata.orderedIds()) {
            addMetadataNote(metadata.notes().get(id), filesByKey, reconciled, addedIds);
        }
        for (MetadataNote note : metadata.notes().values()) {
            addMetadataNote(note, filesByKey, reconciled, addedIds);
        }

        for (Path path : filesByKey.values()) {
            String body;
            try {
                body = Files.readString(path, StandardCharsets.UTF_8);
            } catch (CharacterCodingException invalidUtf8) {
                // Only valid UTF-8 Markdown files participate in Notebook.
                continue;
            }
            reconciled.add(new NotebookNote(
                    UUID.randomUUID(),
                    titleFromFileName(path.getFileName().toString()),
                    path.getFileName().toString(),
                    body
            ));
        }

        orderedNotes.clear();
        orderedNotes.addAll(reconciled);
        selectedId = containsId(metadata.selectedId())
                ? metadata.selectedId()
                : orderedNotes.isEmpty() ? null : orderedNotes.getFirst().id();

        persistMetadataIfChanged();
    }

    public synchronized List<NotebookNote> notes() {
        return List.copyOf(orderedNotes);
    }

    public synchronized Optional<UUID> selectedId() {
        return Optional.ofNullable(selectedId);
    }

    public synchronized Optional<NotebookNote> find(UUID id) {
        Objects.requireNonNull(id, "id");
        return orderedNotes.stream().filter(note -> note.id().equals(id)).findFirst();
    }

    public synchronized void select(UUID id) throws IOException {
        requireNote(id);
        if (id.equals(selectedId)) {
            return;
        }

        UUID previous = selectedId;
        selectedId = id;
        try {
            persistMetadataIfChanged();
        } catch (IOException failure) {
            selectedId = previous;
            throw failure;
        }
    }

    public synchronized NotebookNote create(String title, String body) throws IOException {
        ensureLayout();
        String normalizedTitle = normalizeTitle(title);
        Objects.requireNonNull(body, "body");

        String fileName = availableFileName(normalizedTitle, null);
        Path noteFile = notesDirectory.resolve(fileName);
        NotebookNote note = new NotebookNote(UUID.randomUUID(), normalizedTitle, fileName, body);
        UUID previousSelection = selectedId;

        writeAtomic(noteFile, body);
        orderedNotes.add(note);
        selectedId = note.id();
        try {
            persistMetadataIfChanged();
        } catch (IOException failure) {
            orderedNotes.remove(note);
            selectedId = previousSelection;
            try {
                Files.deleteIfExists(noteFile);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        return note;
    }

    public synchronized NotebookNote rename(UUID id, String title) throws IOException {
        int index = requireNoteIndex(id);
        NotebookNote previous = orderedNotes.get(index);
        String normalizedTitle = normalizeTitle(title);
        if (previous.title().equals(normalizedTitle)) {
            return previous;
        }

        String fileName = availableFileName(normalizedTitle, previous.fileName());
        Path previousPath = notesDirectory.resolve(previous.fileName());
        Path renamedPath = notesDirectory.resolve(fileName);
        boolean pathChanged = !previousPath.equals(renamedPath);
        if (pathChanged) {
            moveAtomic(previousPath, renamedPath, false);
        }

        NotebookNote renamed = new NotebookNote(id, normalizedTitle, fileName, previous.body());
        orderedNotes.set(index, renamed);
        try {
            persistMetadataIfChanged();
        } catch (IOException failure) {
            orderedNotes.set(index, previous);
            if (pathChanged) {
                try {
                    moveAtomic(renamedPath, previousPath, false);
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }
        return renamed;
    }

    public synchronized NotebookNote updateBody(UUID id, String body) throws IOException {
        Objects.requireNonNull(body, "body");
        int index = requireNoteIndex(id);
        NotebookNote previous = orderedNotes.get(index);
        if (previous.body().equals(body)) {
            return previous;
        }

        rotateBackup(previous.id(), previous.body());
        writeAtomic(notesDirectory.resolve(previous.fileName()), body);
        NotebookNote updated = new NotebookNote(
                previous.id(), previous.title(), previous.fileName(), body
        );
        orderedNotes.set(index, updated);
        return updated;
    }

    public synchronized boolean delete(UUID id) throws IOException {
        int index = indexOf(id);
        if (index < 0) {
            return false;
        }

        NotebookNote removed = orderedNotes.get(index);
        rotateBackup(removed.id(), removed.body());
        Path noteFile = notesDirectory.resolve(removed.fileName());
        Files.deleteIfExists(noteFile);

        UUID previousSelection = selectedId;
        orderedNotes.remove(index);
        if (removed.id().equals(selectedId)) {
            selectedId = orderedNotes.isEmpty()
                    ? null
                    : orderedNotes.get(Math.min(index, orderedNotes.size() - 1)).id();
        }

        try {
            persistMetadataIfChanged();
        } catch (IOException failure) {
            orderedNotes.add(index, removed);
            selectedId = previousSelection;
            try {
                writeAtomic(noteFile, removed.body());
            } catch (IOException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
        return true;
    }

    /** Moves a note to a zero-based index in the explicit manual order. */
    public synchronized void move(UUID id, int targetIndex) throws IOException {
        int sourceIndex = requireNoteIndex(id);
        if (targetIndex < 0 || targetIndex >= orderedNotes.size()) {
            throw new IndexOutOfBoundsException("targetIndex: " + targetIndex);
        }
        if (sourceIndex == targetIndex) {
            return;
        }

        NotebookNote moved = orderedNotes.remove(sourceIndex);
        orderedNotes.add(targetIndex, moved);
        try {
            persistMetadataIfChanged();
        } catch (IOException failure) {
            orderedNotes.remove(targetIndex);
            orderedNotes.add(sourceIndex, moved);
            throw failure;
        }
    }

    private void addMetadataNote(
            MetadataNote metadataNote,
            Map<String, Path> filesByKey,
            List<NotebookNote> destination,
            Set<UUID> addedIds
    ) throws IOException {
        if (metadataNote == null || !addedIds.add(metadataNote.id())) {
            return;
        }

        Path path = filesByKey.remove(fileKey(metadataNote.fileName()));
        if (path == null) {
            return;
        }

        String body;
        try {
            body = Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException invalidUtf8) {
            return;
        }

        destination.add(new NotebookNote(
                metadataNote.id(),
                normalizeMetadataTitle(metadataNote.title(), path.getFileName().toString()),
                path.getFileName().toString(),
                body
        ));
    }

    private LinkedHashMap<String, Path> scanMarkdownFiles() throws IOException {
        List<Path> markdownFiles;
        try (Stream<Path> stream = Files.list(notesDirectory)) {
            markdownFiles = stream
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted(Comparator
                            .comparing((Path path) -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(path -> path.getFileName().toString()))
                    .toList();
        }

        LinkedHashMap<String, Path> files = new LinkedHashMap<>();
        for (Path path : markdownFiles) {
            files.putIfAbsent(fileKey(path.getFileName().toString()), path);
        }
        return files;
    }

    private Metadata readMetadata() throws IOException {
        if (!Files.isRegularFile(metadataFile, LinkOption.NOFOLLOW_LINKS)) {
            return Metadata.empty();
        }

        String json;
        try {
            json = Files.readString(metadataFile, StandardCharsets.UTF_8);
        } catch (CharacterCodingException invalidUtf8) {
            return Metadata.empty();
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Metadata.empty();
            }
            JsonObject rootObject = parsed.getAsJsonObject();
            LinkedHashMap<UUID, MetadataNote> notes = parseMetadataNotes(rootObject.get("notes"));
            List<UUID> order = parseOrder(rootObject.get("order"));
            UUID selected = parseUuid(stringValue(rootObject.get("selectedId")));
            return new Metadata(notes, order, selected);
        } catch (RuntimeException malformed) {
            return Metadata.empty();
        }
    }

    private LinkedHashMap<UUID, MetadataNote> parseMetadataNotes(JsonElement element) {
        LinkedHashMap<UUID, MetadataNote> notes = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) {
            return notes;
        }

        for (Map.Entry<String, JsonElement> jsonEntry : element.getAsJsonObject().entrySet()) {
            UUID id = parseUuid(jsonEntry.getKey());
            if (id == null || !jsonEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject noteObject = jsonEntry.getValue().getAsJsonObject();
            String fileName = stringValue(noteObject.get("file"));
            if (!isSafeMarkdownFileName(fileName)) {
                continue;
            }
            String title = stringValue(noteObject.get("title"));
            notes.putIfAbsent(id, new MetadataNote(id, title, fileName));
        }
        return notes;
    }

    private List<UUID> parseOrder(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }

        ArrayList<UUID> order = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (JsonElement value : element.getAsJsonArray()) {
            UUID id = parseUuid(stringValue(value));
            if (id != null && seen.add(id)) {
                order.add(id);
            }
        }
        return List.copyOf(order);
    }

    private void persistMetadataIfChanged() throws IOException {
        String json = metadataJson();
        if (Files.isRegularFile(metadataFile, LinkOption.NOFOLLOW_LINKS)) {
            try {
                if (Files.readString(metadataFile, StandardCharsets.UTF_8).equals(json)) {
                    return;
                }
            } catch (CharacterCodingException ignored) {
                // Replace invalid metadata with the repaired representation.
            }
        }
        writeAtomic(metadataFile, json);
    }

    private String metadataJson() {
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("format", METADATA_FORMAT);
        if (selectedId == null) {
            rootObject.add("selectedId", null);
        } else {
            rootObject.addProperty("selectedId", selectedId.toString());
        }

        JsonArray order = new JsonArray();
        JsonObject notes = new JsonObject();
        for (NotebookNote note : orderedNotes) {
            String id = note.id().toString();
            order.add(id);

            JsonObject noteObject = new JsonObject();
            noteObject.addProperty("title", note.title());
            noteObject.addProperty("file", note.fileName());
            notes.add(id, noteObject);
        }
        rootObject.add("order", order);
        rootObject.add("notes", notes);
        return GSON.toJson(rootObject) + System.lineSeparator();
    }

    private void rotateBackup(UUID id, String body) throws IOException {
        Path directory = backupsDirectory.resolve(id.toString());
        Files.createDirectories(directory);

        for (int version = BACKUP_COUNT; version >= 2; version--) {
            Path source = directory.resolve("previous-" + (version - 1) + ".md");
            Path target = directory.resolve("previous-" + version + ".md");
            if (Files.exists(source)) {
                moveAtomic(source, target, true);
            }
        }
        writeAtomic(directory.resolve("previous-1.md"), body);
    }

    private void ensureLayout() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(notesDirectory);
        Files.createDirectories(backupsDirectory);
    }

    private String availableFileName(String title, String currentFileName) {
        String stem = sanitizeFileStem(title);
        String candidate = stem + ".md";
        int suffix = 2;
        while (!fileNameAvailable(candidate, currentFileName)) {
            candidate = stem + " (" + suffix++ + ").md";
        }
        return candidate;
    }

    private boolean fileNameAvailable(String candidate, String currentFileName) {
        if (currentFileName != null && candidate.equalsIgnoreCase(currentFileName)) {
            return true;
        }
        boolean usedByLoadedNote = orderedNotes.stream()
                .anyMatch(note -> note.fileName().equalsIgnoreCase(candidate));
        return !usedByLoadedNote && !Files.exists(notesDirectory.resolve(candidate));
    }

    private static String sanitizeFileStem(String title) {
        String normalized = normalizeTitle(title);
        StringBuilder safe = new StringBuilder();
        normalized.codePoints().forEach(codePoint -> {
            if (Character.isISOControl(codePoint) || "<>:\"/\\|?*".indexOf(codePoint) >= 0) {
                safe.append('-');
            } else {
                safe.appendCodePoint(codePoint);
            }
        });

        String stem = stripTrailingDotsAndSpaces(safe.toString().strip());
        if (stem.isBlank()) {
            stem = "Untitled";
        }
        int codePointCount = stem.codePointCount(0, stem.length());
        if (codePointCount > MAX_FILE_STEM_CODE_POINTS) {
            stem = stem.substring(0, stem.offsetByCodePoints(0, MAX_FILE_STEM_CODE_POINTS));
            stem = stripTrailingDotsAndSpaces(stem);
        }
        if (isWindowsReservedName(stem)) {
            stem += "_";
        }
        return stem;
    }

    private static String stripTrailingDotsAndSpaces(String value) {
        int end = value.length();
        while (end > 0 && (value.charAt(end - 1) == '.' || value.charAt(end - 1) == ' ')) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isWindowsReservedName(String stem) {
        String base = stem.toUpperCase(Locale.ROOT);
        int dot = base.indexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        if (base.equals("CON") || base.equals("PRN") || base.equals("AUX") || base.equals("NUL")) {
            return true;
        }
        return base.matches("COM[1-9]") || base.matches("LPT[1-9]");
    }

    private static String normalizeTitle(String title) {
        Objects.requireNonNull(title, "title");
        String normalized = title.strip();
        return normalized.isEmpty() ? "Untitled" : normalized;
    }

    private static String normalizeMetadataTitle(String title, String fileName) {
        return title == null || title.isBlank() ? titleFromFileName(fileName) : title.strip();
    }

    private static String titleFromFileName(String fileName) {
        String title = fileName.substring(0, fileName.length() - 3).strip();
        return title.isEmpty() ? "Untitled" : title;
    }

    private static boolean isSafeMarkdownFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return false;
        }
        Path path = Path.of(fileName);
        return !path.isAbsolute()
                && path.getNameCount() == 1
                && !fileName.contains("/")
                && !fileName.contains("\\");
    }

    private static String fileKey(String fileName) {
        return fileName.toLowerCase(Locale.ROOT);
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static String stringValue(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString()
                : null;
    }

    private boolean containsId(UUID id) {
        return id != null && indexOf(id) >= 0;
    }

    private NotebookNote requireNote(UUID id) {
        return orderedNotes.get(requireNoteIndex(id));
    }

    private int requireNoteIndex(UUID id) {
        Objects.requireNonNull(id, "id");
        int index = indexOf(id);
        if (index < 0) {
            throw new NoSuchElementException("Unknown note: " + id);
        }
        return index;
    }

    private int indexOf(UUID id) {
        for (int index = 0; index < orderedNotes.size(); index++) {
            if (orderedNotes.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("Cannot safely write a path without a parent: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".notebook-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            moveAtomic(temporary, target, true);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomic(Path source, Path target, boolean replace) throws IOException {
        try {
            if (replace) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException unsupported) {
            if (replace) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }

    private record Metadata(
            LinkedHashMap<UUID, MetadataNote> notes,
            List<UUID> orderedIds,
            UUID selectedId
    ) {
        private static Metadata empty() {
            return new Metadata(new LinkedHashMap<>(), List.of(), null);
        }
    }

    private record MetadataNote(UUID id, String title, String fileName) {
    }
}
