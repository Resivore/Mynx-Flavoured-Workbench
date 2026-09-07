package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** C10's Inventory Search-derived visual vocabulary backed solely by a live QSN snapshot. */
public final class NearbySearchScreen extends QuickStackRulesScreenBase {
    private static final int PAD = 12, ROW_H = 20, DETAIL_H = 18;
    private final Screen parent;
    private EditBox query;
    private final List<Row> rows = new ArrayList<>();
    private final List<QuickStackHitboxButton> hitboxes = new ArrayList<>();
    private String lastQuery = "";
    private String expanded = "";
    private int x, y, w, h, listTop, listBottom, scrollRow;
    private boolean waiting;

    public NearbySearchScreen(Screen parent) { super(Component.literal("Nearby Search")); this.parent = parent; }

    @Override protected void init() {
        w = Math.min(480, width - 24); h = Math.min(290, height - 24); x = (width - w) / 2; y = (height - h) / 2;
        listTop = y + 62; listBottom = y + h - PAD;
        clearWidgets();
        query = new EditBox(font, x + PAD + 6, y + 31, w - PAD * 2 - 12, 14, Component.literal("Search nearby items"));
        query.setBordered(false); query.setMaxLength(64); addRenderableWidget(query); setInitialFocus(query);
        addRenderableWidget(new QuickStackModalIconButton(x + w - PAD - 16, y + 7, 16, QuickStackModalIconButton.CLOSE,
                Component.literal("Close"), b -> closeToParent()));
        NearbySearchClientState.setActive(this);
        refreshFromServer();
        if (!QuickStackClientNetworking.requestNearbySearch()) waiting = false; else waiting = true;
    }

    @Override public void tick() { super.tick(); if (query != null && !query.getValue().equals(lastQuery)) { lastQuery = query.getValue(); rebuildRows(); } }
    @Override public void onClose() { NearbySearchClientState.clearActive(this); closeToParent(); }

    void refreshFromServer() { waiting = false; rebuildRows(); }
    void targetRejected() { waiting = false; }
    void targetAccepted() { closeToParent(); }

    private void rebuildRows() {
        Map<QuickStackMoveEngine.StackKey, Row> grouped = new LinkedHashMap<>();
        String needle = lastQuery == null ? "" : lastQuery.trim().toLowerCase(Locale.ROOT);
        for (NearbySearchPayload.Entry entry : NearbySearchClientState.snapshot()) {
            if (!matches(entry.stack(), needle)) continue;
            QuickStackMoveEngine.StackKey key = QuickStackMoveEngine.StackKey.of(entry.stack());
            Row row = grouped.computeIfAbsent(key, ignored -> new Row(entry.stack().copyWithCount(1)));
            row.locations.add(entry); row.total += entry.count();
        }
        rows.clear(); rows.addAll(grouped.values()); scrollRow = Math.min(scrollRow, Math.max(0, rows.size() - 1));
        rows.forEach(row -> row.locations.sort(Comparator.comparingDouble(NearbySearchPayload.Entry::distance)));
        rows.sort(Comparator.comparingInt((Row row) -> row.total).reversed().thenComparing(row -> row.stack.getHoverName().getString()));
        rebuildHitboxes();
    }

    private static boolean matches(ItemStack stack, String query) {
        if (query.isEmpty()) return true;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())).toLowerCase(Locale.ROOT);
        String components = String.valueOf(stack.getComponentsPatch()).toLowerCase(Locale.ROOT);
        if (query.startsWith("<")) return components.contains(query.substring(1));
        if (query.startsWith(":")) return name.contains(query.substring(1)) || id.contains(query.substring(1));
        return name.contains(query) || id.contains(query) || components.contains(query);
    }

    private void rebuildHitboxes() {
        hitboxes.clear(); clearWidgets();
        if (query != null) { addRenderableWidget(query); }
        addRenderableWidget(new QuickStackModalIconButton(x + w - PAD - 16, y + 7, 16, QuickStackModalIconButton.CLOSE, Component.literal("Close"), b -> closeToParent()));
        int cursor = listTop;
        if (scrollRow > 0) addRenderableWidget(new QuickStackTextButton(x + w - PAD - 28, listTop, 28, 14, Component.literal("Up"), b -> { scrollRow--; rebuildHitboxes(); }));
        for (int i = scrollRow; i < rows.size() && cursor + ROW_H <= listBottom; i++) {
            Row row = rows.get(i); int rowIndex = i;
            QuickStackHitboxButton button = new QuickStackHitboxButton(x + PAD, cursor, w - PAD * 2, ROW_H, Component.literal("Nearby result"), b -> activate(rowIndex, -1));
            addRenderableWidget(button); hitboxes.add(button); cursor += ROW_H + 3;
            if (row.key().equals(expanded)) for (int location = 0; location < row.locations.size() && cursor + DETAIL_H <= listBottom; location++) {
                int locationIndex = location;
                QuickStackHitboxButton detail = new QuickStackHitboxButton(x + PAD + 20, cursor, w - PAD * 2 - 20, DETAIL_H,
                        Component.literal("Target container"), b -> activate(rowIndex, locationIndex));
                addRenderableWidget(detail); hitboxes.add(detail); cursor += DETAIL_H;
            }
        }
        if (scrollRow + 1 < rows.size()) addRenderableWidget(new QuickStackTextButton(x + w - PAD - 38, listBottom - 14, 38, 14, Component.literal("Down"), b -> { scrollRow++; rebuildHitboxes(); }));
    }

    private void activate(int rowIndex, int locationIndex) {
        if (waiting || rowIndex < 0 || rowIndex >= rows.size()) return;
        Row row = rows.get(rowIndex);
        if (locationIndex < 0 && row.locations.size() > 1) { expanded = row.key().equals(expanded) ? "" : row.key(); rebuildHitboxes(); return; }
        NearbySearchPayload.Entry target = row.locations.get(Math.max(0, locationIndex));
        waiting = true; QuickStackClientNetworking.targetNearbyContainer(target.position(), target.stack(), target.nestedName());
    }

    @Override protected void paintScreen(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        QuickStackUi.scrim(g, width, height); QuickStackUi.window(g, x, y, w, h);
        g.drawString(font, "Nearby Search", x + PAD, y + 9, QuickStackUi.TEXT, false);
        QuickStackUi.inset(g, x + PAD, y + 26, w - PAD * 2, 24);
        if (query != null && query.getValue().isEmpty()) g.drawString(font, "Search items, ids, :category, or <components", x + PAD + 6, y + 34, QuickStackUi.TEXT_DIM, false);
        if (waiting) { g.drawString(font, "Refreshing nearby storage...", x + PAD, listTop + 4, QuickStackUi.TEXT_MUTED, false); return; }
        if (rows.isEmpty()) { g.drawString(font, "No nearby items match that search.", x + PAD, listTop + 4, QuickStackUi.TEXT_MUTED, false); return; }
        int cursor = listTop;
        for (int i = scrollRow; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (cursor + ROW_H > listBottom) break;
            boolean hover = mouseX >= x + PAD && mouseX <= x + w - PAD && mouseY >= cursor && mouseY < cursor + ROW_H;
            g.fill(x + PAD, cursor, x + w - PAD, cursor + ROW_H, hover ? QuickStackUi.CARD_HOVER : QuickStackUi.CARD);
            g.renderItem(row.stack, x + PAD + 3, cursor + 2);
            String name = font.plainSubstrByWidth(row.stack.getHoverName().getString(), w - 112);
            g.drawString(font, name, x + PAD + 23, cursor + 6, QuickStackUi.TEXT, false);
            String count = "x" + row.total; g.drawString(font, count, x + w - PAD - font.width(count) - 22, cursor + 6, QuickStackUi.TEXT_MUTED, false);
            g.drawString(font, row.key().equals(expanded) ? "-" : row.locations.size() > 1 ? "+" : "•", x + w - PAD - 11, cursor + 6, QuickStackUi.TEXT_MUTED, false);
            cursor += ROW_H + 3;
            if (row.key().equals(expanded)) for (NearbySearchPayload.Entry entry : row.locations) {
                if (cursor + DETAIL_H > listBottom) break;
                String path = entry.containerName() + (entry.nestedName().isEmpty() ? "" : " -> " + entry.nestedName());
                String detail = font.plainSubstrByWidth(path, w - 150);
                g.drawString(font, detail, x + PAD + 24, cursor + 5, QuickStackUi.TEXT_MUTED, false);
                String suffix = "x" + entry.count() + "  " + String.format(Locale.ROOT, "%.1f", entry.distance()) + " blocks";
                g.drawString(font, suffix, x + w - PAD - font.width(suffix) - 4, cursor + 5, QuickStackUi.TEXT_DIM, false);
                cursor += DETAIL_H;
            }
        }
        if (scrollRow > 0) g.drawString(font, "^", x + w - PAD - 17, listTop + 3, QuickStackUi.TEXT_MUTED, false);
        if (scrollRow + 1 < rows.size()) g.drawString(font, "v", x + w - PAD - 17, listBottom - 12, QuickStackUi.TEXT_MUTED, false);
    }

    private void closeToParent() { NearbySearchClientState.clearActive(this); ClientScreenCompat.setScreen(Minecraft.getInstance(), parent); }
    private static final class Row { final ItemStack stack; final List<NearbySearchPayload.Entry> locations = new ArrayList<>(); int total; Row(ItemStack stack) { this.stack = stack; } String key() { return String.valueOf(QuickStackMoveEngine.StackKey.of(stack).identity()); } }
}
