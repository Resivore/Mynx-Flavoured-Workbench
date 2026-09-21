package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.client.CatalogPreviewGeneratedResources;
import dev.aero.cnmterraincompat.client.CatalogPreviewGeneratedResources.PreviewRole;
import dev.aero.cnmterraincompat.client.ExternalMaterialGeneratedResources;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Focused production contracts for C93 wall, path-culling, UV and selector preview rendering. */
public final class RenderingContractGameTests implements CustomTestMethodInvoker {
    private static final List<String> PATHS = List.of("celestial_path", "corrupt_path");

    @GameTest(maxTicks = 40)
    public void loweredPathStandardFormsExposeTheTopNeighborStripToFaceOcclusion(
            GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        VoxelShape topStrip = Block.box(0, 15, 0, 16, 16, 16);
        for (String name : PATHS) {
            ExternalMaterialFamilies.Binding binding = external("enderscape:" + name);
            helper.assertTrue(binding.stairs() instanceof LoweredPathStairsBlock
                            && binding.verticalSlab() instanceof LoweredPathVerticalSlabBlock
                            && binding.step() instanceof LoweredPathStepBlock,
                    "External Path standard forms do not use the shape-only lowered carriers: " + name);

            BlockState stairs = binding.stairs().defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH)
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
            BlockState vertical = binding.verticalSlab().defaultBlockState()
                    .setValue(VerticalSlabBlock.FACING, Direction.NORTH)
                    .setValue(VerticalSlabBlock.DOUBLE, false);
            BlockState step = binding.step().defaultBlockState()
                    .setValue(StepBlock.FACING, Direction.NORTH)
                    .setValue(StepBlock.SLAB_TYPE, SlabType.DOUBLE);
            for (Map.Entry<String, BlockState> entry : Map.of(
                    "Stairs", stairs, "Vertical Slab", vertical, "Step", step).entrySet()) {
                BlockState state = entry.getValue();
                VoxelShape outline = state.getShape(helper.getLevel(), pos, CollisionContext.empty());
                VoxelShape occlusion = state.getOcclusionShape();
                helper.assertTrue(outline.bounds().maxY == 15.0 / 16.0
                                && occlusion.bounds().maxY == 15.0 / 16.0,
                        name + " " + entry.getKey() + " lost its exact 15/16 outline/occlusion ceiling");

                boolean exercisedSide = false;
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    VoxelShape faceOcclusion = state.getFaceOcclusionShape(face);
                    if (faceOcclusion.isEmpty()) continue;
                    exercisedSide = true;
                    helper.assertTrue(faceOcclusion.bounds().maxY == 15.0 / 16.0,
                            name + " " + entry.getKey() + " face cache still occludes y=15..16 on " + face);
                    helper.assertTrue(Shapes.joinIsNotEmpty(topStrip, faceOcclusion,
                                    BooleanOp.ONLY_FIRST),
                            name + " " + entry.getKey() + " hides the one-pixel neighbor strip on " + face);
                }
                helper.assertTrue(exercisedSide,
                        name + " " + entry.getKey() + " did not expose a side face-occlusion shape");
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void pathStepsCopyTheExactProviderTemplateTopologyAndUvs(GameTestHelper helper) {
        ResourceManager manager = ExternalMaterialFamilyGameTests
                .clientFixtureManagerForBbbBeamResourceRegression();
        ExternalMaterialGeneratedResources.generate(manager);
        for (String name : PATHS) {
            ExternalMaterialFamilies.Binding binding = external("enderscape:" + name);
            Identifier step = id(binding.step());
            for (String suffix : List.of("", "_top", "_double")) {
                String templateName = "path_step" + suffix + ".json";
                JsonObject expected = classpathJson(Identifier.fromNamespaceAndPath("clutternomore",
                        "models/block/templates/provider/" + templateName));
                JsonObject actual = generatedJson(modelResource(step, suffix));
                expected.remove("textures");
                actual.remove("textures");
                helper.assertTrue(actual.equals(expected),
                        name + " Step does not exactly copy provider " + templateName + " topology/UVs");
                JsonObject generated = generatedJson(modelResource(step, suffix));
                JsonObject textures = generated.getAsJsonObject("textures");
                helper.assertTrue(textures.get("side").getAsString()
                                .equals(binding.profile().textureRoles().side())
                                && textures.get("top").getAsString()
                                        .equals(binding.profile().textureRoles().top())
                                && textures.get("bottom").getAsString()
                                        .equals(binding.profile().textureRoles().bottom())
                                && !generated.toString().contains("minecraft:block/dirt_path"),
                        name + " Step copied a reference texture instead of only its authored UV topology");
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void enderscapePlankAndPrivateBeamWallsRemainDistinctContracts(GameTestHelper helper) {
        ResourceManager manager = ExternalMaterialFamilyGameTests
                .clientFixtureManagerForBbbBeamResourceRegression();
        ExternalMaterialGeneratedResources.generate(manager);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        for (String family : List.of("veiled", "celestial", "murublight")) {
            ExternalMaterialFamilies.Binding planks = external("enderscape:" + family + "_planks");
            ExternalMaterialFamilies.Binding beam = external(
                    CnmTerrainCompat.MOD_ID + ":enderscape/" + family + "_beam");
            helper.assertTrue(planks.wall() instanceof WallBlock
                            && !(planks.wall() instanceof WoodenPlankWallBlock)
                            && beam.wall() instanceof WoodenPlankWallBlock,
                    family + " Planks and private Beam no longer use ordinary/BBB wall topology respectively");

            Identifier plankWall = id(planks.wall());
            JsonObject plankState = generatedJson(blockStateResource(plankWall));
            helper.assertTrue(plankState.toString().contains("_wall_post")
                            && plankState.toString().contains("_wall_side_tall")
                            && plankState.toString().contains("\"up\":\"true\""),
                    family + " Planks wall is not the ordinary conditional-post wall selector");
            helper.assertTrue(generatedJson(modelResource(plankWall, "_post"))
                            .get("parent").getAsString().equals("minecraft:block/template_wall_post")
                            && generatedJson(modelResource(plankWall, "_inventory"))
                                    .get("parent").getAsString().equals("minecraft:block/wall_inventory"),
                    family + " Planks wall does not inherit ordinary vanilla wall models");

            Identifier beamWall = id(beam.wall());
            JsonArray beamParts = generatedJson(blockStateResource(beamWall)).getAsJsonArray("multipart");
            helper.assertTrue(beamParts.size() == 9
                            && !beamParts.get(0).getAsJsonObject().has("when")
                            && beamParts.get(0).toString().contains("_post")
                            && beamParts.asList().stream().skip(1)
                                    .allMatch(part -> part.toString().contains("_side")
                                            && !part.toString().contains("_side_tall")),
                    family + " Beam wall does not use BBB's unconditional post/eight side selectors");
            JsonObject post = generatedJson(modelResource(beamWall, "_post"));
            JsonObject side = generatedJson(modelResource(beamWall, "_side"));
            helper.assertTrue(bounds(post).equals(List.of(4, 0, 4, 12, 16, 12))
                            && bounds(side).equals(List.of(4, 0, 0, 12, 16, 4))
                            && post.getAsJsonObject("display").getAsJsonObject("gui")
                                    .getAsJsonArray("rotation").get(1).getAsInt() == 225
                            && side.getAsJsonObject("textures").get("top").getAsString()
                                    .equals(beam.profile().textureRoles().side())
                            && side.getAsJsonObject("textures").get("sides").getAsString()
                                    .equals(beam.profile().textureRoles().top()),
                    family + " Beam wall model dimensions, GUI transform or end/side grain aliases drifted");

            JsonObject item = generatedJson(itemResource(beamWall));
            helper.assertTrue(item.getAsJsonObject("model").get("model").getAsString()
                            .equals(model(beamWall) + "_post"),
                    family + " Beam item is not the exact BBB post-only reference");

            BlockState base = beam.wall().defaultBlockState().setValue(WallBlock.UP, true);
            BlockState straight = wall(base, WallSide.LOW, WallSide.NONE,
                    WallSide.LOW, WallSide.NONE);
            BlockState tee = wall(base, WallSide.LOW, WallSide.LOW,
                    WallSide.LOW, WallSide.NONE);
            BlockState cross = wall(base, WallSide.LOW, WallSide.LOW,
                    WallSide.LOW, WallSide.LOW);
            VoxelShape center = Block.box(4, 0, 4, 12, 16, 12);
            helper.assertTrue(!covers(beam.wall(), straight, center, helper, pos)
                            && covers(beam.wall(), tee, center, helper, pos)
                            && !covers(beam.wall(), cross, center, helper, pos),
                    family + " Beam wall runtime post policy is not BBB straight/T/cross topology");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 160)
    public void finalShapeMapPreviewsAreItemOnlyAndEveryParentRouteResolves(GameTestHelper helper) {
        ResourceManager providerFixture = ExternalMaterialFamilyGameTests
                .clientFixtureManagerForBbbBeamResourceRegression();
        ExternalMaterialGeneratedResources.generate(providerFixture);

        Map<Identifier, PreviewRole> expectedRoles = new LinkedHashMap<>();
        Map<Identifier, String> expectedParents = new LinkedHashMap<>();
        Map<Identifier, String> placedSelectors = new LinkedHashMap<>();
        Set<Item> actualVisible = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<Item> expectedGroup : ExplicitShapeMapFamilies.expectedItemGroups()) {
            List<Item> actualGroup = ShapeMap.getShapes(expectedGroup.getFirst());
            helper.assertTrue(actualGroup.equals(expectedGroup),
                    "Preview audit is not walking the final exact ShapeMap group rooted at "
                            + itemId(expectedGroup.getFirst()));
            actualVisible.addAll(actualGroup);
        }
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            List<Item> variation = ExplicitShapeMapFamilies.variationItems(profile);
            putRole(helper, actualVisible, expectedRoles, expectedParents, placedSelectors,
                    variation.get(2), PreviewRole.STAIRS);
            putRole(helper, actualVisible, expectedRoles, expectedParents, placedSelectors,
                    variation.get(3), PreviewRole.WALL);
            putRole(helper, actualVisible, expectedRoles, expectedParents, placedSelectors,
                    variation.get(4), PreviewRole.VERTICAL_SLAB);
            putRole(helper, actualVisible, expectedRoles, expectedParents, placedSelectors,
                    variation.get(5), PreviewRole.STEP);
        }

        ResourceManager manager = previewFixtureManager(providerFixture, expectedParents, placedSelectors);
        Map<Identifier, String> beforeBlockStates = new LinkedHashMap<>();
        for (Identifier block : expectedRoles.keySet()) {
            Identifier state = blockStateResource(block);
            beforeBlockStates.put(state, resolvedText(manager, state));
        }

        CatalogPreviewGeneratedResources.GenerationSummary summary =
                CatalogPreviewGeneratedResources.generate(manager);
        helper.assertTrue(summary.roles().equals(expectedRoles),
                "Final preview pass did not cover the exact four visible roles of every ShapeMap variation");

        int fixedMagnia = 0;
        int blinklamp = 0;
        int beamPosts = 0;
        for (Map.Entry<Identifier, PreviewRole> entry : expectedRoles.entrySet()) {
            Identifier block = entry.getKey();
            PreviewRole role = entry.getValue();
            JsonObject definition = generatedJson(itemResource(block));
            String target = definition.getAsJsonObject("model").get("model").getAsString();
            NibaruMaterialProfile profile = profileFor(block).orElseThrow();
            if (role == PreviewRole.WALL
                    && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId())) {
                helper.assertTrue(target.equals(model(block) + "_post"),
                        "Private Beam wall preview stopped using its exact post reference: " + block);
                beamPosts++;
                continue;
            }

            String previewBase = model(block) + "_bge_preview";
            helper.assertTrue(target.equals(previewBase) || target.startsWith(previewBase + "_"),
                    "Visible ShapeMap role has no item-only preview wrapper: " + block + " -> " + target);
            JsonObject wrapper = generatedJson(modelResource(target));
            JsonObject gui = wrapper.getAsJsonObject("display").getAsJsonObject("gui");
            helper.assertTrue(gui.getAsJsonArray("rotation").equals(numbers(30, 135, 0))
                            && gui.getAsJsonArray("scale").equals(numbers(.625, .625, .625)),
                    "Preview GUI transform drifted for " + block);
            double expectedTranslation = role == PreviewRole.VERTICAL_SLAB || role == PreviewRole.STEP
                    ? 1.75 : 0;
            helper.assertTrue(gui.getAsJsonArray("translation").get(0).getAsDouble()
                            == expectedTranslation,
                    "Preview centering translation drifted for " + block + " " + role);

            String parent = wrapper.get("parent").getAsString();
            helper.assertTrue(!parent.contains("_bge_preview")
                            && resolveJson(manager, modelResource(parent)) != null,
                    "Preview wrapper parent model does not resolve: " + block + " -> " + parent);
            String source = profile.canonicalParentId().toString();
            if (source.equals("enderscape:alluring_magnia")
                    || source.equals("enderscape:repulsive_magnia")) {
                helper.assertTrue(target.equals(previewBase)
                                && parent.equals(expectedParents.get(block)),
                        "Fixed Magnia preview must resolve through its emitted unsuffixed base model: " + block);
                fixedMagnia++;
            }
            if (source.equals("enderscape:blinklamp")) {
                helper.assertTrue(target.equals(previewBase + "_luminance4")
                                && parent.equals(expectedParents.get(block) + "_luminance4"),
                        "Blinklamp preview suffix did not propagate to the production parent model: " + block);
                blinklamp++;
            }
        }
        helper.assertTrue(fixedMagnia == 8 && blinklamp == 4 && beamPosts == 3,
                "Did not exercise all fixed-Magnia, Blinklamp and private-Beam preview routes: "
                        + fixedMagnia + "/" + blinklamp + "/" + beamPosts);

        for (Map.Entry<Identifier, String> state : beforeBlockStates.entrySet()) {
            String after = resolvedText(manager, state.getKey());
            helper.assertTrue(after.equals(state.getValue()) && !after.contains("_bge_preview"),
                    "Item preview pass changed a placed blockstate/world-model selector: " + state.getKey());
        }
        helper.succeed();
    }

    private static void putRole(GameTestHelper helper, Set<Item> actualVisible,
            Map<Identifier, PreviewRole> roles, Map<Identifier, String> parents,
            Map<Identifier, String> selectors, Item item, PreviewRole role) {
        helper.assertTrue(actualVisible.contains(item),
                "Expected preview role is absent from the actual final ShapeMap: " + itemId(item));
        Block block = ((BlockItem) item).getBlock();
        Identifier id = id(block);
        PreviewRole previous = roles.putIfAbsent(id, role);
        helper.assertTrue(previous == null || previous == role,
                "Actual ShapeMap item has conflicting preview roles: " + id);
        String placed = role == PreviewRole.WALL ? model(id) + "_post" : model(id);
        String parent = role == PreviewRole.WALL ? model(id) + "_inventory" : model(id);
        selectors.put(id, placed);
        parents.put(id, parent);
    }

    private static ResourceManager previewFixtureManager(ResourceManager delegate,
            Map<Identifier, String> parents, Map<Identifier, String> placedSelectors) {
        Map<Identifier, String> json = new HashMap<>();
        for (Map.Entry<Identifier, String> entry : parents.entrySet()) {
            Identifier block = entry.getKey();
            json.put(itemResource(block), itemDefinition(entry.getValue()).toString());
            json.put(modelResource(entry.getValue()), "{}");
            json.put(blockStateResource(block),
                    ("{\"variants\":{\"\":{\"model\":\"%s\"}}}")
                            .formatted(placedSelectors.get(block)));
        }
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Identifier parent = profile.canonicalParentId();
            json.putIfAbsent(itemResource(parent), itemDefinition(model(parent)).toString());
        }
        PackResources pack = (PackResources) Proxy.newProxyInstance(
                RenderingContractGameTests.class.getClassLoader(), new Class<?>[] {PackResources.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "packId" -> "bge-c93-rendering-fixtures";
                    case "knownPackInfo" -> Optional.empty();
                    case "getNamespaces" -> Set.of("minecraft", "clutternomore",
                            CnmTerrainCompat.MOD_ID, "enderscape", "bbb", "mynx_trees", "ribbits");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C93 rendering contract fixtures";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected PackResources call " + method);
                });
        Map<Identifier, Resource> fixtures = new HashMap<>();
        json.forEach((id, content) -> fixtures.put(id, new Resource(pack,
                () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))));
        return (ResourceManager) Proxy.newProxyInstance(RenderingContractGameTests.class.getClassLoader(),
                new Class<?>[] {ResourceManager.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getResource" -> {
                        Identifier requested = (Identifier) args[0];
                        Resource fixture = fixtures.get(requested);
                        yield fixture != null ? Optional.of(fixture) : delegate.getResource(requested);
                    }
                    case "getResourceStack" -> {
                        Identifier requested = (Identifier) args[0];
                        Resource fixture = fixtures.get(requested);
                        yield fixture != null ? List.of(fixture) : delegate.getResourceStack(requested);
                    }
                    case "getNamespaces" -> {
                        Set<String> result = new LinkedHashSet<>(delegate.getNamespaces());
                        fixtures.keySet().forEach(id -> result.add(id.getNamespace()));
                        yield Set.copyOf(result);
                    }
                    case "listResources" -> {
                        Map<Identifier, Resource> result = new HashMap<>(delegate.listResources(
                                (String) args[0], (Predicate<Identifier>) args[1]));
                        fixtures.entrySet().stream()
                                .filter(entry -> entry.getKey().getPath().startsWith((String) args[0]))
                                .filter(entry -> ((Predicate<Identifier>) args[1]).test(entry.getKey()))
                                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
                        yield result;
                    }
                    case "listResourceStacks" -> Map.of();
                    case "listPacks" -> Stream.concat(delegate.listPacks(), Stream.of(pack));
                    case "toString" -> "BGE C93 complete preview fixture manager";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected ResourceManager call " + method);
                });
    }

    private static JsonObject itemDefinition(String model) {
        JsonObject selected = new JsonObject();
        selected.addProperty("type", "minecraft:model");
        selected.addProperty("model", model);
        JsonObject root = new JsonObject();
        root.add("model", selected);
        return root;
    }

    private static BlockState wall(BlockState state, WallSide north, WallSide east,
            WallSide south, WallSide west) {
        return state.setValue(WallBlock.NORTH, north).setValue(WallBlock.EAST, east)
                .setValue(WallBlock.SOUTH, south).setValue(WallBlock.WEST, west);
    }

    private static boolean covers(Block block, BlockState state, VoxelShape target,
            GameTestHelper helper, BlockPos pos) {
        VoxelShape shape = state.getShape(helper.getLevel(), pos, CollisionContext.empty());
        return !Shapes.joinIsNotEmpty(target, shape, BooleanOp.ONLY_FIRST);
    }

    private static List<Integer> bounds(JsonObject model) {
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        List<Integer> result = new ArrayList<>(6);
        element.getAsJsonArray("from").forEach(value -> result.add(value.getAsInt()));
        element.getAsJsonArray("to").forEach(value -> result.add(value.getAsInt()));
        return List.copyOf(result);
    }

    private static Optional<NibaruMaterialProfile> profileFor(Identifier derived) {
        Block block = BuiltInRegistries.BLOCK.getValue(derived);
        if (!derived.equals(BuiltInRegistries.BLOCK.getKey(block))) return Optional.empty();
        return NibaruProviderAdapter.runtimeBinding(block).map(NibaruProviderAdapter.RuntimeBinding::profile)
                .or(() -> NibaruMaterialProfiles.all().stream()
                        .filter(profile -> ExplicitShapeMapFamilies.variationItems(profile).stream()
                                .anyMatch(item -> item == block.asItem()))
                        .findFirst());
    }

    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) result.add(value);
        return result;
    }

    private static ExternalMaterialFamilies.Binding external(String source) {
        return ExternalMaterialFamilies.fromSource(Identifier.parse(source)).orElseThrow();
    }

    private static Identifier id(Block block) { return BuiltInRegistries.BLOCK.getKey(block); }
    private static Identifier itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item); }
    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static Identifier blockStateResource(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
    }
    private static Identifier itemResource(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }
    private static Identifier modelResource(Identifier id, String suffix) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/block/" + id.getPath() + suffix + ".json");
    }
    private static Identifier modelResource(String model) {
        Identifier id = Identifier.parse(model);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json");
    }

    private static JsonObject generatedJson(Identifier id) {
        JsonObject result = resolveJson(null, id);
        if (result == null) throw new IllegalStateException("Missing generated client resource " + id);
        return result;
    }

    private static JsonObject resolveJson(ResourceManager manager, Identifier id) {
        try {
            var supplier = ClutterNoMore.RESOURCES.getResource(PackType.CLIENT_RESOURCES, id);
            if (supplier != null) {
                try (InputStream input = supplier.get();
                        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            if (manager != null) {
                Resource resource = manager.getResource(id).orElse(null);
                if (resource != null) try (var reader = resource.openAsReader()) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            InputStream classpath = RenderingContractGameTests.class.getClassLoader()
                    .getResourceAsStream("assets/" + id.getNamespace() + "/" + id.getPath());
            if (classpath == null) return null;
            try (classpath; var reader = new InputStreamReader(classpath, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect client resource " + id, exception);
        }
    }

    private static String resolvedText(ResourceManager manager, Identifier id) {
        JsonObject json = resolveJson(manager, id);
        if (json == null) throw new IllegalStateException("Unresolved client resource " + id);
        return json.toString();
    }

    private static JsonObject classpathJson(Identifier id) {
        JsonObject result = resolveClasspathJson(id);
        if (result == null) throw new IllegalStateException("Missing classpath resource " + id);
        return result;
    }

    private static JsonObject resolveClasspathJson(Identifier id) {
        try {
            InputStream input = RenderingContractGameTests.class.getClassLoader()
                    .getResourceAsStream("assets/" + id.getNamespace() + "/" + id.getPath());
            if (input == null) return null;
            try (input; var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect classpath resource " + id, exception);
        }
    }

    @Override public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException { method.invoke(this, helper); }
}
