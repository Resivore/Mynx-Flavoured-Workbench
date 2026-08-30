package dev.resivore.mossystone;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JeiPlugin
public final class MossyStoneJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(MossyStoneMod.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var ingredientManager = runtime.getIngredientManager();
        Set<Item> existingItems = ingredientManager.getAllItemStacks().stream()
                .map(ItemStack::getItem)
                .collect(Collectors.toSet());
        List<ItemStack> missingStacks = MossyStoneExposure.missing(
                        MossyStoneMod.ownedItems(), existingItems).stream()
                .map(Item::getDefaultInstance)
                .toList();
        if (!missingStacks.isEmpty()) {
            ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, missingStacks);
        }
    }
}
