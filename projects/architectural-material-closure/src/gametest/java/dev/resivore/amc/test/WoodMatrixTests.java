package dev.resivore.amc.test;

import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.WindowBarred;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.util.*;

public final class WoodMatrixTests {
    private static final BlockPos POS = new BlockPos(1, 2, 1);
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("architectural_material_closure", name); }
    private static boolean present(String wood) {
        return wood.equals("bamboo") || FabricLoader.getInstance().isModLoaded(wood.equals("mossy_oak") ? "ribbits" : "enderscape");
    }

    static void registry(GameTestHelper h, Set<Identifier> items) {
        for (String wood : List.of("bamboo", "veiled", "celestial", "murublight", "mossy_oak")) {
            for (String prefix : List.of(wood, "stripped_" + wood, wood + "_plank")) {
                for (String suffix : List.of("window", "window2", "four_window", "pane_window")) {
                    var key = id(prefix + "_" + suffix);
                    h.assertTrue(BuiltInRegistries.ITEM.containsKey(key) == present(wood), "Provider gating " + key);
                    if (present(wood)) h.assertTrue(items.add(key), "Duplicate " + key);
                    boolean recipe = h.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, key)).isPresent();
                    h.assertTrue(recipe == (present(wood) && suffix.equals("window")), "Parent recipe " + key);
                }
            }
        }
        if (present("mossy_oak")) for (String form : List.of("sign", "hanging_sign", "shelf", "lantern"))
            h.assertTrue(items.add(id("mossy_oak_" + form)), "Duplicate " + form);
    }

    @GameTest public void windowStatesAndPlacement(GameTestHelper h) {
        for (String wood : List.of("bamboo", "veiled", "celestial", "murublight", "mossy_oak")) {
            if (!present(wood)) continue;
            for (String prefix : List.of(wood, "stripped_" + wood, wood + "_plank")) {
                for (String form : List.of("window", "window2", "four_window", "pane_window")) {
                    String oak = prefix.startsWith("stripped_") ? (form.equals("pane_window") ? "stripped_oak" : "stripped_oak_log")
                            : prefix.endsWith("_plank") ? "oak_plank" : "oak";
                    var source = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("mcwwindows:" + oak + "_" + form)).orElseThrow();
                    var block = BuiltInRegistries.BLOCK.getOptional(id(prefix + "_" + form)).orElseThrow();
                    BambooTests.checkForm(h, source, block, prefix + "_" + form, false);
                    h.setBlock(POS, block);
                    if (form.equals("window")) {
                        h.setBlock(POS.above(), block);
                        h.assertBlockProperty(POS, ConnectedWindow.PART, ConnectedWindow.ConnectionStatus.BOTTOM);
                        h.setBlock(POS.above(), Blocks.AIR);
                    } else {
                        h.useBlock(POS);
                        h.assertTrue(h.getBlockState(POS).getValue(WindowBarred.WINDOWSTATE) != WindowBarred.WindowState.CLOSED, "Opens " + block);
                        h.useBlock(POS);
                        h.assertBlockProperty(POS, WindowBarred.WINDOWSTATE, WindowBarred.WindowState.CLOSED);
                    }
                    h.setBlock(POS, Blocks.AIR);
                }
            }
        }
        h.succeed();
    }

    @GameTest public void nativeSignPlacements(GameTestHelper h) {
        if (!present("mossy_oak")) { h.succeed(); return; }
        signPlacements(h, "architectural_material_closure:mossy_oak_");
        h.succeed();
    }

    static void signPlacements(GameTestHelper h, String prefix) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (String form : List.of("sign", "hanging_sign")) {
            for (Direction face : List.of(form.equals("sign") ? Direction.UP : Direction.DOWN, Direction.NORTH)) {
                var support = POS.relative(face.getOpposite());
                h.setBlock(support, Blocks.STONE);
                player.setYRot(0);
                player.setXRot(face == Direction.UP ? 80 : face == Direction.DOWN ? -80 : 0);
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(prefix + form)).orElseThrow();
                var stack = new ItemStack(item, 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                h.placeAt(player, stack, support, face);
                var block = h.getBlockState(POS).getBlock();
                String placed = (face == Direction.NORTH ? "wall_" : "") + form;
                h.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(Identifier.parse(prefix + placed))
                        && stack.getCount() == 1, "Sign placement " + placed + " got " + block);
                h.setBlock(POS, Blocks.AIR);
                h.setBlock(support, Blocks.AIR);
            }
        }
    }

    @GameTest public void mossyFixtures(GameTestHelper h) {
        if (!present("mossy_oak")) { h.succeed(); return; }
        fixtures(h, "architectural_material_closure:mossy_oak_", "architectural_material_closure:mossy_oak_");
        h.succeed();
    }

    static void fixtures(GameTestHelper h, String prefix, String lantern) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        for (String form : List.of("sign", "wall_sign", "hanging_sign", "wall_hanging_sign", "shelf", "lantern")) {
            var own = BuiltInRegistries.BLOCK.getOptional(Identifier.parse((form.equals("lantern") ? lantern : prefix) + form)).orElseThrow();
            var ref = BuiltInRegistries.BLOCK.getOptional(Identifier.parse((form.equals("lantern") ? "bbb" : "minecraft") + ":oak_" + form)).orElseThrow();
            h.assertTrue(own.getClass() == ref.getClass() && own.asItem().getClass() == ref.asItem().getClass()
                    && own.asItem().getDefaultMaxStackSize() == ref.asItem().getDefaultMaxStackSize(), "Native fixture " + form);
            h.setBlock(POS.below(), Blocks.STONE);
            h.setBlock(POS.above(), Blocks.STONE);
            h.setBlock(POS.south(), Blocks.STONE);
            h.setBlock(POS, own);
            var entity = level.getBlockEntity(pos);
            if (entity instanceof SignBlockEntity sign) {
                sign.setText(sign.getText(true).setMessage(1, Component.literal("Front")), true);
                sign.setText(sign.getText(false).setMessage(2, Component.literal("Back")), false);
                sign.setWaxed(true);
                var copy = (SignBlockEntity) BlockEntity.loadStatic(pos, h.getBlockState(POS),
                        sign.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                h.assertTrue(copy != null && copy.isWaxed() && copy.getText(true).getMessage(1, false).getString().equals("Front")
                        && copy.getText(false).getMessage(2, false).getString().equals("Back"), "Sign persistence " + form);
            } else if (entity instanceof ShelfBlockEntity shelf) {
                var emeralds = new ItemStack(Items.EMERALD, 7);
                player.setItemInHand(InteractionHand.MAIN_HAND, emeralds);
                var facing = h.getBlockState(POS).getValue(ShelfBlock.FACING);
                var shelfHit = new BlockHitResult(Vec3.atCenterOf(pos), facing, pos, false);
                h.getBlockState(POS).useItemOn(emeralds, level, player, InteractionHand.MAIN_HAND, shelfHit);
                h.assertTrue(shelf.getItems().get(1).is(Items.EMERALD), "Shelf front interaction");
                var copy = (ShelfBlockEntity) BlockEntity.loadStatic(pos, h.getBlockState(POS),
                        shelf.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                h.assertTrue(copy != null && copy.getItems().get(1).is(Items.EMERALD) && copy.getItems().get(1).getCount() == 7, "Shelf contents");
                h.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
                h.assertBlockProperty(POS, ShelfBlock.POWERED, true);
                h.setBlock(POS.east(), Blocks.AIR);
            } else {
                h.assertTrue(form.equals("lantern") && h.getBlockState(POS).getLightEmission() == 15, "Lantern light or missing entity " + form);
                player.setItemInHand(InteractionHand.MAIN_HAND, BBBContent.HAMMER.getDefaultInstance());
                for (boolean hanging : List.of(true, false)) {
                    UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, hit);
                    h.assertBlockProperty(POS, BlockStateProperties.HANGING, hanging);
                }
            }
            var drops = Block.getDrops(h.getBlockState(POS), level, pos, entity);
            h.assertTrue(drops.stream().anyMatch(s -> s.is(own.asItem())), "Drop " + form);
            h.setBlock(POS, Blocks.AIR);
        }
    }
}

