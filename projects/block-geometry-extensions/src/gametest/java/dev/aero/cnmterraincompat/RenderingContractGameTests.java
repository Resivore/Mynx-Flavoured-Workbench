package dev.aero.cnmterraincompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.client.BeamItemModelContract;
import dev.aero.cnmterraincompat.client.CatalogItemGeneratedResources;
import dev.aero.cnmterraincompat.client.CatalogItemGeneratedResources.ItemRole;
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

/** Focused production contracts for C94 wall, path-culling, UV and item rendering. */
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

    @GameTest(maxTicks = 240)
    public void c92CatalogPresentationIsRestoredAndBeamItemsUsePlacedAxisModels(
            GameTestHelper helper) {
        ResourceManager providerFixture = ExternalMaterialFamilyGameTests
                .clientFixtureManagerForBbbBeamResourceRegression();
        ExternalMaterialGeneratedResources.generate(providerFixture);

        Map<Identifier, ItemRole> expectedRoles = new LinkedHashMap<>();
        Map<Identifier, String> c92Targets = new LinkedHashMap<>();
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
            putRole(helper, actualVisible, expectedRoles, c92Targets, placedSelectors,
                    profile, variation.get(2), ItemRole.STAIRS);
            putRole(helper, actualVisible, expectedRoles, c92Targets, placedSelectors,
                    profile, variation.get(3), ItemRole.WALL);
            putRole(helper, actualVisible, expectedRoles, c92Targets, placedSelectors,
                    profile, variation.get(4), ItemRole.VERTICAL_SLAB);
            putRole(helper, actualVisible, expectedRoles, c92Targets, placedSelectors,
                    profile, variation.get(5), ItemRole.STEP);
        }

        ResourceManager manager = catalogFixtureManager(
                providerFixture, expectedRoles, c92Targets, placedSelectors);
        Map<Identifier, String> beforePlaced = placedResourceSnapshot(manager, expectedRoles.keySet());

        CatalogItemGeneratedResources.GenerationSummary summary =
                CatalogItemGeneratedResources.generate(manager);
        helper.assertTrue(summary.roles().equals(expectedRoles),
                "Final item pass did not cover the exact four visible roles of every ShapeMap variation");
        helper.assertTrue(expectedRoles.size() == 1968
                        && summary.itemDefinitionCount() == expectedRoles.size()
                        && summary.beamItemModelCount() == 30,
                "Final item pass count drifted: roles=" + expectedRoles.size()
                        + " items=" + summary.itemDefinitionCount()
                        + " Beam models=" + summary.beamItemModelCount());

        int beamItems = 0;
        int beamWalls = 0;
        int fixedMagnia = 0;
        int blinklamp = 0;
        boolean stairC92 = false;
        boolean verticalC92 = false;
        boolean stepC92 = false;
        boolean paleOakWallC92 = false;
        Set<String> beamSides = new LinkedHashSet<>();
        Set<String> beamEnds = new LinkedHashSet<>();
        for (Map.Entry<Identifier, ItemRole> entry : expectedRoles.entrySet()) {
            Identifier block = entry.getKey();
            ItemRole role = entry.getValue();
            JsonObject definition = generatedJson(itemResource(block));
            String target = definition.getAsJsonObject("model").get("model").getAsString();
            NibaruMaterialProfile profile = profileFor(block).orElseThrow();
            boolean beam = BeamItemModelContract.applies(profile.canonicalParentId());
            if (beam && (role == ItemRole.VERTICAL_SLAB || role == ItemRole.STEP)) {
                String expectedTarget = model(block) + BeamItemModelContract.MODEL_SUFFIX;
                helper.assertTrue(target.equals(expectedTarget),
                        "Beam item did not select its inventory-only axis model: " + block + " -> " + target);
                JsonObject wrapper = generatedJson(modelResource(target));
                String expectedParent = role == ItemRole.VERTICAL_SLAB
                        ? AxisModelContract.verticalDirectHalfModelId(
                                block, Direction.Axis.Y, Direction.NORTH)
                        : AxisModelContract.stepDirectPairModelId(block, Direction.Axis.Y,
                                new AxisModelContract.HalfSpacePair(Direction.NORTH, Direction.DOWN));
                helper.assertTrue(wrapper.get("parent").getAsString().equals(expectedParent),
                        "Beam item did not reuse the unchanged default-axis placed model: " + block);
                assertGui(helper, wrapper.getAsJsonObject("display").getAsJsonObject("gui"),
                        -135, -1.75, "Beam " + role + " " + block);
                assertFirstPerson(helper, wrapper.getAsJsonObject("display"),
                        "Beam " + role + " " + block);

                JsonObject placedModel = generatedJson(modelResource(expectedParent));
                JsonObject textures = placedModel.getAsJsonObject("textures");
                List<String> expectedTextures = exactBeamTextures(profile.canonicalParentId());
                helper.assertTrue(textures.get("side").getAsString()
                                .equals(profile.textureRoles().side())
                                && textures.get("side").getAsString().equals(expectedTextures.get(0))
                                && textures.get("top").getAsString()
                                        .equals(profile.textureRoles().top())
                                && textures.get("top").getAsString().equals(expectedTextures.get(1))
                                && textures.get("bottom").getAsString()
                                        .equals(profile.textureRoles().top())
                                && textures.get("bottom").getAsString().equals(expectedTextures.get(1)),
                        "Beam item parent copied another family's material: " + block);
                JsonObject element = placedModel.getAsJsonArray("elements").get(0).getAsJsonObject();
                List<Integer> expectedBounds = role == ItemRole.VERTICAL_SLAB
                        ? List.of(0, 0, 0, 16, 16, 8)
                        : List.of(0, 0, 0, 16, 8, 8);
                JsonObject faces = element.getAsJsonObject("faces");
                helper.assertTrue(bounds(placedModel).equals(expectedBounds)
                                && List.of("north", "east", "south", "west").stream()
                                        .allMatch(face -> faces.getAsJsonObject(face)
                                                .get("texture").getAsString().equals("#side"))
                                && faces.getAsJsonObject("up").get("texture").getAsString()
                                        .equals("#top")
                                && faces.getAsJsonObject("down").get("texture").getAsString()
                                        .equals("#bottom")
                                && !faces.getAsJsonObject("north").has("uv")
                                && !faces.getAsJsonObject("south").has("uv")
                                && BeamItemModelContract.AUTHORITATIVE_SIDE_BAND_U >= 0
                                && BeamItemModelContract.AUTHORITATIVE_SIDE_BAND_U < 16,
                        "Beam broad vertical faces no longer retain the full 0..16 side frame "
                                + "containing the authored U=7 band: " + block);
                beamSides.add(textures.get("side").getAsString());
                beamEnds.add(textures.get("top").getAsString());
                beamItems++;
            } else {
                String expectedTarget = c92Targets.get(block);
                if (profile.canonicalParentId().toString().equals("enderscape:blinklamp")) {
                    expectedTarget += "_luminance4";
                }
                helper.assertTrue(target.equals(expectedTarget) && !target.contains("_bge_preview"),
                        "Catalog item did not restore its direct C92 model chain: "
                                + block + " -> " + target + " expected " + expectedTarget);
            }
            helper.assertTrue(definition.getAsJsonObject("model").has("tints"),
                    "Catalog restoration discarded an existing item tint payload: " + block);
            String source = profile.canonicalParentId().toString();
            if (source.equals("enderscape:alluring_magnia")
                    || source.equals("enderscape:repulsive_magnia")) fixedMagnia++;
            if (source.equals("enderscape:blinklamp")) blinklamp++;

            if (role == ItemRole.STAIRS && profile.canonicalParentId()
                    .equals(Identifier.parse("ribbits:red_toadstool"))) {
                assertGui(helper, effectiveGui(manager, target), 135, 0,
                        "Ribbits Red Toadstool Stair");
                stairC92 = true;
            }
            if (role == ItemRole.VERTICAL_SLAB && profile.canonicalParentId()
                    .equals(Identifier.parse("mynx_trees:wisteria_log"))) {
                assertGui(helper, effectiveGui(manager, target), -135, -1.75,
                        "Wisteria Log Vertical Slab");
                verticalC92 = true;
            }
            if (role == ItemRole.STEP && profile.canonicalParentId()
                    .equals(Identifier.parse("mynx_trees:wisteria_log"))) {
                assertGui(helper, effectiveGui(manager, target), -135, -1.75,
                        "Wisteria Log Step");
                stepC92 = true;
            }
            if (role == ItemRole.WALL && profile.canonicalParentId()
                    .equals(Identifier.parse("bbb:pale_oak_beam"))) {
                assertGui(helper, effectiveGui(manager, target), 225, 0,
                        "BBB Pale Oak Wall");
                paleOakWallC92 = true;
            }
            if (role == ItemRole.WALL
                    && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId())) {
                helper.assertTrue(target.equals(model(block) + "_post"),
                        "Private Beam Wall lost C93's corrected post-only item model: " + block);
                beamWalls++;
            }
        }
        helper.assertTrue(beamItems == 30 && beamWalls == 3
                        && beamSides.size() == 15 && beamEnds.size() == 15,
                "Did not cover every BBB/private Beam item/material independently: items="
                        + beamItems + " walls=" + beamWalls + " sides=" + beamSides.size()
                        + " ends=" + beamEnds.size());
        helper.assertTrue(fixedMagnia == 8 && blinklamp == 4,
                "C93's material-state item routes did not survive C92 presentation restoration: "
                        + fixedMagnia + "/" + blinklamp);
        helper.assertTrue(stairC92 && verticalC92 && stepC92 && paleOakWallC92,
                "Did not exercise every representative exact C92 display contract: stair="
                        + stairC92 + " vertical=" + verticalC92 + " step=" + stepC92
                        + " paleOakWall=" + paleOakWallC92);

        Map<Identifier, String> afterPlaced = placedResourceSnapshot(manager, expectedRoles.keySet());
        helper.assertTrue(afterPlaced.equals(beforePlaced),
                "Item-only restoration changed a C93 placed blockstate or selected model JSON");
        helper.succeed();
    }

    private static void putRole(GameTestHelper helper, Set<Item> actualVisible,
            Map<Identifier, ItemRole> roles, Map<Identifier, String> targets,
            Map<Identifier, String> selectors, NibaruMaterialProfile profile,
            Item item, ItemRole role) {
        helper.assertTrue(actualVisible.contains(item),
                "Expected item role is absent from the actual final ShapeMap: " + itemId(item));
        Block block = ((BlockItem) item).getBlock();
        Identifier id = id(block);
        ItemRole previous = roles.putIfAbsent(id, role);
        helper.assertTrue(previous == null || previous == role,
                "Actual ShapeMap item has conflicting catalog roles: " + id);
        String placed = role == ItemRole.WALL ? model(id) + "_post" : model(id);
        String target;
        if (role == ItemRole.WALL && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId())) {
            target = model(id) + "_post";
        } else if (role == ItemRole.WALL && id.getNamespace().equals("bbb")
                && id.getPath().endsWith("_wall")) {
            target = id.getNamespace() + ":item/" + id.getPath();
        } else {
            target = role == ItemRole.WALL ? model(id) + "_inventory" : model(id);
        }
        selectors.put(id, placed);
        targets.put(id, target);
    }

    private static ResourceManager catalogFixtureManager(ResourceManager delegate,
            Map<Identifier, ItemRole> roles, Map<Identifier, String> targets,
            Map<Identifier, String> placedSelectors) {
        Map<Identifier, String> json = new HashMap<>();
        for (Map.Entry<Identifier, String> entry : targets.entrySet()) {
            Identifier block = entry.getKey();
            ItemRole role = roles.get(block);
            String direct = entry.getValue();
            NibaruMaterialProfile profile = profileFor(block).orElseThrow();
            boolean privateBeamWall = role == ItemRole.WALL
                    && PrivateBeamFamilies.isPrivateBeam(profile.canonicalParentId());
            String historical = model(block) + "_bge_preview";
            String historicalParent = direct;
            if (profile.canonicalParentId().toString().equals("enderscape:blinklamp")) {
                historical += "_luminance4";
                historicalParent += "_luminance4";
            }
            json.put(itemResource(block), itemDefinition(
                    privateBeamWall ? direct : historical).toString());
            if (!privateBeamWall) {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty("parent", historicalParent);
                JsonObject c93Display = new JsonObject();
                c93Display.add("gui", gui(135, role == ItemRole.VERTICAL_SLAB
                        || role == ItemRole.STEP ? 1.75 : 0));
                wrapper.add("display", c93Display);
                json.put(modelResource(historical), wrapper.toString());
            }

            if (block.getNamespace().equals("bbb") && block.getPath().endsWith("_wall")) {
                String material = block.getPath().substring(0,
                        block.getPath().length() - "_wall".length());
                String wallModel = "bbb:block/wall/" + material;
                JsonObject itemModel = new JsonObject();
                itemModel.addProperty("parent", wallModel);
                json.put(modelResource(direct), itemModel.toString());
                json.put(modelResource(wallModel), displayModel(225, 0).toString());
            } else {
                double c92Yaw = role == ItemRole.VERTICAL_SLAB || role == ItemRole.STEP ? -135 : 135;
                json.put(modelResource(direct), displayModel(
                        c92Yaw,
                        role == ItemRole.VERTICAL_SLAB || role == ItemRole.STEP ? -1.75 : 0).toString());
                json.put(modelResource(historicalParent), displayModel(
                        c92Yaw,
                        role == ItemRole.VERTICAL_SLAB || role == ItemRole.STEP ? -1.75 : 0).toString());
            }
            json.put(blockStateResource(block),
                    ("{\"variants\":{\"\":{\"model\":\"%s\"}}}")
                            .formatted(placedSelectors.get(block)));
            json.putIfAbsent(modelResource(placedSelectors.get(block)), "{}");
        }
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Identifier parent = profile.canonicalParentId();
            json.putIfAbsent(itemResource(parent), itemDefinition(model(parent)).toString());
        }
        PackResources pack = (PackResources) Proxy.newProxyInstance(
                RenderingContractGameTests.class.getClassLoader(), new Class<?>[] {PackResources.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "packId" -> "bge-c94-rendering-fixtures";
                    case "knownPackInfo" -> Optional.empty();
                    case "getNamespaces" -> Set.of("minecraft", "clutternomore",
                            CnmTerrainCompat.MOD_ID, "enderscape", "bbb", "mynx_trees", "ribbits");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C94 rendering contract fixtures";
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
                    case "toString" -> "BGE C94 catalog restoration fixture manager";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected ResourceManager call " + method);
                });
    }

    private static JsonObject itemDefinition(String model) {
        JsonObject selected = new JsonObject();
        selected.addProperty("type", "minecraft:model");
        selected.addProperty("model", model);
        JsonObject tint = new JsonObject();
        tint.addProperty("type", "minecraft:constant");
        tint.addProperty("value", -1);
        JsonArray tints = new JsonArray();
        tints.add(tint);
        selected.add("tints", tints);
        JsonObject root = new JsonObject();
        root.add("model", selected);
        return root;
    }

    private static JsonObject displayModel(double yaw, double translationX) {
        JsonObject display = new JsonObject();
        display.add("gui", gui(yaw, translationX));
        JsonObject root = new JsonObject();
        root.add("display", display);
        return root;
    }

    private static JsonObject gui(double yaw, double translationX) {
        JsonObject gui = new JsonObject();
        gui.add("rotation", numbers(30, yaw, 0));
        gui.add("translation", numbers(translationX, 0, 0));
        gui.add("scale", numbers(0.625, 0.625, 0.625));
        return gui;
    }

    private static void assertGui(GameTestHelper helper, JsonObject gui,
            double yaw, double translationX, String label) {
        helper.assertTrue(gui != null
                        && values(gui.getAsJsonArray("rotation")).equals(List.of(30.0, yaw, 0.0))
                        && (gui.has("translation")
                                ? values(gui.getAsJsonArray("translation"))
                                : List.of(0.0, 0.0, 0.0))
                                .equals(List.of(translationX, 0.0, 0.0))
                        && values(gui.getAsJsonArray("scale"))
                                .equals(List.of(0.625, 0.625, 0.625)),
                label + " does not retain its exact C92 GUI transform: " + gui);
    }

    private static void assertFirstPerson(GameTestHelper helper, JsonObject display, String label) {
        for (String hand : List.of("firstperson_righthand", "firstperson_lefthand")) {
            JsonObject transform = display.getAsJsonObject(hand);
            helper.assertTrue(transform != null
                            && values(transform.getAsJsonArray("rotation"))
                                    .equals(List.of(0.0, -45.0, 0.0))
                            && values(transform.getAsJsonArray("scale"))
                                    .equals(List.of(0.4, 0.4, 0.4))
                            && !transform.has("translation"),
                    label + " does not retain C92's exact " + hand + " transform");
        }
    }

    private static List<Double> values(JsonArray array) {
        if (array == null) return List.of();
        List<Double> values = new ArrayList<>(array.size());
        array.forEach(value -> values.add(value.getAsDouble()));
        return List.copyOf(values);
    }

    private static JsonObject effectiveGui(ResourceManager manager, String initialModel) {
        String model = initialModel;
        Set<String> visited = new LinkedHashSet<>();
        for (int depth = 0; depth < 16 && visited.add(model); depth++) {
            JsonObject json = resolveJson(manager, modelResource(model));
            if (json == null) throw new IllegalStateException("Unresolved item model " + model);
            if (json.has("display") && json.getAsJsonObject("display").has("gui")) {
                return json.getAsJsonObject("display").getAsJsonObject("gui");
            }
            if (!json.has("parent")) break;
            model = json.get("parent").getAsString();
        }
        throw new IllegalStateException("No GUI display in item model chain " + initialModel);
    }

    private static Map<Identifier, String> placedResourceSnapshot(ResourceManager manager,
            Set<Identifier> blocks) {
        Map<Identifier, String> result = new LinkedHashMap<>();
        Set<String> selectedModels = new LinkedHashSet<>();
        for (Identifier block : blocks) {
            Identifier stateResource = blockStateResource(block);
            JsonObject state = resolveJson(manager, stateResource);
            if (state == null) throw new IllegalStateException("Unresolved blockstate " + stateResource);
            result.put(stateResource, state.toString());
            collectSelectedModels(state, selectedModels);
        }
        for (String selectedModel : selectedModels) {
            Identifier modelResource = modelResource(selectedModel);
            JsonObject model = resolveJson(manager, modelResource);
            if (model == null) throw new IllegalStateException("Unresolved placed model " + modelResource);
            result.put(modelResource, model.toString());
        }
        return Map.copyOf(result);
    }

    private static void collectSelectedModels(JsonElement element, Set<String> result) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("model") && object.get("model").isJsonPrimitive()) {
                result.add(object.get("model").getAsString());
            }
            object.entrySet().forEach(entry -> collectSelectedModels(entry.getValue(), result));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectSelectedModels(child, result));
        }
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

    private static List<String> exactBeamTextures(Identifier canonicalParent) {
        if (canonicalParent.getNamespace().equals("bbb")
                && canonicalParent.getPath().endsWith("_beam")) {
            String material = canonicalParent.getPath().substring(0,
                    canonicalParent.getPath().length() - "_beam".length());
            String side = "bbb:block/beam/" + material;
            return List.of(side, side + "_top");
        }
        if (canonicalParent.getNamespace().equals(CnmTerrainCompat.MOD_ID)
                && canonicalParent.getPath().startsWith("enderscape/")
                && canonicalParent.getPath().endsWith("_beam")) {
            String family = canonicalParent.getPath().substring("enderscape/".length(),
                    canonicalParent.getPath().length() - "_beam".length());
            String side = CnmTerrainCompat.MOD_ID
                    + ":block/private/enderscape/" + family + "_beam";
            return List.of(side, side + "_top");
        }
        throw new IllegalStateException("No exact Beam texture contract for " + canonicalParent);
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
