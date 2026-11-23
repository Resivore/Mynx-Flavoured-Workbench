package dev.resivore.amc.test;

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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BambooTests {
    private static final List<String> FORMS = List.of("ranch_trapdoor", "log_parapet", "plank_parapet",
            "blinds", "curtain_rod", "louvered_shutter");
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static Identifier id(String form) {
        return Identifier.parse("architectural_material_closure:bamboo_" + form);
    }

    private static Block bamboo(String form) {
        return BuiltInRegistries.BLOCK.getOptional(id(form)).orElseThrow();
    }

    static void checkRegistry(GameTestHelper h, Set<Identifier> all) {
        for (var form : FORMS) {
            var key = id(form);
            boolean enabled = !form.equals("ranch_trapdoor") || FabricLoader.getInstance().isModLoaded("mcwtrpdoors");
            if (enabled) h.assertTrue(all.add(key), "Duplicate " + key);
            h.assertTrue(h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,key)).isPresent()==enabled,
                    "Optional recipe " + key);
            for (String provider : List.of("minecraft","mcwwindows","mcwtrpdoors")) {
                var foreign = Identifier.fromNamespaceAndPath(provider,key.getPath());
                h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(foreign) && !BuiltInRegistries.ITEM.containsKey(foreign),
                        "Provider owns " + foreign);
            }
        }
    }

    @GameTest public void logParapetSupport(GameTestHelper h) {
        StoneTests.supportsAndShutterInteraction(h,bamboo("log_parapet"),bamboo("louvered_shutter"));
    }

    @GameTest public void plankParapetSupport(GameTestHelper h) {
        StoneTests.supportsAndShutterInteraction(h,bamboo("plank_parapet"),bamboo("louvered_shutter"));
    }

    @GameTest public void blindsCycle(GameTestHelper h) {
        checkBlinds(h,bamboo("blinds"));
        h.succeed();
    }

    static void checkBlinds(GameTestHelper h, Block actual) {
        var source = BuiltInRegistries.BLOCK.getValue(Identifier.parse("mcwwindows:oak_blinds"));
        var traces = new ArrayList<List<String>>();
        for (var block : List.of(source,actual)) {
            h.setBlock(POS,block);
            h.setBlock(POS.east(),block);
            var trace = new ArrayList<String>();
            for (int i=0; i<4; i++) {
                var state = h.getBlockState(POS);
                h.assertTrue(h.getBlockState(POS.east()).equals(state), "Joined blind interaction");
                trace.add(state.getValues().toList().toString());
                h.useBlock(POS);
            }
            h.assertTrue(Set.copyOf(trace).size()==3 && trace.getFirst().equals(trace.getLast()), "Open/closed/raised cycle");
            traces.add(trace);
        }
        h.assertTrue(traces.getFirst().equals(traces.getLast()), "Reference blind states");
    }

    @GameTest public void forms(GameTestHelper h) {
        for (var form : FORMS) {
            boolean ranch = form.equals("ranch_trapdoor");
            if (ranch && !FabricLoader.getInstance().isModLoaded("mcwtrpdoors")) continue;
            var source = BuiltInRegistries.BLOCK.getOptional(Identifier.parse((ranch?"mcwtrpdoors":"mcwwindows")+":oak_"+form)).orElseThrow();
            var block = bamboo(form);
            checkForm(h,source,block,form,ranch);
        }
        h.succeed();
    }

    static void checkForm(GameTestHelper h, Block source, Block block, String form, boolean ranch) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        h.assertTrue(block.getClass()==source.getClass() && block.asItem().getClass()==source.asItem().getClass()
                && ((BlockItem)block.asItem()).getBlock()==block, "Provider class/item " + form);
        for (var state : source.getStateDefinition().getPossibleStates()) {
            var copy = block.withPropertiesOf(state);
            h.assertTrue(copy.getValues().toList().equals(state.getValues().toList())
                    && copy.getShape(level,pos).toAabbs().equals(state.getShape(level,pos).toAabbs())
                    && copy.getCollisionShape(level,pos).toAabbs().equals(state.getCollisionShape(level,pos).toAabbs())
                    && state.getFluidState()==copy.getFluidState() && copy.getSoundType()==state.getSoundType()
                    && copy.getDestroySpeed(level,pos)==state.getDestroySpeed(level,pos), "State " + form);
            for (var rot : Rotation.values())
                h.assertTrue(copy.rotate(rot)==block.withPropertiesOf(state.rotate(rot)), "Rotation " + form);
        }
        var drops = Block.getDrops(block.defaultBlockState(),level,pos,null);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount()==1,"Drop "+form);
        h.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)==source.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE), "Axe tag " + form);
        for (var face : Direction.Plane.HORIZONTAL) {
            if (ranch) {
                for (double height : List.of(0.25,0.75)) for (boolean wet : List.of(false,true))
                    TrapdoorTests.exercise(h,(TrapDoorBlock)block,face,height,wet);
            } else {
                var player = h.makeMockPlayer(GameType.CREATIVE);
                player.setYRot(face.toYRot());
                h.setBlock(POS.below(),Blocks.STONE);
                for (double offset : List.of(0.2,0.8)) {
                    var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(offset,0,offset),Direction.UP,pos.below(),false);
                    var reference = source.getStateForPlacement(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                            source.asItem().getDefaultInstance(),hit));
                    var item = (BlockItem)block.asItem();
                    h.assertTrue(item.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,item.getDefaultInstance(),hit)).consumesAction()
                            && h.getBlockState(POS)==block.withPropertiesOf(reference),"Placement "+form);
                    h.setBlock(POS,Blocks.AIR);
                }
            }
        }
    }

    @GameTest public void recipes(GameTestHelper h) {
        if (FabricLoader.getInstance().isModLoaded("mcwtrpdoors")) grid(h,"ranch_trapdoor","BSBSSSBSB",3,2);
        grid(h,"log_parapet","SSSSBS",3,5);
        grid(h,"plank_parapet","SSSSPS",3,5);
        grid(h,"blinds","LLLSSSSSS",3,3);
        grid(h,"curtain_rod","SBS",3,5);
        grid(h,"louvered_shutter","LLL",1,3);
        h.succeed();
    }

    private static void grid(GameTestHelper h,String form,String slots,int width,int yield) {
        var stacks = new ArrayList<ItemStack>();
        for (char c : slots.toCharArray()) {
            String input = switch (c) {
                case 'S' -> "stick";
                case 'B' -> "bamboo_block";
                case 'P' -> "bamboo_planks";
                case 'L' -> "bamboo_slab";
                default -> throw new IllegalArgumentException();
            };
            stacks.add(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(input)).getDefaultInstance());
        }
        var grid = CraftingInput.of(width,slots.length()/width,stacks);
        var level = h.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
        var output = recipe.value().assemble(grid);
        h.assertTrue(recipe.id().identifier().equals(id(form)) && output.is(bamboo(form).asItem()) && output.getCount()==yield,
                "Selected recipe " + recipe.id() + " for " + form);
    }
}
