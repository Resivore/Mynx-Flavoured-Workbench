package dev.aero.cnmterraincompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
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
import net.penumbra.enderscape.block.BlisteredMagniaBlock;
import net.penumbra.enderscape.block.HasMagniaPolarity;
import net.penumbra.enderscape.block.HasMagniaPowerSignal;
import net.penumbra.enderscape.block.MagniaBlock;
import net.penumbra.enderscape.block.state.OptionalMagniaPolarityProperty;

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
        helper.assertTrue(ExternalMaterialCatalog.specs().size() == 125,
                "External source allowlist is not exactly 125");
        helper.assertTrue(ExternalMaterialCatalog.sourceCount("mcwpaths") == 57
                        && ExternalMaterialCatalog.sourceCount("mynx_trees") == 6
                        && ExternalMaterialCatalog.sourceCount("ribbits") == 4
                        && ExternalMaterialCatalog.sourceCount("bbb") == 12
                        && ExternalMaterialCatalog.sourceCount("enderscape") == 46,
                "Provider source partition is not 57/6/4/12/46");
        helper.assertTrue(ExternalMaterialFamilies.all().size() == 125,
                "Provider completion did not register all 125 allowlisted families: "
                        + ExternalMaterialFamilies.all().size());
        helper.assertTrue(NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() != null).count() == 314
                        && NibaruMaterialProfiles.all().stream().filter(profile -> profile.family() == null
                                && !profile.canonicalParentId().getNamespace().equals("minecraft")).count() == 125,
                "External append changed the frozen native inventory or lost an external source");

        Set<Identifier> actual = new LinkedHashSet<>();
        ExternalMaterialFamilies.all().forEach(binding -> actual.add(binding.spec().id()));
        Set<Identifier> expected = new LinkedHashSet<>();
        ExternalMaterialCatalog.specs().forEach(spec -> expected.add(spec.id()));
        helper.assertTrue(actual.equals(expected),
                "Registered external sources differ from the exact allowlist");
        helper.assertTrue(actual.stream().filter(id -> id.getNamespace().equals("mcwpaths"))
                        .allMatch(ExternalMaterialFamilyGameTests::isRequestedMacawSource),
                "Macaw family is outside the 52 full-pattern plus five plain-Path scope");
        System.out.println("EXTERNAL_C91_INVENTORY|sources=125|mcwpaths=57"
                + "|mynx_trees=6|ribbits=4|bbb=12|enderscape=46|relations=1125");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void enderscapeCatalogUsesExactSourcesAndDeclaredMaterialSemantics(GameTestHelper helper) {
        Set<Identifier> expected = Set.of(Identifier.parse("enderscape:veiled_log"),
                Identifier.parse("enderscape:veiled_wood"), Identifier.parse("enderscape:celestial_stem"),
                Identifier.parse("enderscape:celestial_hyphae"), Identifier.parse("enderscape:murublight_stem"),
                Identifier.parse("enderscape:murublight_hyphae"), Identifier.parse("enderscape:shadoline_pillar"),
                Identifier.parse("enderscape:dusk_purpur_pillar"), Identifier.parse("enderscape:stripped_veiled_log"),
                Identifier.parse("enderscape:stripped_veiled_wood"), Identifier.parse("enderscape:stripped_celestial_stem"),
                Identifier.parse("enderscape:stripped_celestial_hyphae"), Identifier.parse("enderscape:stripped_murublight_stem"),
                Identifier.parse("enderscape:stripped_murublight_hyphae"), Identifier.parse("enderscape:chiseled_end_stone"),
                Identifier.parse("enderscape:cracked_end_stone_bricks"), Identifier.parse("enderscape:chiseled_purpur"),
                Identifier.parse("enderscape:nebulite_block"), Identifier.parse("enderscape:chiseled_shadoline"),
                Identifier.parse("enderscape:chiseled_veradite"), Identifier.parse("enderscape:chiseled_mirestone"),
                Identifier.parse("enderscape:cracked_mirestone_bricks"), Identifier.parse("enderscape:chiseled_kurodite"),
                Identifier.parse("enderscape:alluring_magnia"), Identifier.parse("enderscape:repulsive_magnia"),
                Identifier.parse("enderscape:chiseled_dusk_purpur"), Identifier.parse("enderscape:blistered_magnia"),
                Identifier.parse("enderscape:void_shale"), Identifier.parse("enderscape:veiled_leaves"),
                Identifier.parse("enderscape:celestial_cap"), Identifier.parse("enderscape:murublight_cap"),
                Identifier.parse("enderscape:end_lamp"), Identifier.parse("enderscape:drift_jelly_block"),
                Identifier.parse("enderscape:blinklamp"), Identifier.parse("enderscape:veiled_end_stone"),
                Identifier.parse("enderscape:celestial_overgrowth"), Identifier.parse("enderscape:corrupt_overgrowth"),
                Identifier.parse("enderscape:celestial_path"), Identifier.parse("enderscape:corrupt_path"));
        expected = new LinkedHashSet<>(expected);
        expected.add(Identifier.parse("enderscape:raw_shadoline_block"));
        for (String family : List.of("veiled", "celestial", "murublight")) {
            expected.add(Identifier.parse("enderscape:" + family + "_planks"));
            expected.add(Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                    "enderscape/" + family + "_beam"));
        }
        Set<Identifier> actual = new LinkedHashSet<>();
        ExternalMaterialFamilies.all().stream().filter(binding -> binding.spec().provider().equals("enderscape"))
                .forEach(binding -> actual.add(binding.spec().id()));
        helper.assertTrue(actual.equals(expected) && actual.size() == 46,
                "Enderscape source allowlist drifted: " + actual);
        helper.assertTrue(ExternalMaterialCatalog.requestedEnderscapeExclusions().size() == 1
                        && ExternalMaterialCatalog.requestedEnderscapeExclusions().getFirst().requestedId()
                                .equals(Identifier.parse("enderscape:block_of_raw_magnia")),
                "The absent raw-magnia request must remain one exact recorded exclusion");
        helper.assertTrue(actual.contains(Identifier.parse("enderscape:nebulite_block"))
                        && !actual.contains(Identifier.parse("enderscape:block_of_nebulite")),
                "Block of Nebulite must use Enderscape's real nebulite_block identity");

        for (Identifier id : List.of(Identifier.parse("enderscape:veiled_log"),
                Identifier.parse("enderscape:veiled_wood"), Identifier.parse("enderscape:celestial_stem"),
                Identifier.parse("enderscape:celestial_hyphae"), Identifier.parse("enderscape:murublight_stem"),
                Identifier.parse("enderscape:murublight_hyphae"), Identifier.parse("enderscape:shadoline_pillar"),
                Identifier.parse("enderscape:dusk_purpur_pillar"), Identifier.parse("enderscape:stripped_veiled_log"),
                Identifier.parse("enderscape:stripped_veiled_wood"), Identifier.parse("enderscape:stripped_celestial_stem"),
                Identifier.parse("enderscape:stripped_celestial_hyphae"), Identifier.parse("enderscape:stripped_murublight_stem"),
                Identifier.parse("enderscape:stripped_murublight_hyphae"))) {
            ExternalMaterialFamilies.Binding binding = ExternalMaterialFamilies.fromSource(id).orElseThrow();
            helper.assertTrue(binding.profile().visualProfile() == VisualProfile.PILLAR
                            && binding.profile().orientationPolicy()
                                    == games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED
                            && binding.canonicalDerived().stream().filter(block -> block != binding.wall())
                                    .allMatch(block -> block.defaultBlockState().hasProperty(BlockStateProperties.AXIS)),
                    "Enderscape axial family lost its axis-aware roles: " + id);
        }
        for (Identifier id : List.of(Identifier.parse("enderscape:veiled_end_stone"),
                Identifier.parse("enderscape:celestial_overgrowth"),
                Identifier.parse("enderscape:corrupt_overgrowth"))) {
            helper.assertTrue(ExternalMaterialFamilies.fromSource(id).orElseThrow().profile().visualProfile()
                            == VisualProfile.TOP_SIDE_BOTTOM
                            && ExternalMaterialFamilies.fromSource(id).orElseThrow().profile().textureRoles()
                                    .overlay().equals(ExternalMaterialFamilies.fromSource(id).orElseThrow()
                                            .profile().textureRoles().side()),
                    "Enderscape terrain family lost the Crimson Nylium face/UV overlay contract: " + id);
        }
        for (Identifier id : List.of(Identifier.parse("enderscape:celestial_path"),
                Identifier.parse("enderscape:corrupt_path"))) {
            helper.assertTrue(ExternalMaterialFamilies.fromSource(id).orElseThrow().profile().visualProfile()
                            == VisualProfile.PATH
                            && ExternalMaterialFamilies.fromSource(id).orElseThrow().profile().surfaceSamplingPolicy()
                            == games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE,
                    "Enderscape path family lost its lowered path surface contract: " + id);
        }
        ExternalMaterialFamilies.Binding rawShadoline = external("enderscape:raw_shadoline_block");
        helper.assertTrue(rawShadoline.profile().textureRoles().side()
                        .equals("enderscape:block/raw_shadoline_block")
                        && rawShadoline.roles().size() == 9,
                "Block of Raw Shadoline did not retain its exact source texture/family");
        for (PrivateBeamFamilies.Definition definition : PrivateBeamFamilies.definitions()) {
            ExternalMaterialFamilies.Binding plank = external(definition.planks().toString());
            ExternalMaterialFamilies.Binding beam = external(definition.root().toString());
            helper.assertTrue(beam.profile().orientationPolicy()
                            == games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED
                            && beam.canonicalDerived().stream().filter(block -> block != beam.wall())
                                    .allMatch(block -> block.defaultBlockState().hasProperty(BlockStateProperties.AXIS))
                            && !beam.wall().defaultBlockState().hasProperty(BlockStateProperties.AXIS)
                            && beam.wall() instanceof WoodenPlankWallBlock
                            && plank.wall() instanceof WoodenPlankWallBlock,
                    "C91 Beam/plank wooden-wall or axis contract drifted for " + definition.family());
            List<Item> component = ShapeMap.getShapes(plank.source().asItem());
            List<Item> beamFamily = beam.roles().values().stream().map(Block::asItem).toList();
            helper.assertTrue(component.indexOf(beam.source().asItem()) > component.indexOf(plank.source().asItem())
                            && beamFamily.stream().allMatch(component::contains),
                    "Private Beam family is not grouped after its plank root: " + definition.family());
        }
        helper.assertTrue(ExternalMaterialFamilies.fromSource(Identifier.parse("enderscape:drift_jelly_block"))
                        .orElseThrow().profile().visualProfile() == VisualProfile.SLIME_INSET,
                "Drift Jelly Block must retain the slime-style inset visual contract");
        ExternalMaterialFamilies.Binding voidShale = external("enderscape:void_shale");
        helper.assertTrue(voidShale.profile().orientationPolicy()
                        == games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile.OrientationPolicy.UNIFORM
                        && voidShale.profile().textureRoles().side().equals("enderscape:block/void_shale_side")
                        && voidShale.profile().textureRoles().top().equals("enderscape:block/void_shale_end")
                        && voidShale.profile().textureRoles().bottom().equals("enderscape:block/void_shale_end"),
                "Void Shale must remain non-directional while using its real side/end textures");
        helper.assertTrue(ExternalMaterialFamilies.fromSource(Identifier.parse("enderscape:corrupt_overgrowth"))
                        .orElseThrow().profile().textureRoles().bottom().equals("enderscape:block/mirestone")
                        && ExternalMaterialFamilies.fromSource(Identifier.parse("enderscape:corrupt_path"))
                        .orElseThrow().profile().textureRoles().bottom().equals("enderscape:block/mirestone"),
                "Corrupt Enderscape terrain must retain its provider mirestone underside");
        helper.succeed();
    }

    /** C86: Directional Enderscape terrain must retain its shared canonical facing at Layer completion. */
    @GameTest(maxTicks = 40)
    public void enderscapeDirectionalLayersPreserveCanonicalFacingWithoutDisturbingMaterialState(
            GameTestHelper helper) {
        List<String> directionalSources = List.of("veiled_end_stone", "celestial_overgrowth",
                "corrupt_overgrowth", "celestial_path", "corrupt_path");
        BgeMaterialBindings.requireValid();
        for (String source : directionalSources) {
            ExternalMaterialFamilies.Binding binding = external("enderscape:" + source);
            BlockState canonical = binding.source().defaultBlockState();
            BlockState layer = binding.layer().defaultBlockState()
                    .setValue(BgeLayerBlock.LAYERS, 4)
                    .setValue(BgeLayerBlock.FACING, Direction.WEST);
            helper.assertTrue(canonical.hasProperty(BlockStateProperties.FACING)
                            && layer.hasProperty(BlockStateProperties.FACING),
                    "Enderscape DirectionalBlock fixture lost the exact shared FACING property: " + source);
            BlockState projected = BgeMaterialBindings.projectToCanonical(layer).orElseThrow();
            helper.assertTrue(projected.is(binding.source())
                            && projected.getValue(BlockStateProperties.FACING) == Direction.WEST,
                    "Full Enderscape Layer did not preserve canonical facing for " + source + ": "
                            + projected);
        }

        ExternalMaterialFamilies.Binding axial = external("enderscape:veiled_log");
        BlockState axisLayer = axial.layer().defaultBlockState()
                .setValue(BgeLayerBlock.LAYERS, 4)
                .setValue(BgeLayerBlock.FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        BlockState axisProjection = BgeMaterialBindings.projectToCanonical(axisLayer).orElseThrow();
        helper.assertTrue(axisProjection.is(axial.source())
                        && axisProjection.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "Layer facing classification disturbed the existing canonical AXIS projection");
        helper.succeed();
    }

    /** C84: source material state and geometry state must coexist on every generated form. */
    @GameTest(maxTicks = 40)
    public void enderscapeMaterialStateBridgePreservesMagniaAndBlinklampContracts(GameTestHelper helper) {
        ExternalMaterialFamilies.Binding blistered = external("enderscape:blistered_magnia");
        for (Map.Entry<String, Block> role : blistered.roles().entrySet()) {
            if (role.getKey().equals("block")) continue;
            Block block = role.getValue();
            BlockState none = block.defaultBlockState();
            helper.assertTrue(none.hasProperty(BlisteredMagniaBlock.POLARITY)
                            && none.getValue(BlisteredMagniaBlock.POLARITY)
                                    == OptionalMagniaPolarityProperty.NONE
                            && none.getLightEmission() == 0,
                    "Blistered Magnia bridge lost NONE material state for " + role.getKey());
            BlockState alluring = none.setValue(BlisteredMagniaBlock.POLARITY,
                    OptionalMagniaPolarityProperty.ALLURING);
            BlockState repulsive = none.setValue(BlisteredMagniaBlock.POLARITY,
                    OptionalMagniaPolarityProperty.REPULSIVE);
            helper.assertTrue(alluring.getLightEmission() == 14 && repulsive.getLightEmission() == 14
                            && block instanceof HasMagniaPolarity && block instanceof HasMagniaPowerSignal,
                    "Blistered Magnia light/interface bridge failed for " + role.getKey());
        }

        Map<String, String> geometryProperties = Map.of(
                "slab", "type", "stairs", "facing", "wall", "north", "vertical_slab", "facing",
                "step", "type", "corner", "facing", "quarter_column", "occupancy", "layer", "layers");
        for (Map.Entry<String, String> requirement : geometryProperties.entrySet()) {
            BlockState state = blistered.roles().get(requirement.getKey()).defaultBlockState();
            helper.assertTrue(state.getProperties().stream().anyMatch(property ->
                            property.getName().equals(requirement.getValue()))
                            && state.hasProperty(BlisteredMagniaBlock.POLARITY),
                    "Blistered Magnia material property collided with " + requirement.getKey()
                            + " geometry state");
        }

        for (String source : List.of("alluring_magnia", "repulsive_magnia")) {
            for (Map.Entry<String, Block> role : external("enderscape:" + source).roles().entrySet()) {
                if (role.getKey().equals("block")) continue;
                BlockState state = role.getValue().defaultBlockState();
                helper.assertTrue(state.hasProperty(MagniaBlock.POWER)
                                && state.getValue(MagniaBlock.POWER) == 0
                                && role.getValue() instanceof HasMagniaPolarity
                                && role.getValue() instanceof HasMagniaPowerSignal,
                        "Fixed-polarity Magnia bridge lost power/interface state for " + source + "/"
                                + role.getKey());
            }
        }

        for (Map.Entry<String, Block> role : external("enderscape:blinklamp").roles().entrySet()) {
            if (role.getKey().equals("block")) continue;
            BlockState state = role.getValue().defaultBlockState();
            helper.assertTrue(state.getProperties().stream().anyMatch(property ->
                            property.getName().equals("luminance")) && state.getLightEmission() == 15,
                    "Blinklamp luminance bridge lost its copied light callback for " + role.getKey());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void enderscapeMaterialStateResourcesCoverEveryGeneratedGeometry(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        ExternalMaterialGeneratedResources.generate(manager);
        LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.generateExternalForValidation(manager);

        ExternalMaterialFamilies.Binding blistered = external("enderscape:blistered_magnia");
        for (Map.Entry<String, Block> role : blistered.roles().entrySet()) {
            if (role.getKey().equals("block")) continue;
            JsonObject state = generatedClientJson(blockStateResource(role.getValue()));
            if (state.has("variants")) {
                Set<String> selectors = state.getAsJsonObject("variants").keySet();
                helper.assertTrue(selectors.stream().anyMatch(key -> key.contains("polarity=none"))
                                && selectors.stream().anyMatch(key -> key.contains("polarity=alluring"))
                                && selectors.stream().anyMatch(key -> key.contains("polarity=repulsive")),
                        "Blistered Magnia variants do not cover material polarity for " + role.getKey());
            } else {
                helper.assertTrue(state.has("multipart") && state.getAsJsonArray("multipart").asList().stream()
                                .map(JsonElement::getAsJsonObject).map(part -> part.getAsJsonObject("when"))
                                .anyMatch(when -> when.has("polarity")
                                        && when.get("polarity").getAsString().equals("alluring")),
                        "Blistered Magnia wall multipart state lost polarity selector");
            }
        }
        JsonObject alluringSlab = generatedClientJson(blockStateResource(
                external("enderscape:alluring_magnia").slab())).getAsJsonObject("variants");
        helper.assertTrue(alluringSlab.keySet().stream().anyMatch(key -> key.contains("power=0"))
                        && alluringSlab.keySet().stream().anyMatch(key -> key.contains("power=15")),
                "Fixed Magnia resources do not cover the full power state range");
        JsonObject blinklampSlab = generatedClientJson(blockStateResource(
                external("enderscape:blinklamp").slab())).getAsJsonObject("variants");
        helper.assertTrue(blinklampSlab.keySet().stream().anyMatch(key -> key.contains("luminance=0"))
                        && blinklampSlab.keySet().stream().anyMatch(key -> key.contains("luminance=7")),
                "Blinklamp resources do not cover the full luminance state range");
        Identifier blinklampSlabId = BuiltInRegistries.BLOCK.getKey(external("enderscape:blinklamp").slab());
        JsonObject luminanceOneModel = generatedClientJson(Identifier.fromNamespaceAndPath(
                blinklampSlabId.getNamespace(), "models/block/" + blinklampSlabId.getPath()
                        + "_luminance1.json"));
        helper.assertTrue(blinklampSlab.entrySet().stream().filter(entry ->
                                entry.getKey().contains("luminance=2"))
                        .allMatch(entry -> entry.getValue().getAsJsonObject().get("model").getAsString()
                                .endsWith("_luminance1"))
                        && luminanceOneModel.toString().contains("enderscape:block/blinklamp_luminance1"),
                "Blinklamp luminance 2 did not retain Enderscape's shared luminance-1 model");
        helper.succeed();
    }

    /** C87: every Blinklamp selector and inventory route targets a real luminance model. */
    @GameTest(maxTicks = 80)
    public void blinklampGeneratedModelsAndItemsUseOnlyRealLuminanceResources(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        ExternalMaterialGeneratedResources.generate(manager);
        LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialFamilies.Binding blinklamp = external("enderscape:blinklamp");
        int checked = 0;
        for (Map.Entry<String, Block> role : blinklamp.roles().entrySet()) {
            if (role.getKey().equals("block") || !blinklamp.isGeneratedRole(role.getKey())) continue;
            JsonObject item = generatedClientJson(itemResource(role.getValue()));
            Set<String> models = new LinkedHashSet<>();
            collectModelReferences(generatedClientJson(blockStateResource(role.getValue())), models);
            collectModelReferences(item, models);
            helper.assertTrue(models.stream().allMatch(model -> model.matches(".*_luminance[0-4]$")),
                    "Blinklamp generated selector/item retained a nonexistent base model for "
                            + role.getKey() + ": " + models);
            for (String model : models) {
                JsonObject generated = generatedClientJson(modelResource(model));
                helper.assertTrue(!generated.toString().contains("\"enderscape:block/blinklamp\""),
                        "Blinklamp generated model refers to nonexistent base texture: " + model);
                checked++;
            }
            helper.assertTrue(item.getAsJsonObject("model").get("model").getAsString()
                            .endsWith("_luminance4"),
                    "Blinklamp inventory route did not retain canonical luminance 4: " + role.getKey());
        }
        helper.assertTrue(checked >= 8, "Did not inspect every generated Blinklamp geometry route");
        helper.succeed();
    }

    /** C87: terrain/path resources retain the selected native BGE topology while changing textures only. */
    @GameTest(maxTicks = 80)
    public void enderscapeTerrainAndPathModelsProjectReferenceUvContracts(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        ExternalMaterialGeneratedResources.generate(manager);
        LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        for (String source : List.of("veiled_end_stone", "celestial_overgrowth", "corrupt_overgrowth")) {
            assertStructuralReferenceFamily(helper, external("enderscape:" + source), "crimson_nylium", true);
        }
        for (String source : List.of("celestial_path", "corrupt_path")) {
            assertStructuralReferenceFamily(helper, external("enderscape:" + source), "dirt_path", false);
        }
        helper.succeed();
    }

    /** C85: Enderscape Veiled Leaves has a canonical model-only item definition. */
    @GameTest(maxTicks = 40)
    public void enderscapeModelOnlyItemDefinitionKeepsBlockTintAndValidInheritance(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        Identifier canonical = Identifier.parse("enderscape:items/veiled_leaves.json");
        JsonObject canonicalModel = resourceJson(manager, canonical).getAsJsonObject("model");
        helper.assertTrue(canonicalModel.get("type").getAsString().equals("minecraft:model")
                        && canonicalModel.get("model").getAsString()
                                .equals("enderscape:block/veiled_leaves")
                        && !canonicalModel.has("tints"),
                "Enderscape Veiled Leaves fixture no longer matches its canonical model-only item definition");

        LayerGeneratedResources.generateExternalForValidation(manager);
        JsonObject generatedVeiled = generatedClientJson(itemResource(
                external("enderscape:veiled_leaves").layer())).getAsJsonObject("model");
        helper.assertTrue(!generatedVeiled.has("tints"),
                "Generated Veiled Leaves Layer invented an item tint instead of using its block tint source");

        JsonObject inheritedSilverBirch = generatedClientJson(itemResource(
                external("mynx_trees:silver_birch_leaves").layer())).getAsJsonObject("model");
        helper.assertTrue(inheritedSilverBirch.getAsJsonArray("tints").get(0).getAsJsonObject()
                        .get("value").getAsInt() == -8034015,
                "Generated Silver Birch Layer stopped inheriting its explicit canonical item tint");
        helper.succeed();
    }

    /** C87: a model-tinted source can omit BlockColors without changing its item/model route. */
    @GameTest(maxTicks = 40)
    public void sourceProviderWithoutBlockColorsUsesIdentityTintAndKeepsModelOwnedAppearance(
            GameTestHelper helper) {
        helper.assertTrue(SourceProviderTintFallback.IDENTITY_MULTIPLIER == -1,
                "A model-owned source tint fell through to a null BlockColors callback");
        ExternalMaterialFamilies.Binding leaves = external("enderscape:veiled_leaves");
        helper.assertTrue(leaves.profile().tintProfile()
                        == games.twinhead.moreslabsstairsandwalls.api.material.TintProfile.SOURCE_PROVIDER,
                "Veiled Leaves stopped using the provider tint route");
        ResourceManager manager = clientFixtureManager();
        LayerGeneratedResources.generateExternalForValidation(manager);
        JsonObject item = generatedClientJson(itemResource(leaves.layer())).getAsJsonObject("model");
        Identifier layer = BuiltInRegistries.BLOCK.getKey(leaves.layer());
        helper.assertTrue(!item.has("tints") && item.get("model").getAsString()
                        .startsWith(layer.getNamespace() + ":block/" + layer.getPath() + "_"),
                "Veiled Leaves model-owned item route was replaced by a synthetic tint");
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
        helper.assertTrue(relations == 1125 && canonicalDerived.size() == 1000 && bgeGenerated.size() == 876,
                "C91 relation/canonical/generated identity count mismatch: " + relations + "/"
                        + canonicalDerived.size() + "/" + bgeGenerated.size());
        helper.assertTrue(audit.variantCount() == 125 && audit.missing().isEmpty()
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
        helper.assertTrue(reused == 124, "Expected 124 reused provider roles, found " + reused);

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
        helper.assertTrue(walls.getAsJsonArray("values").size() == 125,
                "External wall classification does not contain every scoped full-parent family");
        helper.assertTrue(loot == 501, "Expected 501 BGE-owned external loot tables, found " + loot);
        System.out.println("EXTERNAL_C91_SERVER_RESOURCES|standardLoot=501|wallTags=125|materialFamilies=125");
        helper.succeed();
    }

    @GameTest(maxTicks = 80)
    public void actualClientWritersCloseAll826BgeOwnedGeometryResources(GameTestHelper helper) {
        ResourceManager manager = clientFixtureManager();
        LayerGeneratedResources.GenerationSummary layers =
                LayerGeneratedResources.generateExternalForValidation(manager);
        QuarterGeometryGeneratedResources.GenerationSummary quarters =
                QuarterGeometryGeneratedResources.generateExternalForValidation(manager);
        ExternalMaterialGeneratedResources.GenerationSummary standard =
                ExternalMaterialGeneratedResources.generate(manager);
        helper.assertTrue(layers.familyCount() == 125
                        && quarters.cornerFamilyCount() == 125
                        && quarters.columnFamilyCount() == 125
                        && standard.familyCount() == 125
                        && standard.blockStateCount() == 501
                        && standard.itemCount() == 501,
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
                    generatedOrFixtureJson(manager, Identifier.fromNamespaceAndPath(modelId.getNamespace(),
                            "models/" + modelId.getPath() + ".json"));
                    resolvedModelReferences++;
                }
                generatedRelations++;
            }
        }
        helper.assertTrue(generatedRelations == 876 && resolvedModelReferences >= 876,
                "External client resource closure mismatch: relations=" + generatedRelations
                        + ", modelReferences=" + resolvedModelReferences);
        System.out.println("EXTERNAL_C91_CLIENT_RESOURCES|generatedRelations=876|blockstates=876|items=876"
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
                    "minecraft:block/wall_inventory");
            JsonObject logPost = wallModel(log, "_post");
            helper.assertTrue(logPost.getAsJsonObject("textures").get("side").getAsString()
                            .equals(log.profile().textureRoles().side())
                            && logPost.getAsJsonObject("textures").get("top").getAsString()
                            .equals(log.profile().textureRoles().top())
                            && logPost.getAsJsonObject("textures").get("bottom").getAsString()
                            .equals(log.profile().textureRoles().bottom()),
                    "Log wall did not preserve bark/end-grain texture roles: " + log.spec().id());

            for (String suffix : List.of("_post", "_side", "_side_tall")) {
                assertWallModel(helper, wood, suffix, switch (suffix) {
                    case "_post" -> "more_slabs_stairs_and_walls:block/template_column_wall_post";
                    case "_side" -> "more_slabs_stairs_and_walls:block/template_column_wall_side";
                    default -> "more_slabs_stairs_and_walls:block/template_column_wall_side_tall";
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
            assertWallModel(helper, wood, "_inventory", "minecraft:block/wall_inventory");
        }

        for (ExternalMaterialFamilies.Binding pillar : List.of(
                external("mynx_trees:wisteria_log"), external("mynx_trees:wisteria_wood"))) {
            JsonObject inventory = wallModel(pillar, "_inventory");
            helper.assertTrue(inventory.getAsJsonObject("textures").get("wall").getAsString()
                            .equals(pillar.profile().textureRoles().side()),
                    "Axis pillar wall inventory lost its bark-only normal-wall preview: "
                            + pillar.spec().id());
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

    /** A selector may intentionally reference one pre-existing native full model. */
    private static JsonObject generatedOrFixtureJson(ResourceManager manager, Identifier id) {
        var supplier = ClutterNoMore.RESOURCES.getResource(PackType.CLIENT_RESOURCES, id);
        if (supplier != null) {
            try (var input = supplier.get(); var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot inspect generated client resource " + id, exception);
            }
        }
        return resourceJson(manager, id);
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

    private static void assertStructuralReferenceFamily(GameTestHelper helper,
            ExternalMaterialFamilies.Binding binding, String reference, boolean requiresOverlay) {
        Identifier slab = BuiltInRegistries.BLOCK.getKey(binding.slab());
        Identifier stairs = BuiltInRegistries.BLOCK.getKey(binding.stairs());
        Identifier wall = BuiltInRegistries.BLOCK.getKey(binding.wall());
        for (Map.Entry<Identifier, String> target : Map.of(
                slab, "_slab", stairs, "_stairs", wall, "_wall_post").entrySet()) {
            String generatedSuffix = target.getKey().equals(wall) ? "_post" : "";
            JsonObject model = generatedClientJson(Identifier.fromNamespaceAndPath(target.getKey().getNamespace(),
                    "models/block/" + target.getKey().getPath() + generatedSuffix + ".json"));
            String expectedParent = "more_slabs_stairs_and_walls:block/template_" + reference
                    + target.getValue();
            helper.assertTrue(model.get("parent").getAsString().equals(expectedParent)
                            && model.toString().contains("[1,2,15,14]"),
                    "Generated " + binding.spec().id() + " lost native " + reference
                            + " topology/UV marker for " + target.getKey());
            JsonObject textures = model.getAsJsonObject("textures");
            helper.assertTrue(textures.get("side").getAsString()
                            .equals(binding.profile().textureRoles().side())
                            && textures.get("top").getAsString()
                                    .equals(binding.profile().textureRoles().top())
                            && !model.toString().contains("minecraft:block/crimson_nylium"),
                    "Generated " + binding.spec().id() + " retained a reference texture instead of its own textures");
        }

        Identifier vertical = BuiltInRegistries.BLOCK.getKey(binding.verticalSlab());
        Identifier step = BuiltInRegistries.BLOCK.getKey(binding.step());
        Identifier layer = BuiltInRegistries.BLOCK.getKey(binding.layer());
        Identifier corner = BuiltInRegistries.BLOCK.getKey(binding.corner());
        JsonObject verticalModel = generatedClientJson(Identifier.fromNamespaceAndPath(vertical.getNamespace(),
                "models/block/" + vertical.getPath() + ".json"));
        JsonObject stepModel = generatedClientJson(Identifier.fromNamespaceAndPath(step.getNamespace(),
                "models/block/" + step.getPath() + ".json"));
        JsonObject layerModel = generatedClientJson(Identifier.fromNamespaceAndPath(layer.getNamespace(),
                "models/block/" + layer.getPath() + "_4_full.json"));
        JsonObject cornerModel = generatedClientJson(Identifier.fromNamespaceAndPath(corner.getNamespace(),
                "models/block/" + corner.getPath() + "_south_west.json"));
        if (requiresOverlay) {
            helper.assertTrue(verticalModel.toString().contains("#overlay")
                            && stepModel.toString().contains("#overlay")
                            && layerModel.toString().contains("#overlay")
                            && cornerModel.toString().contains("#overlay"),
                    "Terrain structural overlay did not reach every BGE geometry for "
                            + binding.spec().id());
        } else {
            helper.assertTrue(modelHasBounds(verticalModel, 0, 0, 0, 16, 15, 8)
                            && modelHasBounds(stepModel, 0, 0, 0, 16, 7, 16)
                            && !modelHasBounds(stepModel, 0, 7, 8, 16, 15, 16)
                            && modelHasTop(layerModel, 15)
                            && modelHasTop(cornerModel, 15),
                    "Dirt Path lowered geometry/UV contract did not reach every BGE geometry for "
                            + binding.spec().id());
        }
    }

    private static boolean modelHasBounds(JsonObject model, int x0, int y0, int z0,
            int x1, int y1, int z1) {
        return model.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject)
                .anyMatch(element -> element.getAsJsonArray("from").get(0).getAsInt() == x0
                        && element.getAsJsonArray("from").get(1).getAsInt() == y0
                        && element.getAsJsonArray("from").get(2).getAsInt() == z0
                        && element.getAsJsonArray("to").get(0).getAsInt() == x1
                        && element.getAsJsonArray("to").get(1).getAsInt() == y1
                        && element.getAsJsonArray("to").get(2).getAsInt() == z1);
    }

    private static boolean modelHasTop(JsonObject model, int top) {
        return model.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject)
                .anyMatch(element -> element.getAsJsonArray("to").get(1).getAsInt() == top);
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
        return clientFixtureManager(false);
    }

    private static ResourceManager clientFixtureManager(boolean genericCnmModelFallback) {
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
        addStructuralReferenceFixtures(json, "crimson_nylium");
        addStructuralReferenceFixtures(json, "dirt_path");
        for (String path : List.of("wisteria_log", "wisteria_wood",
                "silver_birch_log", "silver_birch_wood")) {
            Identifier source = Identifier.fromNamespaceAndPath("mynx_trees", path);
            String model = "mynx_trees:block/" + path;
            json.put(Identifier.fromNamespaceAndPath("mynx_trees", "blockstates/" + path + ".json"),
                    "{\"variants\":{\"axis=x\":{\"model\":\"" + model
                            + "\",\"x\":90,\"y\":90},\"axis=y\":{\"model\":\"" + model
                            + "\"},\"axis=z\":{\"model\":\"" + model + "\",\"x\":90}}}");
        }
        for (String path : List.of("veiled_log", "veiled_wood", "celestial_stem",
                "celestial_hyphae", "murublight_stem", "murublight_hyphae", "shadoline_pillar",
                "dusk_purpur_pillar", "stripped_veiled_log", "stripped_veiled_wood",
                "stripped_celestial_stem", "stripped_celestial_hyphae",
                "stripped_murublight_stem", "stripped_murublight_hyphae")) {
            String model = "enderscape:block/" + path;
            json.put(Identifier.fromNamespaceAndPath("enderscape", "blockstates/" + path + ".json"),
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
            addBbbBeamStandardFormFixtures(json, material);
        }
        json.put(Identifier.parse("mynx_trees:items/silver_birch_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"mynx_trees:block/silver_birch_leaves\","
                        + "\"tints\":[{\"type\":\"minecraft:constant\",\"value\":-8034015}]}}" );
        json.put(Identifier.parse("mynx_trees:items/wisteria_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"mynx_trees:block/wisteria_leaves\"}}" );
        json.put(Identifier.parse("enderscape:items/veiled_leaves.json"),
                "{\"model\":{\"type\":\"minecraft:model\","
                        + "\"model\":\"enderscape:block/veiled_leaves\"}}" );
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
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb", "ribbits", "enderscape");
                    case "listResources", "close" -> null;
                    case "getRootResource", "getResource", "getMetadataSection", "location" -> null;
                    case "toString" -> "BGE C80 client fixture pack";
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
                    case "getResource" -> {
                        Identifier requested = (Identifier) args[0];
                        Resource resource = resources.get(requested);
                        // Generic CNM candidates can be admitted from any provider namespace.
                        // Their first-bake test needs a real model response while remaining
                        // independent of provider texture bytes.
                        if (resource == null && genericCnmModelFallback
                                && requested.getPath().startsWith("models/block/")) {
                            String fallback = "{\"textures\":{\"all\":\"minecraft:block/stone\"}}";
                            resource = new Resource(pack, () -> new ByteArrayInputStream(
                                    fallback.getBytes(StandardCharsets.UTF_8)));
                        }
                        yield Optional.ofNullable(resource);
                    }
                    case "getResourceStack" -> Optional.ofNullable(resources.get((Identifier) args[0]))
                            .map(List::of).orElseGet(List::of);
                    case "getNamespaces" -> Set.of("minecraft", "mynx_trees", "bbb", "ribbits", "enderscape");
                    case "listResources" -> resources.entrySet().stream()
                            .filter(entry -> entry.getKey().getPath().startsWith((String) args[0]))
                            .filter(entry -> ((Predicate<Identifier>) args[1]).test(entry.getKey()))
                            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    case "listResourceStacks" -> Map.of();
                    case "listPacks" -> Stream.of(pack);
                    case "toString" -> "BGE C80 client fixture manager";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected ResourceManager call " + method);
                });
    }

    /** Shared fixture seam for the generic-CNM first-bake resource regression. */
    static ResourceManager clientFixtureManagerForCnmRegression() {
        return clientFixtureManager(true);
    }

    /** BBB resource regression fixture: direct conventional models must remain genuinely absent. */
    static ResourceManager clientFixtureManagerForBbbBeamResourceRegression() {
        return clientFixtureManager(false);
    }

    /** Small native-resource fixture with distinguishable topology/UV data for C87 copying tests. */
    private static void addStructuralReferenceFixtures(Map<Identifier, String> json, String reference) {
        String namespace = "more_slabs_stairs_and_walls";
        String model = namespace + ":block/" + reference;
        json.put(Identifier.fromNamespaceAndPath(namespace, "blockstates/" + reference + "_slab.json"),
                "{\"variants\":{\"type=bottom\":{\"model\":\"" + model + "_slab\"},"
                        + "\"type=top\":{\"model\":\"" + model + "_slab_top\"},"
                        + "\"type=double\":{\"model\":\"" + model + "\"}}}");
        json.put(Identifier.fromNamespaceAndPath(namespace, "blockstates/" + reference + "_stairs.json"),
                "{\"variants\":{\"facing=north,half=bottom,shape=straight\":{\"model\":\""
                        + model + "_stairs\",\"uvlock\":true}}}");
        json.put(Identifier.fromNamespaceAndPath(namespace, "blockstates/" + reference + "_wall.json"),
                "{\"multipart\":[{\"when\":{\"up\":\"true\"},\"apply\":{\"model\":\""
                        + model + "_wall_post\",\"uvlock\":true}}]}");
        for (String suffix : List.of("_slab", "_slab_top", "_stairs", "_stairs_inner",
                "_stairs_outer", "_stairs_up", "_stairs_inner_up", "_stairs_outer_up",
                "_wall_post", "_wall_side", "_wall_side_tall", "_wall_inventory")) {
            json.put(Identifier.fromNamespaceAndPath(namespace, "models/block/" + reference + suffix + ".json"),
                    "{\"parent\":\"" + namespace + ":block/template_" + reference + suffix
                            + "\",\"textures\":{\"side\":\"minecraft:block/" + reference
                            + "_side\",\"top\":\"minecraft:block/" + reference
                            + "_top\",\"bottom\":\"minecraft:block/" + reference
                            + "_bottom\",\"particle\":\"minecraft:block/" + reference
                            + "_side\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],"
                            + "\"faces\":{\"north\":{\"texture\":\"#side\",\"uv\":[1,2,15,14]}}}]}");
        }
        json.put(Identifier.fromNamespaceAndPath(namespace, "models/block/" + reference + ".json"),
                "{\"parent\":\"minecraft:block/block\",\"textures\":{\"all\":\"minecraft:block/"
                        + reference + "_full\"}}");
    }

    /**
     * BBB's retained beam slab/stair models live below {@code block/beam/}; there is deliberately
     * no {@code models/block/<material>_beam_<form>.json}. These tiny JSON-only fixtures preserve
     * that resource topology without importing any provider texture bytes.
     */
    private static void addBbbBeamStandardFormFixtures(Map<Identifier, String> json, String material) {
        String beam = "bbb:block/beam/" + material;
        String stairs = material + "_beam_stairs";
        String slab = material + "_beam_slab";
        json.put(Identifier.fromNamespaceAndPath("bbb", "blockstates/" + stairs + ".json"),
                "{\"variants\":{\"facing=north,half=bottom,shape=inner_left\":{\"model\":\""
                        + beam + "_beam_stairs_inner\"},\"facing=north,half=bottom,shape=straight\":{\"model\":\""
                        + beam + "_beam_stairs\"}}}");
        json.put(Identifier.fromNamespaceAndPath("bbb", "models/block/beam/" + material
                        + "_beam_stairs_inner.json"),
                "{\"textures\":{\"side\":\"" + beam + "\",\"top\":\"" + beam + "_top\"}}");
        json.put(Identifier.fromNamespaceAndPath("bbb", "models/block/beam/" + material
                        + "_beam_stairs.json"),
                "{\"textures\":{\"side\":\"" + beam + "_top\",\"bottom\":\"" + beam + "\"}}");
        json.put(Identifier.fromNamespaceAndPath("bbb", "blockstates/" + slab + ".json"),
                "{\"variants\":{\"facing=up,type=bottom\":{\"model\":\"" + beam
                        + "_beam_slab\"}}}");
        json.put(Identifier.fromNamespaceAndPath("bbb", "models/block/beam/" + material
                        + "_beam_slab.json"),
                "{\"textures\":{\"side\":\"" + beam + "\",\"top\":\"" + beam
                        + "_top\",\"bottom\":\"" + beam + "_top\"}}");
    }

    private static List<String> bbbBeamMaterials() {
        return List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak");
    }

    @Override public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException { method.invoke(this, helper); }
}
