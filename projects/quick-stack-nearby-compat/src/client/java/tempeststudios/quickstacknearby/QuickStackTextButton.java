package tempeststudios.quickstacknearby;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class QuickStackTextButton extends QuickStackCustomButtonBase {
    private final boolean primary;

    public QuickStackTextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, false);
    }

    public QuickStackTextButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            boolean primary
    ) {
        super(x, y, width, height, message, onPress, null, null);
        this.primary = primary;
    }

    @Override
    protected void paintButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int color = QuickStackUi.button(
                guiGraphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                isHoveredOrFocused(),
                active,
                primary
        );
        Font font = Minecraft.getInstance().font;
        String text = fittedText(font, getMessage().getString(), Math.max(0, getWidth() - 6));
        int textX = getX() + (getWidth() - font.width(text)) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        guiGraphics.drawString(font, text, textX, textY, color, false);
    }

    private static String fittedText(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }
}
