package dev.resivore.matchajei.client;

import com.mojang.serialization.Codec;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.helpers.IColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A full-component ItemStack identity for the few vanilla item bases whose
 * subtype interpretation is already owned by JEI and intentionally left alone.
 */
public final class MatchaExactIngredient {
    public static final IIngredientType<MatchaExactIngredient> TYPE = new IIngredientType<>() {
        @Override
        public Class<? extends MatchaExactIngredient> getIngredientClass() {
            return MatchaExactIngredient.class;
        }

        @Override
        public String getUid() {
            return "matcha_jei_integration:exact_item_stack";
        }
    };
    public static final Codec<MatchaExactIngredient> CODEC = ItemStack.CODEC.xmap(
            MatchaExactIngredient::of,
            MatchaExactIngredient::stack
    );

    private final ItemStack stack;

    private MatchaExactIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("A Matcha exact ingredient cannot be empty");
        }
        this.stack = stack.copy();
    }

    public static MatchaExactIngredient of(ItemStack stack) {
        return new MatchaExactIngredient(stack);
    }

    public ItemStack stack() {
        return stack.copy();
    }

    ItemStack stackView() {
        return stack;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MatchaExactIngredient that
                && ItemStack.isSameItemSameComponents(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }

    @Override
    public String toString() {
        return "MatchaExactIngredient[" + stack + "]";
    }

    public static final class Helper implements IIngredientHelper<MatchaExactIngredient> {
        private final IColorHelper colorHelper;

        public Helper(IColorHelper colorHelper) {
            this.colorHelper = colorHelper;
        }

        @Override
        public IIngredientType<MatchaExactIngredient> getIngredientType() {
            return TYPE;
        }

        @Override
        public String getDisplayName(MatchaExactIngredient ingredient) {
            return ingredient.stack.getHoverName().getString();
        }

        @Override
        public Object getUid(MatchaExactIngredient ingredient, UidContext context) {
            // Equality deliberately ignores stack count but compares every real component.
            return ingredient;
        }

        @Override
        public long getAmount(MatchaExactIngredient ingredient) {
            return ingredient.stack.getCount();
        }

        @Override
        public MatchaExactIngredient copyWithAmount(MatchaExactIngredient ingredient, long amount) {
            ItemStack copy = ingredient.stack();
            copy.setCount(Math.toIntExact(amount));
            return MatchaExactIngredient.of(copy);
        }

        @Override
        public Iterable<Integer> getColors(MatchaExactIngredient ingredient) {
            return colorHelper.getColors(ingredient.stack, 2);
        }

        @Override
        public Identifier getIdentifier(MatchaExactIngredient ingredient) {
            return BuiltInRegistries.ITEM.getKey(ingredient.stack.getItem());
        }

        @Override
        public ItemStack getCheatItemStack(MatchaExactIngredient ingredient) {
            return ingredient.stack();
        }

        @Override
        public MatchaExactIngredient copyIngredient(MatchaExactIngredient ingredient) {
            return MatchaExactIngredient.of(ingredient.stack);
        }

        @Override
        public boolean isValidIngredient(MatchaExactIngredient ingredient) {
            return !ingredient.stack.isEmpty();
        }

        @Override
        public String getErrorInfo(MatchaExactIngredient ingredient) {
            return ingredient.toString();
        }
    }

    public static final class Renderer implements IIngredientRenderer<MatchaExactIngredient> {
        @Override
        public void render(GuiGraphicsExtractor graphics, MatchaExactIngredient ingredient) {
            ItemStack stack = ingredient.stackView();
            Minecraft minecraft = Minecraft.getInstance();
            graphics.fakeItem(stack, 0, 0);
            graphics.itemDecorations(minecraft.font, stack, 0, 0);
        }

        @Override
        public List<Component> getTooltip(MatchaExactIngredient ingredient, TooltipFlag flag) {
            Minecraft minecraft = Minecraft.getInstance();
            return ingredient.stack.getTooltipLines(
                    Item.TooltipContext.of(minecraft.level),
                    minecraft.player,
                    flag
            );
        }
    }
}
