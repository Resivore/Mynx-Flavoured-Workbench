package dev.resivore.amc.test;

import com.google.gson.JsonParser;
import dev.resivore.amc.mixin.TrapdoorAccess;
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
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TrapdoorTests {
    private static final String AMC = "architectural_material_closure";
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final List<String> MATERIALS = List.of("veiled","celestial","murublight","mossy_oak");

    private static final List<String> DESIGNS = List.of("bamboo","bark","barn","barred","barrel","beach","blossom","classic","cottage",
                "four_panel","glass","mystic","paper","ranch","swamp","tropical","whispering");

    private static Identifier id(String ns, String wood, String design) {
        return Identifier.fromNamespaceAndPath(ns, wood + "_" + design + "_trapdoor");
    }

    static void checkRegistry(GameTestHelper h, Set<Identifier> expected, boolean end, boolean ribbits) {
        h.assertTrue(FabricLoader.getInstance().isModLoaded("mcwtrpdoors") == Boolean.getBoolean("amc.test.trapdoors"), "Macaw provider configuration");
        for (var wood : MATERIALS) {
            boolean enabled = FabricLoader.getInstance().isModLoaded("mcwtrpdoors") && (wood.equals("mossy_oak") ? ribbits : end);
            for (var design : DESIGNS) {
                var key = id(AMC,wood,design);
                if (enabled) h.assertTrue(expected.add(key), "Duplicate " + key);
                h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,key)).isPresent() == enabled,
                        "Conditional recipe " + key);
                for (var ns : List.of("mcwtrpdoors","enderscape","ribbits"))
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
        if (FabricLoader.getInstance().isModLoaded(ns) && FabricLoader.getInstance().isModLoaded("mcwtrpdoors")) {
            for (String design : DESIGNS) {
                String ref = design.equals("classic") ? "spruce" : ((design.equals("bark") || design.equals("ranch")) && (wood.equals("celestial") || wood.equals("murublight"))) ? "warped" : "oak";
                var src = id("mcwtrpdoors",ref,design);
                var key = id(AMC,wood,design);
                var door = (TrapDoorBlock) BuiltInRegistries.BLOCK.getOptional(key).orElseThrow();
                var source = (TrapDoorBlock) BuiltInRegistries.BLOCK.getOptional(src).orElseThrow();
                h.assertTrue(door.getClass() == source.getClass() && ((TrapdoorAccess)door).amc$type() == ((TrapdoorAccess)source).amc$type()
                                && door.asItem().getClass() == source.asItem().getClass()
                                && ((BlockItem) door.asItem()).getBlock() == door, "Provider class/type/item " + key);
                h.assertTrue(door.defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS)
                        && door.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                        && door.asItem().getDefaultInstance().is(ItemTags.WOODEN_TRAPDOORS), "Effective tags " + key);
                var pos = h.absolutePos(CENTER);
                var level = h.getLevel();
                for (var state : source.getStateDefinition().getPossibleStates()) {
                    var actual = door.withPropertiesOf(state);
                    h.assertTrue(actual.getValues().toList().equals(state.getValues().toList())
                                    && actual.getShape(level,pos).toAabbs().equals(state.getShape(level,pos).toAabbs())
                                    && actual.getCollisionShape(level,pos).toAabbs().equals(state.getCollisionShape(level,pos).toAabbs())
                                    && actual.getFluidState() == state.getFluidState()
                                    && actual.getSoundType() == state.getSoundType()
                                    && actual.getDestroySpeed(level,pos) == state.getDestroySpeed(level,pos), "State geometry/material " + key);
                    for (var rotation : net.minecraft.world.level.block.Rotation.values()) {
                        h.assertTrue(actual.rotate(rotation) == door.withPropertiesOf(state.rotate(rotation)), "Rotation " + key);
                    }
                    for (var mirror : net.minecraft.world.level.block.Mirror.values()) {
                        h.assertTrue(actual.mirror(mirror) == door.withPropertiesOf(state.mirror(mirror)), "Mirror " + key);
                    }
                }
                for (var face : Direction.Plane.HORIZONTAL) {
                    for (double height : List.of(0.25,0.75)) {
                        for (boolean wet : List.of(false,true)) {
                            exercise(h,door,face,height,wet);
                        }
                    }
                }
                grid(h,src,key,ns,wood,ref);
            }
        }
        h.succeed();
    }

    static void exercise(GameTestHelper h, TrapDoorBlock door, Direction face, double height, boolean wet) {
        var level = h.getLevel();
        var base = h.absolutePos(CENTER);
        var support = CENTER.relative(face.getOpposite());
        h.setBlock(support,Blocks.STONE);
        h.setBlock(CENTER,wet ? Blocks.WATER : Blocks.AIR);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(support)).relative(face,0.5).add(0,height-0.5,0),
                face,h.absolutePos(support),false);
        var item = (BlockItem)door.asItem();
        h.assertTrue(item.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,item.getDefaultInstance(),hit))
                .consumesAction(), "Placement " + door);
        for (int step = 0; step < 5; step++) {
            var click = new BlockHitResult(Vec3.atCenterOf(base),face,base,false);
            if (step == 1 || step == 2) level.getBlockState(base).useWithoutItem(level,player,click);
            if (step == 3) h.setBlock(CENTER.above(),Blocks.REDSTONE_BLOCK);
            if (step == 4) h.setBlock(CENTER.above(),Blocks.AIR);
            var state = level.getBlockState(base);
            h.assertTrue(state.is(door) && state.getValue(TrapDoorBlock.FACING) == face
                    && state.getValue(TrapDoorBlock.HALF) == (height > 0.5 ? Half.TOP : Half.BOTTOM)
                    && state.getValue(TrapDoorBlock.OPEN) == (step == 1 || step == 3)
                    && state.getValue(TrapDoorBlock.POWERED) == (step == 3)
                    && state.getValue(TrapDoorBlock.WATERLOGGED) == wet, "Interaction " + door + " " + step + " " + state);
            if (step == 1) {
                var drops = Block.getDrops(state,level,base,null);
                h.assertTrue(drops.size()==1 && drops.getFirst().is(item) && drops.getFirst().getCount()==1, "Loot " + door);
            }
        }
        h.setBlock(CENTER,Blocks.AIR);
        h.setBlock(support,Blocks.AIR);
    }

    private static void grid(GameTestHelper h, Identifier src, Identifier key, String ns, String wood, String ref) {
        String path = "/data/mcwtrpdoors/recipe/" + src.getPath() + ".json";
        try (var reader = new InputStreamReader(TrapdoorTests.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            var recipe = JsonParser.parseReader(reader).getAsJsonObject();
            var pattern = recipe.getAsJsonArray("pattern");
            var stacks = new ArrayList<ItemStack>();
            for (var row : pattern) {
                for (char c : row.getAsString().toCharArray()) {
                    if (c == ' ') { stacks.add(ItemStack.EMPTY); continue; }
                    String ingredient = recipe.getAsJsonObject("key").get(String.valueOf(c)).getAsString();
                    if (ingredient.equals("minecraft:"+ref+"_planks")) ingredient = ns+":"+wood+"_planks";
                    else if (ingredient.equals("minecraft:"+ref+"_slab")) ingredient = ns+":"+wood+(ns.equals("ribbits")?"_planks_slab":"_slab");
                    else if (ingredient.equals("minecraft:"+ref+"_log")) ingredient = ns.equals("ribbits") && BuiltInRegistries.BLOCK.containsKey(Identifier.parse("mynx_trees:mossy_oak_log")) ? "mynx_trees:mossy_oak_log" : ns+":"+wood+(ns.equals("ribbits")?(key.getPath().endsWith("_bark_trapdoor")?"_planks_slab":"_planks"):"_log");
                    else if (ingredient.equals("minecraft:"+ref+"_stem")) ingredient = ns+":"+wood+"_stem";
                    else if (ingredient.equals("mcwtrpdoors:"+ref+"_glass_trapdoor")) ingredient = AMC+":"+wood+"_glass_trapdoor";
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
