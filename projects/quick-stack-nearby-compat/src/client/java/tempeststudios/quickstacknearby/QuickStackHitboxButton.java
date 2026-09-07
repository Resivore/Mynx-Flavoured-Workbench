package tempeststudios.quickstacknearby;

import net.minecraft.network.chat.Component;

public class QuickStackHitboxButton extends QuickStackCustomButtonBase {
    private String targetId;

    public QuickStackHitboxButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, null, null);
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setBounds(int x, int y, int width, int height) {
        setButtonBounds(x, y, width, height);
    }

    @Override
    protected void paintButton(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
