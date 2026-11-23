package dev.resivore.amc.test;

import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MossyOakTests {
    private static final List<String> FORMS = List.of("frame","lattice","trim","balustrade","support","pallet");
    private static final BlockPos CENTER = new BlockPos(1,1,1);
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    private static Identifier id(String form, boolean reference) {
        return Identifier.fromNamespaceAndPath(reference ? "bbb" : "architectural_material_closure",
                (reference ? "oak_" : "mossy_oak_") + form);
    }
    private static Block block(String form, boolean reference) {
        return BuiltInRegistries.BLOCK.getOptional(id(form, reference)).orElseThrow();
    }

    private static void sameState(GameTestHelper helper, BlockState own, BlockState ref) {
        helper.assertTrue(own != null && ref != null && own.getValues().toList().equals(ref.getValues().toList()),
                "State mismatch: " + own + " vs " + ref);
    }

    @GameTest
    public void completeStateAndShapeParity(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTER);
        for (String form : FORMS) {
            var own = block(form, false);
            var ref = block(form, true);
            helper.assertTrue(own != ref && own.getClass() == ref.getClass(), form + " implementation");
            var item = BuiltInRegistries.ITEM.getValue(id(form, false));
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == own
                    && item == own.asItem() && item.getClass() == ref.asItem().getClass(), form + " item class/binding");
            var other = Identifier.fromNamespaceAndPath("bbb", "mossy_oak_" + form);
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(other) && !BuiltInRegistries.ITEM.containsKey(other),
                    form + " already registered by BBB");
            helper.assertTrue(own.builtInRegistryHolder().tags().collect(Collectors.toSet()).equals(
                    ref.builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " loaded block tags");
            helper.assertTrue(item.builtInRegistryHolder().tags().collect(Collectors.toSet()).equals(
                    ref.asItem().builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " loaded item tags");
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,
                    id(form, false))).isPresent(), form + " crafting recipe");
            var drops = Block.getDrops(own.defaultBlockState(), level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    form + " self-drop");
            sameState(helper, own.defaultBlockState(), ref.defaultBlockState());
            var states = ref.getStateDefinition().getPossibleStates();
            helper.assertTrue(states.size() == own.getStateDefinition().getPossibleStates().size(), form + " state count");
            for (var state : states) {
                var tested = own.withPropertiesOf(state);
                sameState(helper, tested, state);
                helper.assertTrue(tested.getShape(level, pos).toAabbs().equals(state.getShape(level, pos).toAabbs())
                        && tested.getCollisionShape(level, pos).toAabbs().equals(state.getCollisionShape(level, pos).toAabbs())
                        && tested.getFluidState() == state.getFluidState()
                        && tested.getPistonPushReaction() == state.getPistonPushReaction(), "Geometry/fluid/piston " + tested);
                for (var rotation : Rotation.values()) sameState(helper, tested.rotate(rotation), state.rotate(rotation));
                for (var mirror : Mirror.values()) sameState(helper, tested.mirror(mirror), state.mirror(mirror));
            }
        }
        helper.succeed();
    }

    @GameTest
    public void wetAndDryPlacement(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (boolean wet : List.of(false, true)) {
            helper.setBlock(CENTER, wet ? Blocks.WATER : Blocks.AIR);
            for (var face : Direction.values()) {
                var support = CENTER.relative(face.getOpposite());
                helper.setBlock(support, Blocks.STONE);
                var pos = helper.absolutePos(support);
                for (int yaw : List.of(0, 90, 180, 270)) {
                    player.setYRot(yaw);
                    for (double height : List.of(0.25, 0.75)) {
                        var location = Vec3.atCenterOf(pos).relative(face, 0.5)
                                .add(0, face.getAxis().isHorizontal() ? height - 0.5 : 0, 0);
                        var hit = new BlockHitResult(location, face, pos, false);
                        for (String form : FORMS) {
                            var own = block(form, false);
                            var ref = block(form, true);
                            var actual = own.getStateForPlacement(new BlockPlaceContext(player, HAND,
                                    own.asItem().getDefaultInstance(), hit));
                            var expected = ref.getStateForPlacement(new BlockPlaceContext(player, HAND,
                                    ref.asItem().getDefaultInstance(), hit));
                            if (expected == null) {
                                helper.assertTrue(actual == null, form + " rejected placement");
                                continue;
                            }
                            sameState(helper, actual, expected);
                            if (actual.hasProperty(BlockStateProperties.WATERLOGGED)) helper.assertTrue(actual.getValue(BlockStateProperties.WATERLOGGED) == wet,
                                    form + " wet placement " + face);
                        }
                    }
                }
                helper.setBlock(support, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper) {
        var absolute = helper.absolutePos(CENTER);
        return new BlockHitResult(Vec3.atCenterOf(absolute).add(0, -0.4, 0), Direction.NORTH, absolute, false);
    }


    @GameTest
    public void sixCraftingGrids(GameTestHelper helper) {
        boolean timber = BuiltInRegistries.BLOCK.containsKey(Identifier.parse("mynx_trees:mossy_oak_log"));
        var keys = Map.of('P', "ribbits:mossy_oak_planks", 'S', "minecraft:stick",
                'L', "ribbits:mossy_oak_planks_slab", 'A', timber ? "mynx_trees:stripped_mossy_oak_log" : "minecraft:stripped_oak_log", 'M', "minecraft:moss_block");
        record Grid(String form, int count, String... rows) {}
        var grids = List.of(new Grid("frame",12,"LLL","S S","LLL"), new Grid("lattice",6,"SPS","PSP","SPS"),
                new Grid("trim",3,"P","P","P"), new Grid("balustrade",6,timber ? new String[]{"A","A","A"} : new String[]{"A ","AM","A "}),
                new Grid("support",4,"PP","PS"), new Grid("pallet",8,"SSS","PPP"));
        for (var grid : grids) {
            var stacks = new java.util.ArrayList<ItemStack>();
            for (String row : grid.rows()) {
                for (char c : row.toCharArray()) {
                    stacks.add(c == ' ' ? ItemStack.EMPTY :
                            BuiltInRegistries.ITEM.getOptional(Identifier.parse(keys.get(c))).orElseThrow().getDefaultInstance());
                }
            }
            var level = helper.getLevel();
            var input = CraftingInput.of(grid.rows()[0].length(), grid.rows().length, stacks);
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
            var result = recipe.value().assemble(input);
            helper.assertTrue(recipe.id().identifier().equals(id(grid.form(), false)) &&
                    result.is(block(grid.form(), false).asItem()) && result.getCount() == grid.count(),
                    grid.form() + " selected recipe " + recipe.id());
        }
        helper.succeed();
    }

    @GameTest
    public void connectedAndInteractiveStates(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (String form : FORMS) {
            var expected = new java.util.ArrayList<BlockState>();
            for (boolean reference : List.of(true, false)) {
                var block = block(form, reference);
                var observed = new java.util.ArrayList<BlockState>();
                helper.setBlock(CENTER, block);
                for (Direction direction : Direction.values()) {
                    var adjacent = CENTER.relative(direction);
                    helper.setBlock(adjacent, block);
                    observed.add(helper.getBlockState(CENTER));
                    helper.setBlock(adjacent, Blocks.AIR);
                    observed.add(helper.getBlockState(CENTER));
                }
                if (form.equals("lattice")) {
                    for (var item : List.of(Items.VINE, Items.SHEARS, Items.GLOW_BERRIES, Items.BONE_MEAL)) {
                        var stack = item.getDefaultInstance();
                        player.setItemInHand(HAND, stack);
                        helper.getBlockState(CENTER).useItemOn(stack, helper.getLevel(), player, HAND, hit(helper));
                        observed.add(helper.getBlockState(CENTER));
                    }
                } else if (!form.equals("trim")) {
                    player.setItemInHand(HAND, BBBContent.HAMMER.getDefaultInstance());
                    var pos = helper.absolutePos(CENTER);
                    for (double offset : List.of(-0.32, 0.12, 0.34)) {
                        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(offset, offset, offset),
                                Direction.NORTH, pos, false);
                        UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), HAND, hit);
                        observed.add(helper.getBlockState(CENTER));
                    }
                    if (form.equals("pallet")) {
                        player.setShiftKeyDown(true);
                        helper.getBlockState(CENTER).useWithoutItem(helper.getLevel(), player, hit(helper));
                        player.setShiftKeyDown(false);
                        observed.add(helper.getBlockState(CENTER));
                    }
                }
                if (reference) expected.addAll(observed);
                else for (int i = 0; i < expected.size(); i++) sameState(helper, observed.get(i), expected.get(i));
                helper.setBlock(CENTER, Blocks.AIR);
            }
        }
        helper.succeed();
    }
}
