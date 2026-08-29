package dev.resivore.inventorycrafting.client.jei;

import dev.resivore.inventorycrafting.InventoryCraftingLayout;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

final class Inventory3x3RecipeTransferInfo
        implements IRecipeTransferInfo<InventoryMenu, RecipeHolder<CraftingRecipe>> {
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
    public boolean canHandle(InventoryMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        return menu.getGridWidth() == InventoryCraftingLayout.GRID_WIDTH
                && menu.getGridHeight() == InventoryCraftingLayout.GRID_HEIGHT
                && menu.getInputGridSlots().size() == 9;
    }

    @Override
    public List<Slot> getRecipeSlots(InventoryMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        return menu.getInputGridSlots();
    }

    @Override
    public List<Slot> getInventorySlots(InventoryMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        return InventoryCraftingLayout.ordinaryInventorySlots(menu);
    }
}
