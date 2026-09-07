package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The compact magnifier counterpart to QSN's existing quick-stack control. */
public final class NearbySearchIconButton extends QuickStackCustomButtonBase {
    public NearbySearchIconButton(int x, int y, Component tooltip, OnPress onPress) {
        super(x, y, QuickStackIconButtonRenderer.SIZE, QuickStackIconButtonRenderer.SIZE, Component.empty(), onPress, tooltip, null);
    }

    @Override protected void paintButton(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        QuickStackIconButtonRenderer.renderSearch(g, getX(), getY(), isHoveredOrFocused());
    }
}
