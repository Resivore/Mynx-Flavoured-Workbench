package dev.resivore.workbenchtestmarker.mixin;

import dev.resivore.workbenchtestmarker.MarkerState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
abstract class TitleScreenMixin {
    private static final int ACTIVE_COLOR = 0xFFFFFF55;
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int DETAIL_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xB0000000;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void workbenchTestMarker$render(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci) {
        MarkerState state = MarkerState.current();
        Font font = ((TitleScreen) (Object) this).getFont();
        List<String> lines = new ArrayList<>();
        lines.add("WORKBENCH TEST");
        lines.addAll(state.lines());

        int x = 8;
        int y = 8;
        int lineHeight = font.lineHeight + 2;
        int contentWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        graphics.fill(x - 4, y - 4, x + contentWidth + 4, y + lineHeight * lines.size() + 2, BACKGROUND_COLOR);
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0
                    ? (state.status() == MarkerState.Status.ACTIVE ? ACTIVE_COLOR : WARNING_COLOR)
                    : DETAIL_COLOR;
            graphics.text(font, lines.get(index), x, y + lineHeight * index, color, true);
        }
    }
}
