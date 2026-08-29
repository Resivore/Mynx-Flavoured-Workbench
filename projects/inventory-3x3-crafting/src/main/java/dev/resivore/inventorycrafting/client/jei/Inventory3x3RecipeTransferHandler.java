package dev.resivore.inventorycrafting.client.jei;

import dev.resivore.inventorycrafting.InventoryCraftingLayout;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

final class Inventory3x3RecipeTransferHandler
        implements IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper helper;
    private final Inventory3x3RecipeTransferInfo transferInfo;
    private final IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> delegate;

    Inventory3x3RecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        this.transferInfo = new Inventory3x3RecipeTransferInfo();
        this.delegate = helper.createUnregisteredRecipeTransferHandler(this.transferInfo);
    }

    @Override
    public Class<? extends InventoryMenu> getContainerClass() {
        return InventoryMenu.class;
    }

    @Override
    public Optional<MenuType<InventoryMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            InventoryMenu menu,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        if (!this.helper.recipeTransferHasServerSupport()) {
            return this.helper.createUserErrorWithTooltip(
                    Component.translatable("jei.tooltip.error.recipe.transfer.no.server")
            );
        }
        if (!this.transferInfo.canHandle(menu, recipe)
                || this.transferInfo.getInventorySlots(menu, recipe).size()
                != player.getInventory().getNonEquipmentItems().size()) {
            return this.helper.createInternalError();
        }

        // The crafting category exposes nine ordered input views. The basic
        // handler transports each corresponding Slot.index, so the packet uses
        // 1,2,74,3,4,75,76,77,78 rather than assuming a contiguous range.
        if (menu.getInputGridSlots().stream().anyMatch(slot ->
                !InventoryCraftingLayout.isCraftingInput(menu, slot))) {
            return this.helper.createInternalError();
        }
        return this.delegate.transferRecipe(menu, recipe, recipeSlots, player, maxTransfer, doTransfer);
    }
}
