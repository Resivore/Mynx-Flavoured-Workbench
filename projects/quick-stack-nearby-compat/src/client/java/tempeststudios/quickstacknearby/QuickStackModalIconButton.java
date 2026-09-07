package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class QuickStackModalIconButton extends QuickStackCustomButtonBase {
    public static final int CLOSE = 0;

    private final int icon;
    private final int size;

    public QuickStackModalIconButton(int x, int y, int size, int icon, Component tooltip, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, tooltip, null);
        this.icon = icon;
        this.size = size;
    }

    @Override
    protected void paintButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int color = QuickStackUi.button(
                guiGraphics,
                getX(),
                getY(),
                size,
                size,
                isHoveredOrFocused(),
                active,
                false
        );
        if (icon == CLOSE) {
            int x = getX() + 5;
            int y = getY() + 5;
            guiGraphics.fill(x, y, x + 2, y + 2, color);
            guiGraphics.fill(x + 2, y + 2, x + 4, y + 4, color);
            guiGraphics.fill(x + 4, y + 4, x + 6, y + 6, color);
            guiGraphics.fill(x + 4, y, x + 6, y + 2, color);
            guiGraphics.fill(x + 2, y + 2, x + 4, y + 4, color);
            guiGraphics.fill(x, y + 4, x + 2, y + 6, color);
        }
    }
}
