package dev.resivore.enderscapepruning.client;

import dev.resivore.enderscapepruning.PruningContract;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Optional JEI integration: remove every matching component variant, not base IDs globally. */
@JeiPlugin
public final class EnderscapePruningJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("enderscape_pruning", "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(stack -> PruningContract.isSuppressedItem(stack)
                        || PruningContract.hasSuppressedStoredEnchantment(stack))
                .map(ItemStack::copy)
                .toList();
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
