package dev.resivore.inventorycrafting.mixin.client;

import dev.resivore.inventorycrafting.Inventory3x3Crafting;
import dev.resivore.inventorycrafting.InventoryCraftingLayout;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    @Unique
    private static final Identifier inventory3x3$INVENTORY_TEXTURE = Identifier.fromNamespaceAndPath(
            Inventory3x3Crafting.MOD_ID,
            "textures/gui/container/inventory.png"
    );

    protected InventoryScreenMixin(
            InventoryMenu menu,
            RecipeBookComponent<?> recipeBookComponent,
            Inventory inventory,
            Component title
    ) {
        super(menu, recipeBookComponent, inventory, title);
    }

    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true, require = 1)
    private void inventory3x3$placeRecipeBookBelowMatrix(CallbackInfoReturnable<ScreenPosition> cir) {
        cir.setReturnValue(new ScreenPosition(
                this.leftPos + InventoryCraftingLayout.RECIPE_BOOK_X,
                this.topPos + InventoryCraftingLayout.RECIPE_BOOK_Y
        ));
    }

    @ModifyArg(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
            ),
            index = 1,
            require = 1
    )
    private Identifier inventory3x3$useAuthoritativeInventoryArtwork(Identifier original) {
        return inventory3x3$INVENTORY_TEXTURE;
    }
}
