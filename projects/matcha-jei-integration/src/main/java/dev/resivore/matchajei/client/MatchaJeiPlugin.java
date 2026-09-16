package dev.resivore.matchajei.client;

import dev.resivore.matchajei.client.category.MatchaAcquisitionCategory;
import dev.resivore.matchajei.client.category.MatchaVillagerTradeCategory;
import dev.resivore.matchajei.data.MatchaDisplayData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

@JeiPlugin
public final class MatchaJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("matcha_jei_integration", "jei");
    public static final IRecipeType<MatchaDisplayData.Trade> MATCHA_VILLAGER_TRADES = IRecipeType.create(
            "matcha_jei_integration", "villager_trades", MatchaDisplayData.Trade.class
    );
    public static final IRecipeType<MatchaDisplayData.Acquisition> MATCHA_ACQUISITIONS = IRecipeType.create(
            "matcha_jei_integration", "acquisition", MatchaDisplayData.Acquisition.class
    );
    private static final Set<Identifier> JEI_OWNED_SUBTYPE_ITEMS = Set.of(
            Identifier.withDefaultNamespace("tipped_arrow"),
            Identifier.withDefaultNamespace("potion"),
            Identifier.withDefaultNamespace("splash_potion"),
            Identifier.withDefaultNamespace("lingering_potion"),
            Identifier.withDefaultNamespace("enchanted_book"),
            Identifier.withDefaultNamespace("light"),
            Identifier.withDefaultNamespace("painting"),
            Identifier.withDefaultNamespace("goat_horn"),
            Identifier.withDefaultNamespace("firework_rocket"),
            Identifier.withDefaultNamespace("firework_star"),
            Identifier.withDefaultNamespace("suspicious_stew"),
            Identifier.withDefaultNamespace("ominous_bottle"),
            Identifier.withDefaultNamespace("shield"),
            Identifier.withDefaultNamespace("decorated_pot")
    );

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        var componentTypes = MatchaComponentInventory.identityComponentTypes();
        for (Identifier itemId : MatchaComponentInventory.itemIds()) {
            if (JEI_OWNED_SUBTYPE_ITEMS.contains(itemId)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                    .orElseThrow(() -> new IllegalStateException("Unknown audited Matcha base item " + itemId));
            registration.registerFromDataComponentTypes(item, componentTypes);
        }
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(
                MatchaExactIngredient.TYPE,
                List.of(),
                new MatchaExactIngredient.Helper(registration.getColorHelper()),
                new MatchaExactIngredient.Renderer(),
                MatchaExactIngredient.CODEC
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MatchaVillagerTradeCategory(guiHelper),
                new MatchaAcquisitionCategory(guiHelper)
        );
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new MatchaExactRecipeBridge());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        MatchaJeiRuntimeData.onRuntimeAvailable(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        MatchaJeiRuntimeData.onRuntimeUnavailable();
    }

    public static void addExactAwareIngredient(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            IIngredientAcceptor<?> visibleSlot,
            ItemStack stack
    ) {
        visibleSlot.add(stack);
        if (!usesJeiOwnedSubtype(stack)) {
            return;
        }
        IIngredientAcceptor<?> exactIngredient = builder.addInvisibleIngredients(role)
                .add(MatchaExactIngredient.TYPE, MatchaExactIngredient.of(stack));
        builder.createFocusLink(visibleSlot, exactIngredient);
    }

    static boolean usesJeiOwnedSubtype(ItemStack stack) {
        return JEI_OWNED_SUBTYPE_ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

}
