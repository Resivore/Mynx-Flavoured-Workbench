package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.client.RenderHelper;
import dev.tazer.clutternomore.client.ShapeSwitcherOverlay;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ShapeSwitcherOverlay.class, remap = false)
abstract class ShapeSwitcherOverlayViewportMixin {
    private static final int INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE = 12;
    private static final int INTERCHANGEABLE_BLOCK_FAMILIES$SPACING = 22;

    @Shadow @Final public Minecraft minecraft;
    @Shadow @Final public List<Item> shapes;
    @Shadow public int selectedIndex;
    @Shadow public float currentIndex;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void interchangeableBlockFamilies$renderAuditedViewport(
            GuiGraphicsExtractor guiGraphics,
            float partialTick,
            CallbackInfo ci
    ) {
        if (this.shapes.size() <= INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE
                || this.shapes.isEmpty()
                || !AuditedShapeRuntime.isAudited(this.shapes.get(0))) {
            return;
        }

        ci.cancel();
        int y = guiGraphics.guiHeight() / 2 + 20;
        int centreX = guiGraphics.guiWidth() / 2 - 8;

        float smoothing = 1.0f - (float) Math.exp(-5.0f * partialTick);
        this.currentIndex = Mth.lerp(smoothing, this.currentIndex, this.selectedIndex);

        Identifier background = ClutterNoMore.location("textures/gui/shape_background.png");
        Identifier selected = ClutterNoMore.location("textures/gui/selected_shape.png");
        ItemStack heldStack = this.minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);

        int startX = Mth.floor(centreX - this.currentIndex * INTERCHANGEABLE_BLOCK_FAMILIES$SPACING);
        RenderHelper.blit(guiGraphics, selected, centreX - 3, y - 3,
                0, 0, 22, 22, 22, 22);
        for (int index = 0; index < this.shapes.size(); index++) {
            int x = startX + index * INTERCHANGEABLE_BLOCK_FAMILIES$SPACING;
            if (x < 0 || x + 16 > guiGraphics.guiWidth()) {
                continue;
            }
            RenderHelper.blit(guiGraphics, background, x, y, 0, 0, 16, 16, 16, 16);
            RenderHelper.item(guiGraphics, ShapeMap.transferStack(heldStack, index), x, y);
        }
    }
}
