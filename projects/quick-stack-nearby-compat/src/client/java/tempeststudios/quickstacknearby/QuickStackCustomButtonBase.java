package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

abstract class QuickStackCustomButtonBase extends Button {
    private final OnPress secondaryOnPress;

    protected QuickStackCustomButtonBase(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            Component tooltip,
            OnPress secondaryOnPress
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.secondaryOnPress = secondaryOnPress;
        if (tooltip != null && !tooltip.getString().isEmpty()) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (handleSecondaryClick(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        paintButton(new GuiGraphics(extractor), mouseX, mouseY, partialTick);
    }

    protected abstract void paintButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    protected final void setButtonBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    private boolean handleSecondaryClick(double mouseX, double mouseY, int button) {
        if (secondaryOnPress != null && button == 1 && active && visible && isMouseOver(mouseX, mouseY)) {
            secondaryOnPress.onPress(this);
            return true;
        }
        return false;
    }
}
