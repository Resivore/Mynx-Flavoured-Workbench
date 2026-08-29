package dev.resivore.matchajei.client.category;

import dev.resivore.matchajei.client.MatchaJeiPlugin;
import dev.resivore.matchajei.data.MatchaDisplayData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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

public final class MatchaAcquisitionCategory extends AbstractRecipeCategory<MatchaDisplayData.Acquisition> {
    private static final int COLOR = 0xFF404040;

    public MatchaAcquisitionCategory(IGuiHelper guiHelper) {
        super(
                MatchaJeiPlugin.MATCHA_ACQUISITIONS,
                Component.translatable("jei.matcha_jei_integration.acquisition"),
                guiHelper.createDrawableItemLike(Items.CHEST),
                160,
                76
        );
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            MatchaDisplayData.Acquisition recipe,
            IFocusGroup focuses
    ) {
        IRecipeSlotBuilder output = builder.addOutputSlot(7, 18)
                .setOutputSlotBackground();
        MatchaJeiPlugin.addExactAwareIngredient(
                builder, RecipeIngredientRole.OUTPUT, output, recipe.output()
        );
    }

    @Override
    public void draw(
            MatchaDisplayData.Acquisition recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor graphics,
            double mouseX,
            double mouseY
    ) {
        graphics.textWithWordWrap(
                Minecraft.getInstance().font,
                Component.literal(recipe.description()),
                32,
                18,
                124,
                COLOR,
                false
        );
    }

    @Override
    public Identifier getIdentifier(MatchaDisplayData.Acquisition recipe) {
        return Identifier.fromNamespaceAndPath(
                "matcha_jei_integration",
                "acquisition/" + recipe.sourceId().getNamespace() + "/"
                        + recipe.sourceId().getPath() + "/" + recipe.displayKey()
        );
    }
}
