package dev.aero.cnmterraincompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.cnmterraincompat.AxisModelContract;
import dev.aero.cnmterraincompat.BgeGeometryRole;
import dev.aero.cnmterraincompat.CnmTerrainCompat;
import dev.aero.cnmterraincompat.ExternalMaterialFamilies;
import dev.aero.cnmterraincompat.InsetModelContract;
import dev.aero.cnmterraincompat.LayerGeneratedData;
import dev.aero.cnmterraincompat.QuarterGeometryGeneratedData;
import dev.tazer.clutternomore.client.assets.AssetGenerator;
import games.twinhead.moreslabsstairsandwalls.api.material.NativeAxisModelContract;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.aero.cnmterraincompat.client.CuboidListModelProjection.Bounds;
import static dev.aero.cnmterraincompat.client.CuboidListModelProjection.Cuboid;

/** Complete client resource projection for the five late forms CNM could not have scanned. */
public final class ExternalMaterialGeneratedResources {
    private ExternalMaterialGeneratedResources() {}

    public static GenerationSummary generate(ResourceManager manager) {
        int blockStates = 0;
        int models = 0;
        int items = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            validateKnownProviderIndirection(manager, binding);
            NibaruMaterialProfile profile = binding.profile();
            Identifier slab = id(binding.slab());
            Identifier stairs = id(binding.stairs());
            Identifier wall = id(binding.wall());
            Identifier vertical = id(binding.verticalSlab());
            Identifier step = id(binding.step());

            if (binding.isGeneratedRole("slab")) {
                if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                    NativeAxisModelContract.AxisUvPolicy nativePolicy = NativeAxisModelContract.AxisUvPolicy.valueOf(
                            AxisGeneratedResources.policy(manager, profile.canonicalParentId()).name());
                    NativeAxisModelContract.GeneratedBlockResources resources =
                            NativeAxisModelContract.slab(profile, nativePolicy);
                    write(blockState(slab), resources.blockState());
                    models += writeModels(resources.models());
                } else models += writeSlab(manager, profile, slab);
                blockStates++;
            }
            if (binding.isGeneratedRole("stairs")) {
                if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                    NativeAxisModelContract.AxisUvPolicy nativePolicy = NativeAxisModelContract.AxisUvPolicy.valueOf(
                            AxisGeneratedResources.policy(manager, profile.canonicalParentId()).name());
                    NativeAxisModelContract.GeneratedBlockResources resources =
                            NativeAxisModelContract.stairs(profile, nativePolicy);
                    write(blockState(stairs), resources.blockState());
                    models += writeModels(resources.models());
                } else models += writeStairs(manager, profile, stairs);
                blockStates++;
            }
            if (binding.isGeneratedRole("wall")) {
                // Wall state remains the normal post/low/tall multipart contract. Directional
                // material faces are expressed by the model templates, never an AXIS property.
                models += writeWall(manager, binding, wall);
                blockStates++;
            }

            boolean generatedVertical = binding.isGeneratedRole("vertical_slab");
            boolean generatedStep = binding.isGeneratedRole("step");
            if (generatedVertical != generatedStep) {
                throw new IllegalStateException("External tail ownership split for "
                        + profile.canonicalParentId());
            }
            if (generatedVertical) {
                if (profile.orientationPolicy() == NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED) {
                    // AxisModelContract produces only placed-state signature models. Keep a neutral
                    // base geometry for the final catalog-wide item-only preview wrapper.
                    models += writeAxisItemBaseModels(profile, vertical, step);
                    AxisModelContract.AxisUvPolicy policy = AxisGeneratedResources.policy(
                            manager, profile.canonicalParentId());
                    Map<String, JsonObject> verticalModels =
                            AxisModelContract.verticalGeneratedModels(profile, vertical, policy);
                    verticalModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                    write(blockState(vertical), AxisModelContract.verticalBlockState(vertical, policy));
                    models += verticalModels.size();
                    Map<String, JsonObject> stepModels =
                            AxisModelContract.stepGeneratedModels(profile, step, policy);
                    stepModels.forEach((modelId, model) -> write(modelResource(modelId), model));
                    write(blockState(step), AxisModelContract.stepBlockState(step, policy));
                    models += stepModels.size();
                } else if (profile.insetVisualContract().isPresent()) {
                    models += writeInsetVertical(profile, vertical);
                    models += writeInsetStep(profile, step);
                } else {
                    models += writeVertical(profile, vertical);
                    models += writeStep(manager, profile, step);
                }
                blockStates += 2;
            }

            if (binding.isGeneratedRole("slab")) {
                write(item(slab), GeneratedItemModelSupport.itemDefinition(manager, profile, model(slab)));
                items++;
            }
            if (binding.isGeneratedRole("stairs")) {
                write(item(stairs), GeneratedItemModelSupport.itemDefinition(manager, profile, model(stairs)));
                items++;
            }
            if (binding.isGeneratedRole("wall")) {
                String itemModel = dev.aero.cnmterraincompat.PrivateBeamFamilies.isPrivateBeam(binding.spec().id())
                        ? model(wall) + "_post" : model(wall) + "_inventory";
                write(item(wall), GeneratedItemModelSupport.itemDefinition(manager, profile, itemModel));
                items++;
            }
            if (generatedVertical) {
                write(item(vertical), GeneratedItemModelSupport.itemDefinition(manager, profile, model(vertical)));
                write(item(step), GeneratedItemModelSupport.itemDefinition(manager, profile, model(step)));
                items += 2;
            }
        }
        write(Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID + "_generated", "lang/en_us.json"),
                combinedLanguage());
        return new GenerationSummary(ExternalMaterialFamilies.all().size(), blockStates, models, items);
    }

    /** C88 behavior retained narrowly for the twelve explicitly supported BBB Beam families. */
    private static void validateKnownProviderIndirection(ResourceManager manager,
            ExternalMaterialFamilies.Binding binding) {
        Identifier source = binding.spec().id();
        if (!source.getNamespace().equals("bbb") || !source.getPath().endsWith("_beam")) return;
        Identifier stairs = Identifier.fromNamespaceAndPath("bbb", source.getPath() + "_stairs");
        Identifier state = Identifier.fromNamespaceAndPath("bbb", "blockstates/" + stairs.getPath() + ".json");
        if (manager.getResource(state).isPresent()) {
            ProviderModelTextureResolver.Textures resolved = ProviderModelTextureResolver.resolve(manager, stairs);
            ProviderModelTextureResolver.Textures expected = new ProviderModelTextureResolver.Textures(
                    texture(binding.profile().textureRoles().side()),
                    texture(binding.profile().textureRoles().top()),
                    texture(binding.profile().textureRoles().bottom()));
            if (!resolved.equals(expected)) {
                throw new IllegalStateException("BBB indirect model texture contract drifted for "
                        + stairs + ": expected=" + expected + " resolved=" + resolved);
            }
        }
    }

    private static int writeSlab(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        String reference = structuralReference(profile);
        if (reference != null) {
            JsonObject state = referenceBlockState(manager, reference + "_slab", id);
            // Native double selectors point to their full reference material. BGE retains only
            // its face/UV topology, never that material identity.
            replaceStrings(state, "minecraft:block/" + reference, model(id) + "_double");
            write(blockState(id), state);
            writeReferenceModel(manager, reference + "_slab", id, "", profile);
            writeReferenceModel(manager, reference + "_slab_top", id, "_top", profile);
            write(modelResource(id, "_double"), cuboidModel(profile,
                    List.of(new int[] {0, 0, 0, 16, 16, 16})));
            return 3;
        }
        JsonObject variants = new JsonObject();
        variants.add("type=bottom", selection(model(id)));
        variants.add("type=top", selection(model(id) + "_top"));
        variants.add("type=double", selection(model(id) + "_double"));
        write(blockState(id), variants(variants));
        if (profile.tintProfile() == TintProfile.NONE
                && profile.visualProfile() != games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM) {
            write(modelResource(id), template("minecraft:block/slab", profile));
            write(modelResource(id, "_top"), template("minecraft:block/slab_top", profile));
            write(modelResource(id, "_double"), template("minecraft:block/cube_bottom_top", profile));
        } else {
            write(modelResource(id), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 8, 16})));
            write(modelResource(id, "_top"), cuboidModel(profile, List.of(new int[] {0, 8, 0, 16, 16, 16})));
            write(modelResource(id, "_double"), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 16, 16})));
        }
        return 3;
    }

    private static int writeStairs(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        String reference = structuralReference(profile);
        if (reference != null) {
            write(blockState(id), referenceBlockState(manager, reference + "_stairs", id));
            for (String suffix : List.of("", "_inner", "_outer", "_up", "_inner_up", "_outer_up")) {
                writeReferenceModel(manager, reference + "_stairs" + suffix, id, suffix, profile);
            }
            return 6;
        }
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/oak_stairs.json"),
                "minecraft:block/oak_stairs", model(id));
        write(blockState(id), state);
        if (profile.tintProfile() == TintProfile.NONE
                && profile.visualProfile() != games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM) {
            write(modelResource(id), template("minecraft:block/stairs", profile));
            write(modelResource(id, "_inner"), template("minecraft:block/inner_stairs", profile));
            write(modelResource(id, "_outer"), template("minecraft:block/outer_stairs", profile));
        } else {
            write(modelResource(id), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {0, 8, 8, 16, 16, 16})));
            write(modelResource(id, "_inner"), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {0, 8, 8, 16, 16, 16},
                    new int[] {8, 8, 0, 16, 16, 8})));
            write(modelResource(id, "_outer"), cuboidModel(profile, List.of(
                    new int[] {0, 0, 0, 16, 8, 16}, new int[] {8, 8, 8, 16, 16, 16})));
        }
        return 3;
    }

    private static int writeWall(ResourceManager manager, ExternalMaterialFamilies.Binding binding,
            Identifier id) {
        NibaruMaterialProfile profile = binding.profile();
        String reference = structuralReference(profile);
        if (reference != null) {
            write(blockState(id), referenceBlockState(manager, reference + "_wall", id));
            for (String suffix : List.of("_post", "_side", "_side_tall", "_inventory")) {
                writeReferenceModel(manager, reference + "_wall" + suffix, id, suffix, profile);
            }
            return 4;
        }
        if (dev.aero.cnmterraincompat.PrivateBeamFamilies.isPrivateBeam(binding.spec().id())) {
            write(blockState(id), woodenBeamWallBlockState(id));
            write(modelResource(id, "_post"), woodenBeamWallPost(profile));
            write(modelResource(id, "_side"), woodenBeamWallSide(profile));
            // Retain deterministic compatibility aliases while the production blockstate and
            // item follow BBB's exact two-model/post-only topology.
            write(modelResource(id, "_side_tall"), parentModel(model(id) + "_side"));
            write(modelResource(id, "_inventory"), parentModel(model(id) + "_post"));
            return 4;
        }
        JsonObject state = templateBlockState(manager,
                Identifier.fromNamespaceAndPath("minecraft", "blockstates/cobblestone_wall.json"),
                "minecraft:block/cobblestone_wall", model(id));
        write(blockState(id), state);
        if (profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.PILLAR) {
            write(modelResource(id, "_post"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_post", profile));
            write(modelResource(id, "_side"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_side", profile));
            write(modelResource(id, "_side_tall"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_side_tall", profile));
            // The Wall remains non-axis geometry, while its preview preserves the same material
            // face roles as the placed wall: bark/side vertically and end grain horizontally.
            write(modelResource(id, "_inventory"), columnWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_column_wall_inventory", profile));
        } else if (profile.visualProfile()
                == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.LEAVES_CUTOUT_TINTED) {
            write(modelResource(id, "_post"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_post", profile));
            write(modelResource(id, "_side"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side", profile));
            write(modelResource(id, "_side_tall"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_side_tall", profile));
            write(modelResource(id, "_inventory"), leafWallTemplate(
                    "more_slabs_stairs_and_walls:block/template_leaves_wall_inventory", profile));
        } else if (profile.tintProfile() == TintProfile.NONE
                && profile.visualProfile() != games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM) {
            write(modelResource(id, "_post"), wallTemplate("minecraft:block/template_wall_post", profile));
            write(modelResource(id, "_side"), wallTemplate("minecraft:block/template_wall_side", profile));
            write(modelResource(id, "_side_tall"), wallTemplate("minecraft:block/template_wall_side_tall", profile));
            write(modelResource(id, "_inventory"), wallTemplate("minecraft:block/wall_inventory", profile));
        } else {
            write(modelResource(id, "_post"), cuboidModel(profile, List.of(new int[] {4, 0, 4, 12, 16, 12})));
            write(modelResource(id, "_side"), cuboidModel(profile, List.of(new int[] {5, 0, 0, 11, 14, 11})));
            write(modelResource(id, "_side_tall"), cuboidModel(profile, List.of(new int[] {5, 0, 0, 11, 16, 11})));
            write(modelResource(id, "_inventory"), cuboidModel(profile, List.of(
                    new int[] {4, 0, 4, 12, 16, 12}, new int[] {0, 0, 5, 16, 14, 11},
                    new int[] {5, 0, 0, 11, 14, 16})));
        }
        // Vanilla's cobblestone blockstate names its multipart models _post/_side/_side_tall.
        return 4;
    }

    private static int writeVertical(NibaruMaterialProfile profile, Identifier id) {
        JsonObject variants = new JsonObject();
        int rotation = 0;
        for (String facing : List.of("north", "east", "south", "west")) {
            variants.add("facing=" + facing + ",double=false", selection(model(id), 0, rotation, true));
            variants.add("facing=" + facing + ",double=true", selection(model(id) + "_double", 0, 0, true));
            rotation += 90;
        }
        write(blockState(id), variants(variants));
        if (structuralReference(profile) != null) {
            // There is no dedicated native Crimson Nylium/Dirt Path Vertical Slab asset.  Use
            // the same cuboid projection as BGE's Corner/Quarter geometry so its face frame,
            // side overlay, and lowered Path surface stay structural rather than merely using
            // the generic TOP/SIDE/BOTTOM template.
            write(modelResource(id), projectedModel(profile,
                    List.of(Cuboid.world(new Bounds(0, 0, 0, 16, 16, 8)))));
            write(modelResource(id, "_double"), projectedModel(profile,
                    List.of(Cuboid.world(new Bounds(0, 0, 0, 16, 16, 16)))));
        } else if (profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM) {
            write(modelResource(id), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 16, 8})));
            write(modelResource(id, "_double"), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 16, 16})));
        } else {
            String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
            write(modelResource(id), template("clutternomore:block/templates/vertical_slab" + tint, profile));
            write(modelResource(id, "_double"), template("clutternomore:block/templates/vertical_slab_double" + tint, profile));
        }
        return 2;
    }

    private static int writeStep(ResourceManager manager, NibaruMaterialProfile profile, Identifier id) {
        JsonObject variants = new JsonObject();
        int rotation = 0;
        for (String facing : List.of("north", "east", "south", "west")) {
            variants.add("facing=" + facing + ",type=bottom", selection(model(id), 0, rotation, true));
            variants.add("facing=" + facing + ",type=top", selection(model(id) + "_top", 0, rotation, true));
            variants.add("facing=" + facing + ",type=double", selection(model(id) + "_double", 0, rotation, true));
            rotation += 90;
        }
        write(blockState(id), variants(variants));
        if (profile.visualProfile()
                == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.PATH) {
            // Reuse Dirt Path Step's authored UV topology exactly.  Its one-pixel vertical
            // side crop cannot be reconstructed by merely lowering a generic cuboid.
            writePathStepTemplate(manager, "path_step.json", id, "", profile);
            writePathStepTemplate(manager, "path_step_top.json", id, "_top", profile);
            writePathStepTemplate(manager, "path_step_double.json", id, "_double", profile);
        } else if (structuralReference(profile) != null) {
            // A Step is one half-depth member, never a full-depth Slab or a two-tier Stair.
            // Its double form combines the upper facing half with the lower opposite half.
            write(modelResource(id), projectedModel(profile,
                    List.of(Cuboid.world(new Bounds(0, 0, 0, 16, 8, 8)))));
            write(modelResource(id, "_top"), projectedModel(profile,
                    List.of(Cuboid.world(new Bounds(0, 8, 0, 16, 16, 8)))));
            write(modelResource(id, "_double"), projectedModel(profile,
                    List.of(Cuboid.world(new Bounds(0, 8, 0, 16, 16, 8)),
                            Cuboid.world(new Bounds(0, 0, 8, 16, 8, 16)))));
        } else if (profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM) {
            write(modelResource(id), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 8, 16})));
            write(modelResource(id, "_top"), cuboidModel(profile, List.of(new int[] {0, 8, 0, 16, 16, 16})));
            write(modelResource(id, "_double"), cuboidModel(profile, List.of(new int[] {0, 0, 0, 16, 16, 16})));
        } else {
            String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
            write(modelResource(id), template("clutternomore:block/templates/step" + tint, profile));
            write(modelResource(id, "_top"), template("clutternomore:block/templates/step_top" + tint, profile));
            write(modelResource(id, "_double"), template("clutternomore:block/templates/step_double" + tint, profile));
        }
        return 3;
    }

    private static JsonObject projectedModel(NibaruMaterialProfile profile, List<Cuboid> cuboids) {
        return CuboidListModelProjection.worldModel(profile, cuboids, null, false);
    }

    /** Mirrors CNM's inset Vertical/Step route without assuming CNM owns this namespace. */
    private static int writeInsetVertical(NibaruMaterialProfile profile, Identifier id) {
        write(modelResource(id), InsetModelContract.verticalModel(profile, Direction.NORTH, false));
        for (Direction facing : InsetModelContract.horizontalDirections()) {
            write(modelResource(id, InsetModelContract.suffix(facing, SlabType.BOTTOM)),
                    InsetModelContract.verticalModel(profile, facing, false));
        }
        write(modelResource(id, "_inset_double"),
                InsetModelContract.verticalModel(profile, Direction.NORTH, true));
        write(blockState(id), InsetModelContract.verticalBlockState(id));
        return 6;
    }

    private static int writeInsetStep(NibaruMaterialProfile profile, Identifier id) {
        write(modelResource(id), InsetModelContract.stepModel(profile, Direction.NORTH,
                SlabType.BOTTOM));
        for (Direction facing : InsetModelContract.horizontalDirections()) {
            for (SlabType type : SlabType.values()) {
                write(modelResource(id, InsetModelContract.suffix(facing, type)),
                        InsetModelContract.stepModel(profile, facing, type));
            }
        }
        write(blockState(id), InsetModelContract.stepBlockState(id));
        return 13;
    }

    /**
     * Writes only the normal CNM base models for an axis-aligned late family. These models
     * intentionally remain absent from the axis-aware placed-state selectors.
     */
    private static int writeAxisItemBaseModels(
            NibaruMaterialProfile profile, Identifier vertical, Identifier step) {
        String tint = profile.tintProfile() == TintProfile.NONE ? "" : "_tinted";
        write(modelResource(vertical), template("clutternomore:block/templates/vertical_slab" + tint, profile));
        write(modelResource(step), template("clutternomore:block/templates/step" + tint, profile));
        return 2;
    }

    private static JsonObject template(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.add("textures", textures(profile));
        return result;
    }

    /**
     * Reuse the exact native BGE face/UV topology, replacing only material variables.  Crimson
     * Nylium is the terrain reference; Dirt Path is the lowered-path reference.  This is a
     * structural model contract rather than a registry-name family decision.
     */
    private static String structuralReference(NibaruMaterialProfile profile) {
        if (profile.visualProfile() == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.PATH)
            return "dirt_path";
        return profile.textureRoles().overlay().isEmpty() ? null : "crimson_nylium";
    }

    private static JsonObject referenceBlockState(ResourceManager manager, String reference, Identifier target) {
        return referenceJson(manager, "blockstates/" + reference + ".json", reference, target);
    }

    private static void writeReferenceModel(ResourceManager manager, String reference, Identifier target,
            String targetSuffix, NibaruMaterialProfile profile) {
        JsonObject model = referenceJson(manager, "models/block/" + reference + ".json", reference, target);
        // Do not inherit any source texture alias from the structural template. In particular,
        // Crimson Nylium is a topology reference only and may never leak into Enderscape models.
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(profile.textureRoles().side()));
        textures.addProperty("top", texture(profile.textureRoles().top()));
        textures.addProperty("bottom", texture(profile.textureRoles().bottom()));
        textures.addProperty("particle", texture(profile.textureRoles().particle()));
        textures.addProperty("layer0", texture(profile.textureRoles().side()));
        if (!profile.textureRoles().overlay().isEmpty()) {
            textures.addProperty("overlay", texture(profile.textureRoles().overlay()));
            textures.addProperty("layer1", texture(profile.textureRoles().overlay()));
        }
        model.add("textures", textures);
        sanitizeCullfaces(model);
        write(modelResource(target, targetSuffix), model);
    }

    private static void writePathStepTemplate(ResourceManager manager, String template,
            Identifier target, String targetSuffix, NibaruMaterialProfile profile) {
        Identifier resourceId = Identifier.fromNamespaceAndPath("clutternomore",
                "models/block/templates/provider/" + template);
        Resource resource = manager.getResource(resourceId).orElse(null);
        try (var reader = resource != null ? resource.openAsReader() : classpathReader(resourceId)) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            model.add("textures", textures(profile));
            write(modelResource(target, targetSuffix), model);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot project Dirt Path Step template " + resourceId,
                    exception);
        }
    }

    private static InputStreamReader classpathReader(Identifier resourceId) {
        var stream = ExternalMaterialGeneratedResources.class.getClassLoader().getResourceAsStream(
                "assets/" + resourceId.getNamespace() + "/" + resourceId.getPath());
        if (stream == null) throw new IllegalStateException(
                "Missing authored Dirt Path Step template " + resourceId);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    /** Removes inherited cull hints unless the model really fills the complete boundary plane. */
    private static void sanitizeCullfaces(JsonObject model) {
        if (!model.has("elements") || !model.get("elements").isJsonArray()) return;
        List<Cuboid> cuboids = new java.util.ArrayList<>();
        for (JsonElement value : model.getAsJsonArray("elements")) {
            if (!value.isJsonObject()) continue;
            JsonObject element = value.getAsJsonObject();
            if (!element.has("from") || !element.has("to")) continue;
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            cuboids.add(Cuboid.world(new Bounds(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                    from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(),
                    to.get(2).getAsDouble())));
        }
        for (JsonElement value : model.getAsJsonArray("elements")) {
            if (!value.isJsonObject()) continue;
            JsonObject element = value.getAsJsonObject();
            JsonObject faces = element.getAsJsonObject("faces");
            if (faces == null) continue;
            Bounds elementBounds = null;
            if (element.has("from") && element.has("to")) {
                JsonArray from = element.getAsJsonArray("from");
                JsonArray to = element.getAsJsonArray("to");
                elementBounds = new Bounds(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                        from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(),
                        to.get(2).getAsDouble());
            }
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject face = entry.getValue().getAsJsonObject();
                if (!face.has("cullface")) continue;
                Direction direction = Direction.valueOf(face.get("cullface").getAsString()
                        .toUpperCase(Locale.ROOT));
                if (elementBounds == null || !elementBounds.onBoundary(direction)
                        || !CuboidListModelProjection.coversFullBoundaryPlane(cuboids, direction)) {
                    face.remove("cullface");
                }
            }
        }
    }

    private static JsonObject referenceJson(ResourceManager manager, String path, String reference,
            Identifier target) {
        Identifier resourceId = Identifier.fromNamespaceAndPath("more_slabs_stairs_and_walls", path);
        Resource resource = manager.getResource(resourceId).orElseThrow(() ->
                new IllegalStateException("Missing native structural reference " + resourceId));
        try (var reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            replaceStrings(root, "more_slabs_stairs_and_walls:block/" + reference, model(target));
            return root;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot project native structural reference " + resourceId, exception);
        }
    }

    private static JsonObject wallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        JsonObject textures = new JsonObject();
        textures.addProperty("wall", texture(profile.textureRoles().side()));
        result.add("textures", textures);
        return result;
    }

    /** Accepted Oak Log wall route: bark on vertical faces and end grain on horizontal faces. */
    private static JsonObject columnWallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.add("textures", textures(profile));
        return result;
    }

    /** Accepted normal leaf-wall geometry; tint registration remains profile-controlled. */
    private static JsonObject leafWallTemplate(String parent, NibaruMaterialProfile profile) {
        JsonObject result = new JsonObject();
        result.addProperty("parent", parent);
        result.addProperty("render_type", "cutout_mipped");
        JsonObject textures = new JsonObject();
        textures.addProperty("wall", texture(profile.textureRoles().side()));
        result.add("textures", textures);
        return result;
    }

    private static JsonObject woodenBeamWallBlockState(Identifier id) {
        JsonArray multipart = new JsonArray();
        JsonObject post = new JsonObject();
        post.add("apply", selection(model(id) + "_post", 0, 0, true));
        multipart.add(post);
        int rotation = 0;
        for (String direction : List.of("north", "east", "south", "west")) {
            for (String height : List.of("low", "tall")) {
                JsonObject part = new JsonObject();
                JsonObject when = new JsonObject();
                when.addProperty(direction, height);
                part.add("when", when);
                part.add("apply", selection(model(id) + "_side", 0, rotation, true));
                multipart.add(part);
            }
            rotation += 90;
        }
        JsonObject root = new JsonObject();
        root.add("multipart", multipart);
        return root;
    }

    private static JsonObject woodenBeamWallPost(NibaruMaterialProfile profile) {
        JsonObject root = new JsonObject();
        root.addProperty("credit", "Made with Blockbench");
        JsonObject textures = new JsonObject();
        textures.addProperty("top", texture(profile.textureRoles().top()));
        textures.addProperty("particle", texture(profile.textureRoles().side()));
        textures.addProperty("sides", texture(profile.textureRoles().side()));
        root.add("textures", textures);
        JsonObject faces = new JsonObject();
        for (String direction : List.of("north", "east", "south", "west")) {
            faces.add(direction, texturedFace("#sides", 8, 0, 16, 16));
        }
        faces.add("up", texturedFace("#top", 8, 8, 16, 16));
        faces.add("down", texturedFace("#top", 16, 16, 8, 8));
        root.add("elements", elements(4, 0, 4, 12, 16, 12, faces));
        root.add("display", woodenBeamDisplay());
        return root;
    }

    private static JsonObject woodenBeamWallSide(NibaruMaterialProfile profile) {
        JsonObject root = new JsonObject();
        root.addProperty("credit", "Made with Blockbench");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture(profile.textureRoles().side()));
        // These aliases intentionally match BBB's authored Pale Oak side model: its vertical
        // faces are beam side grain and its horizontal faces expose the beam end grain.
        textures.addProperty("top", texture(profile.textureRoles().side()));
        textures.addProperty("sides", texture(profile.textureRoles().top()));
        root.add("textures", textures);
        JsonObject faces = new JsonObject();
        faces.add("north", texturedFace("#top", 8, 0, 16, 16));
        faces.add("east", texturedFace("#top", 8, 0, 12, 16));
        faces.add("south", texturedFace("#top", 8, 0, 16, 16));
        faces.add("west", texturedFace("#top", 12, 0, 16, 16));
        faces.add("up", texturedFace("#sides", 8, 12, 16, 16));
        faces.add("down", texturedFace("#sides", 16, 12, 8, 8));
        root.add("elements", elements(4, 0, 0, 12, 16, 4, faces));
        return root;
    }

    private static JsonArray elements(int x0, int y0, int z0, int x1, int y1, int z1,
            JsonObject faces) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(x0, y0, z0));
        element.add("to", numbers(x1, y1, z1));
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        return elements;
    }

    private static JsonObject texturedFace(String texture, int... uv) {
        JsonObject face = new JsonObject();
        face.add("uv", numbers(uv));
        face.addProperty("texture", texture);
        return face;
    }

    private static JsonObject woodenBeamDisplay() {
        JsonObject display = new JsonObject();
        display.add("thirdperson_righthand", displayTransform(
                new int[] {75, 45, 0}, new double[] {0, 2.5, 0}, new double[] {.375, .375, .375}));
        display.add("thirdperson_lefthand", displayTransform(
                new int[] {75, 45, 0}, new double[] {0, 2.5, 0}, new double[] {.375, .375, .375}));
        display.add("firstperson_righthand", displayTransform(
                new int[] {0, 45, 0}, null, new double[] {.4, .4, .4}));
        display.add("firstperson_lefthand", displayTransform(
                new int[] {0, 225, 0}, null, new double[] {.4, .4, .4}));
        display.add("ground", displayTransform(
                null, new double[] {0, 3, 0}, new double[] {.25, .25, .25}));
        display.add("gui", displayTransform(
                new int[] {30, 225, 0}, null, new double[] {.625, .625, .625}));
        display.add("fixed", displayTransform(null, null, new double[] {.5, .5, .5}));
        return display;
    }

    private static JsonObject displayTransform(int[] rotation, double[] translation, double[] scale) {
        JsonObject transform = new JsonObject();
        if (rotation != null) transform.add("rotation", numbers(rotation));
        if (translation != null) transform.add("translation", decimals(translation));
        if (scale != null) transform.add("scale", decimals(scale));
        return transform;
    }

    private static JsonObject parentModel(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    /** Complete tinted geometry for the standard forms whose vanilla parents have no tint index. */
    private static JsonObject cuboidModel(NibaruMaterialProfile profile, List<int[]> cuboids) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        root.add("textures", textures(profile));
        JsonArray elements = new JsonArray();
        for (int[] bounds : cuboids) {
            JsonObject element = new JsonObject();
            element.add("from", numbers(bounds[0], bounds[1], bounds[2]));
            element.add("to", numbers(bounds[3], bounds[4], bounds[5]));
            JsonObject faces = new JsonObject();
            for (Direction face : Direction.values()) {
                JsonObject encoded = new JsonObject();
                boolean cut = profile.visualProfile()
                        == games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile.HUGE_MUSHROOM
                        && !onBoundary(bounds, face);
                encoded.addProperty("texture", cut ? "#interior" : switch (face) {
                    case UP -> "#top";
                    case DOWN -> "#bottom";
                    default -> "#side";
                });
                if (profile.tintProfile() != TintProfile.NONE) encoded.addProperty("tintindex", 0);
                faces.add(face.getSerializedName(), encoded);
            }
            element.add("faces", faces);
            elements.add(element);
        }
        root.add("elements", elements);
        return root;
    }

    private static boolean onBoundary(int[] bounds, Direction face) {
        return switch (face) {
            case WEST -> bounds[0] == 0; case EAST -> bounds[3] == 16;
            case DOWN -> bounds[1] == 0; case UP -> bounds[4] == 16;
            case NORTH -> bounds[2] == 0; case SOUTH -> bounds[5] == 16;
        };
    }

    private static JsonObject textures(NibaruMaterialProfile profile) {
        JsonObject textures = new JsonObject();
        textures.addProperty("side", texture(profile.textureRoles().side()));
        textures.addProperty("top", texture(profile.textureRoles().top()));
        textures.addProperty("bottom", texture(profile.textureRoles().bottom()));
        textures.addProperty("particle", texture(profile.textureRoles().particle()));
        if (!profile.textureRoles().interior().isEmpty()) {
            textures.addProperty("interior", texture(profile.textureRoles().interior()));
        }
        return textures;
    }

    private static JsonObject templateBlockState(ResourceManager manager, Identifier resourceId,
            String sourceModel, String targetModel) {
        Resource resource = manager.getResource(resourceId).orElseThrow(() ->
                new IllegalStateException("Missing vanilla blockstate template " + resourceId));
        try (var reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            replaceStrings(root, sourceModel, targetModel);
            return root;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot project vanilla blockstate " + resourceId, exception);
        }
    }

    private static void replaceStrings(JsonElement value, String source, String target) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (String key : List.copyOf(object.keySet())) {
                JsonElement child = object.get(key);
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    object.addProperty(key, child.getAsString().replace(source, target));
                } else replaceStrings(child, source, target);
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) replaceStrings(child, source, target);
        }
    }

    private static JsonObject combinedLanguage() {
        JsonObject language = new JsonObject();
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".layers", "Layers");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".corners", "Corners");
        language.addProperty("tag.item." + CnmTerrainCompat.MOD_ID + ".quarter_columns", "Quarter Columns");
        for (LayerGeneratedData.Binding layer : LayerGeneratedData.bindings())
            language.addProperty(translation(layer.id()), LayerGeneratedResources.displayName(layer.profile()));
        for (QuarterGeometryGeneratedData.Binding corner :
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.CORNER))
            language.addProperty(translation(corner.id()), QuarterGeometryGeneratedResources.cornerDisplayName(corner.profile()));
        for (QuarterGeometryGeneratedData.Binding column :
                QuarterGeometryGeneratedData.bindings(BgeGeometryRole.QUARTER_COLUMN))
            language.addProperty(translation(column.id()), QuarterGeometryGeneratedResources.quarterColumnDisplayName(column.profile()));
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            if (dev.aero.cnmterraincompat.PrivateBeamFamilies.isPrivateBeam(binding.spec().id())) {
                language.addProperty(translation(id(binding.source())), AssetGenerator.langName(
                        dev.aero.cnmterraincompat.ExternalMaterialCatalog.displayNameBase(binding.spec())));
            }
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                if (role.getKey().equals("block") || role.getKey().equals("layer")
                        || role.getKey().equals("corner") || role.getKey().equals("quarter_column")) continue;
                if (!binding.isGeneratedRole(role.getKey())) continue;
                Identifier id = id(role.getValue());
                language.addProperty(translation(id), AssetGenerator.langName(
                        dev.aero.cnmterraincompat.ExternalMaterialCatalog.displayNameBase(binding.spec())
                                + "_" + role.getKey()));
            }
        }
        return language;
    }

    private static int writeModels(Map<String, JsonObject> models) {
        models.forEach((modelId, model) -> write(modelResource(modelId), model));
        return models.size();
    }
    private static JsonObject variants(JsonObject variants) { JsonObject root = new JsonObject(); root.add("variants", variants); return root; }
    private static JsonObject selection(String model) { return selection(model, 0, 0, false); }
    private static JsonObject selection(String model, int x, int y, boolean uvlock) {
        JsonObject value = new JsonObject(); value.addProperty("model", model);
        if (x != 0) value.addProperty("x", x); if (y != 0) value.addProperty("y", y);
        if (uvlock) value.addProperty("uvlock", true); return value;
    }
    private static JsonArray numbers(int... values) { JsonArray result = new JsonArray(); for (int value : values) result.add(value); return result; }
    private static JsonArray decimals(double... values) { JsonArray result = new JsonArray(); for (double value : values) result.add(value); return result; }
    private static String texture(String path) { return path.contains(":") ? path : "minecraft:block/" + path; }
    private static String translation(Identifier id) { return "block." + id.getNamespace() + "." + id.getPath().replace('/', '.'); }
    private static String model(Identifier id) { return id.getNamespace() + ":block/" + id.getPath(); }
    private static Identifier id(Block block) { return BuiltInRegistries.BLOCK.getKey(block); }
    private static Identifier blockState(Identifier id) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "blockstates/" + id.getPath() + ".json"); }
    private static Identifier item(Identifier id) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json"); }
    private static Identifier modelResource(Identifier id) { return modelResource(id, ""); }
    private static Identifier modelResource(Identifier id, String suffix) { return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/block/" + id.getPath() + suffix + ".json"); }
    private static Identifier modelResource(String model) { Identifier id = Identifier.parse(model); return Identifier.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json"); }
    private static void write(Identifier id, JsonElement json) { BgeGeneratedResourceWriter.write(id, json); }

    public record GenerationSummary(int familyCount, int blockStateCount, int modelCount, int itemCount) {}
}
