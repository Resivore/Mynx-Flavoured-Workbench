package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class QuickStackUi {
    public static final int ACCENT = 0xFFFF7A59;

    public static final int STANDARD_MODAL_W = 448;
    public static final int STANDARD_MODAL_H = 262;
    public static final int MIN_MODAL_W = 360;
    public static final int MIN_MODAL_H = 236;

    public static final int SCRIM = 0xC4000000;
    public static final int SHADOW = 0x55000000;
    public static final int WINDOW_BG = 0xF21B1E25;
    public static final int BORDER_DARK = 0xFF090A0D;
    public static final int BORDER = 0xFF373C47;
    public static final int BORDER_HI = 0xFF4C5260;
    public static final int PANEL = 0xFF161922;
    public static final int CARD = 0xFF222630;
    public static final int CARD_HOVER = 0xFF2B313C;
    public static final int SLOT = 0xFF2A2E37;
    public static final int SLOT_HOVER = 0xFF353B45;
    public static final int TEXT = 0xFFECEEF2;
    public static final int TEXT_MUTED = 0xFF9CA2AE;
    public static final int TEXT_DIM = 0xFF686E78;
    public static final int TEXT_DISABLED = 0xFF565C66;
    public static final int TEXT_ON_ACCENT = 0xFF1A1206;

    private static final int BTN = 0xFF272C36;
    private static final int BTN_HOVER = 0xFF323845;
    private static final int BTN_BORDER = 0xFF3E4450;
    private static final int BTN_BORDER_HOVER = 0xFF565E6D;
    private static final int BTN_DISABLED = 0xFF1D2027;

    private QuickStackUi() {
    }

    public static void scrim(GuiGraphics g, int width, int height) {
        g.fill(0, 0, width, height, SCRIM);
    }

    public static void window(GuiGraphics g, int x, int y, int w, int h) {
        fillRound(g, x + 2, y + 4, w, h, SHADOW);
        fillRound(g, x + 1, y + 2, w, h, SHADOW);
        fillRound(g, x, y, w, h, WINDOW_BG);
        borderRound(g, x, y, w, h, BORDER_DARK);
        g.fill(x + 1, y + 3, x + 2, y + h - 2, mix(WINDOW_BG, BORDER, 0.6f));
        g.fill(x + w - 2, y + 3, x + w - 1, y + h - 2, BORDER_DARK);
        g.fill(x + 2, y + 1, x + w - 2, y + 3, ACCENT);
        g.fill(x + 2, y + 3, x + w - 2, y + 4, mix(ACCENT, BORDER_DARK, 0.45f));
    }

    public static void divider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BORDER_DARK);
        g.fill(x, y + 1, x + w, y + 2, mix(WINDOW_BG, BORDER_HI, 0.5f));
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        fillRound(g, x, y, w, h, PANEL);
        insetBorder(g, x, y, w, h);
    }

    public static void insetBorder(GuiGraphics g, int x, int y, int w, int h) {
        borderRound(g, x, y, w, h, BORDER_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF0B0D12);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF0B0D12);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, mix(PANEL, BORDER_HI, 0.35f));
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, mix(PANEL, BORDER_HI, 0.35f));
    }

    public static void slot(GuiGraphics g, int x, int y, int size, boolean hovered, boolean selected) {
        int face = selected ? mix(SLOT, ACCENT, 0.42f) : hovered ? SLOT_HOVER : SLOT;
        int x2 = x + size;
        int y2 = y + size;
        g.fill(x, y, x2, y2, BORDER_DARK);
        g.fill(x + 1, y + 1, x2 - 1, y2 - 1, face);
        g.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF15171C);
        g.fill(x + 1, y + 1, x + 2, y2 - 1, 0xFF15171C);
        g.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, mix(face, 0xFFFFFFFF, 0.06f));
        if (selected) {
            g.fill(x, y, x2, y + 1, ACCENT);
            g.fill(x, y2 - 1, x2, y2, ACCENT);
            g.fill(x, y, x + 1, y2, ACCENT);
            g.fill(x2 - 1, y, x2, y2, ACCENT);
        }
    }

    public static void countBadge(GuiGraphics g, Font font, String text, int rightX, int bottomY, int color) {
        int tw = font.width(text);
        g.fill(rightX - tw - 2, bottomY - 9, rightX + 1, bottomY + 1, 0xD6000000);
        g.drawString(font, text, rightX - tw, bottomY - 8, color, false);
    }

    public static int button(GuiGraphics g, int x, int y, int w, int h,
                             boolean hovered, boolean enabled, boolean primary) {
        int x2 = x + w;
        int y2 = y + h;
        if (!enabled) {
            g.fill(x, y, x2, y2, BTN_DISABLED);
            border(g, x, y, w, h, BORDER);
            return TEXT_DISABLED;
        }
        if (primary) {
            int bg = hovered ? mix(ACCENT, 0xFFFFFFFF, 0.14f) : ACCENT;
            g.fill(x, y, x2, y2, bg);
            border(g, x, y, w, h, mix(ACCENT, 0xFF000000, 0.40f));
            g.fill(x + 1, y + 1, x2 - 1, y + 2, mix(bg, 0xFFFFFFFF, 0.28f));
            return TEXT_ON_ACCENT;
        }
        int bg = hovered ? BTN_HOVER : BTN;
        g.fill(x, y, x2, y2, bg);
        border(g, x, y, w, h, hovered ? BTN_BORDER_HOVER : BTN_BORDER);
        g.fill(x + 1, y + 1, x2 - 1, y + 2, mix(bg, 0xFFFFFFFF, 0.07f));
        return hovered ? TEXT : TEXT_MUTED;
    }

    public static void fillRound(GuiGraphics g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x + 1, y, x2 - 1, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    public static void borderRound(GuiGraphics g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x + 1, y, x2 - 1, y + 1, color);
        g.fill(x + 1, y2 - 1, x2 - 1, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x, y, x2, y + 1, color);
        g.fill(x, y2 - 1, x2, y2, color);
        g.fill(x, y + 1, x + 1, y2 - 1, color);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, color);
    }

    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
