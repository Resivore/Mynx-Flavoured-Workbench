package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Narrow, data-only bridge for material providers which BGE deliberately does
 * not depend on.  Keeping the IDs here makes save identities deterministic
 * while lookup remains harmless when the supplying mod is absent.
 */
public final class ExternalMaterialCatalog {
    private static final List<Spec> SPECS = specs();

    private ExternalMaterialCatalog() {}

    public static List<NibaruMaterialProfile> presentProfiles() {
        List<NibaruMaterialProfile> result = new ArrayList<>();
        for (Spec spec : SPECS) {
            if (!FabricLoader.getInstance().isModLoaded(spec.id.getNamespace())) continue;
            Block block = BuiltInRegistries.BLOCK.getValue(spec.id);
            if (block == null || !BuiltInRegistries.BLOCK.getKey(block).equals(spec.id)) continue;
            result.add(new NibaruMaterialProfile("bge-c59-external-v1", null, block, spec.id,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(block), Optional.of(block),
                    Optional.empty(), Optional.empty(), Optional.empty(), Set.of(), Set.of(), spec.visual,
                    NibaruMaterialProfile.VisualSupport.GENERIC_SUPPORTED,
                    games.twinhead.moreslabsstairsandwalls.api.material.TintProfile.NONE,
                    NibaruMaterialProfile.RenderLayer.SOLID, spec.orientation,
                    NibaruMaterialProfile.SurfaceSamplingPolicy.BLOCK_ABSOLUTE,
                    NibaruMaterialProfile.DoubleFormPolicy.COMPOSE_SEMANTIC_SURFACES,
                    new NibaruMaterialProfile.TextureRoles(spec.side, spec.top, spec.bottom, "", spec.side),
                    Optional.empty(), Optional.empty(), false, List.of()));
        }
        return List.copyOf(result);
    }

    private static Spec uniform(String id) {
        Identifier key = Identifier.parse(id);
        String texture = key.getNamespace() + ":block/" + key.getPath();
        return new Spec(key, VisualProfile.UNIFORM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                texture, texture, texture);
    }

    private static Spec pillar(String id, String side, String end) {
        return new Spec(Identifier.parse(id), VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED, side, end, end);
    }

    private static List<Spec> specs() {
        List<Spec> result = new ArrayList<>(List.of(
                uniform("ribbits:mossy_oak_planks"),
                pillar("mynx_trees:wisteria_log", "mynx_trees:block/wisteria_log", "mynx_trees:block/wisteria_log_top"),
                pillar("mynx_trees:wisteria_wood", "mynx_trees:block/wisteria_log", "mynx_trees:block/wisteria_log"),
                pillar("mynx_trees:silver_birch_log", "mynx_trees:block/silver_birch_log", "minecraft:block/birch_log_top"),
                pillar("mynx_trees:silver_birch_wood", "mynx_trees:block/silver_birch_log", "mynx_trees:block/silver_birch_log")));
        result.addAll(pathSpecs());
        return List.copyOf(result);
    }

    /** Exact path-block registry catalog from mcw-paths 1.1.1 for Minecraft 26.2. */

    private static List<Spec> pathSpecs() {
        List<Spec> result = new ArrayList<>();
        String[] materials = "andesite diorite granite sandstone red_sandstone brick stone mossy_stone cobbled_deepslate deepslate mud_brick blackstone dark_prismarine".split(" ");
        String[] patterns = "running_bond windmill_weave flagstone crystal_floor".split(" ");
        for (String material : materials) for (String pattern : patterns) {
            String id = material + "_" + pattern + "_path";
            Identifier key = Identifier.fromNamespaceAndPath("mcwpaths", id);
            // Macaw's path models are one-material surfaces.  The generated full-volume geometry
            // intentionally samples that material on each role rather than copying its source model.
            String texture = "mcwpaths:block/" + (id.endsWith("_path") ? id.substring(0, id.length() - 5) : id);
            result.add(new Spec(key, VisualProfile.UNIFORM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                    texture, texture, texture));
        }
        for (String id : "podzol_path_block dirt_path_block gravel_path_block sand_path_block red_sand_path_block".split(" ")) {
            Identifier key = Identifier.fromNamespaceAndPath("mcwpaths", id);
            String texture = "mcwpaths:block/" + id.substring(0, id.length() - "_block".length());
            result.add(new Spec(key, VisualProfile.UNIFORM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                    texture, texture, texture));
        }
        return result;
    }

    private record Spec(Identifier id, VisualProfile visual, NibaruMaterialProfile.OrientationPolicy orientation,
                        String side, String top, String bottom) {}
}
