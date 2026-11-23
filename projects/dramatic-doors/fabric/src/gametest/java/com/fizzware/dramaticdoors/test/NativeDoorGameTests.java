package com.fizzware.dramaticdoors.test;

import com.fizzware.dramaticdoors.blocks.ShortDoorBlock;
import com.fizzware.dramaticdoors.blocks.TallDoorBlock;
import com.fizzware.dramaticdoors.fabric.FabricUtils;
import com.fizzware.dramaticdoors.state.properties.TripleBlockPart;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NativeDoorGameTests {
    @GameTest
    public void nativeDoorsAcrossTwoReloads(GameTestHelper helper) {
        String matrix = System.getProperty("dramaticdoors.test.matrix", "vanilla");
        boolean enderscape = matrix.equals("enderscape") || matrix.equals("combined");
        boolean ribbits = matrix.equals("ribbits") || matrix.equals("combined");
        helper.assertValueEqual(FabricUtils.INSTANCE.isModLoaded("enderscape"), enderscape, "Enderscape input");
        helper.assertValueEqual(FabricUtils.INSTANCE.isModLoaded("ribbits"), ribbits, "Ribbits input");
        var server = helper.getLevel().getServer();
        var recipes = server.getRecipeManager().getRecipes().stream().map(r -> r.id()).collect(java.util.stream.Collectors.toSet());
        var advancements = server.getAdvancements().getAllAdvancements().stream().map(a -> a.id()).collect(java.util.stream.Collectors.toSet());
        for (int pass = 0; pass <= 2; pass++) {
            if (pass > 0) {
                var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
                server.managedBlock(reload::isDone);
                reload.join();
                testReloadIds(helper, recipes, server.getRecipeManager().getRecipes().stream().map(r -> r.id()));
                testReloadIds(helper, advancements, server.getAdvancements().getAllAdvancements().stream().map(a -> a.id()));
            }
            AmcDoorChecks.check(helper);
            for (String wood : List.of("silver_birch", "wisteria")) {
                String source = "mynx_trees:" + wood + "_door";
                checkPair(helper, source, wood.equals("wisteria") ? "mynx_wisteria" : wood, BuiltInRegistries.ITEM.containsKey(Identifier.parse(source)));
            }
            checkPair(helper, "minecraft:oak_door", "oak", true);
            checkPair(helper, "minecraft:pale_oak_door", "pale_oak", true);
            checkPair(helper, "enderscape:celestial_door", "celestial", enderscape);
            checkPair(helper, "enderscape:murublight_door", "murushroom", enderscape);
            checkPair(helper, "enderscape:veiled_door", "veiled", enderscape);
            checkPair(helper, "ribbits:mossy_oak_door", "mossy_oak", ribbits);
        }
        helper.succeed();
    }

    private static <T> void testReloadIds(GameTestHelper helper, Set<T> initial, java.util.stream.Stream<T> reloaded) {
        helper.assertTrue(initial.equals(reloaded.collect(java.util.stream.Collectors.toSet())), "Stable IDs after data reload");
    }

    @GameTest
    public void placeOperateAndBreakNativeDoors(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos base = test.absolutePos(new BlockPos(1, 1, 1));
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var floor = base.below();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false);
        var ids = new java.util.ArrayList<>(AmcDoorChecks.doors());
        for (String material : List.of("pale_oak", "celestial", "murushroom", "veiled", "mossy_oak", "silver_birch", "mynx_wisteria")) {
            for (String height : List.of("short", "tall")) {
                ids.add(Identifier.parse("dramaticdoors:" + height + "_" + material + "_door"));
            }
        }
        for (var id : ids) {
            if (BuiltInRegistries.ITEM.getOptional(id).isEmpty()) continue;
            var item = (BlockItem) BuiltInRegistries.ITEM.getValue(id);
            int count = id.getPath().startsWith("short_") ? 1 : 3;
            // Break every segment independently after actual item placement and interaction.
            for (int segment = 0; segment < count; segment++) {
                level.setBlockAndUpdate(base, Blocks.WATER.defaultBlockState());
                var stack = item.getDefaultInstance();
                player.setYRot(90 * segment);
                item.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
                var first = level.getBlockState(base);
                test.assertTrue(first.is(item.getBlock()) && first.getValue(BlockStateProperties.WATERLOGGED), "Wet placement " + id);
                var target = base.above(segment);
                var use = new BlockHitResult(Vec3.atCenterOf(target), Direction.NORTH, target, false);
                level.getBlockState(target).useWithoutItem(level, player, use);
                for (int y = 0; y < count; y++) {
                    var state = level.getBlockState(base.above(y));
                    test.assertTrue(state.is(item.getBlock()) && state.getValue(BlockStateProperties.OPEN)
                            && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == first.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            && state.getValue(BlockStateProperties.DOOR_HINGE) == first.getValue(BlockStateProperties.DOOR_HINGE)
                            && state.isPathfindable(PathComputationType.LAND), "Hand, orientation and path " + id);
                    var saved = net.minecraft.nbt.NbtUtils.writeBlockState(state);
                    test.assertTrue(net.minecraft.nbt.NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                            saved) == state, "Block state save/load " + id);
                    if (count == 3) test.assertTrue(state.getValue(TallDoorBlock.THIRD) ==
                            List.of(TripleBlockPart.LOWER, TripleBlockPart.MIDDLE, TripleBlockPart.UPPER).get(y), "Third " + id);
                }
                level.setBlockAndUpdate(target.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                test.assertTrue(level.getBlockState(base).getValue(BlockStateProperties.POWERED), "Power " + id);
                level.setBlockAndUpdate(target.east(), Blocks.AIR.defaultBlockState());
                test.assertFalse(level.getBlockState(base).getValue(BlockStateProperties.OPEN), "Unpower " + id);
                level.destroyBlock(target, true);
                for (int y = 0; y < count; y++) {
                    test.assertFalse(level.getBlockState(base.above(y)).is(item.getBlock()), "Break cleanup " + id);
                }
                var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(base).inflate(3), drop -> drop.getItem().is(item));
                test.assertTrue(drops.stream().mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                        "One drop breaking segment " + segment + " of " + id);
                drops.forEach(drop -> drop.discard());
            }
        }
        test.succeed();
    }

    private static void checkPair(GameTestHelper test, String sourceId, String material, boolean present) {
        var level = test.getLevel();
        Set<Identifier> searchable = new HashSet<>();
        FabricUtils.addMainTabEntries((stack, visibility) -> {
            if (visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS) {
                searchable.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
        });
        for (String height : List.of("short", "tall")) {
            String name = height + "_" + material + "_door";
            Identifier id = Identifier.fromNamespaceAndPath("dramaticdoors", name);
            var block = BuiltInRegistries.BLOCK.getOptional(id);
            var item = BuiltInRegistries.ITEM.getOptional(id);
            var recipeKey = ResourceKey.create(Registries.RECIPE, id);
            var recipe = level.recipeAccess().byKey(recipeKey);
            var unlock = level.getServer().getAdvancements().get(id.withPrefix("recipes/redstone/"));
            test.assertTrue(block.isPresent() == present && item.isPresent() == present
                    && recipe.isPresent() == present && (unlock != null) == present
                    && searchable.contains(id) == present, "Registry, recipe, unlock and search for " + id);
            if (!present) continue;
            DoorBlock source = (DoorBlock) BuiltInRegistries.BLOCK.getOptional(Identifier.parse(sourceId)).orElseThrow();
            var door = block.orElseThrow();
            var doorItem = (BlockItem) item.orElseThrow();
            test.assertTrue(doorItem.getBlock() == door && (material.equals("oak") ||
                    doorItem.getDescriptionId().equals("block.dramaticdoors." + name)), "Item identity and name " + id);
            test.assertTrue(unlock.value().rewards().recipes().equals(List.of(recipeKey)), "Exact recipe reward " + id);
            var state = door.defaultBlockState();
            test.assertTrue(state.getSoundType() == source.defaultBlockState().getSoundType()
                    && state.getDestroySpeed(level, BlockPos.ZERO) == source.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO)
                    && door.defaultMapColor() == source.defaultMapColor(), "Copied sound, strength, map color " + id);
            ItemStack input = source.asItem().getDefaultInstance();
            ItemStack result;
            if (height.equals("tall")) {
                test.assertTrue(((TallDoorBlock) door).type() == source.type(), "Tall block set " + id);
                var shaped = (ShapedRecipe) recipe.orElseThrow().value();
                var grid = CraftingInput.of(1, 3, List.of(input, input, input));
                test.assertTrue(shaped.matches(grid, level), "Three source doors vertically " + id);
                result = shaped.assemble(grid);
                for (var third : TripleBlockPart.values()) {
                    var drops = Block.getDrops(state.setValue(TallDoorBlock.THIRD, third), level, BlockPos.ZERO, null);
                    if (third == TripleBlockPart.LOWER) {
                        test.assertTrue(drops.size() == 1 && drops.getFirst().is(doorItem) && drops.getFirst().getCount() == 1, "Lower drop " + id);
                    } else test.assertTrue(drops.isEmpty(), "No duplicate drop " + third + " " + id);
                }
            } else {
                test.assertTrue(((ShortDoorBlock) door).type() == source.type(), "Short block set " + id);
                var cut = (StonecutterRecipe) recipe.orElseThrow().value();
                var single = new SingleRecipeInput(input);
                test.assertTrue(cut.matches(single, level), "One source door in stonecutter " + id);
                result = cut.assemble(single);
                var drops = Block.getDrops(state, level, BlockPos.ZERO, null);
                test.assertTrue(drops.size() == 1 && drops.getFirst().is(doorItem) && drops.getFirst().getCount() == 1, "Short drop " + id);
            }
            test.assertTrue(result.is(doorItem) && result.getCount() == 2, "Two derivative outputs " + id);
            if (material.equals("mossy_oak")) {
                var fire = FlammableBlockRegistry.getDefaultInstance().get(door);
                test.assertTrue(fire.equals(FlammableBlockRegistry.getDefaultInstance().get(source))
                        && fire.getIgniteOdds() == 5 && fire.getBurnOdds() == 20, "Ribbits fire odds " + id);
            }
        }
    }
}
