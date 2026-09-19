package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.aero.cnmterraincompat.AxisModelContract.AxisUvPolicy;
import dev.aero.cnmterraincompat.client.ExternalMaterialGeneratedResources;
import dev.aero.cnmterraincompat.client.LayerGeneratedResources;
import dev.aero.cnmterraincompat.client.QuarterGeometryGeneratedResources;
import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Production-lifecycle coverage for the exact optional-provider catalog. */
public final class ExternalMaterialFamilyGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void ribbitsToadstoolProfilesUseUniformAssignedTextureAcrossNineRoles(
            GameTestHelper helper) {
        List<Identifier> sources = List.of(Identifier.parse("ribbits:red_toadstool"),
                Identifier.parse("ribbits:brown_toadstool"), Identifier.parse("ribbits:toadstool_stem"));
        helper.assertTrue(ExternalMaterialCatalog.sourceCount("ribbits") == 4,
                "Ribbits provider catalog must contain mossy oak plus three huge-toadstools");
        for (Identifier sourceId : sources) {
            ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(sourceId).orElseThrow();
            String assignedTexture = "ribbits:block/" + sourceId.getPath();
            helper.assertTrue(binding.source() instanceof HugeMushroomBlock
                            && binding.profile().visualProfile() == VisualProfile.UNIFORM
                            && binding.profile().orientationPolicy()
                                    == games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile.OrientationPolicy.UNIFORM
                            && binding.profile().textureRoles().side().equals(assignedTexture)
                            && binding.profile().textureRoles().top().equals(assignedTexture)
                            && binding.profile().textureRoles().bottom().equals(assignedTexture)
                            && binding.profile().textureRoles().particle().equals(assignedTexture)
                            && binding.profile().textureRoles().interior().isEmpty()
                            && binding.roles().size() == 9,
                    "Ribbits toadstool profile is not uniformly skinned by its assigned texture: "
                            + sourceId);
            helper.assertTrue(binding.roles().values().stream().distinct().count() == 9,
                    "Ribbits toadstool roles were merged across material identities: " + sourceId);
            for (Block derived : binding.canonicalDerived()) {
                helper.assertTrue(!derived.getClass().getSimpleName().startsWith("HugeMushroom"),
                        "Generated Ribbits geometry still uses directional HugeMushroom state: "
                                + BuiltInRegistries.BLOCK.getKey(derived));
            }
            BlockState completed = BgeMaterialBindings.projectToCanonical(binding.slab().defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.DOUBLE)).orElseThrow();
            helper.assertTrue(completed.is(binding.source()),
                    "Completed Ribbits toadstool slab lost its canonical source: " + sourceId);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void ribbitsToadstoolGeneratedModelsNeverReferenceInteriorTexture(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        for (String source : List.of("red_toadstool", "brown_toadstool", "toadstool_stem")) {
            Identifier providerModel = Identifier.fromNamespaceAndPath("ribbits",
                    "models/block/" + source + ".json");
            JsonObject canonical = resourceJson(manager, providerModel);
            helper.assertTrue(canonical.toString().contains("toadstool_inside"),
                    "Ribbits validation fixture does not contain the canonical provider model route: "
                            + providerModel);
        }
        LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialGeneratedResources.generate(manager);

        int generatedRoles = 0;
        int checkedModels = 0;
        for (String source : List.of("red_toadstool", "brown_toadstool", "toadstool_stem")) {
            ExternalMaterialFamilies.Binding binding = external("ribbits:" + source);
            String assignedTexture = "ribbits:block/" + source;
            Block layer = binding.roles().get("layer");
            JsonObject layerVariants = generatedClientJson(blockStateResource(layer))
                    .getAsJsonObject("variants");
            Set<String> legacyFullModels = new LinkedHashSet<>();
            for (Direction facing : Direction.values()) {
                legacyFullModels.add(layerVariants.getAsJsonObject(
                        "facing=" + facing.getSerializedName() + ",layers=4")
                        .get("model").getAsString());
            }
            helper.assertTrue(legacyFullModels.size() == 1
                            && !legacyFullModels.contains("ribbits:block/" + source)
                            && legacyFullModels.iterator().next().endsWith("_4_full"),
                    "Legacy full Ribbits Layer reused the present provider canonical model: "
                            + source + " -> " + legacyFullModels);
            String legacyFullModel = legacyFullModels.iterator().next();
            assertOnlyAssignedToadstoolTexture(helper, generatedClientJson(
                    modelResource(legacyFullModel)), assignedTexture,
                    source + "/layer/legacy-full");
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                if (!binding.isGeneratedRole(role.getKey())) continue;
                Set<String> models = new LinkedHashSet<>();
                collectModelReferences(generatedClientJson(blockStateResource(role.getValue())), models);
                collectModelReferences(generatedClientJson(itemResource(role.getValue())), models);
                helper.assertTrue(!models.isEmpty(),
                        "Generated Ribbits role has no model references: " + source + "/" + role.getKey());
                for (String modelId : models) {
                    Identifier model = Identifier.parse(modelId);
                    JsonObject json = generatedClientJson(Identifier.fromNamespaceAndPath(
                            model.getNamespace(), "models/" + model.getPath() + ".json"));
                    assertOnlyAssignedToadstoolTexture(helper, json, assignedTexture,
                            source + "/" + role.getKey() + "/" + modelId);
                    checkedModels++;
                }
                generatedRoles++;
            }
        }
        helper.assertTrue(generatedRoles == 24 && checkedModels >= generatedRoles,
                "Did not inspect every generated Ribbits toadstool geometry role: roles="
                        + generatedRoles + ", models=" + checkedModels);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void exactAllowlistAndProviderCompletionInventory(GameTestHelper helper) {
        helper.assertTrue(ExternalMaterialCatalog.specs().size() == 79,
                "External source allowlist is not exactly 79");
        helper.assertTrue(ExternalMaterialCatalog.sourceCount("mcwpaths") == 57
                        && ExternalMaterialCatalog.sourceCount("mynx_trees") == 6
                        && ExternalMaterialCatalog.sourceCount("ribbits") == 4
                        && ExternalMaterialCatalog.sourceCount("bbb") == 12,
                "Provider source partition is not 57/6/4/12");
        helper.assertTrue(ExternalMaterialFamilies.all().size() == 79,
                "Late provider completion did not register all 79 families: "
                        + ExternalMaterialFamilies.all().size());
        helper.assertTrue(NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() != null).count() == 311
                        && NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() == null
                                && !profile.canonicalParentId().getNamespace().equals("minecraft")).count() == 79,
                "External append changed the frozen native inventory or lost an external source");

        Set<Identifier> actual = new LinkedHashSet<>();
        ExternalMaterialFamilies.all().forEach(binding -> actual.add(binding.spec().id()));
        Set<Identifier> expected = new LinkedHashSet<>();
        ExternalMaterialCatalog.specs().forEach(spec -> expected.add(spec.id()));
        helper.assertTrue(actual.equals(expected), "Registered external sources differ from exact allowlist");
        helper.assertTrue(actual.stream().filter(id -> id.getNamespace().equals("mcwpaths"))
                        .allMatch(ExternalMaterialFamilyGameTests::isRequestedMacawSource),
                "Macaw family is outside the 52 full-pattern plus five plain-Path scope");
        System.out.println("EXTERNAL_C74_INVENTORY|sources=79|mcwpaths=57|mynx_trees=6|ribbits=4|bbb=12|relations=711");
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
    public void everySourceHasExactCanonicalNineRoleShapeMapFamily(GameTestHelper helper) {
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
            boolean exactMembership = expected.stream().allMatch(item ->
                    java.util.Collections.frequency(component, item) == 1);
            helper.assertTrue(exactMembership, "ShapeMap relation lacks exact canonical nine-role membership for "
                    + binding.spec().id() + ": " + component.stream().map(BuiltInRegistries.ITEM::getKey).toList());
            relations += roles.size();
        }
        CanonicalShapeMapAudit.Report audit = CanonicalShapeMapAudit.inspectExternalFamilies();
        helper.assertTrue(relations == 711 && canonicalDerived.size() == 632 && bgeGenerated.size() == 514,
                "C69 relation/canonical/generated identity count mismatch: " + relations + "/"
                        + canonicalDerived.size() + "/" + bgeGenerated.size());
        helper.assertTrue(audit.variantCount() == 79 && audit.missing().isEmpty()
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
        helper.assertTrue(reused == 118, "Expected 118 reused provider roles, found " + reused);

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

    @GameTest(maxTicks = 80)
    public void bbbBeamCanonicalSlabsAndStairsHaveIndependentMaterialAxes(GameTestHelper helper)
            throws ReflectiveOperationException {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos target = new BlockPos(2, 1, 2);
        int slabStates = 0;
        int stairStates = 0;
        for (String material : bbbBeamMaterials()) {
            ExternalMaterialFamilies.Binding beam = external("bbb:" + material + "_beam");
            Identifier providerSlab = Identifier.parse("bbb:" + material + "_beam_slab");
            Identifier providerStairs = Identifier.parse("bbb:" + material + "_beam_stairs");
            Identifier providerWall = Identifier.parse("bbb:" + material + "_wall");
            helper.assertTrue(beam.spec().providerRoles().equals(Map.of("wall", providerWall)),
                    "BBB Beam Slab/Stair must not be selected as a canonical provider role: " + material);
            helper.assertTrue(beam.isGeneratedRole("slab") && beam.isGeneratedRole("stairs")
                            && !beam.isGeneratedRole("wall")
                            && BuiltInRegistries.BLOCK.getKey(beam.slab())
                                    .equals(ExternalMaterialFamilies.id(beam.spec(), "slab"))
                            && BuiltInRegistries.BLOCK.getKey(beam.stairs())
                                    .equals(ExternalMaterialFamilies.id(beam.spec(), "stairs"))
                            && BuiltInRegistries.BLOCK.getKey(beam.wall()).equals(providerWall),
                    "BBB Beam canonical ownership drifted for " + material);
            helper.assertTrue(!BuiltInRegistries.BLOCK.getValue(providerSlab).defaultBlockState()
                            .hasProperty(BlockStateProperties.AXIS)
                            && !BuiltInRegistries.BLOCK.getValue(providerStairs).defaultBlockState()
                                    .hasProperty(BlockStateProperties.AXIS),
                    "Provider BBB Beam standard forms unexpectedly gained an independent AXIS: " + material);
            helper.assertTrue(beam.wall() instanceof WallBlock
                            && !beam.wall().defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                    "BBB Beam Wall must remain the provider-owned no-AXIS WallBlock: " + material);
            for (String role : List.of("slab", "stairs", "vertical_slab", "step", "corner",
                    "quarter_column", "layer")) {
                Block block = beam.roles().get(role);
                helper.assertTrue(block.defaultBlockState().hasProperty(BlockStateProperties.AXIS),
                        "Axis-aware BGE beam " + role + " missing for " + material);
            }

            SlabBlock slab = (SlabBlock) beam.slab();
            StairBlock stairs = (StairBlock) beam.stairs();
            assertBbbSlabStateContract(helper, slab, material, target, player);
            assertBbbStairStateContract(helper, stairs, material);
            assertBbbAxisModelContract(helper, beam, material);
            slabStates += Direction.Axis.values().length * SlabType.values().length;
            stairStates += Direction.Axis.values().length * 4 * Half.values().length
                    * StairsShape.values().length;
        }
        assertBbbStairNeighborResolutionRetainsAxis(helper,
                (StairBlock) external("bbb:oak_beam").stairs(), target);
        helper.assertTrue(slabStates == 108 && stairStates == 1440,
                "BBB Beam independent-state matrix changed: slabs=" + slabStates + " stairs=" + stairStates);
        System.out.println("BBB_C69_STANDARD_AXIS|materials=12|slabStates=108|stairStates=1440"
                + "|wallAxis=absent|shapeMap=roundtrip");
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
        helper.assertTrue(walls.getAsJsonArray("values").size() == 79,
                "External wall classification does not contain every scoped full-parent family");
        helper.assertTrue(loot == 277, "Expected 277 BGE-owned external loot tables, found " + loot);
        System.out.println("EXTERNAL_C74_SERVER_RESOURCES|standardLoot=277|wallTags=79|materialFamilies=79");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void actualClientWritersCloseAll514BgeOwnedGeometryResources(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        LayerGeneratedResources.GenerationSummary layers =
                LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.GenerationSummary quarters =
                QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialGeneratedResources.GenerationSummary standard =
                ExternalMaterialGeneratedResources.generate(manager);
        helper.assertTrue(layers.familyCount() == 79
                        && quarters.cornerFamilyCount() == 79
                        && quarters.columnFamilyCount() == 79
                        && standard.familyCount() == 79
                        && standard.blockStateCount() == 277
                        && standard.itemCount() == 277,
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
        helper.assertTrue(generatedRelations == 514 && resolvedModelReferences >= 514,
                "External client resource closure mismatch: relations=" + generatedRelations
                        + ", modelReferences=" + resolvedModelReferences);
        System.out.println("EXTERNAL_C74_CLIENT_RESOURCES|generatedRelations=514|blockstates=514|items=514"
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

    private static void assertBbbSlabStateContract(GameTestHelper helper, SlabBlock slab,
            String material, BlockPos target, ServerPlayer player) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (SlabType type : SlabType.values()) {
                BlockState state = slab.defaultBlockState()
                        .setValue(BlockStateProperties.AXIS, axis)
                        .setValue(SlabBlock.TYPE, type)
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                helper.assertTrue(state.getValue(BlockStateProperties.AXIS) == axis
                                && state.getValue(SlabBlock.TYPE) == type
                                && state.getValue(BlockStateProperties.WATERLOGGED),
                        "BBB Beam Slab geometry and material axis are not independent: " + material
                                + " type=" + type + " axis=" + axis);
                assertCodecRoundTrip(helper, state, material + " slab " + type + "/" + axis);
                BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
                helper.assertTrue(rotated.getValue(BlockStateProperties.AXIS) == rotatedAxis(axis)
                                && rotated.getValue(SlabBlock.TYPE) == type
                                && rotated.getValue(BlockStateProperties.WATERLOGGED),
                        "BBB Beam Slab rotation changed geometry instead of only rotating material axis: "
                                + material + " type=" + type + " axis=" + axis);
                BlockState mirrored = state.mirror(Mirror.FRONT_BACK);
                helper.assertTrue(mirrored.getValue(BlockStateProperties.AXIS) == axis
                                && mirrored.getValue(SlabBlock.TYPE) == type
                                && mirrored.getValue(BlockStateProperties.WATERLOGGED),
                        "BBB Beam Slab mirror changed material axis or geometry state: " + material
                                + " type=" + type + " axis=" + axis);
            }
        }

        ItemStack stack = new ItemStack(slab);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (Direction face : Direction.values()) {
            for (double localY : List.of(0.25, 0.75)) {
                helper.setBlock(target, Blocks.AIR);
                BlockState placed = slab.getStateForPlacement(
                        placementContext(helper, player, stack, target, face, localY));
                SlabType expectedType = face == Direction.DOWN ||
                        (face.getAxis().isHorizontal() && localY > 0.5)
                        ? SlabType.TOP : SlabType.BOTTOM;
                helper.assertTrue(placed != null
                                && placed.getValue(BlockStateProperties.AXIS) == face.getAxis()
                                && placed.getValue(SlabBlock.TYPE) == expectedType,
                        "BBB Beam Slab placement did not preserve independent face axis/type: " + material
                                + " face=" + face + " y=" + localY);
            }
        }
        for (Direction.Axis retained : Direction.Axis.values()) {
            helper.setBlock(target, slab.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, retained)
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            BlockState combined = slab.getStateForPlacement(
                    placementContext(helper, player, stack, target, Direction.EAST, 0.75));
            helper.assertTrue(combined != null && combined.getValue(SlabBlock.TYPE) == SlabType.DOUBLE
                            && combined.getValue(BlockStateProperties.AXIS) == retained,
                    "Compatible BBB Beam Slab combination reset its material axis: " + material
                            + " retained=" + retained);
        }
    }

    private static void assertBbbStairStateContract(GameTestHelper helper, StairBlock stairs,
            String material) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                for (Half half : Half.values()) {
                    for (StairsShape shape : StairsShape.values()) {
                        BlockState state = stairs.defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, axis)
                                .setValue(StairBlock.FACING, facing)
                                .setValue(StairBlock.HALF, half)
                                .setValue(StairBlock.SHAPE, shape)
                                .setValue(BlockStateProperties.WATERLOGGED, true);
                        helper.assertTrue(state.getValue(BlockStateProperties.AXIS) == axis
                                        && state.getValue(StairBlock.FACING) == facing
                                        && state.getValue(StairBlock.HALF) == half
                                        && state.getValue(StairBlock.SHAPE) == shape
                                        && state.getValue(BlockStateProperties.WATERLOGGED),
                                "BBB Beam Stair geometry and material axis are not independent: " + material
                                        + " " + facing + "/" + half + "/" + shape + "/" + axis);
                        assertCodecRoundTrip(helper, state, material + " stair " + facing + "/" + half
                                + "/" + shape + "/" + axis);
                        BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
                        helper.assertTrue(rotated.getValue(BlockStateProperties.AXIS) == rotatedAxis(axis)
                                        && rotated.getValue(StairBlock.FACING)
                                                == Rotation.CLOCKWISE_90.rotate(facing)
                                        && rotated.getValue(StairBlock.HALF) == half
                                        && rotated.getValue(StairBlock.SHAPE) == shape
                                        && rotated.getValue(BlockStateProperties.WATERLOGGED),
                                "BBB Beam Stair rotation corrupted independent state: " + material
                                        + " " + facing + "/" + half + "/" + shape + "/" + axis);
                        BlockState mirrored = state.mirror(Mirror.LEFT_RIGHT);
                        helper.assertTrue(mirrored.getValue(BlockStateProperties.AXIS) == axis
                                        && mirrored.getValue(StairBlock.FACING)
                                                == Mirror.LEFT_RIGHT.mirror(facing)
                                        && mirrored.getValue(StairBlock.HALF) == half
                                        && mirrored.getValue(BlockStateProperties.WATERLOGGED),
                                "BBB Beam Stair mirror corrupted independent state: " + material
                                        + " " + facing + "/" + half + "/" + shape + "/" + axis);
                    }
                }
            }
        }
    }

    private static void assertBbbAxisModelContract(GameTestHelper helper,
            ExternalMaterialFamilies.Binding beam, String material) {
        NativeAxisModelContract.GeneratedBlockResources slab = NativeAxisModelContract.slab(
                beam.profile(), NativeAxisModelContract.AxisUvPolicy.STANDARD_ROTATED);
        NativeAxisModelContract.GeneratedBlockResources stairs = NativeAxisModelContract.stairs(
                beam.profile(), NativeAxisModelContract.AxisUvPolicy.STANDARD_ROTATED);
        for (NativeAxisModelContract.GeneratedBlockResources resources : List.of(slab, stairs)) {
            helper.assertTrue(resources.selectors().values().stream().allMatch(selection ->
                            resources.models().containsKey(selection.model()))
                            && resources.models().values().stream().allMatch(model -> {
                                JsonObject textures = model.getAsJsonObject("textures");
                                return textures.get("side").getAsString().equals("bbb:block/beam/" + material)
                                        && textures.get("top").getAsString().equals(
                                                "bbb:block/beam/" + material + "_top")
                                        && textures.get("bottom").getAsString().equals(
                                                "bbb:block/beam/" + material + "_top");
                            }),
                    "BBB Beam generated model lost semantic side/end-grain texture roles: " + material);
        }
        helper.assertTrue(slab.selectors().size() == 9 && stairs.selectors().size() == 120,
                "BBB Beam axis model selector matrix changed: " + material + " slabs="
                        + slab.selectors().size() + " stairs=" + stairs.selectors().size());
        List<Item> component = ShapeMap.getShapes(beam.source().asItem());
        helper.assertTrue(java.util.Collections.frequency(component, beam.slab().asItem()) == 1
                        && java.util.Collections.frequency(component, beam.stairs().asItem()) == 1
                        && ShapeMap.getParent(beam.slab().asItem())
                                == ShapeMap.getParent(beam.source().asItem())
                        && ShapeMap.getParent(beam.stairs().asItem())
                                == ShapeMap.getParent(beam.source().asItem()),
                "BBB Beam ShapeMap did not round-trip BGE-owned canonical slab/stair: " + material);
    }

    private static void assertBbbStairNeighborResolutionRetainsAxis(GameTestHelper helper,
            StairBlock stairs, BlockPos target) throws ReflectiveOperationException {
        int shapeChanges = 0;
        BlockState current = stairs.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z)
                .setValue(StairBlock.FACING, Direction.NORTH)
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                for (Direction clear : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                    helper.setBlock(target.relative(clear), Blocks.AIR);
                }
                helper.setBlock(target, current);
                BlockPos neighbor = target.relative(direction);
                BlockState neighborState = stairs.defaultBlockState()
                        .setValue(BlockStateProperties.AXIS, Direction.Axis.X)
                        .setValue(StairBlock.FACING, facing)
                        .setValue(StairBlock.HALF, Half.BOTTOM)
                        .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
                helper.setBlock(neighbor, neighborState);
                BlockState updated = invokeStairUpdateShape(stairs, current, helper, target,
                        direction, neighbor, neighborState);
                helper.assertTrue(updated.getValue(BlockStateProperties.AXIS) == Direction.Axis.Z,
                        "BBB Beam Stair neighbor shape update lost the existing material axis: "
                                + direction + "/" + facing);
                if (updated.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT) shapeChanges++;
            }
        }
        helper.assertTrue(shapeChanges > 0,
                "BBB Beam Stair neighbor matrix never exercised normal corner resolution");
    }

    private static BlockPlaceContext placementContext(GameTestHelper helper, ServerPlayer player,
            ItemStack stack, BlockPos target, Direction face, double localY) {
        BlockPos absolute = helper.absolutePos(target);
        return new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(new Vec3(absolute.getX() + 0.5, absolute.getY() + localY,
                        absolute.getZ() + 0.5), face, absolute, false));
    }

    private static BlockState invokeStairUpdateShape(StairBlock stairs, BlockState state,
            GameTestHelper helper, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState) throws ReflectiveOperationException {
        Method update = null;
        for (Class<?> type = stairs.getClass(); type != null && update == null; type = type.getSuperclass()) {
            update = Arrays.stream(type.getDeclaredMethods()).filter(method ->
                            method.getName().equals("updateShape") && method.getParameterCount() == 8
                                    && method.getParameterTypes()[0] == BlockState.class)
                    .findFirst().orElse(null);
        }
        if (update == null) throw new NoSuchMethodException("BBB Beam Stair updateShape");
        update.setAccessible(true);
        return (BlockState) update.invoke(stairs, state, helper.getLevel(), helper.getLevel(),
                helper.absolutePos(pos), direction, helper.absolutePos(neighborPos), neighborState,
                RandomSource.create(0x4242425F433639L));
    }

    private static Direction.Axis rotatedAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Z;
            case Y -> Direction.Axis.Y;
            case Z -> Direction.Axis.X;
        };
    }

    private static void assertCodecRoundTrip(GameTestHelper helper, BlockState state, String label) {
        var encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result()
                .orElseThrow(() -> new IllegalStateException("BlockState.CODEC could not encode " + label));
        BlockState decoded = BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).result()
                .orElseThrow(() -> new IllegalStateException("BlockState.CODEC could not decode " + label));
        helper.assertTrue(decoded.equals(state), "BlockState.CODEC changed " + label + ": " + encoded);
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

    private static Identifier modelResource(String modelId) {
        Identifier id = Identifier.parse(modelId);
        return Identifier.fromNamespaceAndPath(id.getNamespace(),
                "models/" + id.getPath() + ".json");
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

    private static JsonObject resourceJson(ResourceManager manager, Identifier id) {
        try {
            Resource resource = manager.getResource(id)
                    .orElseThrow(() -> new IllegalStateException("Missing fixture resource " + id));
            try (var input = resource.open();
                    var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot inspect fixture resource " + id, exception);
        }
    }

    private static void assertOnlyAssignedToadstoolTexture(GameTestHelper helper, JsonObject model,
            String assignedTexture, String label) {
        String encoded = model.toString();
        helper.assertTrue(!encoded.contains("toadstool_inside") && !encoded.contains("#interior"),
                "Generated Ribbits model exposes an interior texture route: " + label);
        JsonObject textures = model.getAsJsonObject("textures");
        helper.assertTrue(textures != null && !textures.entrySet().isEmpty()
                        && textures.entrySet().stream().allMatch(entry ->
                                entry.getValue().isJsonPrimitive()
                                        && entry.getValue().getAsString().equals(assignedTexture)),
                "Generated Ribbits model has a non-source texture binding: " + label + " " + textures);
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
        for (String source : List.of("red_toadstool", "brown_toadstool", "toadstool_stem")) {
            json.put(Identifier.fromNamespaceAndPath("ribbits", "models/block/" + source + ".json"),
                    "{\"parent\":\"ribbits:block/provider_huge_mushroom\","
                            + "\"textures\":{\"outside\":\"ribbits:block/" + source
                            + "\",\"inside\":\"ribbits:block/toadstool_inside\"}}" );
        }

        PackResources pack = (PackResources) Proxy.newProxyInstance(
                ExternalMaterialFamilyGameTests.class.getClassLoader(),
                new Class<?>[] {PackResources.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "packId" -> "bge-c77-client-fixtures";
                    case "knownPackInfo" -> Optional.empty();
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb", "ribbits");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C78 client fixture pack";
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
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb", "ribbits");
                    case "listResources" -> resources.entrySet().stream()
                            .filter(entry -> entry.getKey().getPath().startsWith((String) args[0]))
                            .filter(entry -> ((Predicate<Identifier>) args[1]).test(entry.getKey()))
                            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    case "listResourceStacks" -> Map.of();
                    case "listPacks" -> Stream.of(pack);
                    case "toString" -> "BGE C78 client fixture manager";
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
