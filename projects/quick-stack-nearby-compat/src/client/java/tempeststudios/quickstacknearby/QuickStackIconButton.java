package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class QuickStackIconButton extends QuickStackCustomButtonBase {
    public QuickStackIconButton(int x, int y, Component tooltip, OnPress onPress) {
        this(x, y, tooltip, onPress, null);
    }

    public QuickStackIconButton(int x, int y, Component tooltip, OnPress onPress, OnPress secondaryOnPress) {
        super(
                x,
                y,
                QuickStackIconButtonRenderer.SIZE,
                QuickStackIconButtonRenderer.SIZE,
                Component.empty(),
                onPress,
                tooltip,
                secondaryOnPress
        );
    }

    @Override
    protected void paintButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        QuickStackIconButtonRenderer.render(guiGraphics, getX(), getY(), isHoveredOrFocused());
    }
}
