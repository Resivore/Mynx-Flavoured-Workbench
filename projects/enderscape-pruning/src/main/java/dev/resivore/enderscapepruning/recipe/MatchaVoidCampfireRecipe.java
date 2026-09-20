package dev.resivore.enderscapepruning.recipe;

import com.mojang.serialization.MapCodec;
import dev.resivore.enderscapepruning.EnderscapePruningRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * C2's exact shapeless Kindling + Void Shale recipe. Matcha Kindling is a
 * component-bearing chicken spawn egg, so a normal item-only ingredient would
 * accidentally accept ordinary spawn eggs. Matcha itself recognizes this item
 * by its beacon_kindling item-model component; this matcher preserves that
 * authoritative identity without creating or registering a companion item.
 */
public final class MatchaVoidCampfireRecipe extends CustomRecipe {
    public static final Identifier MATCHA_KINDLING_MODEL = Identifier.withDefaultNamespace("beacon_kindling");
    public static final Identifier VOID_SHALE_ID = Identifier.parse("enderscape:void_shale");
    public static final Identifier VOID_CAMPFIRE_ID = Identifier.parse("enderscape:void_campfire");
    public static final MatchaVoidCampfireRecipe INSTANCE = new MatchaVoidCampfireRecipe();
    public static final MapCodec<MatchaVoidCampfireRecipe> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, MatchaVoidCampfireRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }

        boolean kindling = false;
        boolean voidShale = false;
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (!kindling && isMatchaKindling(stack)) {
                kindling = true;
            } else if (!voidShale && isVoidShale(stack)) {
                voidShale = true;
            } else {
                return false;
            }
        }
        return kindling && voidShale;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Item output = BuiltInRegistries.ITEM.getValue(VOID_CAMPFIRE_ID);
        return output == null ? ItemStack.EMPTY : new ItemStack(output);
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "enderscape_pruning:void_campfire";
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public List<RecipeDisplay> display() {
        Item voidShale = BuiltInRegistries.ITEM.getValue(VOID_SHALE_ID);
        Item voidCampfire = BuiltInRegistries.ITEM.getValue(VOID_CAMPFIRE_ID);
        if (voidShale == null || voidCampfire == null) {
            return List.of();
        }
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(
                        new SlotDisplay.ItemStackSlotDisplay(
                                ItemStackTemplate.fromNonEmptyStack(matchaKindlingDisplayStack())),
                        new SlotDisplay.ItemSlotDisplay(voidShale)),
                new SlotDisplay.ItemSlotDisplay(voidCampfire),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<MatchaVoidCampfireRecipe> getSerializer() {
        return EnderscapePruningRecipes.MATCHA_VOID_CAMPFIRE;
    }

    public static boolean isMatchaKindling(ItemStack stack) {
        return stack.is(Items.CHICKEN_SPAWN_EGG)
                && MATCHA_KINDLING_MODEL.equals(stack.get(DataComponents.ITEM_MODEL));
    }

    private static boolean isVoidShale(ItemStack stack) {
        return VOID_SHALE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ItemStack matchaKindlingDisplayStack() {
        ItemStack stack = new ItemStack(Items.CHICKEN_SPAWN_EGG);
        stack.set(DataComponents.ITEM_MODEL, MATCHA_KINDLING_MODEL);
        return stack;
    }
}
