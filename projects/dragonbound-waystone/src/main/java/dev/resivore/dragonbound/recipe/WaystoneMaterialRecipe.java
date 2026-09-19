package dev.resivore.dragonbound.recipe;

import com.mojang.serialization.MapCodec;
import dev.resivore.dragonbound.DragonboundContent;
import dev.resivore.dragonbound.material.WaystoneMaterial;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
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
import java.util.Optional;

/** Shapeless two-input recipe which replaces, rather than layers, Waystone material identity. */
public final class WaystoneMaterialRecipe extends CustomRecipe {
    public static final WaystoneMaterialRecipe INSTANCE = new WaystoneMaterialRecipe();
    public static final MapCodec<WaystoneMaterialRecipe> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneMaterialRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return donor(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Optional<ItemStack> donor = donor(input);
        if (donor.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(DragonboundContent.WAYSTONE_ITEM);
        WaystoneMaterial.eligibleDonorId(donor.get()).ifPresent(id -> result.set(WaystoneMaterial.BLOCK_ID, id));
        return result;
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
        return "dragonbound_waystone:material";
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.BUILDING;
    }

    @Override
    public List<RecipeDisplay> display() {
        // Recipe-book/JEI discovery uses one representative opaque building block; the matcher
        // itself remains broad and validates every actual donor dynamically.
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(
                        new SlotDisplay.ItemSlotDisplay(DragonboundContent.WAYSTONE_ITEM),
                        new SlotDisplay.ItemSlotDisplay(Items.END_STONE_BRICKS)),
                new SlotDisplay.ItemSlotDisplay(DragonboundContent.WAYSTONE_ITEM),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<WaystoneMaterialRecipe> getSerializer() {
        return DragonboundContent.WAYSTONE_MATERIAL_RECIPE;
    }

    private static Optional<ItemStack> donor(CraftingInput input) {
        if (input.ingredientCount() != 2) {
            return Optional.empty();
        }

        ItemStack donor = ItemStack.EMPTY;
        int waystones = 0;
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(DragonboundContent.WAYSTONE_ITEM)) {
                waystones++;
                continue;
            }
            if (!donor.isEmpty() || WaystoneMaterial.eligibleDonorId(stack).isEmpty()) {
                return Optional.empty();
            }
            donor = stack;
        }
        return waystones == 1 && !donor.isEmpty() ? Optional.of(donor) : Optional.empty();
    }
}
