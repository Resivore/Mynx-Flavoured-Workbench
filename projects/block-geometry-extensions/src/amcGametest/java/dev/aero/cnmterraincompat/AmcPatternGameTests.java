package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.client.CatalogItemGeneratedResources;
import dev.aero.cnmterraincompat.client.ExternalMaterialGeneratedResources;
import dev.aero.cnmterraincompat.client.LayerGeneratedResources;
import dev.aero.cnmterraincompat.client.QuarterGeometryGeneratedResources;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class AmcPatternGameTests {
    private static final String AMC = "architectural_material_closure";
    private static final List<String> END_STONES = List.of("veradite", "mirestone", "kurodite", "dusk_purpur", "shadoline_block");
    private static final List<String> ROLES = List.of("block", "slab", "stairs", "wall",
            "vertical_slab", "step", "corner", "quarter_column", "layer");

    private static String materialName(Identifier id) {
        return id.getPath().replaceFirst("_(running_bond|flagstone|windmill_weave|crystal_floor)$", "");
    }

    @GameTest
    public void productionPatternFamilies(GameTestHelper helper) throws Exception {
        var loader = FabricLoader.getInstance();
        String version = System.getProperty("bge.amcVersion");
        boolean present = !version.equals("none");
        boolean enderscape = Boolean.getBoolean("bge.withEnderscape");
        int canary = present ? Integer.parseInt(version) : 0;
        boolean patterns = enderscape && canary >= 24;
        boolean purpurAndResin = canary >= 26;
        helper.assertTrue(loader.isModLoaded(AMC) == present && loader.isModLoaded("enderscape") == enderscape,
                "Unexpected optional-provider configuration");
        if (present) {
            var hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(System.getProperty("bge.amcJar"))));
            String expectedHash = switch (version) {
                case "23" -> "8e850efa8d6e8f17e8bfa0b2da7058a7b1b0cd91e4c42e133fabedf3062589f5";
                case "24" -> "ddafc326d6e41e98170ad5c059a14774858fb1a6bc28b902d95e4b64206d6018";
                case "25" -> "dd4f53d297c51c74436b0f240d12943b9de91749669ec7cc0e48107d212e8f81";
                case "26" -> "06eed8311abd74bc2cc9e522d4c7667ebaca92539ff1289b3f290d1efc326456";
                case "27" -> "4eaaebe4de7bb9927c474dff1821863a7bfae931610c11787fac7926fd4e10ce";
                case "28" -> "c90194fb12306d1c47b22023eab23e62869f8310a56ab2c322c8f635bbce7fa9";
                default -> throw new IllegalArgumentException(version);
            };
            helper.assertTrue(HexFormat.of().formatHex(hash).equals(expectedHash), "Unexpected AMC artifact");
        }
        var families = ExternalMaterialFamilies.all().stream().filter(b -> b.spec().provider().equals(AMC)).toList();
        int expectedCount = (present ? 32 : 0) + (patterns ? 12 : 0)
                + (purpurAndResin ? 8 : 0) + (purpurAndResin && patterns ? 4 : 0)
                + (canary >= 28 ? 4 : 0) + (canary >= 28 && enderscape ? 4 : 0);
        helper.assertTrue(families.size() == expectedCount, "AMC family count");
        for (var spec : ExternalMaterialCatalog.specs()) {
            if (!spec.provider().equals(AMC)) continue;
            boolean expected = switch (materialName(spec.id())) {
                case "calcite" -> canary >= 28;
                case "shadoline_block" -> canary >= 28 && enderscape;
                case "purpur", "resin" -> purpurAndResin;
                case "dusk_purpur" -> purpurAndResin && patterns;
                case "veradite", "mirestone", "kurodite" -> patterns;
                default -> present;
            };
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(spec.id()) == expected
                    && BuiltInRegistries.ITEM.containsKey(spec.id()) == expected
                    && ExternalMaterialFamilies.fromSource(spec.id()).isPresent() == expected, "Optional source " + spec.id());
            if (!expected) {
                String fragment = AMC + "/" + spec.id().getPath() + "_";
                helper.assertTrue(BuiltInRegistries.BLOCK.keySet().stream().noneMatch(id -> id.getPath().contains(fragment))
                        && BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.getPath().contains(fragment)),
                        "Phantom derived registration for " + spec.id());
            }
        }
        for (String material : END_STONES) {
            var id = Identifier.fromNamespaceAndPath("enderscape", material.equals("dusk_purpur") ? "dusk_purpur_block" : material);
            var plain = ExternalMaterialFamilies.fromSource(id);
            helper.assertTrue(plain.isPresent() == enderscape, "Ordinary Enderscape family " + id);
            if (enderscape) {
                var binding = plain.orElseThrow();
                helper.assertTrue(ShapeMap.getShapes(binding.source().asItem()).equals(
                        binding.roles().values().stream().map(Block::asItem).toList()), "Ordinary selector " + id);
                for (String role : List.of("slab", "stairs", "wall")) {
                    helper.assertTrue(BuiltInRegistries.BLOCK.getKey(binding.roles().get(role)).equals(
                            id.withPath(material + "_" + role)) && !binding.isGeneratedRole(role), "Provider role " + id + " " + role);
                }
            }
        }
        var macawPatterns = ExternalMaterialFamilies.all().stream().filter(b -> b.spec().provider().equals("mcwpaths")
                && !b.spec().providerRoles().isEmpty()).toList();
        helper.assertTrue(macawPatterns.size() == 52, "Production Macaw patterned catalog");
        for (var macaw : macawPatterns) {
            for (String role : List.of("slab", "stairs")) {
                var id = macaw.spec().id();
                helper.assertTrue(BuiltInRegistries.BLOCK.getKey(macaw.roles().get(role))
                        .equals(id.withPath(id.getPath() + "_" + role)) && !macaw.isGeneratedRole(role),
                        "Production Macaw role " + id + " " + role);
            }
        }
        var unique = new HashSet<Block>();
        var membership = new JsonObject();
        for (var binding : families) {
            var id = binding.spec().id();
            var root = binding.source();
            helper.assertTrue(root == BuiltInRegistries.BLOCK.getValue(id)
                    && root.getClass().getName().equals("com.mcwpaths.kikoz.objects.EngravedBlock")
                    && root.getStateDefinition().getProperty("engraved") != null, "Provider parent " + id);
            helper.assertTrue(binding.generated().equals(binding.canonicalDerived())
                    && binding.spec().providerRoles().isEmpty(), "Role ownership " + id);
            var blocks = binding.roles();
            var sequence = blocks.values().stream().map(Block::asItem).toList();
            helper.assertTrue(List.copyOf(blocks.keySet()).equals(ROLES)
                    && ShapeMap.getShapes(root.asItem()).equals(sequence), "ShapeMap order " + id);
            var sample = new ItemStack(root, 11);
            sample.set(DataComponents.CUSTOM_NAME, Component.literal(id.toString()));
            var roles = new JsonObject();
            for (int index = 0; index < sequence.size(); index++) {
                var item = sequence.get(index);
                var block = blocks.get(ROLES.get(index));
                var key = BuiltInRegistries.BLOCK.getKey(block);
                roles.addProperty(ROLES.get(index), key.toString());
                helper.assertTrue(unique.add(block) && BuiltInRegistries.ITEM.getKey(item).equals(key)
                        && ShapeMap.getParent(item) == root.asItem(), "Duplicate or unbound role " + key);
                var transferred = ShapeMap.transferStack(sample, index);
                helper.assertTrue(transferred.is(item) && transferred.getCount() == sample.getCount()
                        && sample.get(DataComponents.CUSTOM_NAME).equals(transferred.get(DataComponents.CUSTOM_NAME)),
                        "Selector transfer " + key);
                if (index > 0) {
                    helper.assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)
                            && block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "Material properties " + key);
                    var wet = block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
                    helper.assertTrue(wet.getFluidState().isSource() && !wet.getCollisionShape(helper.getLevel(), BlockPos.ZERO).isEmpty(),
                            "Waterlogging or collision " + key);
                    helper.assertTrue(ClutterNoMore.RESOURCES.getResource(PackType.SERVER_DATA,
                            key.withPath("loot_table/blocks/" + key.getPath() + ".json")) != null, "Missing drop " + key);
                }
            }
            for (int bad : List.of(-1, sequence.size())) {
                helper.assertTrue(ItemStack.matches(ShapeMap.transferStack(sample, bad), sample), "Invalid selector " + id);
            }
            for (String role : List.of("slab", "stairs", "wall")) {
                helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(id.withPath(id.getPath() + "_" + role)),
                        "Unexpected provider role " + id + " " + role);
            }
            var slab = binding.slab().defaultBlockState();
            helper.assertTrue(FullOccupancyNormalizer.normalize(slab) == slab
                    && FullOccupancyNormalizer.normalize(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE))
                    == root.defaultBlockState(), "Horizontal slab completion " + id);
            var layer = binding.layer().defaultBlockState();
            helper.assertTrue(FullOccupancyNormalizer.normalize(layer.setValue(BgeLayerBlock.LAYERS, 4))
                    == root.defaultBlockState(), "Layer completion " + id);
            for (var from : sequence) {
                var stack = sample.transmuteCopy(from, sample.getCount());
                for (int to = 0; to < sequence.size(); to++) {
                    var result = ShapeMap.transferStack(stack, to);
                    helper.assertTrue(ItemStack.matches(result, sample.transmuteCopy(sequence.get(to), sample.getCount())),
                            "Component or count lost for " + from + " -> " + sequence.get(to));
                }
            }
            membership.add(id.toString(), roles);
        }
        for (var ordinary : List.of(Blocks.PURPUR_BLOCK, Blocks.RESIN_BRICKS)) {
            var shapes = ShapeMap.getShapes(ordinary.asItem());
            helper.assertTrue(shapes.contains(ordinary.asItem()) && shapes.stream().noneMatch(item ->
                    unique.contains(Block.byItem(item))), "Ordinary material selector overlaps AMC: " + ordinary);
        }
        var packs = new ArrayList<PackResources>();
        for (String name : System.getProperty("bge.testResourceJars").split(File.pathSeparator)) {
            var location = new PackLocationInfo(name, Component.literal(name), PackSource.DEFAULT, Optional.empty());
            packs.add(new FilePackResources.FileResourcesSupplier(new File(name)).openPrimary(location));
        }
        if (patterns) {
            var root = loader.getModContainer(AMC).orElseThrow().findPath("resourcepacks/enderscape").orElseThrow();
            var location = new PackLocationInfo(AMC + ":enderscape", Component.literal("AMC Enderscape"), PackSource.DEFAULT, Optional.empty());
            packs.add(new PathPackResources(location, root));
        }
        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, packs)) {
            ExternalMaterialGeneratedResources.generate(manager);
            QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
            LayerGeneratedResources.generateExternalForValidation(manager);
            CatalogItemGeneratedResources.generate(manager);
        }
        var resources = new JsonObject();
        for (String namespace : ClutterNoMore.RESOURCES.getNamespaces(PackType.CLIENT_RESOURCES)) {
            for (String prefix : List.of("blockstates", "items", "models")) {
                ClutterNoMore.RESOURCES.listResources(PackType.CLIENT_RESOURCES, namespace, prefix, (key, source) -> {
                    if (!key.getPath().contains(AMC)) return;
                    try (var reader = new InputStreamReader(source.get(), StandardCharsets.UTF_8)) {
                        resources.add(key.toString(), JsonParser.parseReader(reader));
                    } catch (IOException e) { throw new IllegalStateException(key.toString(), e); }
                });
            }
        }
        var audit = new JsonObject();
        audit.add("membership", membership);
        audit.add("resources", resources);
        Files.writeString(Path.of(System.getProperty("bge.amcAudit")), audit.toString());
        helper.succeed();
    }

    @GameTest
    public void derivedStateShapesMatchEstablishedPattern(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded(AMC)) {
            helper.succeed();
            return;
        }
        var reference = ExternalMaterialFamilies.fromSource(
                Identifier.fromNamespaceAndPath(AMC, "prismarine_bricks_running_bond")).orElseThrow();
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var context = CollisionContext.empty();
        for (var family : ExternalMaterialFamilies.all()) {
            if (!family.spec().provider().equals(AMC)) continue;
            for (String role : ROLES.subList(1, ROLES.size())) {
                var block = family.roles().get(role);
                var original = reference.roles().get(role);
                helper.assertTrue(block.getStateDefinition().getProperties().equals(original.getStateDefinition().getProperties()),
                        "State properties " + family.spec().id() + " " + role);
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    var expected = original.withPropertiesOf(state);
                    helper.assertTrue(state.getFluidState() == expected.getFluidState()
                            && Shapes.equal(state.getShape(level, pos, context), expected.getShape(level, pos, context))
                            && Shapes.equal(state.getCollisionShape(level, pos, context), expected.getCollisionShape(level, pos, context)),
                            "Outline, collision or fluid mismatch for " + state);
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public void survivalPlacementEngravingAndRecipes(GameTestHelper helper) {
        var support = new BlockPos(1, 1, 1);
        var target = support.above();
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.setBlock(support, Blocks.STONE);
        for (var family : ExternalMaterialFamilies.all()) {
            if (!family.spec().provider().equals(AMC)) continue;
            var id = family.spec().id();
            String materialName = materialName(id);
            String base = switch (materialName) {
                case "quartz", "purpur", "resin", "dusk_purpur" -> materialName + "_block";
                default -> materialName;
            };
            var ingredient = Identifier.fromNamespaceAndPath(END_STONES.contains(materialName) ? "enderscape" : "minecraft", base);
            var material = BuiltInRegistries.BLOCK.getOptional(ingredient).orElseThrow();
            var input = new SingleRecipeInput(new ItemStack(material));
            var recipe = level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).orElseThrow().value();
            helper.assertTrue(recipe instanceof StonecutterRecipe cut && cut.matches(input, level)
                    && ItemStack.matches(cut.assemble(input), new ItemStack(family.source())), "Stonecutting " + id);

            helper.setBlock(target, Blocks.AIR);
            var stack = new ItemStack(family.layer(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            for (int n = 1; n <= 4; n++) {
                helper.placeAt(player, stack, support, Direction.UP);
                var placed = helper.getBlockState(target);
                if (n < 4) {
                    helper.assertTrue(placed.is(family.layer()) && placed.getValue(BgeLayerBlock.LAYERS) == n
                            && stack.getCount() == 1, "Funded Layer " + n + " " + id);
                } else helper.assertTrue(placed == family.source().defaultBlockState() && stack.isEmpty(),
                        "Completed Layer " + id);
            }
            helper.setBlock(target, family.slab().defaultBlockState());
            stack = new ItemStack(family.slab());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.placeAt(player, stack, support, Direction.UP);
            helper.assertTrue(helper.getBlockState(target) == family.source().defaultBlockState()
                    && stack.isEmpty(), "Completed Slab " + id);
            helper.setBlock(target.above(), Blocks.STONE);
            var blocked = new ItemStack(family.layer());
            player.setItemInHand(InteractionHand.MAIN_HAND, blocked);
            helper.placeAt(player, blocked, support, Direction.UP);
            helper.assertTrue(helper.getBlockState(target) == family.source().defaultBlockState()
                    && blocked.getCount() == 1, "Obstructed placement " + id);
            helper.setBlock(target.above(), Blocks.AIR);
            var tool = new ItemStack(Items.IRON_PICKAXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            for (int n = 1; n <= 2; n++) {
                helper.useBlock(target, player);
                var state = helper.getBlockState(target);
                double height = state.getShape(level, helper.absolutePos(target), CollisionContext.empty()).bounds().maxY;
                var engraved = (net.minecraft.world.level.block.state.properties.BooleanProperty)
                        family.source().getStateDefinition().getProperty("engraved");
                helper.assertTrue(state.is(family.source()) && state.getValue(engraved) == (n == 1)
                                && height == (n == 1 ? 15 / 16.0 : 1),
                        "Pickaxe engraving " + n + " " + state + " height=" + height + " damage=" + tool.getDamageValue());
            }
            var drops = Block.getDrops(helper.getBlockState(target), level, helper.absolutePos(target), null, player, tool);
            helper.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst(), new ItemStack(family.source())),
                    "Canonical drop " + id);
        }
        helper.succeed();
    }
}
