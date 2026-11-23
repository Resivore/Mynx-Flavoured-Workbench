package com.fizzware.dramaticdoors.test;

import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallStableDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallSlidingDoorBlock;
import com.fizzware.dramaticdoors.fabric.FabricUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

final class AmcDoorChecks {
    static List<Identifier> doors() {
        var ids = new ArrayList<Identifier>();
        for (String wood : List.of("veiled", "celestial", "murublight", "mossy_oak")) {
            String glass = wood.equals("celestial") || wood.equals("murublight") ? "stem_glass" : "bark_glass";
            for (String design : List.of("bamboo", glass, "barn", "barn_glass", "beach", "classic", "cottage",
                    "four_panel", "glass", "japanese", "japanese2", "modern", "mystic", "nether", "paper", "stable",
                    "stable_head", "swamp", "tropical", "waffle", "western", "whispering")) {
                ids.add(Identifier.parse("dramaticdoors:tall_macaw_" + wood + "_" + design + "_door"));
            }
        }
        return ids;
    }

    static void check(GameTestHelper test) {
        var loader = FabricLoader.getInstance();
        var level = test.getLevel();
        var visible = new HashSet<Identifier>();
        FabricUtils.addMacawTabEntries((stack, visibility) -> visible.add(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        int count = 0;
        for (var id : doors()) {
            String path = id.getPath().substring("tall_macaw_".length());
            var key = Identifier.parse("architectural_material_closure:" + path);
            boolean expected = loader.isModLoaded("architectural_material_closure") && loader.isModLoaded("mcwdoors")
                    && loader.isModLoaded(path.startsWith("mossy_oak_") ? "ribbits" : "enderscape");
            var source = BuiltInRegistries.BLOCK.getOptional(key);
            var block = BuiltInRegistries.BLOCK.getOptional(id);
            var item = BuiltInRegistries.ITEM.getOptional(id);
            var recipeKey = ResourceKey.create(Registries.RECIPE, id);
            var recipe = level.recipeAccess().byKey(recipeKey);
            var unlock = level.getServer().getAdvancements().get(id.withPrefix("recipes/redstone/"));
            test.assertTrue(source.isPresent() == expected && block.isPresent() == expected && item.isPresent() == expected
                    && recipe.isPresent() == expected && (unlock != null) == expected && visible.contains(id) == expected,
                    "AMC source, tall block/item, recipe, reward and creative tab " + id);
            test.assertTrue(BuiltInRegistries.ITEM.getOptional(Identifier.parse("dramaticdoors:short_macaw_" + path)).isEmpty(),
                    "Tall-only AMC integration " + id);
            if (!expected) continue;
            count++;
            var src = (DoorBlock) source.orElseThrow();
            var door = (TallDoorBlock) block.orElseThrow();
            Class<?> kind = path.contains("_japanese") ? TallSlidingDoorBlock.class :
                    path.contains("_stable") ? TallStableDoorBlock.class : TallDoorBlock.class;
            test.assertTrue(((BlockItem) item.orElseThrow()).getBlock() == door && door.getClass() == kind
                    && door.type() == src.type() && door.defaultMapColor() == src.defaultMapColor()
                    && door.defaultBlockState().getSoundType() == src.defaultBlockState().getSoundType()
                    && door.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) == src.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO),
                    "Source properties and tall class " + id);
            if (kind != TallDoorBlock.class) {
                String design = path.substring(path.indexOf(kind == TallSlidingDoorBlock.class ? "_japanese" : "_stable"));
                var reference = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("dramaticdoors:tall_macaw_oak" + design)).orElseThrow();
                for (var state : door.getStateDefinition().getPossibleStates()) {
                    var expectedState = reference.withPropertiesOf(state);
                    test.assertTrue(state.getCollisionShape(level, BlockPos.ZERO, CollisionContext.empty()).bounds()
                            .equals(expectedState.getCollisionShape(level, BlockPos.ZERO, CollisionContext.empty()).bounds()),
                            "Macaw special collision for " + id);
                }
            }
            var woodenBlock = TagKey.create(Registries.BLOCK, Identifier.parse("dramaticdoors:tall_wooden_doors"));
            var macawItem = TagKey.create(Registries.ITEM, Identifier.parse("dramaticdoors:categories/tall_macaw_doors"));
            test.assertTrue(door.defaultBlockState().is(woodenBlock) && item.orElseThrow().getDefaultInstance().is(macawItem),
                    "Effective tall wood and Macaw tags " + id);
            var ingredient = src.asItem().getDefaultInstance();
            var grid = CraftingInput.of(1, 3, List.of(ingredient, ingredient, ingredient));
            var shaped = (ShapedRecipe) recipe.orElseThrow().value();
            var vanilla = net.minecraft.world.item.Items.OAK_DOOR.getDefaultInstance();
            test.assertTrue(!shaped.matches(CraftingInput.of(1, 3, List.of(vanilla, vanilla, vanilla)), level),
                    "Recipe requires exact AMC source " + id);
            var out = shaped.assemble(grid);
            test.assertTrue(shaped.matches(grid, level) && out.is(item.orElseThrow()) && out.getCount() == 2
                    && unlock.value().rewards().recipes().equals(List.of(recipeKey)), "Exact AMC recipe and reward " + id);
        }
        com.fizzware.dramaticdoors.DramaticDoors.LOGGER.info("Verified {} AMC tall doors across 88 source IDs", count);
    }
}
