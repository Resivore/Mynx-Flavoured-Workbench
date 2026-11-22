package dev.resivore.amc.test;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DoorTests {
    private static final String AMC = "architectural_material_closure";
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final List<String> MATERIALS = List.of("veiled","celestial","murublight","mossy_oak");

    private static List<String> designs(String wood) {
        String glass = wood.equals("veiled") || wood.equals("mossy_oak") ? "bark_glass" : "stem_glass";
        return List.of("bamboo", glass, "barn", "barn_glass", "beach", "classic", "cottage",
                "four_panel", "glass", "japanese", "japanese2", "modern", "mystic", "nether", "paper",
                "stable", "stable_head", "swamp", "tropical", "waffle", "western", "whispering");
    }

    private static Identifier id(String ns, String wood, String design) {
        return Identifier.fromNamespaceAndPath(ns, wood + "_" + design + "_door");
    }

    static void checkRegistry(GameTestHelper h, Set<Identifier> expected, boolean end, boolean ribbits) {
        h.assertTrue(FabricLoader.getInstance().isModLoaded("mcwdoors") == Boolean.getBoolean("amc.test.doors"), "Macaw provider configuration");
        for (var wood : MATERIALS) {
            boolean enabled = FabricLoader.getInstance().isModLoaded("mcwdoors") && (wood.equals("mossy_oak") ? ribbits : end);
            for (var design : designs(wood)) {
                var key = id(AMC,wood,design);
                if (enabled) h.assertTrue(expected.add(key), "Duplicate " + key);
                h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,key)).isPresent() == enabled,
                        "Conditional recipe " + key);
                for (var ns : List.of("mcwdoors","enderscape","ribbits"))
                    h.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(ns,wood,design)), "Provider already owns " + key);
            }
        }
    }

    @GameTest public void veiled(GameTestHelper h) { check(h,"veiled"); }
    @GameTest public void celestial(GameTestHelper h) { check(h,"celestial"); }
    @GameTest public void murublight(GameTestHelper h) { check(h,"murublight"); }
    @GameTest public void mossyOak(GameTestHelper h) { check(h,"mossy_oak"); }

    private static void check(GameTestHelper h, String wood) {
        String ns = wood.equals("mossy_oak") ? "ribbits" : "enderscape";
        if (FabricLoader.getInstance().isModLoaded(ns) && FabricLoader.getInstance().isModLoaded("mcwdoors")) {
            for (String design : designs(wood)) {
                String ref = design.equals("classic") ? "spruce" : design.equals("stem_glass") ? "warped" : "oak";
                var src = id("mcwdoors",ref,design);
                var key = id(AMC,wood,design);
                var door = (DoorBlock) BuiltInRegistries.BLOCK.getOptional(key).orElseThrow();
                var source = (DoorBlock) BuiltInRegistries.BLOCK.getOptional(src).orElseThrow();
                h.assertTrue(door.getClass() == source.getClass() && door.type() == source.type()
                                && door.asItem().getClass() == source.asItem().getClass()
                                && ((BlockItem) door.asItem()).getBlock() == door, "Provider class/type/item " + key);
                var pos = h.absolutePos(CENTER);
                var level = h.getLevel();
                for (var state : source.getStateDefinition().getPossibleStates()) {
                    var actual = door.withPropertiesOf(state);
                    h.assertTrue(actual.getValues().toList().equals(state.getValues().toList())
                                    && actual.getShape(level,pos).toAabbs().equals(state.getShape(level,pos).toAabbs())
                                    && actual.getCollisionShape(level,pos).toAabbs().equals(state.getCollisionShape(level,pos).toAabbs())
                                    && actual.getFluidState() == state.getFluidState(), "State geometry " + key);
                }
                for (int yaw : List.of(0,90,180,270)) {
                    for (double offset : List.of(0.2,0.8)) {
                        var expected = exercise(h,source,yaw,offset);
                        var actual = exercise(h,door,yaw,offset);
                        for (int i = 0; i < expected.size(); i++)
                            h.assertTrue(actual.get(i).getValues().toList().equals(expected.get(i).getValues().toList()),
                                    "Placement/interaction " + key + " at step " + i);
                    }
                }
                grid(h,src,key,ns,wood,ref);
            }
        }
        h.succeed();
    }

    private static List<BlockState> exercise(GameTestHelper h, DoorBlock door, int yaw, double offset) {
        var level = h.getLevel();
        var base = h.absolutePos(CENTER);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.setYRot(yaw);
        h.setBlock(CENTER.below(), Blocks.STONE);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(base).add(offset,0,offset),Direction.UP,base.below(),false);
        var item = (BlockItem) door.asItem();
        h.assertTrue(item.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,item.getDefaultInstance(),hit))
                .consumesAction(), "Placement " + door);
        var trace = new ArrayList<BlockState>();
        for (int step = 0; step < 5; step++) {
            if (step == 1 || step == 2) {
                var pos = step == 1 ? base.above() : base;
                var click = new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
                level.getBlockState(pos).useWithoutItem(level,player,click);
            }
            if (step == 3) h.setBlock(CENTER.east(),Blocks.REDSTONE_BLOCK);
            if (step == 4) h.setBlock(CENTER.east(),Blocks.AIR);
            for (var half : DoubleBlockHalf.values()) {
                var pos = half == DoubleBlockHalf.LOWER ? base : base.above();
                var state = level.getBlockState(pos);
                h.assertTrue(state.is(door) && state.getValue(DoorBlock.HALF) == half, "Paired halves " + door);
                trace.add(state);
                if (step == 0) {
                    var drops = Block.getDrops(state,level,pos,null);
                    h.assertTrue(half == DoubleBlockHalf.UPPER ? drops.isEmpty() :
                            drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                            "Loot " + door + " " + half);
                }
            }
        }
        h.setBlock(CENTER,Blocks.AIR);
        return trace;
    }

    private static void grid(GameTestHelper h, Identifier src, Identifier key, String ns, String wood, String ref) {
        String path = "/data/mcwdoors/recipe/" + src.getPath() + ".json";
        try (var reader = new InputStreamReader(DoorTests.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            var recipe = JsonParser.parseReader(reader).getAsJsonObject();
            var pattern = recipe.getAsJsonArray("pattern");
            var stacks = new ArrayList<ItemStack>();
            for (var row : pattern) {
                for (char c : row.getAsString().toCharArray()) {
                    if (c == ' ') { stacks.add(ItemStack.EMPTY); continue; }
                    String ingredient = recipe.getAsJsonObject("key").get(String.valueOf(c)).getAsString();
                    if (ingredient.equals("minecraft:"+ref+"_planks")) ingredient = ns+":"+wood+"_planks";
                    else if (ingredient.equals("minecraft:"+ref+"_slab")) ingredient = ns+":"+wood+(ns.equals("ribbits")?"_planks_slab":"_slab");
                    else if (ingredient.equals("minecraft:"+ref+"_log")) ingredient = ns+":"+wood+(ns.equals("ribbits")?"_planks":"_log");
                    else if (ingredient.equals("minecraft:"+ref+"_stem")) ingredient = ns+":"+wood+"_stem";
                    else if (ingredient.equals("mcwdoors:"+ref+"_stable_door")) ingredient = AMC+":"+wood+"_stable_door";
                    stacks.add(BuiltInRegistries.ITEM.getOptional(Identifier.parse(ingredient)).orElseThrow().getDefaultInstance());
                }
            }
            var input = CraftingInput.of(pattern.get(0).getAsString().length(),pattern.size(),stacks);
            var selected = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
            var output = selected.value().assemble(input);
            h.assertTrue(selected.id().identifier().equals(key) && output.is(BuiltInRegistries.ITEM.getValue(key))
                    && output.getCount() == recipe.getAsJsonObject("result").get("count").getAsInt(), "Crafting " + key);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(path,ex);
        }
    }
}
