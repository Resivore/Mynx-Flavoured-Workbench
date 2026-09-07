package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.client.ExternalMaterialGeneratedResources;
import dev.aero.cnmterraincompat.client.LayerGeneratedResources;
import dev.aero.cnmterraincompat.client.QuarterGeometryGeneratedResources;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Production-lifecycle coverage for C60's exact 64 source / 576 relation contract. */
public final class ExternalMaterialFamilyGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void exactAllowlistAndProviderCompletionInventory(GameTestHelper helper) {
        helper.assertTrue(ExternalMaterialCatalog.specs().size() == 64,
                "External source allowlist is not exactly 64");
        helper.assertTrue(ExternalMaterialCatalog.sourceCount("mcwpaths") == 57
                        && ExternalMaterialCatalog.sourceCount("mynx_trees") == 6
                        && ExternalMaterialCatalog.sourceCount("ribbits") == 1,
                "Provider source partition is not 57/6/1");
        helper.assertTrue(ExternalMaterialFamilies.all().size() == 64,
                "Late provider completion did not register all 64 families: "
                        + ExternalMaterialFamilies.all().size());
        helper.assertTrue(NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() != null).count() == 311
                        && NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() == null).count() == 64,
                "External append changed the frozen 311-profile native inventory");

        Set<Identifier> actual = new LinkedHashSet<>();
        ExternalMaterialFamilies.all().forEach(binding -> actual.add(binding.spec().id()));
        Set<Identifier> expected = new LinkedHashSet<>();
        ExternalMaterialCatalog.specs().forEach(spec -> expected.add(spec.id()));
        helper.assertTrue(actual.equals(expected), "Registered external sources differ from exact allowlist");
        helper.assertTrue(actual.stream().filter(id -> id.getNamespace().equals("mcwpaths"))
                        .allMatch(id -> isRequestedMacawPath(id.getPath())),
                "Unrequested Macaw material entered C60 scope");
        System.out.println("EXTERNAL_C60_INVENTORY|sources=64|mcwpaths=57|mynx_trees=6|ribbits=1|relations=576");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void absentOptionalProviderIsANoOp(GameTestHelper helper) {
        int before = ExternalMaterialFamilies.all().size();
        ExternalMaterialCatalog.registerProvider("bge_c60_deliberately_absent_provider");
        helper.assertTrue(ExternalMaterialFamilies.all().size() == before,
                "An absent optional provider mutated the external family registry");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void everySourceHasExactOrderedNineRoleShapeMapFamily(GameTestHelper helper) {
        int relations = 0;
        Set<Block> generated = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            Map<String, Block> roles = binding.roles();
            helper.assertTrue(roles.size() == 9 && roles.keySet().stream().toList().equals(List.of(
                            "block", "slab", "stairs", "wall", "vertical_slab", "step",
                            "corner", "quarter_column", "layer")),
                    "Nine-role vocabulary/order changed for " + binding.spec().id());
            for (Map.Entry<String, Block> role : roles.entrySet()) {
                Identifier actualId = BuiltInRegistries.BLOCK.getKey(role.getValue());
                helper.assertTrue(actualId != null && BuiltInRegistries.ITEM.getKey(role.getValue().asItem()).equals(actualId),
                        "Unregistered block/item role " + role.getKey() + " for " + binding.spec().id());
                if (!role.getKey().equals("block")) helper.assertTrue(generated.add(role.getValue()),
                        "Generated geometry was reused across sources: " + actualId);
            }

            List<Item> component = ShapeMap.getShapes(binding.source().asItem());
            List<Item> expected = roles.values().stream().map(Block::asItem).toList();
            int sourceIndex = component.indexOf(binding.source().asItem());
            boolean exactSegment = sourceIndex >= 0 && sourceIndex + expected.size() <= component.size()
                    && component.subList(sourceIndex, sourceIndex + expected.size()).equals(expected);
            helper.assertTrue(exactSegment, "ShapeMap relation lacks the exact ordered nine-role segment for "
                    + binding.spec().id() + ": " + component.stream().map(BuiltInRegistries.ITEM::getKey).toList());
            relations += roles.size();
        }
        helper.assertTrue(relations == 576 && generated.size() == 512,
                "C60 relation/derived identity count mismatch: " + relations + "/" + generated.size());
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void liveSourcePropertiesFireTagsLeavesAndAxesPropagate(GameTestHelper helper) {
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            Block source = binding.source();
            for (Block block : binding.generated()) {
                helper.assertTrue(block.defaultMapColor() == source.defaultMapColor()
                                && block.defaultDestroyTime() == source.defaultDestroyTime()
                                && block.getExplosionResistance() == source.getExplosionResistance()
                                && block.getFriction() == source.getFriction()
                                && block.getSpeedFactor() == source.getSpeedFactor()
                                && block.getJumpFactor() == source.getJumpFactor()
                                && block.defaultBlockState().getLightEmission()
                                        == source.defaultBlockState().getLightEmission()
                                && block.defaultBlockState().canOcclude()
                                        == source.defaultBlockState().canOcclude()
                                && block.defaultBlockState().requiresCorrectToolForDrops()
                                        == source.defaultBlockState().requiresCorrectToolForDrops()
                                && block.defaultBlockState().getSoundType()
                                        == source.defaultBlockState().getSoundType()
                                && block.defaultBlockState().ignitedByLava()
                                        == source.defaultBlockState().ignitedByLava(),
                        "Live BlockBehaviour.Properties did not propagate to " + BuiltInRegistries.BLOCK.getKey(block));
                for (var tag : binding.profile().derivedBlockTags()) helper.assertTrue(
                        block.defaultBlockState().is(tag), "Generated material tag missing: " + tag.location()
                                + " -> " + BuiltInRegistries.BLOCK.getKey(block));
            }

            FlammableBlockRegistry.Entry sourceFire = FlammableBlockRegistry.getDefaultInstance().get(source);
            if (sourceFire != null) for (Block block : binding.generated()) {
                FlammableBlockRegistry.Entry derivedFire = FlammableBlockRegistry.getDefaultInstance().get(block);
                helper.assertTrue(sourceFire.equals(derivedFire),
                        "Exact fire odds did not propagate to " + BuiltInRegistries.BLOCK.getKey(block));
            }

            if (binding.profile().capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)) {
                for (Block block : binding.generated()) helper.assertTrue(
                        block instanceof LeafDistanceCarrier
                                && block.defaultBlockState().hasProperty(BlockStateProperties.DISTANCE)
                                && block.defaultBlockState().hasProperty(BlockStateProperties.PERSISTENT),
                        "Leaf lifecycle state missing from " + BuiltInRegistries.BLOCK.getKey(block));
            }
            if (binding.source().defaultBlockState().hasProperty(BlockStateProperties.AXIS)) {
                for (Block block : binding.generated()) helper.assertTrue(
                        block.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                        "Log/wood material axis missing from " + BuiltInRegistries.BLOCK.getKey(block));
            }
        }
        ExternalMaterialFamilies.Binding silverLeaves = ExternalMaterialFamilies.fromSource(
                Identifier.parse("mynx_trees:silver_birch_leaves")).orElseThrow();
        helper.assertTrue(silverLeaves.generated().stream().allMatch(block ->
                        block.defaultBlockState().is(BlockTags.LEAVES)),
                "Silver Birch leaf forms are not leaf-tagged");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void lateServerDataResourcesCloseEveryStandardFamily(GameTestHelper helper) {
        int loot = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (String role : List.of("slab", "stairs", "wall", "vertical_slab", "step")) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(binding.roles().get(role));
                JsonObject table = generatedServerJson(Identifier.fromNamespaceAndPath(id.getNamespace(),
                        "loot_table/blocks/" + id.getPath() + ".json"));
                helper.assertTrue("minecraft:block".equals(table.get("type").getAsString()),
                        "Generated loot table has wrong type: " + id);
                loot++;
            }
        }
        JsonObject walls = generatedServerJson(Identifier.parse("minecraft:tags/block/walls.json"));
        helper.assertTrue(walls.getAsJsonArray("values").size() == 64,
                "External wall classification does not contain all 64 families");
        helper.assertTrue(loot == 320, "Expected 320 external standard loot tables, found " + loot);
        System.out.println("EXTERNAL_C60_SERVER_RESOURCES|standardLoot=320|wallTags=64|materialFamilies=64");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void actualClientWritersCloseAll512GeneratedGeometryResources(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        LayerGeneratedResources.GenerationSummary layers =
                LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.GenerationSummary quarters =
                QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialGeneratedResources.GenerationSummary standard =
                ExternalMaterialGeneratedResources.generate(manager);
        helper.assertTrue(layers.familyCount() == 64
                        && quarters.cornerFamilyCount() == 64
                        && quarters.columnFamilyCount() == 64
                        && standard.familyCount() == 64
                        && standard.blockStateCount() == 320
                        && standard.itemCount() == 320,
                "External client writers did not process every exact family/role");

        int generatedRelations = 0;
        int resolvedModelReferences = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                if (role.getKey().equals("block")) continue;
                Identifier block = BuiltInRegistries.BLOCK.getKey(role.getValue());
                JsonObject blockState = generatedClientJson(Identifier.fromNamespaceAndPath(block.getNamespace(),
                        "blockstates/" + block.getPath() + ".json"));
                JsonObject item = generatedClientJson(Identifier.fromNamespaceAndPath(block.getNamespace(),
                        "items/" + block.getPath() + ".json"));
                Set<String> models = new LinkedHashSet<>();
                collectModelReferences(blockState, models);
                collectModelReferences(item, models);
                helper.assertTrue(!models.isEmpty(), "No generated model reference for " + block);
                for (String model : models) {
                    Identifier modelId = Identifier.parse(model);
                    generatedClientJson(Identifier.fromNamespaceAndPath(modelId.getNamespace(),
                            "models/" + modelId.getPath() + ".json"));
                    resolvedModelReferences++;
                }
                generatedRelations++;
            }
        }
        helper.assertTrue(generatedRelations == 512 && resolvedModelReferences >= 512,
                "External client resource closure mismatch: relations=" + generatedRelations
                        + ", modelReferences=" + resolvedModelReferences);
        System.out.println("EXTERNAL_C60_CLIENT_RESOURCES|generatedRelations=512|blockstates=512|items=512"
                + "|resolvedModelReferences=" + resolvedModelReferences);
        helper.succeed();
    }

    private static boolean isRequestedMacawPath(String path) {
        if (Set.of("podzol_path_block", "dirt_path_block", "gravel_path_block",
                "sand_path_block", "red_sand_path_block").contains(path)) return true;
        return List.of("running_bond", "windmill_weave", "flagstone", "crystal_floor").stream()
                .anyMatch(pattern -> path.endsWith("_" + pattern + "_path"));
    }

    private static JsonObject generatedServerJson(Identifier id) {
        try {
            var supplier = ClutterNoMore.RESOURCES.getResource(PackType.SERVER_DATA, id);
            if (supplier == null) throw new IllegalStateException("Missing generated server resource " + id);
            try (var input = supplier.get();
                    var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect generated server resource " + id, exception);
        }
    }

    private static JsonObject generatedClientJson(Identifier id) {
        try {
            var supplier = ClutterNoMore.RESOURCES.getResource(PackType.CLIENT_RESOURCES, id);
            if (supplier == null) throw new IllegalStateException("Missing generated client resource " + id);
            try (var input = supplier.get();
                    var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect generated client resource " + id, exception);
        }
    }

    private static void collectModelReferences(com.google.gson.JsonElement value, Set<String> result) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString();
            if (text.contains(":block/")) result.add(text);
        } else if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(child -> collectModelReferences(child, result));
        } else if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet().forEach(entry -> collectModelReferences(entry.getValue(), result));
        }
    }

    private static ResourceManager clientFixtureManager() {
        Map<Identifier, String> json = new HashMap<>();
        json.put(Identifier.parse("minecraft:blockstates/oak_stairs.json"),
                "{\"variants\":{\"facing=north,half=bottom,shape=straight\":"
                        + "{\"model\":\"minecraft:block/oak_stairs\"},"
                        + "\"facing=north,half=bottom,shape=inner_left\":"
                        + "{\"model\":\"minecraft:block/oak_stairs_inner\"},"
                        + "\"facing=north,half=bottom,shape=outer_left\":"
                        + "{\"model\":\"minecraft:block/oak_stairs_outer\"}}}");
        json.put(Identifier.parse("minecraft:blockstates/cobblestone_wall.json"),
                "{\"multipart\":[{\"when\":{\"up\":\"true\"},"
                        + "\"apply\":{\"model\":\"minecraft:block/cobblestone_wall_post\"}},"
                        + "{\"when\":{\"north\":\"low\"},"
                        + "\"apply\":{\"model\":\"minecraft:block/cobblestone_wall_side\"}},"
                        + "{\"when\":{\"north\":\"tall\"},"
                        + "\"apply\":{\"model\":\"minecraft:block/cobblestone_wall_side_tall\"}}]}");
        for (String path : List.of("wisteria_log", "wisteria_wood",
                "silver_birch_log", "silver_birch_wood")) {
            Identifier source = Identifier.fromNamespaceAndPath("mynx_trees", path);
            String model = "mynx_trees:block/" + path;
            json.put(Identifier.fromNamespaceAndPath("mynx_trees", "blockstates/" + path + ".json"),
                    "{\"variants\":{\"axis=x\":{\"model\":\"" + model
                            + "\",\"x\":90,\"y\":90},\"axis=y\":{\"model\":\"" + model
                            + "\"},\"axis=z\":{\"model\":\"" + model + "\",\"x\":90}}}");
        }
        json.put(Identifier.parse("mynx_trees:items/silver_birch_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"mynx_trees:block/silver_birch_leaves\","
                        + "\"tints\":[{\"type\":\"minecraft:constant\",\"value\":8431445}]}}" );

        PackResources pack = (PackResources) Proxy.newProxyInstance(
                ExternalMaterialFamilyGameTests.class.getClassLoader(),
                new Class<?>[] {PackResources.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "packId" -> "bge-c60-client-fixtures";
                    case "knownPackInfo" -> Optional.empty();
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C60 client fixture pack";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected PackResources call " + method);
                });
        Map<Identifier, Resource> resources = new HashMap<>();
        json.forEach((id, content) -> resources.put(id, new Resource(pack,
                () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))));
        return (ResourceManager) Proxy.newProxyInstance(
                ExternalMaterialFamilyGameTests.class.getClassLoader(),
                new Class<?>[] {ResourceManager.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getResource" -> Optional.ofNullable(resources.get((Identifier) args[0]));
                    case "getResourceStack" -> Optional.ofNullable(resources.get((Identifier) args[0]))
                            .map(List::of).orElseGet(List::of);
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees");
                    case "listResources" -> resources.entrySet().stream()
                            .filter(entry -> entry.getKey().getPath().startsWith((String) args[0]))
                            .filter(entry -> ((Predicate<Identifier>) args[1]).test(entry.getKey()))
                            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    case "listResourceStacks" -> Map.of();
                    case "listPacks" -> Stream.of(pack);
                    case "toString" -> "BGE C60 client fixture manager";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected ResourceManager call " + method);
                });
    }

    @Override public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException { method.invoke(this, helper); }
}
