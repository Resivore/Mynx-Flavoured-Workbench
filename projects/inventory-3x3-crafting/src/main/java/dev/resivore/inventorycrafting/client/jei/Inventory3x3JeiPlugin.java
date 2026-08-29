package dev.resivore.inventorycrafting.client.jei;

import dev.resivore.inventorycrafting.Inventory3x3Crafting;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class Inventory3x3JeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(Inventory3x3Crafting.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        Inventory3x3RecipeTransferHandler handler = new Inventory3x3RecipeTransferHandler(
                registration.getTransferHelper()
        );
        // JEI stores one handler per exact container-class/recipe-type key. This
        // deliberately replaces only its four-cell InventoryMenu handler.
        registration.addRecipeTransferHandler(handler, RecipeTypes.CRAFTING);
    }
}
