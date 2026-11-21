package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.List;
import java.util.Map;

/** Loaded only in the real-provider suite. Each interaction gets an independent test structure. */
public final class EnderscapeTests {
    @GameTest
    public void nativeIngredientsAndOutputs(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String material : StoneTests.END_MATERIALS) {
            var base = BuiltInRegistries.ITEM.getValue(Identifier.parse(
                    "enderscape:" + (material.equals("dusk_purpur") ? "dusk_purpur_block" : material))).getDefaultInstance();
            var slab = BuiltInRegistries.ITEM.getValue(Identifier.parse("enderscape:" + material + "_slab")).getDefaultInstance();
            var grids = Map.of(
                    "frame", CraftingInput.of(3, 3, List.of(slab, slab, slab, slab, ItemStack.EMPTY, slab, slab, slab, slab)),
                    "arrow_slit", CraftingInput.of(2, 3, List.of(base, base, slab, slab, base, base)),
                    "louvered_shutter", CraftingInput.of(1, 3, List.of(slab, slab, slab)));
            for (var entry : grids.entrySet()) {
                var id = Identifier.parse("architectural_material_closure:" + material + "_" + entry.getKey());
                var match = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, entry.getValue(), level).orElseThrow();
                var result = match.value().assemble(entry.getValue());
                helper.assertTrue(match.id().identifier().equals(id) && result.is(BuiltInRegistries.ITEM.getValue(id))
                        && result.getCount() == (entry.getKey().equals("frame") ? 12 : 3), id + " crafting grid");
            }
            var input = new SingleRecipeInput(base);
            for (String form : DirectFormTests.PATH_FORMS) {
                var id = Identifier.parse("architectural_material_closure:" + material + "_" + form);
                var recipe = level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id)).orElseThrow().value();
                helper.assertTrue(recipe instanceof StonecutterRecipe, id + " stonecutting recipe");
                var cut = (StonecutterRecipe) recipe;
                var result = cut.assemble(input);
                int count = form.endsWith("_path") || form.endsWith("_paving") ? 4 : 1;
                helper.assertTrue(cut.matches(input, level) && result.is(BuiltInRegistries.ITEM.getValue(id))
                        && result.getCount() == count, id + " independent stonecutting output");
            }
        }
        helper.succeed();
    }


    @GameTest
    public void veraditeReferenceStatesAndData(GameTestHelper helper) {
        DirectFormTests.referenceStatesAndData(helper, "veradite");
    }

    @GameTest
    public void veraditePlacement(GameTestHelper helper) {
        DirectFormTests.placement(helper, "veradite");
    }

    @GameTest
    public void veraditeEngraving(GameTestHelper helper) {
        DirectFormTests.engraving(helper, "veradite");
    }

    @GameTest
    public void veraditeWindowInteractions(GameTestHelper helper) {
        DirectFormTests.windowInteractions(helper, "veradite");
    }

    @GameTest
    public void veraditeStackedMasonry(GameTestHelper helper) {
        DirectFormTests.stackedMasonry(helper, "veradite");
    }

    @GameTest
    public void veraditeCompleteStateAndShapeParity(GameTestHelper helper) {
        MasonryTests.completeStateAndShapeParity(helper, "veradite");
    }

    @GameTest
    public void veraditeWetAndDryPlacement(GameTestHelper helper) {
        MasonryTests.wetAndDryPlacement(helper, "veradite");
    }

    @GameTest
    public void veraditeColumnAndMouldingChanges(GameTestHelper helper) {
        MasonryTests.columnAndMouldingChanges(helper, "veradite");
    }

    @GameTest
    public void veraditeFenceAndFrameChanges(GameTestHelper helper) {
        MasonryTests.fenceAndFrameChanges(helper, "veradite");
    }

    @GameTest
    public void veraditeUrnSoilAndPlant(GameTestHelper helper) {
        MasonryTests.urnSoilAndPlant(helper, "veradite");
    }

    @GameTest
    public void veraditeSupportsAndShutter(GameTestHelper helper) {
        var registry = BuiltInRegistries.BLOCK;
        StoneTests.supportsAndShutterInteraction(helper,
                registry.getValue(Identifier.parse("architectural_material_closure:veradite_parapet")),
                registry.getValue(Identifier.parse("architectural_material_closure:veradite_louvered_shutter")));
    }

    @GameTest
    public void mirestoneReferenceStatesAndData(GameTestHelper helper) {
        DirectFormTests.referenceStatesAndData(helper, "mirestone");
    }

    @GameTest
    public void mirestonePlacement(GameTestHelper helper) {
        DirectFormTests.placement(helper, "mirestone");
    }

    @GameTest
    public void mirestoneEngraving(GameTestHelper helper) {
        DirectFormTests.engraving(helper, "mirestone");
    }

    @GameTest
    public void mirestoneWindowInteractions(GameTestHelper helper) {
        DirectFormTests.windowInteractions(helper, "mirestone");
    }

    @GameTest
    public void mirestoneStackedMasonry(GameTestHelper helper) {
        DirectFormTests.stackedMasonry(helper, "mirestone");
    }

    @GameTest
    public void mirestoneCompleteStateAndShapeParity(GameTestHelper helper) {
        MasonryTests.completeStateAndShapeParity(helper, "mirestone");
    }

    @GameTest
    public void mirestoneWetAndDryPlacement(GameTestHelper helper) {
        MasonryTests.wetAndDryPlacement(helper, "mirestone");
    }

    @GameTest
    public void mirestoneColumnAndMouldingChanges(GameTestHelper helper) {
        MasonryTests.columnAndMouldingChanges(helper, "mirestone");
    }

    @GameTest
    public void mirestoneFenceAndFrameChanges(GameTestHelper helper) {
        MasonryTests.fenceAndFrameChanges(helper, "mirestone");
    }

    @GameTest
    public void mirestoneUrnSoilAndPlant(GameTestHelper helper) {
        MasonryTests.urnSoilAndPlant(helper, "mirestone");
    }

    @GameTest
    public void mirestoneSupportsAndShutter(GameTestHelper helper) {
        var registry = BuiltInRegistries.BLOCK;
        StoneTests.supportsAndShutterInteraction(helper,
                registry.getValue(Identifier.parse("architectural_material_closure:mirestone_parapet")),
                registry.getValue(Identifier.parse("architectural_material_closure:mirestone_louvered_shutter")));
    }

    @GameTest
    public void kuroditeReferenceStatesAndData(GameTestHelper helper) {
        DirectFormTests.referenceStatesAndData(helper, "kurodite");
    }

    @GameTest
    public void kuroditePlacement(GameTestHelper helper) {
        DirectFormTests.placement(helper, "kurodite");
    }

    @GameTest
    public void kuroditeEngraving(GameTestHelper helper) {
        DirectFormTests.engraving(helper, "kurodite");
    }

    @GameTest
    public void kuroditeWindowInteractions(GameTestHelper helper) {
        DirectFormTests.windowInteractions(helper, "kurodite");
    }

    @GameTest
    public void kuroditeStackedMasonry(GameTestHelper helper) {
        DirectFormTests.stackedMasonry(helper, "kurodite");
    }

    @GameTest
    public void kuroditeCompleteStateAndShapeParity(GameTestHelper helper) {
        MasonryTests.completeStateAndShapeParity(helper, "kurodite");
    }

    @GameTest
    public void kuroditeWetAndDryPlacement(GameTestHelper helper) {
        MasonryTests.wetAndDryPlacement(helper, "kurodite");
    }

    @GameTest
    public void kuroditeColumnAndMouldingChanges(GameTestHelper helper) {
        MasonryTests.columnAndMouldingChanges(helper, "kurodite");
    }

    @GameTest
    public void kuroditeFenceAndFrameChanges(GameTestHelper helper) {
        MasonryTests.fenceAndFrameChanges(helper, "kurodite");
    }

    @GameTest
    public void kuroditeUrnSoilAndPlant(GameTestHelper helper) {
        MasonryTests.urnSoilAndPlant(helper, "kurodite");
    }

    @GameTest
    public void kuroditeSupportsAndShutter(GameTestHelper helper) {
        var registry = BuiltInRegistries.BLOCK;
        StoneTests.supportsAndShutterInteraction(helper,
                registry.getValue(Identifier.parse("architectural_material_closure:kurodite_parapet")),
                registry.getValue(Identifier.parse("architectural_material_closure:kurodite_louvered_shutter")));
    }

    @GameTest
    public void duskPurpurRecipes(GameTestHelper helper) {
        DirectFormTests.familyRecipes(helper, "dusk_purpur", "enderscape:dusk_purpur_block", "enderscape:dusk_purpur_slab");
        helper.succeed();
    }

    @GameTest
    public void duskPurpurReferenceStatesAndData(GameTestHelper helper) {
        DirectFormTests.referenceStatesAndData(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurPlacement(GameTestHelper helper) {
        DirectFormTests.placement(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurEngraving(GameTestHelper helper) {
        DirectFormTests.engraving(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurWindowInteractions(GameTestHelper helper) {
        DirectFormTests.windowInteractions(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurStackedMasonry(GameTestHelper helper) {
        DirectFormTests.stackedMasonry(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurCompleteStateAndShapeParity(GameTestHelper helper) {
        MasonryTests.completeStateAndShapeParity(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurWetAndDryPlacement(GameTestHelper helper) {
        MasonryTests.wetAndDryPlacement(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurColumnAndMouldingChanges(GameTestHelper helper) {
        MasonryTests.columnAndMouldingChanges(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurFenceAndFrameChanges(GameTestHelper helper) {
        MasonryTests.fenceAndFrameChanges(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurUrnSoilAndPlant(GameTestHelper helper) {
        MasonryTests.urnSoilAndPlant(helper, "dusk_purpur");
    }

    @GameTest
    public void duskPurpurSupportsAndShutter(GameTestHelper helper) {
        var registry = BuiltInRegistries.BLOCK;
        StoneTests.supportsAndShutterInteraction(helper,
                registry.getValue(Identifier.parse("architectural_material_closure:dusk_purpur_parapet")),
                registry.getValue(Identifier.parse("architectural_material_closure:dusk_purpur_louvered_shutter")));
    }

    @GameTest
    public void duskPurpurButtonPulse(GameTestHelper helper) {
        DirectFormTests.buttonPulse(helper, "dusk_purpur");
    }

    @GameTest(maxTicks = 40)
    public void duskPurpurPressurePlateSensitivity(GameTestHelper helper) {
        DirectFormTests.pressurePlateSensitivity(helper, "dusk_purpur");
    }
}
