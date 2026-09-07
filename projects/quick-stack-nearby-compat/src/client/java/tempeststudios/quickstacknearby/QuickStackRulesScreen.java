package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuickStackRulesScreen extends QuickStackRulesScreenBase {
    private static final int PAD = 12;
    private static final int MARK_LOCKED = 0xFFD15B4A;
    private static final int MARK_KEEP = 0xFFFFC857;
    private static final int MAX_KEEP_COUNT = 64;

    private final AbstractContainerScreen<?> parent;
    private final Player player;
    private final QuickStackRuleStore store = QuickStackRuleStore.getInstance();
    private final List<QuickStackHitboxButton> slotButtons = new ArrayList<>();
    private final Set<Integer> selectedSlots = new LinkedHashSet<>();

    private int panelW;
    private int panelH;
    private int panelX;
    private int panelY;
    private int gridX;
    private int gridY;
    private int gridW;
    private int gridH;
    private int slotSize;
    private int actionsY;
    private int infoX;
    private int infoY;
    private int infoW;
    private int infoH;
    private int keepControlsY;
    private int anchorSlot = -1;

    public QuickStackRulesScreen(AbstractContainerScreen<?> parent, Player player) {
        super(Component.literal("Quick Stack Rules"));
        this.parent = parent;
        this.player = player;
    }

    @Override
    protected void init() {
        computeLayout();
        sanitizeSelection();
        this.clearWidgets();

        this.addRenderableWidget(new QuickStackModalIconButton(
                panelX + panelW - PAD - 16,
                panelY + 7,
                16,
                QuickStackModalIconButton.CLOSE,
                Component.literal("Close"),
                button -> closeToParent()
        ));

        addActionButtons();
        buildHitboxes();
    }

    private void addActionButtons() {
        int bw = (gridW - 8) / 3;
        QuickStackTextButton lock = new QuickStackTextButton(gridX, actionsY, bw, 18,
                Component.literal("Lock"), button -> lockSelectedSlots(), true);
        lock.setTooltip(Tooltip.create(Component.literal("Do not unload the selected slots.")));
        this.addRenderableWidget(lock);

        QuickStackTextButton unlock = new QuickStackTextButton(gridX + bw + 4, actionsY, bw, 18,
                Component.literal("Allow"), button -> unlockSelectedSlots());
        unlock.setTooltip(Tooltip.create(Component.literal("Allow the selected slots to unload.")));
        this.addRenderableWidget(unlock);

        QuickStackTextButton clear = new QuickStackTextButton(gridX + (bw + 4) * 2, actionsY,
                gridW - (bw + 4) * 2, 18, Component.literal("Clear"), button -> clearSelectedSlots());
        clear.setTooltip(Tooltip.create(Component.literal("Remove rules from the selected slots.")));
        this.addRenderableWidget(clear);

        int controlW = Math.max(1, infoW - 18);
        int small = Math.min(28, Math.max(22, (controlW - 54 - 8) / 2));
        int clearKeepW = Math.max(1, controlW - small * 2 - 8);
        QuickStackTextButton minus = new QuickStackTextButton(infoX + 9, keepControlsY, small, 18,
                Component.literal("-"), button -> adjustKeepCount(-1));
        minus.setTooltip(Tooltip.create(Component.literal("Keep one fewer item in selected slots.")));
        this.addRenderableWidget(minus);

        QuickStackTextButton plus = new QuickStackTextButton(infoX + 9 + small + 4, keepControlsY, small, 18,
                Component.literal("+"), button -> adjustKeepCount(1), true);
        plus.setTooltip(Tooltip.create(Component.literal("Keep one more item in selected slots.")));
        this.addRenderableWidget(plus);

        QuickStackTextButton clearKeep = new QuickStackTextButton(infoX + 9 + (small + 4) * 2, keepControlsY,
                clearKeepW, 18,
                Component.literal("Keep 0"), button -> clearKeepCount());
        clearKeep.setTooltip(Tooltip.create(Component.literal("Clear the keep count on selected slots.")));
        this.addRenderableWidget(clearKeep);

        QuickStackTextButton clearAll = new QuickStackTextButton(panelX + panelW - PAD - 58, panelY + 28, 58, 18,
                Component.literal("Reset"), button -> clearCurrentRules());
        clearAll.setTooltip(Tooltip.create(Component.literal("Clear every quick-stack rule in this world/server scope.")));
        this.addRenderableWidget(clearAll);
    }

    @Override
    protected void paintScreen(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        QuickStackUi.scrim(g, this.width, this.height);
        QuickStackUi.window(g, panelX, panelY, panelW, panelH);

        text(g, "Quick Stack Rules", panelX + PAD, panelY + 9, QuickStackUi.TEXT);
        text(g, "Player inventory", panelX + PAD, panelY + 31, QuickStackUi.TEXT_MUTED);
        QuickStackUi.divider(g, panelX + PAD, panelY + 52, panelW - PAD * 2);

        renderSlots(g, mouseX, mouseY);
        renderSelectionInfo(g);

        updateHitboxes();
    }

    private void renderSlots(GuiGraphics g, int mouseX, int mouseY) {
        List<SlotEntry> slots = targetEntries();
        QuickStackUi.inset(g, gridX - 5, gridY - 5, gridW, gridH);

        for (int i = 0; i < slots.size(); i++) {
            SlotEntry entry = slots.get(i);
            int x = gridX + (i % 9) * slotSize;
            int y = gridY + (i / 9) * slotSize;
            int draw = slotSize - 2;
            boolean hovered = isInside(mouseX, mouseY, x, y, draw, draw);
            boolean selected = selectedSlots.contains(entry.inventorySlot());
            QuickStackRuleStore.SlotRule rule = currentRules().ruleFor(entry.inventorySlot());

            QuickStackUi.slot(g, x, y, draw, hovered, selected);

            ItemStack stack = entry.slot().getItem();
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + (draw - 16) / 2, y + (draw - 16) / 2);
                g.renderItemDecorations(this.font, stack, x + (draw - 16) / 2, y + (draw - 16) / 2);
            }

            if (rule.locked) {
                g.fill(x + 1, y + 1, x + draw - 1, y + 2, MARK_LOCKED);
                g.fill(x + 1, y + draw - 5, x + 5, y + draw - 1, MARK_LOCKED);
            }
            if (rule.hasKeepCount()) {
                QuickStackUi.countBadge(g, this.font, String.valueOf(rule.keepCount),
                        x + draw - 2, y + draw - 2, MARK_KEEP);
            }
        }
    }

    private void renderSelectionInfo(GuiGraphics g) {
        QuickStackUi.inset(g, infoX, infoY, infoW, infoH);
        int tx = infoX + 9;
        int ty = infoY + 9;
        int maxW = infoW - 18;
        int count = selectedSlots.size();

        if (count == 0) {
            text(g, "Select slots", tx, ty, QuickStackUi.TEXT);
            wrapText(g, "Ctrl adds slots. Shift selects a range.",
                    tx, ty + 14, maxW, QuickStackUi.TEXT_MUTED, 3);
        } else if (count == 1) {
            int slotIndex = primarySelectedSlot();
            QuickStackRuleStore.SlotRule rule = currentRules().ruleFor(slotIndex);
            ItemStack stack = stackForInventorySlot(slotIndex);
            text(g, "Slot " + slotIndex, tx, ty, QuickStackUi.TEXT);
            text(g, truncate("Item: " + stackLabel(stack), maxW), tx, ty + 13, QuickStackUi.TEXT_MUTED);
            int stateColor = rule.locked ? MARK_LOCKED : rule.hasKeepCount() ? MARK_KEEP : QuickStackUi.TEXT_DIM;
            text(g, truncate(slotStateText(rule), maxW), tx, ty + 26, stateColor);
        } else {
            text(g, count + " slots selected", tx, ty, QuickStackUi.TEXT);
            wrapText(g, "Changes apply to every selected slot.",
                    tx, ty + 14, maxW, QuickStackUi.TEXT_MUTED, 3);
        }

        int ly = keepControlsY - 44;
        g.fill(tx, ly, tx + 9, ly + 9, MARK_LOCKED);
        text(g, "Locked", tx + 15, ly + 1, QuickStackUi.TEXT_MUTED);
        int ly2 = ly + 14;
        QuickStackUi.countBadge(g, this.font, "1", tx + 9, ly2 + 9, MARK_KEEP);
        text(g, "Keep count", tx + 15, ly2 + 1, QuickStackUi.TEXT_MUTED);
        text(g, "Keep selected", tx, keepControlsY - 12, QuickStackUi.TEXT_DIM);
    }

    private void buildHitboxes() {
        slotButtons.clear();
        for (SlotEntry entry : targetEntries()) {
            QuickStackHitboxButton button = new QuickStackHitboxButton(0, 0, 1, 1,
                    Component.literal("Slot"), hitbox -> {
                String id = ((QuickStackHitboxButton) hitbox).getTargetId();
                if (id != null) {
                    selectSlot(Integer.parseInt(id));
                }
            });
            button.setTargetId(String.valueOf(entry.inventorySlot()));
            slotButtons.add(button);
            this.addRenderableWidget(button);
        }
        updateHitboxes();
    }

    private void updateHitboxes() {
        List<SlotEntry> slots = targetEntries();
        for (int i = 0; i < slotButtons.size(); i++) {
            QuickStackHitboxButton button = slotButtons.get(i);
            if (i >= slots.size()) {
                hide(button);
                continue;
            }
            int x = gridX + (i % 9) * slotSize;
            int y = gridY + (i / 9) * slotSize;
            button.setBounds(x, y, slotSize - 2, slotSize - 2);
            button.setTargetId(String.valueOf(slots.get(i).inventorySlot()));
            button.visible = true;
            button.active = true;
        }
    }

    private void hide(QuickStackHitboxButton button) {
        button.visible = false;
        button.active = false;
        button.setTargetId(null);
    }

    private void selectSlot(int slotIndex) {
        if (!isTargetSlot(slotIndex)) {
            return;
        }
        boolean ctrl = isControlDown();
        boolean shift = isShiftDown();
        if (shift && anchorSlot >= 0) {
            if (!ctrl) {
                selectedSlots.clear();
            }
            int start = Math.min(anchorSlot, slotIndex);
            int end = Math.max(anchorSlot, slotIndex);
            for (int i = start; i <= end; i++) {
                if (isTargetSlot(i)) {
                    selectedSlots.add(i);
                }
            }
            return;
        }
        if (ctrl) {
            if (!selectedSlots.add(slotIndex)) {
                selectedSlots.remove(slotIndex);
            }
            anchorSlot = slotIndex;
            return;
        }
        selectedSlots.clear();
        selectedSlots.add(slotIndex);
        anchorSlot = slotIndex;
    }

    private void lockSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            QuickStackRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.locked = true;
            rule.keepCount = 0;
        }
        store.save();
    }

    private void unlockSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            QuickStackRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.locked = false;
            currentRules().cleanupSlotRule(slotIndex);
        }
        store.save();
    }

    private void adjustKeepCount(int delta) {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            QuickStackRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.locked = false;
            rule.keepCount = QuickStackUi.clamp(rule.keepCount + delta, 0, MAX_KEEP_COUNT);
            currentRules().cleanupSlotRule(slotIndex);
        }
        store.save();
    }

    private void clearKeepCount() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            QuickStackRuleStore.SlotRule rule = currentRules().mutableRuleFor(slotIndex);
            rule.keepCount = 0;
            currentRules().cleanupSlotRule(slotIndex);
        }
        store.save();
    }

    private void clearSelectedSlots() {
        if (selectedSlots.isEmpty()) {
            return;
        }
        for (Integer slotIndex : selectedSlots) {
            currentRules().slotRules.remove(slotIndex);
        }
        store.save();
    }

    private void clearCurrentRules() {
        currentRules().clear();
        selectedSlots.clear();
        anchorSlot = -1;
        store.save();
        this.rebuildWidgets();
    }

    private QuickStackRuleStore.SlotRules currentRules() {
        return store.playerRules();
    }

    private void closeToParent() {
        ClientScreenCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void computeLayout() {
        int availableW = Math.max(1, this.width - 8);
        int availableH = Math.max(1, this.height - 8);
        panelW = QuickStackUi.clamp(availableW, QuickStackUi.MIN_MODAL_W, QuickStackUi.STANDARD_MODAL_W);
        panelH = QuickStackUi.clamp(availableH, QuickStackUi.MIN_MODAL_H, QuickStackUi.STANDARD_MODAL_H);
        panelW = Math.min(panelW, Math.max(1, this.width - 4));
        panelH = Math.min(panelH, Math.max(1, this.height - 4));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int contentTop = panelY + 61;
        int contentBottom = panelY + panelH - PAD;
        int contentW = panelW - PAD * 2;
        int rows = 3;
        int gridAvailH = contentBottom - (contentTop + 4) - 6 - 18 - 14;
        int byWidth = (contentW * 9 / 20) / 9;
        int byHeight = gridAvailH / rows;
        slotSize = QuickStackUi.clamp(Math.min(Math.min(byWidth, byHeight), 20), 18, 20);
        gridX = panelX + PAD + 5;
        gridY = contentTop + 4 + 5;
        gridW = 9 * slotSize + 10;
        gridH = rows * slotSize + 10;
        actionsY = gridY - 5 + gridH + 6;
        infoX = panelX + PAD + gridW + PAD;
        infoY = contentTop + 4;
        infoW = panelX + panelW - PAD - infoX;
        infoH = contentBottom - infoY;
        keepControlsY = infoY + infoH - 24;
    }

    private List<SlotEntry> targetEntries() {
        Inventory inventory = player.getInventory();
        List<SlotEntry> entries = new ArrayList<>();
        for (Slot slot : parent.getMenu().slots) {
            if (slot.container != inventory) {
                continue;
            }
            int inventorySlot = slot.getContainerSlot();
            if (isTargetSlot(inventorySlot)) {
                entries.add(new SlotEntry(slot, inventorySlot));
            }
        }
        entries.sort(Comparator.comparingInt(SlotEntry::inventorySlot));
        return entries;
    }

    private boolean isTargetSlot(int inventorySlot) {
        return inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE;
    }

    private ItemStack stackForInventorySlot(int inventorySlot) {
        for (SlotEntry entry : targetEntries()) {
            if (entry.inventorySlot() == inventorySlot) {
                return entry.slot().getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private int primarySelectedSlot() {
        sanitizeSelection();
        return selectedSlots.isEmpty() ? -1 : selectedSlots.iterator().next();
    }

    private void sanitizeSelection() {
        selectedSlots.removeIf(slot -> !isTargetSlot(slot));
        if (!isTargetSlot(anchorSlot)) {
            anchorSlot = selectedSlots.isEmpty() ? -1 : selectedSlots.iterator().next();
        }
    }

    private static boolean isControlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isKeyDown(int key) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        return GLFW.glfwGetKey(WindowCompat.handle(client.getWindow()), key) == GLFW.GLFW_PRESS;
    }

    private void text(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(this.font, text, x, y, color, false);
    }

    private void wrapText(GuiGraphics g, String text, int x, int y, int maxW, int color, int maxLines) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int line0 = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.font.width(candidate) > maxW && line.length() > 0) {
                text(g, line.toString(), x, y + line0 * 10, color);
                line0++;
                line = new StringBuilder(word);
                if (line0 >= maxLines - 1) {
                    break;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line0 < maxLines && line.length() > 0) {
            text(g, truncate(line.toString(), maxW), x, y + line0 * 10, color);
        }
    }

    private String truncate(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("..."))) + "...";
    }

    private static String slotStateText(QuickStackRuleStore.SlotRule rule) {
        if (rule.locked) {
            return "Locked from quick stack";
        }
        if (rule.hasKeepCount()) {
            return "Keep " + rule.keepCount + " item" + (rule.keepCount == 1 ? "" : "s");
        }
        return "Normal quick-stack slot";
    }

    private static String stackLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getHoverName().getString();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private record SlotEntry(Slot slot, int inventorySlot) {
    }
}
