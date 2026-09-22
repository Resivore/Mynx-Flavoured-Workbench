package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes the effective client lookup seam that C95's registration-only tests did not cover. */
public final class SpruceTintClientGameTest implements FabricClientGameTest {
    private static final int FIRST_BIOME_COLOR = 0xFF28643A;
    private static final int SECOND_BIOME_COLOR = 0xFF91B85A;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Set<Block> targets = FoliageTintContract.spruceGeometryTargets();
            require(targets.size() == 8, "Expected exactly eight non-root spruce geometries");
            for (Block target : targets) {
                require(FoliageTintContract.isSpruceGeometry(target),
                        "Frozen final-lookup inventory is missing " + target);
            }
            Map<Item, List<Item>> previousShapes = copyShapes();
            Map<Item, Item> previousInverse = new HashMap<>(ShapeMap.inverseView());
            installSpruceShapeMap(previousShapes, previousInverse, targets);
            try {
                for (Block target : targets) {
                    require(ShapeMap.getParent(target.asItem()) == Blocks.SPRUCE_LEAVES.asItem(),
                            "Spruce geometry does not delegate to canonical spruce: " + target);
                }

                BlockColors colors = new BlockColors();
                colors.register(List.of(BlockTintSources.foliage()),
                        Blocks.SPRUCE_LEAVES, Blocks.OAK_LEAVES);
                colors.register(List.of(BlockTintSources.constant(FoliageTintContract.SPRUCE_FIXED_ARGB)),
                        targets.toArray(Block[]::new));
                colors.register(List.of(BlockTintSources.constant(FoliageTintContract.BIRCH_FIXED_ARGB)),
                        Blocks.BIRCH_LEAVES);

                BlockAndTintGetter firstBiome = tintContext(FIRST_BIOME_COLOR);
                BlockAndTintGetter secondBiome = tintContext(SECOND_BIOME_COLOR);
                for (Block target : targets) {
                    BlockTintSource source = colors.getTintSource(target.defaultBlockState(), 0);
                    require(source != null, "Missing effective spruce tint source for " + target);
                    require(colorInWorld(source, target, firstBiome)
                                    == FoliageTintContract.SPRUCE_FIXED_ARGB,
                            "Spruce geometry used the first biome color: " + target);
                    require(colorInWorld(source, target, secondBiome)
                                    == FoliageTintContract.SPRUCE_FIXED_ARGB,
                            "Spruce geometry used the second biome color: " + target);
                }

                assertBiomeControl(colors, Blocks.SPRUCE_LEAVES, firstBiome, secondBiome);
                assertBiomeControl(colors, Blocks.OAK_LEAVES, firstBiome, secondBiome);
                BlockTintSource birch = colors.getTintSource(Blocks.BIRCH_LEAVES.defaultBlockState(), 0);
                require(birch != null, "Missing birch control tint source");
                require(colorInWorld(birch, Blocks.BIRCH_LEAVES, firstBiome)
                                == FoliageTintContract.BIRCH_FIXED_ARGB,
                        "Birch fixed-color control changed");

                System.out.println(
                        "SPRUCE_FINAL_TINT_LOOKUP|targets=8|contexts=2|canonicalControls=2|result=PASS");
            } finally {
                ShapeMap.setShapeMaps(previousShapes, previousInverse);
            }
        });
    }

    private static Map<Item, List<Item>> copyShapes() {
        Map<Item, List<Item>> copy = new LinkedHashMap<>();
        ShapeMap.shapesView().forEach((parent, shapes) -> copy.put(parent, List.copyOf(shapes)));
        return copy;
    }

    private static void installSpruceShapeMap(Map<Item, List<Item>> previousShapes,
            Map<Item, Item> previousInverse, Set<Block> targets) {
        Map<Item, List<Item>> shapes = new LinkedHashMap<>(previousShapes);
        Map<Item, Item> inverse = new HashMap<>(previousInverse);
        Item root = Blocks.SPRUCE_LEAVES.asItem();
        List<Item> family = new ArrayList<>();
        family.add(root);
        for (Block target : targets) {
            family.add(target.asItem());
            inverse.put(target.asItem(), root);
        }
        shapes.put(root, List.copyOf(family));
        ShapeMap.setShapeMaps(shapes, inverse);
    }

    private static void assertBiomeControl(BlockColors colors, Block block,
            BlockAndTintGetter firstBiome, BlockAndTintGetter secondBiome) {
        BlockTintSource source = colors.getTintSource(block.defaultBlockState(), 0);
        require(source != null, "Missing biome control tint source for " + block);
        require(colorInWorld(source, block, firstBiome) == FIRST_BIOME_COLOR,
                "First biome control was overridden for " + block);
        require(colorInWorld(source, block, secondBiome) == SECOND_BIOME_COLOR,
                "Second biome control was overridden for " + block);
    }

    private static int colorInWorld(BlockTintSource source, Block block, BlockAndTintGetter context) {
        return source.colorInWorld(block.defaultBlockState(), context, BlockPos.ZERO);
    }

    private static BlockAndTintGetter tintContext(int color) {
        return (BlockAndTintGetter) Proxy.newProxyInstance(
                SpruceTintClientGameTest.class.getClassLoader(),
                new Class<?>[] {BlockAndTintGetter.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getBlockTint")) return color;
                    if (method.getName().equals("toString")) return "TintContext[" + color + "]";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    throw new AssertionError("Unexpected tint-context call: " + method);
                });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
