package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.ClutterNoMoreClient;
import dev.tazer.clutternomore.client.ClientShapeTooltip;
import dev.tazer.clutternomore.client.RenderHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ClientShapeTooltip.class, remap = false)
abstract class ClientShapeTooltipViewportMixin {
    private static final int INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE = 12;
    private static final int INTERCHANGEABLE_BLOCK_FAMILIES$SPACING = 22;

    @Shadow @Final public List<ItemStack> stacks;
    @Shadow public int selectedIndex;

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true, require = 1)
    private void interchangeableBlockFamilies$capAuditedWidth(
            Font font,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (interchangeableBlockFamilies$needsViewport()) {
            cir.setReturnValue(ClutterNoMoreClient.iconsRendering()
                    ? INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE * INTERCHANGEABLE_BLOCK_FAMILIES$SPACING
                    : 0);
        }
    }

    @Inject(method = "extractImage", at = @At("HEAD"), cancellable = true, require = 1)
    private void interchangeableBlockFamilies$renderAuditedViewport(
            Font font,
            int mouseX,
            int mouseY,
            int width,
            int height,
            GuiGraphicsExtractor guiGraphics,
            CallbackInfo ci
    ) {
        if (!interchangeableBlockFamilies$needsViewport()) {
            return;
        }

        ci.cancel();
        if (!ClutterNoMoreClient.iconsRendering()) {
            return;
        }

        int startIndex = AuditedShapeRuntime.windowStart(
                this.selectedIndex,
                this.stacks.size(),
                INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE
        );
        int startX = mouseX + 2;
        for (int offset = 0; offset < INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE; offset++) {
            RenderHelper.item(guiGraphics, this.stacks.get(startIndex + offset),
                    startX + offset * INTERCHANGEABLE_BLOCK_FAMILIES$SPACING, mouseY);
        }

        Identifier selected = ClutterNoMore.location("textures/gui/selected_shape_inventory.png");
        int selectedX = startX
                + (this.selectedIndex - startIndex) * INTERCHANGEABLE_BLOCK_FAMILIES$SPACING;
        RenderHelper.blit(guiGraphics, selected, selectedX - 3, mouseY - 3,
                0, 0, 22, 22, 22, 22);
    }

    private boolean interchangeableBlockFamilies$needsViewport() {
        return this.stacks.size() > INTERCHANGEABLE_BLOCK_FAMILIES$MAX_VISIBLE
                && !this.stacks.isEmpty()
                && AuditedShapeRuntime.isAudited(this.stacks.get(0).getItem());
    }
}
