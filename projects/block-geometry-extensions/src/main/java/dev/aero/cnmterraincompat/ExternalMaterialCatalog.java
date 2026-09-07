package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Exact, allowlisted external material sources. Provider lookup happens only at provider-entrypoint RETURN. */
public final class ExternalMaterialCatalog {
    public static final String PROFILE_VERSION = "bge-c61-runtime-fixes-v1";
    private static final List<Spec> SPECS = specs();
    private static final Set<String> REGISTERED_PROVIDERS = new LinkedHashSet<>();

    private ExternalMaterialCatalog() {}

    /** Called by optional pseudo-mixins after the named provider has registered all of its blocks. */
    public static synchronized void registerProvider(String provider) {
        if (!FabricLoader.getInstance().isModLoaded(provider) || !REGISTERED_PROVIDERS.add(provider)) return;
        List<Spec> specs = SPECS.stream().filter(spec -> spec.provider().equals(provider)).toList();
        if (specs.isEmpty()) throw new IllegalArgumentException("Unsupported external provider: " + provider);
        CnmTerrainCompat.registerExternalFamilies(provider, specs);
    }

    public static List<Spec> specs() {
        List<Spec> result = new ArrayList<>(List.of(
                uniform("ribbits:mossy_oak_planks", Set.of(BlockTags.MINEABLE_WITH_AXE)),
                pillar("mynx_trees:wisteria_log", "mynx_trees:block/wisteria_log",
                        "mynx_trees:block/wisteria_log_top", ModBlocks.PALE_OAK_LOG),
                pillar("mynx_trees:wisteria_wood", "mynx_trees:block/wisteria_log",
                        "mynx_trees:block/wisteria_log", ModBlocks.PALE_OAK_WOOD),
                leaves("mynx_trees:wisteria_leaves", TintProfile.NONE),
                pillar("mynx_trees:silver_birch_log", "mynx_trees:block/silver_birch_log",
                        "minecraft:block/birch_log_top", ModBlocks.BIRCH_LOG),
                pillar("mynx_trees:silver_birch_wood", "mynx_trees:block/silver_birch_log",
                        "mynx_trees:block/silver_birch_log", ModBlocks.BIRCH_WOOD),
                // This provider has a live custom color function, not vanilla birch foliage.
                leaves("mynx_trees:silver_birch_leaves", TintProfile.SOURCE_PROVIDER)));
        result.addAll(pathSpecs());
        return List.copyOf(result);
    }

    public static int sourceCount(String provider) {
        return (int) SPECS.stream().filter(spec -> spec.provider().equals(provider)).count();
    }

    private static Spec uniform(String id, Set<TagKey<Block>> tags) {
        Identifier key = Identifier.parse(id);
        String texture = key.getNamespace() + ":block/" + key.getPath();
        return new Spec(key, key.getNamespace(), key, key, VisualProfile.UNIFORM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                texture, texture, texture, TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                tags, Set.of(), List.of());
    }

    private static Spec pillar(String id, String side, String end, ModBlocks strippedTarget) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED, side, end, end,
                TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS),
                Set.copyOf(EnumSet.of(BehaviorCapability.STRIPPABLE)),
                List.of(new MaterialTransition(MaterialTransition.Type.STRIPPED, strippedTarget)));
    }

    private static Spec leaves(String id, TintProfile tint) {
        Identifier key = Identifier.parse(id);
        String texture = key.getNamespace() + ":block/" + key.getPath();
        return new Spec(key, key.getNamespace(), key, key, VisualProfile.LEAVES_CUTOUT_TINTED,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture,
                tint, NibaruMaterialProfile.RenderLayer.CUTOUT_MIPPED,
                Set.of(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES),
                Set.copyOf(EnumSet.of(BehaviorCapability.LEAF_LIFECYCLE)), List.of());
    }

    /**
     * Exact Macaw's Paths catalog.  C60 accidentally rooted these families at the lowered Path
     * blocks.  The Path ID remains an explicit provider reference and generated-ID compatibility
     * key, while the normal full block is the only canonical BGE/ShapeMap parent.
     */
    private static List<Spec> pathSpecs() {
        List<Spec> result = new ArrayList<>();
        String[] materials = "andesite diorite granite sandstone red_sandstone brick stone mossy_stone cobbled_deepslate deepslate mud_brick blackstone dark_prismarine".split(" ");
        String[] patterns = "running_bond windmill_weave flagstone crystal_floor".split(" ");
        for (String material : materials) for (String pattern : patterns) {
            String full = material + "_" + pattern;
            String path = full + "_path";
            Identifier id = Identifier.fromNamespaceAndPath("mcwpaths", full);
            Identifier reference = Identifier.fromNamespaceAndPath("mcwpaths", path);
            String texture = "mcwpaths:block/" + full;
            result.add(new Spec(id, "mcwpaths", reference, reference, VisualProfile.UNIFORM,
                    NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture,
                    TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(BlockTags.MINEABLE_WITH_PICKAXE), Set.of(), List.of()));
        }
        for (String path : "podzol_path_block dirt_path_block gravel_path_block sand_path_block red_sand_path_block".split(" ")) {
            String full = switch (path) {
                case "podzol_path_block" -> "podzol";
                case "dirt_path_block" -> "dirt";
                case "gravel_path_block" -> "gravel";
                case "sand_path_block" -> "sand";
                case "red_sand_path_block" -> "red_sand";
                default -> throw new IllegalArgumentException(path);
            };
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", full);
            Identifier reference = Identifier.fromNamespaceAndPath("mcwpaths", path);
            String texture = "mcwpaths:block/" + path.substring(0, path.length() - "_block".length());
            result.add(new Spec(id, "mcwpaths", reference, reference, VisualProfile.UNIFORM,
                    NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture,
                    TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(BlockTags.MINEABLE_WITH_SHOVEL), Set.of(), List.of()));
        }
        return List.copyOf(result);
    }

    public record Spec(Identifier id, String provider, Identifier providerReference,
            Identifier generatedIdentity, VisualProfile visual,
            NibaruMaterialProfile.OrientationPolicy orientation,
            String side, String top, String bottom,
            TintProfile tint, NibaruMaterialProfile.RenderLayer renderLayer,
            Set<TagKey<Block>> blockTags, Set<BehaviorCapability> capabilities,
            List<MaterialTransition> transitions) {
        public Spec {
            if (!provider.equals(providerReference.getNamespace())) {
                throw new IllegalArgumentException("Provider reference must be owned by " + provider);
            }
            blockTags = Set.copyOf(blockTags);
            capabilities = Set.copyOf(capabilities);
            transitions = List.copyOf(transitions);
        }
    }
}
