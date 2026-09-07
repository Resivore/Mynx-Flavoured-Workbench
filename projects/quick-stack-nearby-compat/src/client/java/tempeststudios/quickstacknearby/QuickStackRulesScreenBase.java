package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class QuickStackRulesScreenBase extends Screen {
    protected QuickStackRulesScreenBase(Component title) {
        super(title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        paintScreen(new GuiGraphics(extractor), mouseX, mouseY, partialTick);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    protected abstract void paintScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}
