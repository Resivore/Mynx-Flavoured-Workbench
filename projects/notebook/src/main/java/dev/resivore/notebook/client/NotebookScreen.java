package dev.resivore.notebook.client;

import dev.resivore.notebook.model.ChecklistParser;
import dev.resivore.notebook.model.NotebookNote;
import dev.resivore.notebook.storage.NotebookStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    private static final int TEXT_LINE_HEIGHT = 11;

    private static final int COLOR_DIM = 0xB0181410;
    private static final int COLOR_INK = 0xFF2A2119;
    private static final int COLOR_MUTED_INK = 0xFF746752;
    private static final int COLOR_RULE = 0x22766B59;
    private static final int COLOR_SELECTED = 0x446B8E5B;
    private static final int COLOR_SELECTED_EDGE = 0xFF58734C;
    private static final int COLOR_HOVER = 0x227B6A52;
    private static final int COLOR_CHECK = 0xFF43693F;
    private static final int COLOR_ERROR = 0xFFFF8A7A;
    private static final int COLOR_STATUS = 0xFFD9C98F;
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(
            "notebook", "textures/gui/notebook_book.png");

    private final Screen parent;
    private final NotebookStore store;
    private final List<CheckboxHit> checkboxHits = new ArrayList<>();

    private List<NotebookNote> notes = List.of();
    private UUID selectedId;
    private BookLayout layout;
    private EditBox titleEditor;
    private MultiLineEditBox bodyEditor;
    private Button editButton;
    private Button deleteButton;

    private boolean editing;
    private boolean deleteArmed;
    private int indexScroll;
    private int noteScroll;
    private int readingHeight;
    private UUID draggedId;
    private int dragTargetIndex = -1;
    private Component statusMessage;
    private boolean statusIsError;
    private int statusTicks;

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
        titleEditor.setHint(Component.translatable("screen.notebook.note_title"));
        addRenderableWidget(titleEditor);

        bodyEditor = MultiLineEditBox.builder()
                .setX(rightX)
                .setY(layout.bodyTop())
                .setPlaceholder(Component.translatable("screen.notebook.note_body"))
                .setTextColor(COLOR_INK)
                .setCursorColor(COLOR_INK)
                .setTextShadow(false)
                .setShowBackground(false)
                .setShowDecorations(true)
                .build(
                        font,
                        rightWidth,
                        layout.bodyHeight(),
                        Component.translatable("screen.notebook.note_body"));
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

        int rightFooterY = layout.pageBottom() - 21;
        editButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.notebook.edit"),
                        ignored -> toggleEditing())
                .bounds(layout.rightContentX(), rightFooterY, 52, 18)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.notebook.done"),
                        ignored -> onClose())
                .bounds(layout.rightContentRight() - 52, rightFooterY, 52, 18)
                .build());

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
        if (editing && !saveEdits()) {
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
        if (editing && !saveEdits()) {
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

    private void toggleEditing() {
        if (selectedId == null) {
            return;
        }
        if (editing) {
            saveEdits();
        } else {
            setEditing(true);
            setInitialFocus(bodyEditor);
        }
    }

    private void setEditing(boolean value) {
        editing = value && selectedId != null;
        resetDeleteConfirmation();
        refreshSelectionEditors();
        updateWidgetState();
    }

    private boolean saveEdits() {
        if (!editing || selectedId == null) {
            setEditing(false);
            return true;
        }

        String title = titleEditor.getValue().strip();
        if (title.isEmpty()) {
            title = Component.translatable("screen.notebook.new_note").getString();
        }
        try {
            NotebookNote current = store.find(selectedId).orElseThrow();
            if (!current.body().equals(bodyEditor.getValue())) {
                store.updateBody(selectedId, bodyEditor.getValue());
            }
            if (!current.title().equals(title)) {
                store.rename(selectedId, title);
            }
            store.select(selectedId);
            notes = store.notes();
            editing = false;
            noteScroll = 0;
            refreshSelectionEditors();
            updateWidgetState();
            return true;
        } catch (IOException | RuntimeException exception) {
            showError("screen.notebook.save_error", exception);
            return false;
        }
    }

    private void selectNote(UUID id) {
        if (id.equals(selectedId)) {
            resetDeleteConfirmation();
            return;
        }
        if (editing && !saveEdits()) {
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
        titleEditor.setValue(selected == null ? "" : selected.title());
        bodyEditor.setValue(selected == null ? "" : selected.body());
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
        editButton.active = hasNote;
        editButton.setMessage(Component.translatable(
                editing ? "screen.notebook.save" : "screen.notebook.edit"));
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
        graphics.fill(0, 0, width, height, COLOR_DIM);
        drawBook(graphics);
        drawIndex(graphics, mouseX, mouseY);
        if (editing) {
            drawEditPaper(graphics);
        } else {
            drawReadingPage(graphics, mouseX, mouseY);
        }
        drawStatus(graphics);
    }

    private void drawBook(GuiGraphicsExtractor graphics) {
        int x = layout.bookX();
        int y = layout.bookY();

        graphics.fill(x - 3, y + 3, x + layout.bookWidth() + 3, y + layout.bookHeight() + 4, 0x66000000);
        graphics.blit(BOOK_TEXTURE, x, y, layout.bookWidth(), layout.bookHeight(), 0.0F, 0.0F, 1.0F, 1.0F);

        for (int ruleY = layout.bodyTop() + TEXT_LINE_HEIGHT;
                ruleY < layout.pageBottom() - FOOTER_HEIGHT;
                ruleY += TEXT_LINE_HEIGHT) {
            graphics.horizontalLine(
                    layout.leftContentX(), layout.leftContentRight(), ruleY, COLOR_RULE);
            graphics.horizontalLine(
                    layout.rightContentX(), layout.rightContentRight(), ruleY, COLOR_RULE);
        }
    }

    private void drawIndex(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.centeredText(
                font,
                Component.translatable("screen.notebook.index").withStyle(ChatFormatting.BOLD),
                layout.leftPageCenterX(),
                layout.pageY() + 10,
                COLOR_INK);

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
            graphics.text(font, title, layout.leftContentX() + 13, rowY + 5, COLOR_INK);
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

    private void drawEditPaper(GuiGraphicsExtractor graphics) {
        if (selectedId == null) {
            return;
        }
        graphics.fill(
                layout.rightContentX() - 2,
                layout.pageY() + 8,
                layout.rightContentRight() + 2,
                layout.pageY() + 31,
                0x22FFFFFF);
        graphics.outline(
                layout.rightContentX() - 2,
                layout.bodyTop() - 2,
                layout.rightContentWidth() + 4,
                layout.bodyHeight() + 4,
                0x55746652);
    }

    private void drawReadingPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        checkboxHits.clear();
        Optional<NotebookNote> selected = selectedNote();
        if (selected.isEmpty()) {
            graphics.centeredText(
                    font,
                    Component.translatable("screen.notebook.empty"),
                    layout.rightPageCenterX(),
                    layout.pageY() + layout.pageHeight() / 2 - 8,
                    COLOR_MUTED_INK);
            graphics.centeredText(
                    font,
                    Component.translatable("screen.notebook.empty_hint"),
                    layout.rightPageCenterX(),
                    layout.pageY() + layout.pageHeight() / 2 + 5,
                    COLOR_MUTED_INK);
            readingHeight = 0;
            return;
        }

        NotebookNote note = selected.orElseThrow();
        graphics.centeredText(
                font,
                Component.literal(elide(note.title(), layout.rightContentWidth()))
                        .withStyle(ChatFormatting.BOLD),
                layout.rightPageCenterX(),
                layout.pageY() + 10,
                COLOR_INK);

        List<ChecklistParser.Entry> checklist = ChecklistParser.parse(note.body());
        Map<Integer, ChecklistParser.Entry> byLine = new HashMap<>();
        for (ChecklistParser.Entry entry : checklist) {
            byLine.put(entry.lineIndex(), entry);
        }

        String[] logicalLines = note.body().split("\\r\\n|\\r|\\n", -1);
        int contentTop = layout.bodyTop();
        int contentBottom = contentTop + layout.bodyHeight();
        int y = contentTop - noteScroll;
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
                int textX = layout.rightContentX() + (entry != null ? 14 : 0);
                int wrapWidth = layout.rightContentRight() - textX;
                Component component = Component.literal(visibleText);
                if (heading) {
                    component = component.copy().withStyle(ChatFormatting.BOLD);
                }
                List<FormattedCharSequence> wrapped = font.split(component, Math.max(12, wrapWidth));
                if (wrapped.isEmpty()) {
                    wrapped = List.of(FormattedCharSequence.EMPTY);
                }

                if (entry != null && y + 9 >= contentTop && y < contentBottom) {
                    int boxX = layout.rightContentX() + 1;
                    int boxY = y + 1;
                    drawCheckbox(graphics, boxX, boxY, entry.checked());
                    checkboxHits.add(new CheckboxHit(
                            boxX, boxY, 10, 10, lineIndex));
                    if (inside(mouseX, mouseY, boxX, boxY, 10, 10)) {
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
                                heading ? COLOR_CHECK : COLOR_INK);
                    }
                    y += TEXT_LINE_HEIGHT;
                    fullHeight += TEXT_LINE_HEIGHT;
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
        graphics.fill(x, y, x + 10, y + 10, 0x44FFFFFF);
        graphics.outline(x, y, 10, 10, checked ? COLOR_CHECK : COLOR_MUTED_INK);
        if (checked) {
            graphics.fill(x + 2, y + 2, x + 8, y + 8, COLOR_CHECK);
            graphics.fill(x + 4, y + 1, x + 6, y + 9, COLOR_CHECK);
        }
    }

    private void drawReadingScrollbar(GuiGraphicsExtractor graphics) {
        int viewport = layout.bodyHeight();
        if (readingHeight <= viewport || readingHeight <= 0) {
            return;
        }
        int trackX = layout.rightContentRight() - 2;
        int thumbHeight = Math.max(10, viewport * viewport / readingHeight);
        int thumbY = layout.bodyTop()
                + (viewport - thumbHeight) * noteScroll / Math.max(1, maxNoteScroll());
        graphics.fill(
                trackX, layout.bodyTop(), trackX + 2, layout.bodyTop() + viewport,
                0x33746652);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, COLOR_MUTED_INK);
    }

    private void drawStatus(GuiGraphicsExtractor graphics) {
        if (statusMessage == null || statusTicks <= 0) {
            return;
        }
        graphics.centeredText(
                font,
                statusMessage,
                width / 2,
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
        }
        return super.mouseClicked(event, doubleClick);
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
                layout.rightContentX(), layout.bodyTop(),
                layout.rightContentWidth(), layout.bodyHeight())) {
            noteScroll = Math.max(
                    0,
                    Math.min(maxNoteScroll(), noteScroll - scrollSteps(verticalAmount) * 18));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editing && event.isEscape()) {
            saveEdits();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    @Override
    public void onClose() {
        if (editing && !saveEdits()) {
            return;
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        if (editing) {
            saveEdits();
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
        return Math.max(0, readingHeight - layout.bodyHeight());
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
