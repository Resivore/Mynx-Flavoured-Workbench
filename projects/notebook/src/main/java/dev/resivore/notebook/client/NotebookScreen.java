package dev.resivore.notebook.client;

import dev.resivore.notebook.model.ChecklistParser;
import dev.resivore.notebook.model.NotebookNote;
import dev.resivore.notebook.storage.NotebookStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsive two-page notebook screen. The left page owns navigation and order;
 * the right page keeps reading and ordinary-text editing as separate states.
 */
public final class NotebookScreen extends Screen {
    private static final int MAX_BOOK_WIDTH = 520;
    private static final int MAX_BOOK_HEIGHT = 325;
    private static final int MIN_BOOK_WIDTH = 280;
    private static final int MIN_BOOK_HEIGHT = 175;
    private static final int OUTER_MARGIN = 8;
    private static final int PAGE_PADDING = 12;
    private static final int HEADER_HEIGHT = 25;
    private static final int FOOTER_HEIGHT = 26;
    private static final int INDEX_ROW_HEIGHT = 18;

    private static final int COLOR_INK = 0xFF2A2119;
    private static final int COLOR_MUTED_INK = 0xFF746752;
    private static final int COLOR_RULE = 0x22766B59;
    private static final int COLOR_SELECTED = 0x446B8E5B;
    private static final int COLOR_SELECTED_EDGE = 0xFF58734C;
    private static final int COLOR_HOVER = 0x227B6A52;
    private static final int COLOR_HEADING = 0xFF43693F;
    private static final int COLOR_ERROR = 0xFFFF8A7A;
    private static final int COLOR_STATUS = 0xFFD9C98F;
    /** A compact glyph that fits inside one live nine-pixel text row. */
    private static final int CHECKBOX_SIZE = 8;
    private static final int CHECKBOX_DRAW_OFFSET = 1;
    private static final int CHECKBOX_TEXT_GAP = 3;
    /** The visual box stays compact while this small halo keeps it easy to click. */
    private static final int CHECKBOX_HIT_PADDING = 1;
    private static final int BOOK_TEXTURE_WIDTH = 640;
    private static final int BOOK_TEXTURE_HEIGHT = 400;
    /** 750 ms at Minecraft's 20 logical client ticks per second. */
    private static final int AUTOSAVE_DELAY_TICKS = 15;
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(
            "notebook", "textures/gui/notebook_book.png");
    /** The supplied 8x8 green-on-transparent checked sprite; do not procedurally reinterpret it. */
    private static final Identifier CHECKED_CHECKBOX_TEXTURE = Identifier.fromNamespaceAndPath(
            "notebook", "textures/gui/notebook_checkbox_checked.png");

    private final Screen parent;
    private final NotebookStore store;
    private final List<CheckboxHit> checkboxHits = new ArrayList<>();

    private List<NotebookNote> notes = List.of();
    private UUID selectedId;
    private BookLayout layout;
    private PageTextLayout pageText;
    private EditBox titleEditor;
    private MultiLineEditBox bodyEditor;
    private Button deleteButton;

    private boolean editing;
    private boolean editorDirty;
    private boolean sessionBodyBackedUp;
    private boolean refreshingEditors;
    private boolean deleteArmed;
    private int indexScroll;
    private int noteScroll;
    private int readingHeight;
    private UUID draggedId;
    private int dragTargetIndex = -1;
    private Component statusMessage;
    private boolean statusIsError;
    private int statusTicks;
    private long clientTicks;
    private long lastEditorMutationTick;

    public NotebookScreen(Screen parent, NotebookStore store) {
        super(Component.translatable("screen.notebook.title"));
        this.parent = parent;
        this.store = store;
    }

    @Override
    public void added() {
        super.added();
        loadFromDisk();
    }

    @Override
    protected void init() {
        layout = BookLayout.fit(width, height);
        pageText = PageTextLayout.forCurrentEditor(layout, font.lineHeight);

        int rightX = layout.rightContentX();
        int rightWidth = layout.rightContentWidth();
        titleEditor = new EditBox(
                font,
                rightX,
                layout.pageY() + 11,
                rightWidth,
                18,
                Component.translatable("screen.notebook.note_title"));
        titleEditor.setMaxLength(180);
        titleEditor.setBordered(false);
        titleEditor.setTextColor(COLOR_INK);
        titleEditor.setTextShadow(false);
        titleEditor.setHint(Component.translatable("screen.notebook.note_title"));
        titleEditor.setResponder(ignored -> markEditorsDirty());
        addRenderableWidget(titleEditor);

        bodyEditor = MultiLineEditBox.builder()
                .setX(rightX)
                .setY(pageText.viewportTop())
                .setPlaceholder(Component.translatable("screen.notebook.note_body"))
                .setTextColor(COLOR_INK)
                .setCursorColor(COLOR_INK)
                .setTextShadow(false)
                .setShowBackground(false)
                .setShowDecorations(true)
                .build(
                        font,
                        rightWidth,
                        pageText.viewportHeight(),
                        Component.translatable("screen.notebook.note_body"));
        bodyEditor.setValueListener(ignored -> markEditorsDirty());
        addRenderableWidget(bodyEditor);

        int leftFooterY = layout.pageBottom() - 21;
        addRenderableWidget(compactButton(
                "+",
                "screen.notebook.add",
                layout.leftContentX(),
                leftFooterY,
                ignored -> createNote()));
        deleteButton = addRenderableWidget(compactButton(
                "−",
                "screen.notebook.delete",
                layout.leftContentX() + 22,
                leftFooterY,
                ignored -> deleteSelected()));
        addRenderableWidget(compactButton(
                "R",
                "screen.notebook.reload",
                layout.leftContentX() + 44,
                leftFooterY,
                ignored -> reloadFromDisk()));

        refreshSelectionEditors();
        updateWidgetState();
    }

    private Button compactButton(
            String label,
            String tooltipKey,
            int x,
            int y,
            Button.OnPress onPress
    ) {
        return Button.builder(Component.literal(label), onPress)
                .bounds(x, y, 18, 18)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                .build();
    }

    private void loadFromDisk() {
        try {
            store.rescan();
            notes = store.notes();
            selectedId = store.selectedId()
                    .filter(this::containsNote)
                    .orElseGet(() -> notes.isEmpty() ? null : notes.getFirst().id());
            if (selectedId != null && !selectedId.equals(store.selectedId().orElse(null))) {
                store.select(selectedId);
            }
            clampScrolls();
        } catch (IOException exception) {
            notes = store.notes();
            selectedId = notes.isEmpty() ? null : notes.getFirst().id();
            showError("screen.notebook.load_error", exception);
        }
    }

    private void reloadFromDisk() {
        if (editing && !flushEdits(true)) {
            return;
        }
        UUID prior = selectedId;
        try {
            store.rescan();
            notes = store.notes();
            selectedId = prior != null && containsNote(prior)
                    ? prior
                    : store.selectedId().filter(this::containsNote)
                            .orElseGet(() -> notes.isEmpty() ? null : notes.getFirst().id());
            if (selectedId != null) {
                store.select(selectedId);
            }
            noteScroll = 0;
            clampScrolls();
            refreshSelectionEditors();
            updateWidgetState();
            showStatus(Component.translatable("screen.notebook.external_reload"), false);
        } catch (IOException exception) {
            showError("screen.notebook.load_error", exception);
        }
    }

    private void createNote() {
        if (editing && !flushEdits(true)) {
            return;
        }
        try {
            NotebookNote note = store.create(
                    Component.translatable("screen.notebook.new_note").getString(), "");
            store.select(note.id());
            selectedId = note.id();
            notes = store.notes();
            noteScroll = 0;
            clampScrolls();
            refreshSelectionEditors();
            setEditing(true);
            setInitialFocus(titleEditor);
        } catch (IOException exception) {
            showError("screen.notebook.save_error", exception);
        }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            return;
        }
        if (editing && !flushEdits(true)) {
            return;
        }
        if (!deleteArmed) {
            deleteArmed = true;
            deleteButton.setMessage(Component.literal("!"));
            deleteButton.setTooltip(Tooltip.create(
                    Component.translatable("screen.notebook.delete_confirm")));
            return;
        }

        try {
            UUID deleting = selectedId;
            int oldIndex = indexOf(deleting);
            if (store.delete(deleting)) {
                notes = store.notes();
                selectedId = notes.isEmpty()
                        ? null
                        : notes.get(Math.min(Math.max(oldIndex, 0), notes.size() - 1)).id();
                if (selectedId != null) {
                    store.select(selectedId);
                }
            }
            setEditing(false);
            resetDeleteConfirmation();
            noteScroll = 0;
            clampScrolls();
            refreshSelectionEditors();
            updateWidgetState();
        } catch (IOException exception) {
            showError("screen.notebook.save_error", exception);
        }
    }

    private void setEditing(boolean value) {
        boolean beginningSession = value && !editing && selectedId != null;
        editing = value && selectedId != null;
        if (beginningSession) {
            editorDirty = false;
            sessionBodyBackedUp = false;
        }
        resetDeleteConfirmation();
        refreshSelectionEditors();
        updateWidgetState();
    }

    /** Flushes only changed editor values; autosave deliberately remains in edit mode. */
    private boolean flushEdits(boolean leaveEditing) {
        if (!editing || selectedId == null) {
            if (leaveEditing) {
                setEditing(false);
            }
            return true;
        }

        if (!editorDirty) {
            if (leaveEditing) {
                editing = false;
                refreshSelectionEditors();
                updateWidgetState();
            }
            return true;
        }
        String title = titleEditor.getValue().strip();
        if (title.isEmpty()) {
            title = Component.translatable("screen.notebook.new_note").getString();
        }
        try {
            NotebookNote current = store.find(selectedId).orElseThrow();
            if (!current.body().equals(bodyEditor.getValue())) {
                store.updateBody(selectedId, bodyEditor.getValue(), !sessionBodyBackedUp);
                sessionBodyBackedUp = true;
            }
            if (!current.title().equals(title)) {
                store.rename(selectedId, title);
            }
            store.select(selectedId);
            notes = store.notes();
            editorDirty = false;
            if (leaveEditing) {
                editing = false;
                noteScroll = 0;
                refreshSelectionEditors();
                updateWidgetState();
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            showError("screen.notebook.save_error", exception);
            return false;
        }
    }

    private void selectNote(UUID id) {
        if (id.equals(selectedId)) {
            // An index-row click is still a meaningful focus change: persist
            // pending text without making the user wait for the debounce.
            if (editing) {
                flushEdits(false);
            }
            resetDeleteConfirmation();
            return;
        }
        if (editing && !flushEdits(true)) {
            return;
        }
        try {
            store.select(id);
            selectedId = id;
            noteScroll = 0;
            resetDeleteConfirmation();
            refreshSelectionEditors();
            updateWidgetState();
        } catch (IOException exception) {
            showError("screen.notebook.save_error", exception);
        }
    }

    private void toggleChecklist(int logicalLine) {
        if (selectedId == null || editing) {
            return;
        }
        Optional<NotebookNote> selected = store.find(selectedId);
        if (selected.isEmpty()) {
            return;
        }
        String toggled = ChecklistParser.toggleAtLine(selected.orElseThrow().body(), logicalLine);
        if (toggled.equals(selected.orElseThrow().body())) {
            return;
        }
        try {
            store.updateBody(selectedId, toggled);
            notes = store.notes();
        } catch (IOException exception) {
            showError("screen.notebook.save_error", exception);
        }
    }

    private void refreshSelectionEditors() {
        if (titleEditor == null || bodyEditor == null) {
            return;
        }
        NotebookNote selected = selectedNote().orElse(null);
        refreshingEditors = true;
        try {
            titleEditor.setValue(selected == null ? "" : selected.title());
            bodyEditor.setValue(selected == null ? "" : selected.body());
        } finally {
            refreshingEditors = false;
        }
    }

    private void updateWidgetState() {
        if (titleEditor == null || bodyEditor == null) {
            return;
        }
        boolean hasNote = selectedId != null;
        titleEditor.visible = editing && hasNote;
        titleEditor.active = editing && hasNote;
        bodyEditor.visible = editing && hasNote;
        bodyEditor.active = editing && hasNote;
        deleteButton.active = hasNote;
    }

    private void resetDeleteConfirmation() {
        deleteArmed = false;
        if (deleteButton != null) {
            deleteButton.setMessage(Component.literal("−"));
            deleteButton.setTooltip(Tooltip.create(Component.translatable("screen.notebook.delete")));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawBook(graphics);
        drawIndex(graphics, mouseX, mouseY);
        if (!editing) {
            drawReadingPage(graphics, mouseX, mouseY);
        }
        drawStatus(graphics);
    }

    private void drawBook(GuiGraphicsExtractor graphics) {
        int x = layout.bookX();
        int y = layout.bookY();

        renderBookArtwork(graphics, x, y);

        int scrollOffset = editing ? editorScrollOffset() : noteScroll;
        for (int ruleY = pageText.firstRuleY(scrollOffset);
                ruleY < pageText.viewportBottom();
                ruleY += pageText.lineHeight()) {
            if (ruleY < pageText.viewportTop()) {
                continue;
            }
            graphics.horizontalLine(
                    layout.rightContentX(), layout.rightContentRight(), ruleY, COLOR_RULE);
        }
    }

    /** Renders the complete supplied 640x400 Bedrock journal, never a GUI sprite crop. */
    private void renderBookArtwork(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BOOK_TEXTURE,
                x,
                y,
                0.0F,
                0.0F,
                layout.bookWidth(),
                layout.bookHeight(),
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT,
                -1);
    }

    private void drawIndex(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCenteredNoShadow(
                graphics, Component.translatable("screen.notebook.index").withStyle(ChatFormatting.BOLD),
                layout.leftPageCenterX(), layout.pageY() + 10, COLOR_INK);

        int visibleRows = visibleIndexRows();
        int end = Math.min(notes.size(), indexScroll + visibleRows);
        for (int index = indexScroll; index < end; index++) {
            NotebookNote note = notes.get(index);
            int row = index - indexScroll;
            int rowY = layout.indexTop() + row * INDEX_ROW_HEIGHT;
            boolean hovered = inside(mouseX, mouseY,
                    layout.leftContentX(), rowY,
                    layout.leftContentWidth(), INDEX_ROW_HEIGHT - 1);
            boolean selected = note.id().equals(selectedId);
            if (selected) {
                graphics.fill(
                        layout.leftContentX(), rowY,
                        layout.leftContentRight(), rowY + INDEX_ROW_HEIGHT - 1,
                        COLOR_SELECTED);
                graphics.verticalLine(
                        layout.leftContentX(), rowY, rowY + INDEX_ROW_HEIGHT - 2,
                        COLOR_SELECTED_EDGE);
            } else if (hovered) {
                graphics.fill(
                        layout.leftContentX(), rowY,
                        layout.leftContentRight(), rowY + INDEX_ROW_HEIGHT - 1,
                        COLOR_HOVER);
            }

            int gripX = layout.leftContentX() + 4;
            int gripY = rowY + 6;
            graphics.horizontalLine(gripX, gripX + 5, gripY, COLOR_MUTED_INK);
            graphics.horizontalLine(gripX, gripX + 5, gripY + 3, COLOR_MUTED_INK);
            String title = elide(note.title(), layout.leftContentWidth() - 17);
            graphics.text(font, title, layout.leftContentX() + 13, rowY + 5, COLOR_INK, false);
        }

        if (draggedId != null && dragTargetIndex >= 0) {
            int markerRow = dragTargetIndex - indexScroll;
            if (markerRow >= 0 && markerRow <= visibleRows) {
                int markerY = layout.indexTop() + markerRow * INDEX_ROW_HEIGHT;
                graphics.horizontalLine(
                        layout.leftContentX(), layout.leftContentRight(), markerY,
                        COLOR_SELECTED_EDGE);
            }
        }
        drawIndexScrollbar(graphics, visibleRows);
    }

    private void drawIndexScrollbar(GuiGraphicsExtractor graphics, int visibleRows) {
        if (notes.size() <= visibleRows || visibleRows <= 0) {
            return;
        }
        int trackX = layout.leftContentRight() - 2;
        int trackY = layout.indexTop();
        int trackHeight = visibleRows * INDEX_ROW_HEIGHT - 2;
        int thumbHeight = Math.max(10, trackHeight * visibleRows / notes.size());
        int maxScroll = notes.size() - visibleRows;
        int thumbY = trackY + (trackHeight - thumbHeight) * indexScroll / Math.max(1, maxScroll);
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0x33746652);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, COLOR_MUTED_INK);
    }

    private void drawReadingPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        checkboxHits.clear();
        Optional<NotebookNote> selected = selectedNote();
        if (selected.isEmpty()) {
            drawCenteredNoShadow(graphics, Component.translatable("screen.notebook.empty"),
                    layout.rightPageCenterX(), layout.pageY() + layout.pageHeight() / 2 - 8, COLOR_MUTED_INK);
            drawCenteredNoShadow(graphics, Component.translatable("screen.notebook.empty_hint"),
                    layout.rightPageCenterX(), layout.pageY() + layout.pageHeight() / 2 + 5, COLOR_MUTED_INK);
            readingHeight = 0;
            return;
        }

        NotebookNote note = selected.orElseThrow();
        drawCenteredNoShadow(graphics,
                Component.literal(elide(note.title(), layout.rightContentWidth())).withStyle(ChatFormatting.BOLD),
                layout.rightPageCenterX(), layout.pageY() + 10, COLOR_INK);

        List<ChecklistParser.Entry> checklist = ChecklistParser.parse(note.body());
        Map<Integer, ChecklistParser.Entry> byLine = new HashMap<>();
        for (ChecklistParser.Entry entry : checklist) {
            byLine.put(entry.lineIndex(), entry);
        }

        String[] logicalLines = note.body().split("\\r\\n|\\r|\\n", -1);
        int contentTop = pageText.viewportTop();
        int contentBottom = pageText.viewportBottom();
        int y = pageText.textY(noteScroll, 0);
        int fullHeight = 0;
        graphics.enableScissor(
                layout.rightContentX(), contentTop,
                layout.rightContentRight(), contentBottom);
        try {
            for (int lineIndex = 0; lineIndex < logicalLines.length; lineIndex++) {
                ChecklistParser.Entry entry = byLine.get(lineIndex);
                String raw = logicalLines[lineIndex];
                boolean heading = entry == null && raw.startsWith("# ");
                String visibleText = entry != null
                        ? entry.text()
                        : heading ? raw.substring(2) : raw;
                int textX = layout.rightContentX() + (entry != null
                        ? CHECKBOX_DRAW_OFFSET + CHECKBOX_SIZE + CHECKBOX_TEXT_GAP
                        : 0);
                int wrapWidth = layout.rightContentRight() - textX;
                Component component = Component.literal(visibleText);
                if (heading) {
                    component = component.copy().withStyle(ChatFormatting.BOLD);
                }
                List<FormattedCharSequence> wrapped = font.split(component, Math.max(12, wrapWidth));
                if (wrapped.isEmpty()) {
                    wrapped = List.of(FormattedCharSequence.EMPTY);
                }

                if (entry != null && y + pageText.lineHeight() >= contentTop && y < contentBottom) {
                    int boxX = layout.rightContentX() + CHECKBOX_DRAW_OFFSET;
                    int boxY = pageText.checkboxY(y, CHECKBOX_SIZE);
                    drawCheckbox(graphics, boxX, boxY, entry.checked());
                    checkboxHits.add(new CheckboxHit(
                            boxX - CHECKBOX_HIT_PADDING,
                            boxY - CHECKBOX_HIT_PADDING,
                            CHECKBOX_SIZE + CHECKBOX_HIT_PADDING * 2,
                            CHECKBOX_SIZE + CHECKBOX_HIT_PADDING * 2,
                            lineIndex));
                    if (inside(mouseX, mouseY,
                            boxX - CHECKBOX_HIT_PADDING,
                            boxY - CHECKBOX_HIT_PADDING,
                            CHECKBOX_SIZE + CHECKBOX_HIT_PADDING * 2,
                            CHECKBOX_SIZE + CHECKBOX_HIT_PADDING * 2)) {
                        graphics.setTooltipForNextFrame(
                                Component.literal(entry.checked()
                                        ? "Mark unfinished"
                                        : "Mark completed"),
                                mouseX,
                                mouseY);
                    }
                }

                for (FormattedCharSequence visualLine : wrapped) {
                    if (y + font.lineHeight >= contentTop && y < contentBottom) {
                        graphics.text(
                                font,
                                visualLine,
                                textX,
                                y,
                                heading ? COLOR_HEADING : COLOR_INK,
                                false);
                    }
                    y += pageText.lineHeight();
                    fullHeight += pageText.lineHeight();
                }
            }
        } finally {
            graphics.disableScissor();
        }
        readingHeight = fullHeight;
        noteScroll = Math.min(noteScroll, maxNoteScroll());
        drawReadingScrollbar(graphics);
    }

    private void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        if (checked) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    CHECKED_CHECKBOX_TEXTURE,
                    x,
                    y,
                    0.0F,
                    0.0F,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    CHECKBOX_SIZE,
                    -1);
            return;
        }
        graphics.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_INK);
    }

    private void drawReadingScrollbar(GuiGraphicsExtractor graphics) {
        int viewport = pageText.viewportHeight();
        if (readingHeight <= viewport || readingHeight <= 0) {
            return;
        }
        int trackX = layout.rightContentRight() - 2;
        int thumbHeight = Math.max(10, viewport * viewport / readingHeight);
        int thumbY = pageText.viewportTop()
                + (viewport - thumbHeight) * noteScroll / Math.max(1, maxNoteScroll());
        graphics.fill(
                trackX, pageText.viewportTop(), trackX + 2, pageText.viewportBottom(),
                0x33746652);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, COLOR_MUTED_INK);
    }

    private void drawStatus(GuiGraphicsExtractor graphics) {
        if (statusMessage == null || statusTicks <= 0) {
            return;
        }
        drawCenteredNoShadow(graphics, statusMessage, width / 2,
                Math.min(height - 10, layout.bookY() + layout.bookHeight() + 3),
                statusIsError ? COLOR_ERROR : COLOR_STATUS);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            if (!editing) {
                for (CheckboxHit hit : checkboxHits) {
                    if (hit.contains(mouseX, mouseY)) {
                        toggleChecklist(hit.logicalLine());
                        return true;
                    }
                }
            }

            int row = indexRowAt(mouseX, mouseY);
            if (row >= 0) {
                int index = indexScroll + row;
                if (index < notes.size()) {
                    NotebookNote note = notes.get(index);
                    selectNote(note.id());
                    if (note.id().equals(selectedId)) {
                        draggedId = note.id();
                        dragTargetIndex = index;
                    }
                    return true;
                }
            }

            if (selectedId != null && inside(mouseX, mouseY,
                    layout.rightContentX(), layout.pageY() + 11,
                    layout.rightContentWidth(), 18)) {
                beginEditingAt(titleEditor, event, doubleClick);
                return true;
            }
            if (selectedId != null && inside(mouseX, mouseY,
                    layout.rightContentX(), pageText.viewportTop(),
                    layout.rightContentWidth(), pageText.viewportHeight())) {
                beginEditingAt(bodyEditor, event, doubleClick);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Delegates the opening click to Minecraft's native widgets so their own
     * hit testing places a title/body caret at the clicked character or row.
     */
    private void beginEditingAt(EditBox editor, MouseButtonEvent event, boolean doubleClick) {
        if (!editing) {
            setEditing(true);
        }
        setInitialFocus(editor);
        editor.onClick(event, doubleClick);
    }

    private void beginEditingAt(MultiLineEditBox editor, MouseButtonEvent event, boolean doubleClick) {
        if (!editing) {
            setEditing(true);
        }
        setInitialFocus(editor);
        editor.onClick(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggedId == null || event.button() != 0) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        int mouseY = (int) event.y();
        int visibleRows = visibleIndexRows();
        if (mouseY < layout.indexTop() + 5 && indexScroll > 0) {
            indexScroll--;
        } else if (mouseY > layout.indexBottom() - 5
                && indexScroll + visibleRows < notes.size()) {
            indexScroll++;
        }
        int relativeRow = Math.max(
                0,
                Math.min(visibleRows - 1, (mouseY - layout.indexTop()) / INDEX_ROW_HEIGHT));
        dragTargetIndex = Math.max(
                0,
                Math.min(notes.size() - 1, indexScroll + relativeRow));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggedId != null && event.button() == 0) {
            try {
                int from = indexOf(draggedId);
                if (dragTargetIndex >= 0 && from != dragTargetIndex) {
                    store.move(draggedId, dragTargetIndex);
                    notes = store.notes();
                    showStatus(Component.translatable("screen.notebook.reordered"), false);
                }
            } catch (IOException exception) {
                showError("screen.notebook.save_error", exception);
            } finally {
                draggedId = null;
                dragTargetIndex = -1;
                clampScrolls();
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (inside(mouseX, mouseY,
                layout.leftContentX(), layout.indexTop(),
                layout.leftContentWidth(), layout.indexHeight())) {
            int max = Math.max(0, notes.size() - visibleIndexRows());
            indexScroll = Math.max(0, Math.min(max, indexScroll - scrollSteps(verticalAmount)));
            return true;
        }
        if (!editing && inside(mouseX, mouseY,
                layout.rightContentX(), pageText.viewportTop(),
                layout.rightContentWidth(), pageText.viewportHeight())) {
            noteScroll = Math.max(
                    0,
                    Math.min(maxNoteScroll(), noteScroll
                            - scrollSteps(verticalAmount) * pageText.lineHeight() * 2));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (editing) {
                // This is deliberately handled before Screen's generic Escape
                // path. A failed flush leaves the editor and its text intact,
                // but either outcome consumes this first Escape press.
                flushEdits(true);
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        super.tick();
        clientTicks++;
        if (statusTicks > 0) {
            statusTicks--;
        }
        if (editing && editorDirty && clientTicks - lastEditorMutationTick >= AUTOSAVE_DELAY_TICKS) {
            flushEdits(false);
        }
    }

    @Override
    public void onClose() {
        if (editing && !flushEdits(true)) {
            return;
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        if (editing) {
            flushEdits(false);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int indexRowAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY,
                layout.leftContentX(), layout.indexTop(),
                layout.leftContentWidth(), layout.indexHeight())) {
            return -1;
        }
        int row = (mouseY - layout.indexTop()) / INDEX_ROW_HEIGHT;
        return row < visibleIndexRows() ? row : -1;
    }

    private int visibleIndexRows() {
        return Math.max(1, layout.indexHeight() / INDEX_ROW_HEIGHT);
    }

    private int maxNoteScroll() {
        return Math.max(0, readingHeight - pageText.viewportHeight());
    }

    /**
     * MultiLineEditBox scrolls its contents independently. Keep the rendered
     * rules in its exact row coordinate system instead of leaving them fixed
     * behind the scrolling editor.
     */
    private int editorScrollOffset() {
        return bodyEditor == null ? 0 : (int) bodyEditor.scrollAmount();
    }

    private void clampScrolls() {
        if (layout == null) {
            return;
        }
        indexScroll = Math.max(
                0,
                Math.min(indexScroll, Math.max(0, notes.size() - visibleIndexRows())));
        noteScroll = Math.max(0, Math.min(noteScroll, maxNoteScroll()));
    }

    private Optional<NotebookNote> selectedNote() {
        return selectedId == null ? Optional.empty() : store.find(selectedId);
    }

    private boolean containsNote(UUID id) {
        return notes.stream().anyMatch(note -> note.id().equals(id));
    }

    private int indexOf(UUID id) {
        for (int index = 0; index < notes.size(); index++) {
            if (notes.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private String elide(String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        String suffix = "…";
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width(suffix))) + suffix;
    }

    private void markEditorsDirty() {
        if (!refreshingEditors && editing) {
            editorDirty = true;
            lastEditorMutationTick = clientTicks;
        }
    }

    private void drawCenteredNoShadow(
            GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color
    ) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void showError(String translationKey, Exception exception) {
        String detail = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        showStatus(Component.translatable(translationKey, detail), true);
    }

    private void showStatus(Component message, boolean error) {
        statusMessage = message;
        statusIsError = error;
        statusTicks = error ? 240 : 100;
    }

    private static boolean inside(
            double x,
            double y,
            int left,
            int top,
            int width,
            int height
    ) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static int scrollSteps(double amount) {
        if (amount == 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.round(Math.abs(amount))) * (amount > 0 ? 1 : -1);
    }

    private record CheckboxHit(int x, int y, int width, int height, int logicalLine) {
        private boolean contains(int mouseX, int mouseY) {
            return inside(mouseX, mouseY, x, y, width, height);
        }
    }

    /**
     * Shared text geometry for custom reading and Minecraft's multiline editor.
     * The editor's public default total padding supplies its actual inner inset;
     * its row height is the live font line height used by the current client.
     */
    static record PageTextLayout(
            int viewportTop,
            int viewportHeight,
            int textTop,
            int lineHeight
    ) {
        static PageTextLayout forCurrentEditor(BookLayout book, int fontLineHeight) {
            int editorInnerInset = AbstractTextAreaWidget.DEFAULT_TOTAL_PADDING / 2;
            return new PageTextLayout(
                    book.bodyTop(),
                    book.bodyHeight(),
                    book.bodyTop() + editorInnerInset,
                    fontLineHeight);
        }

        int viewportBottom() {
            return viewportTop + viewportHeight;
        }

        int firstRuleY(int scrollOffset) {
            return textTop - scrollOffset + lineHeight - 1;
        }

        int textY(int scrollOffset, int visualRow) {
            return textTop - scrollOffset + visualRow * lineHeight;
        }

        /** Centers a compact ballot box in the same line box that owns the rule. */
        int checkboxY(int rowTextY, int checkboxSize) {
            return rowTextY + Math.max(0, (lineHeight - checkboxSize) / 2);
        }
    }

    static record BookLayout(
            int bookX,
            int bookY,
            int bookWidth,
            int bookHeight,
            int pageX,
            int pageY,
            int pageRight,
            int pageBottom,
            int spineLeft,
            int leftPageWidth,
            int rightPageWidth
    ) {
        static BookLayout fit(int screenWidth, int screenHeight) {
            int availableWidth = Math.max(1, screenWidth - OUTER_MARGIN * 2);
            int availableHeight = Math.max(1, screenHeight - OUTER_MARGIN * 2);
            int maximumWidth = Math.min(MAX_BOOK_WIDTH, availableWidth);
            int maximumHeight = Math.min(MAX_BOOK_HEIGHT, availableHeight);
            int bookWidth = maximumWidth;
            int bookHeight = Math.round(bookWidth * 400.0F / 640.0F);
            if (bookHeight > maximumHeight) {
                bookHeight = maximumHeight;
                bookWidth = Math.round(bookHeight * 640.0F / 400.0F);
            }
            if (screenWidth >= MIN_BOOK_WIDTH + OUTER_MARGIN * 2
                    && screenHeight >= MIN_BOOK_HEIGHT + OUTER_MARGIN * 2) {
                bookWidth = Math.max(MIN_BOOK_WIDTH, bookWidth);
                bookHeight = Math.round(bookWidth * 400.0F / 640.0F);
            }
            int bookX = (screenWidth - bookWidth) / 2;
            int bookY = (screenHeight - bookHeight) / 2;
            int pageX = bookX + Math.max(4, Math.round(bookWidth * 18.0F / 640.0F));
            int pageY = bookY + Math.max(3, Math.round(bookHeight * 9.0F / 400.0F));
            int pageRight = bookX + bookWidth - Math.max(4, Math.round(bookWidth * 18.0F / 640.0F));
            int pageBottom = bookY + bookHeight - Math.max(3, Math.round(bookHeight * 18.0F / 400.0F));
            int spineLeft = bookX + Math.round(bookWidth * 317.0F / 640.0F);
            int spineRight = bookX + Math.round(bookWidth * 324.0F / 640.0F);
            int leftWidth = spineLeft - pageX;
            int rightWidth = pageRight - spineRight;
            return new BookLayout(
                    bookX, bookY, bookWidth, bookHeight,
                    pageX, pageY, pageRight, pageBottom,
                    spineLeft, leftWidth, rightWidth);
        }

        int pageHeight() {
            return pageBottom - pageY;
        }

        int leftContentX() {
            return pageX + PAGE_PADDING;
        }

        int leftContentRight() {
            return spineLeft - PAGE_PADDING;
        }

        int leftContentWidth() {
            return leftContentRight() - leftContentX();
        }

        int rightContentX() {
            return pageRight - rightPageWidth + PAGE_PADDING;
        }

        int rightContentRight() {
            return pageRight - PAGE_PADDING;
        }

        int rightContentWidth() {
            return rightContentRight() - rightContentX();
        }

        int leftPageCenterX() {
            return pageX + leftPageWidth / 2;
        }

        int rightPageCenterX() {
            return pageRight - rightPageWidth / 2;
        }

        int indexTop() {
            return pageY + HEADER_HEIGHT;
        }

        int indexBottom() {
            return pageBottom - FOOTER_HEIGHT;
        }

        int indexHeight() {
            return Math.max(INDEX_ROW_HEIGHT, indexBottom() - indexTop());
        }

        int bodyTop() {
            return pageY + HEADER_HEIGHT + 2;
        }

        int bodyHeight() {
            return Math.max(24, pageBottom - FOOTER_HEIGHT - bodyTop());
        }
    }
}
