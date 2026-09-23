package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Controlled final-lookup and native resource ownership regression for the restored C94 path. */
public final class SpruceRollbackClientGameTest implements FabricClientGameTest {
    private static final int FIRST_BIOME_COLOR = 0xFF28643A;
    private static final int SECOND_BIOME_COLOR = 0xFF91B85A;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            assertNoNativeSpruceShadow();
            Set<Block> targets = derivedSpruceGeometries();
            Map<Item, List<Item>> previousShapes = copyShapes();
            Map<Item, Item> previousInverse = new HashMap<>(ShapeMap.inverseView());
            installSpruceShapeMap(previousShapes, previousInverse, targets);
            try {
                BlockColors colors = new BlockColors();
                colors.register(List.of(BlockTintSources.foliage()),
                        Blocks.SPRUCE_LEAVES, Blocks.OAK_LEAVES);
                // Direct registrations are deliberately different from the canonical source.
                // CNM's C94 parent lookup must win without a BGE interception.
                colors.register(List.of(BlockTintSources.constant(0xFF619961)),
                        targets.toArray(Block[]::new));

                BlockAndTintGetter firstBiome = tintContext(FIRST_BIOME_COLOR);
                BlockAndTintGetter secondBiome = tintContext(SECOND_BIOME_COLOR);
                for (Block target : targets) {
                    require(ShapeMap.getParent(target.asItem()) == Blocks.SPRUCE_LEAVES.asItem(),
                            "Spruce geometry lost its canonical parent: " + target);
                    assertBiomeTint(colors, target, firstBiome, secondBiome);
                }
                assertBiomeTint(colors, Blocks.SPRUCE_LEAVES, firstBiome, secondBiome);
                assertBiomeTint(colors, Blocks.OAK_LEAVES, firstBiome, secondBiome);
                System.out.println("SPRUCE_C94_LOOKUP|derived=" + targets.size()
                        + "|contexts=2|nativeShadows=0|result=PASS");
            } finally {
                ShapeMap.setShapeMaps(previousShapes, previousInverse);
            }
        });
    }

    private static Set<Block> derivedSpruceGeometries() {
        NibaruMaterialProfile spruce = NibaruMaterialProfiles.fromBlock(Blocks.SPRUCE_LEAVES)
                .orElseThrow();
        Set<Block> targets = new LinkedHashSet<>();
        Set<BgeMaterialBindings.Role> roles = new LinkedHashSet<>();
        for (BgeMaterialBindings.Binding binding : BgeMaterialBindings.all()) {
            if (binding.materialProfile().orElse(null) != spruce
                    || binding.ownership() != BgeMaterialBindings.Ownership.PRIMARY) continue;
            require(roles.add(binding.role()), "Duplicate spruce role " + binding.role());
            if (binding.role() != BgeMaterialBindings.Role.CANONICAL_BLOCK) {
                require(targets.add(binding.physicalBlock()),
                        "Duplicate spruce geometry " + binding.physicalBlock());
            }
        }
        require(roles.equals(Set.of(BgeMaterialBindings.Role.values()))
                        && targets.size() == 8 && !targets.contains(Blocks.SPRUCE_LEAVES),
                "Expected the canonical spruce block and exactly eight derived geometries: " + roles);
        return targets;
    }

    private static void assertNoNativeSpruceShadow() {
        var bge = FabricLoader.getInstance().getModContainer("cnm_terrain_slabs_compat")
                .orElseThrow();
        for (String path : List.of("blockstates/spruce_leaves.json",
                "models/block/spruce_leaves.json", "textures/block/spruce_leaves.png")) {
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
            require(bge.findPath("assets/minecraft/" + path).isEmpty(),
                    "BGE packages a native spruce asset: " + path);
            require(ClutterNoMore.RESOURCES.getResource(PackType.CLIENT_RESOURCES, id) == null,
                    "Generated client resources shadow native spruce: " + id);
        }
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

    private static void assertBiomeTint(BlockColors colors, Block block,
            BlockAndTintGetter firstBiome, BlockAndTintGetter secondBiome) {
        BlockTintSource source = colors.getTintSource(block.defaultBlockState(), 0);
        require(source != null, "Missing effective tint source for " + block);
        require(source.colorInWorld(block.defaultBlockState(), firstBiome, BlockPos.ZERO)
                        == FIRST_BIOME_COLOR,
                "First biome tint was overridden for " + block);
        require(source.colorInWorld(block.defaultBlockState(), secondBiome, BlockPos.ZERO)
                        == SECOND_BIOME_COLOR,
                "Second biome tint was overridden for " + block);
    }

    private static BlockAndTintGetter tintContext(int color) {
        return (BlockAndTintGetter) Proxy.newProxyInstance(
                SpruceRollbackClientGameTest.class.getClassLoader(),
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
