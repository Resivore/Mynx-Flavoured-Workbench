package dev.resivore.matchajei.client.category;

import dev.resivore.matchajei.client.MatchaJeiPlugin;
import dev.resivore.matchajei.data.MatchaDisplayData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public final class MatchaVillagerTradeCategory extends AbstractRecipeCategory<MatchaDisplayData.Trade> {
    private static final int COLOR = 0xFF404040;
    private final IDrawable arrow;
    private final IDrawable plus;

    public MatchaVillagerTradeCategory(IGuiHelper guiHelper) {
        super(
                MatchaJeiPlugin.MATCHA_VILLAGER_TRADES,
                Component.translatable("jei.matcha_jei_integration.villager_trades"),
                guiHelper.createDrawableItemLike(Items.EMERALD),
                140,
                60
        );
        this.arrow = guiHelper.getRecipeArrow();
        this.plus = guiHelper.getRecipePlusSign();
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            MatchaDisplayData.Trade recipe,
            IFocusGroup focuses
    ) {
        IRecipeSlotBuilder firstInput = builder.addInputSlot(7, 36)
                .setStandardSlotBackground();
        MatchaJeiPlugin.addExactAwareIngredient(
                builder, RecipeIngredientRole.INPUT, firstInput, recipe.firstInput()
        );
        if (!recipe.secondInput().isEmpty()) {
            IRecipeSlotBuilder secondInput = builder.addInputSlot(37, 36)
                    .setStandardSlotBackground();
            MatchaJeiPlugin.addExactAwareIngredient(
                    builder, RecipeIngredientRole.INPUT, secondInput, recipe.secondInput()
            );
        }
        IRecipeSlotBuilder output = builder.addOutputSlot(113, 36)
                .setOutputSlotBackground();
        MatchaJeiPlugin.addExactAwareIngredient(
                builder, RecipeIngredientRole.OUTPUT, output, recipe.output()
        );
    }

    @Override
    public void draw(
            MatchaDisplayData.Trade recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor graphics,
            double mouseX,
            double mouseY
    ) {
        Component profession = recipe.profession().equals("wandering_trader")
                ? Component.translatable("entity.minecraft.wandering_trader")
                : Component.translatable("entity.minecraft.villager." + recipe.profession());
        Component heading = recipe.level() > 0
                ? Component.empty().append(profession).append(" — ")
                        .append(Component.translatable("merchant.level." + recipe.level()))
                : profession;
        graphics.text(Minecraft.getInstance().font, heading, 1, 3, COLOR, false);
        if (recipe.conditional()) {
            graphics.textWithWordWrap(
                    Minecraft.getInstance().font,
                    Component.translatable("jei.matcha_jei_integration.conditional_trade"),
                    1,
                    14,
                    138,
                    COLOR,
                    false
            );
        }
        if (!recipe.secondInput().isEmpty()) {
            plus.draw(graphics, 28, 41);
        }
        arrow.draw(graphics, 75, 37);
    }

    @Override
    public Identifier getIdentifier(MatchaDisplayData.Trade recipe) {
        return Identifier.fromNamespaceAndPath(
                "matcha_jei_integration",
                "villager_trade/" + recipe.sourceId().getNamespace() + "/"
                        + recipe.sourceId().getPath() + "/" + recipe.displayKey()
        );
    }
}
