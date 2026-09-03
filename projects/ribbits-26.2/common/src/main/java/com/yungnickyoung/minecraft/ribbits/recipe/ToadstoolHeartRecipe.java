package com.yungnickyoung.minecraft.ribbits.recipe;

import com.mojang.serialization.MapCodec;
import com.yungnickyoung.minecraft.ribbits.module.BlockModule;
import com.yungnickyoung.minecraft.ribbits.module.ItemModule;
import com.yungnickyoung.minecraft.ribbits.module.RecipeModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exact component-aware recipe for the permanent Ribbits Toadstool Heart item.
 * The Matcha Crystal Heart deliberately has a vanilla base item, so an ordinary
 * shaped ingredient would incorrectly accept every poisonous potato.
 */
public final class ToadstoolHeartRecipe extends CustomRecipe {
    public static final Identifier CRYSTAL_HEART_MODEL = Identifier.withDefaultNamespace("heart_container");
    public static final ToadstoolHeartRecipe INSTANCE = new ToadstoolHeartRecipe();
    public static final MapCodec<ToadstoolHeartRecipe> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToadstoolHeartRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return matchesPattern(input, BlockModule.TOADSTOOL.get().asItem());
    }

    static boolean matchesPattern(CraftingInput input, Item toadstool) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 6) {
            return false;
        }

        return input.getItem(0, 0).is(toadstool)
                && input.getItem(1, 0).isEmpty()
                && input.getItem(2, 0).is(toadstool)
                && input.getItem(0, 1).is(toadstool)
                && isOriginalCrystalHeart(input.getItem(1, 1))
                && input.getItem(2, 1).is(toadstool)
                && input.getItem(0, 2).isEmpty()
                && input.getItem(1, 2).is(toadstool)
                && input.getItem(2, 2).isEmpty();
    }

    public static boolean isOriginalCrystalHeart(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, originalCrystalHeartStack());
    }

    public static ItemStack originalCrystalHeartStack() {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO);
        stack.set(DataComponents.ITEM_MODEL, CRYSTAL_HEART_MODEL);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.kleispack.crystal_heart"));
        stack.set(DataComponents.RARITY, Rarity.RARE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.remove(DataComponents.CONSUMABLE);
        return stack;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input) {
        return new ItemStack(ItemModule.TOADSTOOL_HEART.get(), 1);
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
    public @NotNull String group() {
        return "ribbits:toadstool_heart";
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        SlotDisplay foliage = new SlotDisplay.ItemSlotDisplay(BlockModule.TOADSTOOL.get().asItem());

        return List.of(new ShapedCraftingRecipeDisplay(
                3,
                3,
                List.of(
                        foliage, SlotDisplay.Empty.INSTANCE, foliage,
                        foliage, new SlotDisplay.ItemStackSlotDisplay(
                                ItemStackTemplate.fromNonEmptyStack(originalCrystalHeartStack())), foliage,
                        SlotDisplay.Empty.INSTANCE, foliage, SlotDisplay.Empty.INSTANCE
                ),
                new SlotDisplay.ItemSlotDisplay(ItemModule.TOADSTOOL_HEART.get()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    @Override
    public @NotNull RecipeSerializer<ToadstoolHeartRecipe> getSerializer() {
        return RecipeModule.TOADSTOOL_HEART;
    }
}
