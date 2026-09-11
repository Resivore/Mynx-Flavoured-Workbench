package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
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

/** Production-lifecycle coverage for C65's exact 76 source / 684 relation contract. */
public final class ExternalMaterialFamilyGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void exactAllowlistAndProviderCompletionInventory(GameTestHelper helper) {
        helper.assertTrue(ExternalMaterialCatalog.specs().size() == 76,
                "External source allowlist is not exactly 76");
        helper.assertTrue(ExternalMaterialCatalog.sourceCount("mcwpaths") == 57
                        && ExternalMaterialCatalog.sourceCount("mynx_trees") == 6
                        && ExternalMaterialCatalog.sourceCount("ribbits") == 1
                        && ExternalMaterialCatalog.sourceCount("bbb") == 12,
                "Provider source partition is not 57/6/1/12");
        helper.assertTrue(ExternalMaterialFamilies.all().size() == 76,
                "Late provider completion did not register all 76 families: "
                        + ExternalMaterialFamilies.all().size());
        helper.assertTrue(NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() != null).count() == 311
                        && NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() == null
                                && !profile.canonicalParentId().getNamespace().equals("minecraft")).count() == 76,
                "External append changed the frozen native inventory or lost an external source");

        Set<Identifier> actual = new LinkedHashSet<>();
        ExternalMaterialFamilies.all().forEach(binding -> actual.add(binding.spec().id()));
        Set<Identifier> expected = new LinkedHashSet<>();
        ExternalMaterialCatalog.specs().forEach(spec -> expected.add(spec.id()));
        helper.assertTrue(actual.equals(expected), "Registered external sources differ from exact allowlist");
        helper.assertTrue(actual.stream().filter(id -> id.getNamespace().equals("mcwpaths"))
                        .allMatch(ExternalMaterialFamilyGameTests::isRequestedMacawSource),
                "Macaw family is outside the 52 full-pattern plus five plain-Path scope");
        System.out.println("EXTERNAL_C65_INVENTORY|sources=76|mcwpaths=57|mynx_trees=6|ribbits=1|bbb=12|relations=684");
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
        Set<Block> canonicalDerived = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Set<Block> bgeGenerated = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
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
                if (!role.getKey().equals("block")) helper.assertTrue(canonicalDerived.add(role.getValue()),
                        "Canonical derived geometry was reused across source variants: " + actualId);
            }
            for (Block block : binding.generated()) helper.assertTrue(bgeGenerated.add(block),
                    "BGE-generated geometry was reused across source variants: "
                            + BuiltInRegistries.BLOCK.getKey(block));

            List<Item> component = ShapeMap.getShapes(binding.source().asItem());
            List<Item> expected = roles.values().stream().map(Block::asItem).toList();
            int sourceIndex = component.indexOf(binding.source().asItem());
            boolean exactSegment = sourceIndex >= 0 && sourceIndex + expected.size() <= component.size()
                    && component.subList(sourceIndex, sourceIndex + expected.size()).equals(expected);
            helper.assertTrue(exactSegment, "ShapeMap relation lacks the exact ordered nine-role segment for "
                    + binding.spec().id() + ": " + component.stream().map(BuiltInRegistries.ITEM::getKey).toList());
            relations += roles.size();
        }
        CanonicalShapeMapAudit.Report audit = CanonicalShapeMapAudit.inspectExternalFamilies();
        helper.assertTrue(relations == 684 && canonicalDerived.size() == 608 && bgeGenerated.size() == 466,
                "C65 relation/canonical/generated identity count mismatch: " + relations + "/"
                        + canonicalDerived.size() + "/" + bgeGenerated.size());
        helper.assertTrue(audit.variantCount() == 76 && audit.missing().isEmpty()
                        && audit.duplicates().isEmpty(),
                "Live ShapeMap canonical variant/role audit failed: " + audit);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void providerRolesAreReusedAndDuplicateDetectorKeysByVariantAndRole(GameTestHelper helper) {
        int reused = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (Map.Entry<String, Identifier> role : binding.spec().providerRoles().entrySet()) {
                Block selected = binding.roles().get(role.getKey());
                helper.assertTrue(BuiltInRegistries.BLOCK.getKey(selected).equals(role.getValue()),
                        "Provider-native role was not selected: " + binding.spec().id() + " " + role);
                Identifier generated = ExternalMaterialFamilies.id(binding.spec(), role.getKey());
                Block collision = BuiltInRegistries.BLOCK.getValue(generated);
                helper.assertTrue(!generated.equals(BuiltInRegistries.BLOCK.getKey(collision)),
                        "Equivalent BGE standard role was also registered: " + generated);
                reused++;
            }
        }
        helper.assertTrue(reused == 142, "Expected 142 reused provider roles, found " + reused);

        Identifier family = Identifier.parse("mynx_trees:wisteria_log");
        CanonicalShapeMapAudit.CanonicalKey logSlab = new CanonicalShapeMapAudit.CanonicalKey(
                family, Identifier.parse("mynx_trees:wisteria_log"), CanonicalShapeMapAudit.Role.SLAB);
        CanonicalShapeMapAudit.CanonicalKey woodSlab = new CanonicalShapeMapAudit.CanonicalKey(
                family, Identifier.parse("mynx_trees:wisteria_wood"), CanonicalShapeMapAudit.Role.SLAB);
        helper.assertTrue(CanonicalShapeMapAudit.duplicates(List.of(
                new CanonicalShapeMapAudit.Member(logSlab, Identifier.parse("test:first_log_slab")),
                new CanonicalShapeMapAudit.Member(woodSlab, Identifier.parse("test:wood_slab")))).isEmpty(),
                "Distinct Log and Wood Slabs were incorrectly classified as duplicates");
        helper.assertTrue(CanonicalShapeMapAudit.duplicates(List.of(
                new CanonicalShapeMapAudit.Member(logSlab, Identifier.parse("test:first_log_slab")),
                new CanonicalShapeMapAudit.Member(logSlab, Identifier.parse("test:second_log_slab")))).size() == 1,
                "Second equivalent Log Slab did not trigger canonical duplicate detection");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sharedLogWoodComponentsPreserveBothVariantsIndependently(GameTestHelper helper) {
        for (String stem : List.of("wisteria", "silver_birch")) {
            ExternalMaterialFamilies.Binding log = ExternalMaterialFamilies.fromSource(
                    Identifier.parse("mynx_trees:" + stem + "_log")).orElseThrow();
            ExternalMaterialFamilies.Binding wood = ExternalMaterialFamilies.fromSource(
                    Identifier.parse("mynx_trees:" + stem + "_wood")).orElseThrow();
            List<Item> component = ShapeMap.getShapes(log.source().asItem());
            helper.assertTrue(component == ShapeMap.getShapes(wood.source().asItem()),
                    "Log and Wood no longer share the established ShapeMap family: " + stem);
            for (ExternalMaterialFamilies.Binding variant : List.of(log, wood)) {
                for (Map.Entry<String, Block> role : variant.roles().entrySet()) {
                    long occurrences = component.stream().filter(item -> item == role.getValue().asItem()).count();
                    helper.assertTrue(occurrences == 1, "Shared " + stem + " family has " + occurrences
                            + " entries for " + variant.spec().id() + " " + role.getKey());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void liveSourcePropertiesFireTagsLeavesAndAxesPropagate(GameTestHelper helper) {
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            Block source = binding.source();
            // BBB owns its directional slab/stair/wall forms.  BGE deliberately derives
            // later forms from those authoritative provider blocks, so their inherited
            // construction properties are outside this source-copy assertion.
            if (!binding.spec().provider().equals("bbb")) {
                for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                    if (!binding.isGeneratedRole(role.getKey())) continue;
                    Block block = role.getValue();
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
                if (sourceFire != null) for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                    if (!binding.isGeneratedRole(role.getKey())) continue;
                    Block block = role.getValue();
                    FlammableBlockRegistry.Entry derivedFire = FlammableBlockRegistry.getDefaultInstance().get(block);
                    helper.assertTrue(sourceFire.equals(derivedFire),
                            "Exact fire odds did not propagate to " + BuiltInRegistries.BLOCK.getKey(block));
                }

                if (binding.profile().capabilities().contains(BehaviorCapability.LEAF_LIFECYCLE)) {
                    for (Block block : binding.canonicalDerived()) helper.assertTrue(
                            block instanceof LeafDistanceCarrier
                                    && block.defaultBlockState().hasProperty(BlockStateProperties.DISTANCE)
                                    && block.defaultBlockState().hasProperty(BlockStateProperties.PERSISTENT),
                            "Leaf lifecycle state missing from " + BuiltInRegistries.BLOCK.getKey(block));
                }
            }
            if (binding.source().defaultBlockState().hasProperty(BlockStateProperties.AXIS)) {
                for (String role : List.of("vertical_slab", "step", "corner", "quarter_column", "layer")) {
                    Block block = binding.roles().get(role);
                    helper.assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                            "Log/wood material axis missing from " + BuiltInRegistries.BLOCK.getKey(block));
                }
                helper.assertTrue(!binding.wall().defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                        "Wall inherited inappropriate material AXIS state: " + BuiltInRegistries.BLOCK.getKey(binding.wall()));
            }
        }
        ExternalMaterialFamilies.Binding silverLeaves = ExternalMaterialFamilies.fromSource(
                Identifier.parse("mynx_trees:silver_birch_leaves")).orElseThrow();
        helper.assertTrue(silverLeaves.canonicalDerived().stream().allMatch(block ->
                        block.defaultBlockState().is(BlockTags.LEAVES)),
                "Silver Birch leaf forms are not leaf-tagged");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bbbBeamsReuseOnlyAuthoritativeStandardFormsAndKeepAxisAwareBgeForms(GameTestHelper helper) {
        List<String> materials = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");
        for (String material : materials) {
            ExternalMaterialFamilies.Binding beam = external("bbb:" + material + "_beam");
            helper.assertTrue(beam.spec().providerRoles().equals(Map.of(
                            "slab", Identifier.parse("bbb:" + material + "_beam_slab"),
                            "stairs", Identifier.parse("bbb:" + material + "_beam_stairs"),
                            "wall", Identifier.parse("bbb:" + material + "_wall"))),
                    "BBB standard mapping drifted for " + material);
            for (String role : List.of("slab", "stairs", "wall")) {
                helper.assertTrue(!beam.isGeneratedRole(role)
                                && BuiltInRegistries.BLOCK.getKey(beam.roles().get(role))
                                        .equals(beam.spec().providerRoles().get(role)),
                        "BGE duplicated the BBB " + role + " for " + material);
            }
            helper.assertTrue(!beam.wall().defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                    "BBB wooden wall received an inappropriate axis state for " + material);
            for (String role : List.of("vertical_slab", "step", "corner", "quarter_column", "layer")) {
                Block block = beam.roles().get(role);
                helper.assertTrue(beam.isGeneratedRole(role)
                                && block.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                        "Axis-aware BGE beam " + role + " missing for " + material);
            }
        }
        helper.succeed();
    }

    /**
     * Production BBB 2.0pre4 beam resources use z=x90,y180, not the former test fixture's
     * z=x90,y0. Keep this structural fixture free of BBB assets while exercising the exact
     * ResourceManager parsing/classification seam that writes BGE client resources.
     */
    @GameTest(maxTicks = 40)
    public void productionBbbBeamAxisResourcesClassifyThroughClientResourceSeam(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        for (String material : bbbBeamMaterials()) {
            Identifier parent = Identifier.fromNamespaceAndPath("bbb", material + "_beam");
            Identifier resource = Identifier.fromNamespaceAndPath("bbb",
                    "blockstates/" + material + "_beam.json");
            try (var input = manager.getResource(resource).orElseThrow().open();
                    var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonObject blockState = JsonParser.parseReader(reader).getAsJsonObject();
                helper.assertTrue(AxisModelContract.uvPolicy(parent, blockState) == AxisUvPolicy.STANDARD_ROTATED,
                        "Production BBB beam axis layout was not accepted: " + parent);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot classify production BBB beam resource " + parent, exception);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void lateServerDataResourcesCloseEveryStandardFamily(GameTestHelper helper) {
        int loot = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (String role : List.of("slab", "stairs", "wall", "vertical_slab", "step")) {
                if (!binding.isGeneratedRole(role)) continue;
                Identifier id = BuiltInRegistries.BLOCK.getKey(binding.roles().get(role));
                JsonObject table = generatedServerJson(Identifier.fromNamespaceAndPath(id.getNamespace(),
                        "loot_table/blocks/" + id.getPath() + ".json"));
                helper.assertTrue("minecraft:block".equals(table.get("type").getAsString()),
                        "Generated loot table has wrong type: " + id);
                loot++;
            }
        }
        JsonObject walls = generatedServerJson(Identifier.parse("minecraft:tags/block/walls.json"));
        helper.assertTrue(walls.getAsJsonArray("values").size() == 76,
                "External wall classification does not contain every scoped full-parent family");
        helper.assertTrue(loot == 238, "Expected 238 BGE-owned external loot tables, found " + loot);
        System.out.println("EXTERNAL_C65_SERVER_RESOURCES|standardLoot=238|wallTags=76|materialFamilies=76");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void actualClientWritersCloseAll430BgeOwnedGeometryResources(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        LayerGeneratedResources.GenerationSummary layers =
                LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.GenerationSummary quarters =
                QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialGeneratedResources.GenerationSummary standard =
                ExternalMaterialGeneratedResources.generate(manager);
        helper.assertTrue(layers.familyCount() == 76
                        && quarters.cornerFamilyCount() == 76
                        && quarters.columnFamilyCount() == 76
                        && standard.familyCount() == 76
                        && standard.blockStateCount() == 238
                        && standard.itemCount() == 238,
                "External client writers did not process every exact family/role");

        int generatedRelations = 0;
        int resolvedModelReferences = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                if (role.getKey().equals("block")) continue;
                if (!binding.isGeneratedRole(role.getKey())) continue;
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
        helper.assertTrue(generatedRelations == 466 && resolvedModelReferences >= 466,
                "External client resource closure mismatch: relations=" + generatedRelations
                        + ", modelReferences=" + resolvedModelReferences);
        System.out.println("EXTERNAL_C65_CLIENT_RESOURCES|generatedRelations=466|blockstates=466|items=466"
                + "|resolvedModelReferences=" + resolvedModelReferences);
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void logWoodAndLeafWallsUseAcceptedNormalWallResources(GameTestHelper helper) {
        ExternalMaterialGeneratedResources.generate(clientFixtureManager());
        for (String stem : List.of("wisteria", "silver_birch")) {
            ExternalMaterialFamilies.Binding log = external("mynx_trees:" + stem + "_log");
            ExternalMaterialFamilies.Binding wood = external("mynx_trees:" + stem + "_wood");
            assertNormalWallState(helper, log);
            assertNormalWallState(helper, wood);
            assertWallModel(helper, log, "_post",
                    "more_slabs_stairs_and_walls:block/template_column_wall_post");
            assertWallModel(helper, log, "_side",
                    "more_slabs_stairs_and_walls:block/template_column_wall_side");
            assertWallModel(helper, log, "_side_tall",
                    "more_slabs_stairs_and_walls:block/template_column_wall_side_tall");
            assertWallModel(helper, log, "_inventory",
                    "more_slabs_stairs_and_walls:block/template_column_wall_inventory");
            JsonObject logPost = wallModel(log, "_post");
            helper.assertTrue(logPost.getAsJsonObject("textures").get("side").getAsString()
                            .equals(log.profile().textureRoles().side())
                            && logPost.getAsJsonObject("textures").get("top").getAsString()
                            .equals(log.profile().textureRoles().top())
                            && logPost.getAsJsonObject("textures").get("bottom").getAsString()
                            .equals(log.profile().textureRoles().bottom()),
                    "Log wall did not preserve bark/end-grain texture roles: " + log.spec().id());

            for (String suffix : List.of("_post", "_side", "_side_tall", "_inventory")) {
                assertWallModel(helper, wood, suffix, switch (suffix) {
                    case "_post" -> "more_slabs_stairs_and_walls:block/template_column_wall_post";
                    case "_side" -> "more_slabs_stairs_and_walls:block/template_column_wall_side";
                    case "_side_tall" -> "more_slabs_stairs_and_walls:block/template_column_wall_side_tall";
                    default -> "more_slabs_stairs_and_walls:block/template_column_wall_inventory";
                });
                JsonObject woodModel = wallModel(wood, suffix);
                helper.assertTrue(woodModel.getAsJsonObject("textures").get("side").getAsString()
                                .equals(wood.profile().textureRoles().side())
                                && woodModel.getAsJsonObject("textures").get("top").getAsString()
                                .equals(wood.profile().textureRoles().side())
                                && woodModel.getAsJsonObject("textures").get("bottom").getAsString()
                                .equals(wood.profile().textureRoles().side()),
                        "Wood wall stopped using bark on every face: " + wood.spec().id());
            }
        }

        ExternalMaterialFamilies.Binding silver = external("mynx_trees:silver_birch_leaves");
        ExternalMaterialFamilies.Binding wisteria = external("mynx_trees:wisteria_leaves");
        for (ExternalMaterialFamilies.Binding leaves : List.of(silver, wisteria)) {
            assertNormalWallState(helper, leaves);
            assertWallModel(helper, leaves, "_post",
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_post");
            assertWallModel(helper, leaves, "_side",
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side");
            assertWallModel(helper, leaves, "_side_tall",
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side_tall");
            assertWallModel(helper, leaves, "_inventory",
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_inventory");
        }
        JsonObject silverItem = generatedClientJson(itemResource(silver.wall()));
        JsonObject wisteriaItem = generatedClientJson(itemResource(wisteria.wall()));
        helper.assertTrue(silverItem.getAsJsonObject("model").getAsJsonArray("tints")
                        .get(0).getAsJsonObject().get("value").getAsInt() == -8034015,
                "Silver Birch Leaves wall inventory tint was not inherited from the provider");
        helper.assertTrue(!wisteriaItem.getAsJsonObject("model").has("tints"),
                "Untinted Wisteria Leaves wall gained an inventory tint");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void axisAlignedExternalItemsReuseNativeCnmPreviewModels(GameTestHelper helper) {
        ExternalMaterialGeneratedResources.generate(clientFixtureManager());
        for (String stem : List.of("wisteria", "silver_birch")) {
            for (String variant : List.of("log", "wood")) {
                ExternalMaterialFamilies.Binding binding = external("mynx_trees:" + stem + "_" + variant);
                assertAxisItemPreview(helper, binding, "vertical_slab",
                        "clutternomore:block/templates/vertical_slab", 24);
                assertAxisItemPreview(helper, binding, "step",
                        "clutternomore:block/templates/step", 36);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void macawPatternAndPlainPathParentsRemainSemanticallyDistinct(GameTestHelper helper) {
        int patterns = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            if (!binding.spec().provider().equals("mcwpaths")) continue;
            String path = binding.spec().id().getPath();
            if (isPlainMacawPath(path)) {
                helper.assertTrue(binding.source() == BuiltInRegistries.BLOCK.getValue(binding.spec().id())
                                && binding.spec().providerReference().equals(binding.spec().id())
                                && binding.spec().providerRoles().isEmpty(),
                        "Plain Macaw Path is not its own canonical source: " + binding.spec().id());
                helper.assertTrue(binding.generatedRoles().size() == 8,
                        "Plain Macaw Path does not own all eight derived BGE roles: " + binding.spec().id());
                Block vanilla = switch (path) {
                    case "podzol_path_block" -> Blocks.PODZOL;
                    case "dirt_path_block" -> Blocks.DIRT;
                    case "gravel_path_block" -> Blocks.GRAVEL;
                    case "sand_path_block" -> Blocks.SAND;
                    case "red_sand_path_block" -> Blocks.RED_SAND;
                    default -> throw new IllegalArgumentException(path);
                };
                helper.assertTrue(ShapeMap.getParent(binding.source().asItem())
                                != ShapeMap.getParent(vanilla.asItem()),
                        "Plain Macaw Path was aliased into the vanilla soil family: " + binding.spec().id());
            } else {
                helper.assertTrue(!path.endsWith("_path")
                                && binding.spec().providerReference().getPath().equals(path + "_path")
                                && BuiltInRegistries.BLOCK.getKey(binding.slab()).getNamespace().equals("mcwpaths")
                                && BuiltInRegistries.BLOCK.getKey(binding.stairs()).getNamespace().equals("mcwpaths")
                                && binding.isGeneratedRole("wall")
                                && !binding.isGeneratedRole("slab") && !binding.isGeneratedRole("stairs"),
                        "Patterned Macaw family did not retain full source plus provider-native standard roles: "
                                + binding.spec().id());
                patterns++;
            }
        }
        helper.assertTrue(patterns == 52, "Expected 52 full patterned Macaw parents, found " + patterns);
        helper.succeed();
    }

    private static ExternalMaterialFamilies.Binding external(String id) {
        return ExternalMaterialFamilies.fromSource(Identifier.parse(id)).orElseThrow();
    }

    private static void assertNormalWallState(GameTestHelper helper,
            ExternalMaterialFamilies.Binding binding) {
        helper.assertTrue(binding.wall() instanceof WallBlock
                        && !binding.wall().defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                "External wall is not an ordinary no-AXIS WallBlock: " + binding.spec().id());
        JsonObject state = generatedClientJson(blockStateResource(binding.wall()));
        helper.assertTrue(state.has("multipart") && !state.has("variants"),
                "External wall did not use normal multipart state: " + binding.spec().id());
    }

    private static void assertWallModel(GameTestHelper helper,
            ExternalMaterialFamilies.Binding binding, String suffix, String parent) {
        helper.assertTrue(wallModel(binding, suffix).get("parent").getAsString().equals(parent),
                "Unexpected wall model parent for " + binding.spec().id() + suffix);
    }

    /** Verifies the item-only native preview is not substituted into the axis-aware world map. */
    private static void assertAxisItemPreview(GameTestHelper helper,
            ExternalMaterialFamilies.Binding binding, String role, String parent, int selectorCount) {
        Block block = binding.roles().get(role);
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String preview = id.getNamespace() + ":block/" + id.getPath();
        JsonObject item = generatedClientJson(itemResource(block));
        helper.assertTrue(item.getAsJsonObject("model").get("model").getAsString().equals(preview),
                "Axis item did not resolve to the deterministic CNM preview model: " + id);
        JsonObject previewModel = generatedClientJson(Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/block/" + id.getPath() + ".json"));
        helper.assertTrue(previewModel.get("parent").getAsString().equals(parent),
                "Axis item did not reuse the established CNM preview template: " + id);

        JsonObject variants = generatedClientJson(blockStateResource(block)).getAsJsonObject("variants");
        helper.assertTrue(variants.size() == selectorCount
                        && variants.entrySet().stream().allMatch(entry -> entry.getKey().contains("axis=")
                                && !entry.getValue().getAsJsonObject().get("model").getAsString().equals(preview)),
                "Axis world selectors changed or reused the item-only preview: " + id);
    }

    private static JsonObject wallModel(ExternalMaterialFamilies.Binding binding, String suffix) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(binding.wall());
        return generatedClientJson(Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/block/" + id.getPath() + suffix + ".json"));
    }

    private static Identifier blockStateResource(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
    }

    private static Identifier itemResource(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
    }

    private static boolean isPlainMacawPath(String path) {
        return Set.of("podzol_path_block", "dirt_path_block", "gravel_path_block",
                "sand_path_block", "red_sand_path_block").contains(path);
    }

    private static boolean isRequestedMacawSource(Identifier id) {
        if (isPlainMacawPath(id.getPath())) return true;
        String path = id.getPath();
        return List.of("running_bond", "windmill_weave", "flagstone", "crystal_floor").stream()
                .anyMatch(pattern -> path.endsWith("_" + pattern));
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
        for (String material : bbbBeamMaterials()) {
            String path = material + "_beam";
            String model = "bbb:block/beam/" + material;
            json.put(Identifier.fromNamespaceAndPath("bbb", "blockstates/" + path + ".json"),
                    "{\"variants\":{\"axis=x\":{\"model\":\"" + model
                            + "\",\"x\":90,\"y\":90},\"axis=y\":{\"model\":\"" + model
                            + "\"},\"axis=z\":{\"model\":\"" + model
                            + "\",\"x\":90,\"y\":180}}}");
        }
        json.put(Identifier.parse("mynx_trees:items/silver_birch_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"mynx_trees:block/silver_birch_leaves\","
                        + "\"tints\":[{\"type\":\"minecraft:constant\",\"value\":-8034015}]}}" );
        json.put(Identifier.parse("mynx_trees:items/wisteria_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"mynx_trees:block/wisteria_leaves\"}}" );

        PackResources pack = (PackResources) Proxy.newProxyInstance(
                ExternalMaterialFamilyGameTests.class.getClassLoader(),
                new Class<?>[] {PackResources.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "packId" -> "bge-c65-client-fixtures";
                    case "knownPackInfo" -> Optional.empty();
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C65 client fixture pack";
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
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb");
                    case "listResources" -> resources.entrySet().stream()
                            .filter(entry -> entry.getKey().getPath().startsWith((String) args[0]))
                            .filter(entry -> ((Predicate<Identifier>) args[1]).test(entry.getKey()))
                            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    case "listResourceStacks" -> Map.of();
                    case "listPacks" -> Stream.of(pack);
                    case "toString" -> "BGE C65 client fixture manager";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected ResourceManager call " + method);
                });
    }

    private static List<String> bbbBeamMaterials() {
        return List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");
    }

    @Override public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException { method.invoke(this, helper); }
}
