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
import java.util.Map;
import java.util.Set;

/** Exact, allowlisted external material sources. Provider lookup happens only at provider-entrypoint RETURN. */
public final class ExternalMaterialCatalog {
    public static final String PROFILE_VERSION = "bge-c92-explicit-family-reconstruction-v1";
    private static final List<Spec> SPECS = specs();
    private static final Set<String> REGISTERED_PROVIDERS = new LinkedHashSet<>();

    private ExternalMaterialCatalog() {}

    /** Called by optional pseudo-mixins after the named provider has registered all of its blocks. */
    public static synchronized void registerProvider(String provider) {
        if (!FabricLoader.getInstance().isModLoaded(provider) || !REGISTERED_PROVIDERS.add(provider)) return;
        if (provider.equals("enderscape")) PrivateBeamFamilies.registerRoots();
        List<Spec> specs = SPECS.stream().filter(spec -> spec.provider().equals(provider)).toList();
        if (specs.isEmpty()) throw new IllegalArgumentException("Unsupported external provider: " + provider);
        CnmTerrainCompat.registerExternalFamilies(provider, specs);
    }

    public static List<Spec> specs() {
        List<Spec> result = new ArrayList<>(List.of(
                uniform("ribbits:mossy_oak_planks", Set.of(BlockTags.MINEABLE_WITH_AXE),
                        standardRoles("ribbits:mossy_oak_planks")),
                hugeMushroom("ribbits:red_toadstool", "ribbits:block/red_toadstool"),
                hugeMushroom("ribbits:brown_toadstool", "ribbits:block/brown_toadstool"),
                hugeMushroom("ribbits:toadstool_stem", "ribbits:block/toadstool_stem"),
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
        result.addAll(bbbBeamSpecs());
        result.addAll(enderscapeSpecs());
        return List.copyOf(result);
    }

    /**
     * One requested display name is backed by {@code nebulite_block}; the provider does not
     * expose the guessed {@code block_of_nebulite} registry path.  The requested raw-magnia
     * block has no source entry in the controlled Enderscape 3.0.2+mc26.2 provider bytes, so it
     * remains a declared narrow exclusion rather than a fabricated BGE source identity.
     */
    public static List<RequestedExclusion> requestedEnderscapeExclusions() {
        return List.of(new RequestedExclusion(Identifier.parse("enderscape:block_of_raw_magnia"),
                "Enderscape 3.0.2+mc26.2 has no block/item/model registry entry for this requested ID."));
    }

    public static int sourceCount(String provider) {
        return (int) SPECS.stream().filter(spec -> spec.provider().equals(provider)).count();
    }

    private static Spec uniform(String id, Set<TagKey<Block>> tags,
            Map<String, Identifier> providerRoles) {
        return uniform(id, tags, providerRoles, ExternalMaterialStateBridge.forSource(Identifier.parse(id)));
    }

    private static Spec uniform(String id, Set<TagKey<Block>> tags,
            Map<String, Identifier> providerRoles, ExternalMaterialStateBridge materialStateBridge) {
        Identifier key = Identifier.parse(id);
        String texture = key.getNamespace() + ":block/" + key.getPath();
        return new Spec(key, key.getNamespace(), key, key, providerRoles, materialStateBridge,
                VisualProfile.UNIFORM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                texture, texture, texture, "", TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                tags, Set.of(), List.of());
    }

    private static Spec pillar(String id, String side, String end, ModBlocks strippedTarget) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, Map.of(), VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED, side, end, end, "",
                TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS),
                Set.copyOf(EnumSet.of(BehaviorCapability.STRIPPABLE)),
                List.of(new MaterialTransition(MaterialTransition.Type.STRIPPED, strippedTarget)));
    }

    /**
     * Generated Ribbits geometry is intentionally uniform: every externally visible face uses
     * the assigned source-block texture. Provider HugeMushroom direction flags and the
     * toadstool-inside texture belong only to the provider's source block, never BGE geometry.
     */
    private static Spec hugeMushroom(String id, String exterior) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, Map.of(), VisualProfile.UNIFORM,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM, exterior, exterior, exterior,
                "", TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID, Set.of(), Set.of(), List.of());
    }

    private static Spec leaves(String id, TintProfile tint) {
        Identifier key = Identifier.parse(id);
        String texture = key.getNamespace() + ":block/" + key.getPath();
        return new Spec(key, key.getNamespace(), key, key, Map.of(), VisualProfile.LEAVES_CUTOUT_TINTED,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture, "",
                tint, NibaruMaterialProfile.RenderLayer.CUTOUT_MIPPED,
                Set.of(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES),
                Set.copyOf(EnumSet.of(BehaviorCapability.LEAF_LIFECYCLE)), List.of());
    }

    /**
     * Exact Macaw's Paths catalog. Patterned families are rooted at the full patterned block and
     * reuse the provider's exact Slab and Stairs. The five requested plain soil families are
     * independently rooted at their Macaw Path blocks and generate all eight missing roles.
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
            result.add(new Spec(id, "mcwpaths", reference, id, standardRoles(id.toString()),
                    VisualProfile.UNIFORM,
                    NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture, "",
                    TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(BlockTags.MINEABLE_WITH_PICKAXE), Set.of(), List.of()));
        }
        for (String path : "podzol_path_block dirt_path_block gravel_path_block sand_path_block red_sand_path_block".split(" ")) {
            Identifier id = Identifier.fromNamespaceAndPath("mcwpaths", path);
            String texture = "mcwpaths:block/" + path.substring(0, path.length() - "_block".length());
            result.add(new Spec(id, "mcwpaths", id, id, Map.of(), VisualProfile.UNIFORM,
                    NibaruMaterialProfile.OrientationPolicy.UNIFORM, texture, texture, texture, "",
                    TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(BlockTags.MINEABLE_WITH_SHOVEL), Set.of(), List.of()));
        }
        return List.copyOf(result);
    }

    private static Map<String, Identifier> standardRoles(String source) {
        Identifier id = Identifier.parse(source);
        return Map.of(
                "slab", Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_slab"),
                "stairs", Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_stairs"));
    }

    /**
     * BBB's beam parent is directional, but its provider slab is keyed by {@code facing,type}
     * and its provider stair only by normal stair geometry. Neither can retain an independent
     * material {@code AXIS}. BGE therefore owns the canonical axis-aware slab and stair while
     * BBB retains its ordinary thin wooden wall. The wall's connection-state contract deliberately
     * has no material axis and must not take the BGE pillar/thick-post route.
     */
    private static List<Spec> bbbBeamSpecs() {
        List<Spec> result = new ArrayList<>();
        for (String material : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            Identifier id = Identifier.fromNamespaceAndPath("bbb", material + "_beam");
            String texture = "bbb:block/beam/" + material;
            result.add(new Spec(id, "bbb", id, id, Map.of(
                    "wall", Identifier.fromNamespaceAndPath("bbb", material + "_wall")),
                    VisualProfile.PILLAR, NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED,
                    texture, texture + "_top", texture + "_top", "", TintProfile.NONE,
                    NibaruMaterialProfile.RenderLayer.SOLID,
                    Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS), Set.of(), List.of()));
        }
        return List.copyOf(result);
    }

    /** Exact Enderscape allowlist, registered only after its own entrypoint completes. */
    private static List<Spec> enderscapeSpecs() {
        List<Spec> result = new ArrayList<>();

        // Logs, woods, stems, hyphae, and pillars retain their own resolved side/end contract.
        result.add(pillar("enderscape:veiled_log", "enderscape:block/veiled_log",
                "enderscape:block/veiled_log_top"));
        result.add(pillar("enderscape:veiled_wood", "enderscape:block/veiled_log",
                "enderscape:block/veiled_log"));
        result.add(pillar("enderscape:celestial_stem", "enderscape:block/celestial_stem",
                "enderscape:block/celestial_stem_top"));
        result.add(pillar("enderscape:celestial_hyphae", "enderscape:block/celestial_stem",
                "enderscape:block/celestial_stem"));
        result.add(pillar("enderscape:murublight_stem", "enderscape:block/murublight_stem",
                "enderscape:block/murublight_stem_top"));
        result.add(pillar("enderscape:murublight_hyphae", "enderscape:block/murublight_stem",
                "enderscape:block/murublight_stem"));
        result.add(pillar("enderscape:shadoline_pillar", "enderscape:block/shadoline_pillar",
                "enderscape:block/shadoline_pillar_top"));
        result.add(pillar("enderscape:dusk_purpur_pillar", "enderscape:block/dusk_purpur_pillar",
                "enderscape:block/dusk_purpur_pillar_top"));

        // These three ordinary families were explicitly requested. Their provider-owned standard
        // forms are selected by exact registry identity; Mirestone Bricks intentionally uses the
        // provider's singular role paths.
        result.add(uniform("enderscape:polished_end_stone", Set.of(BlockTags.MINEABLE_WITH_PICKAXE),
                Map.of("slab", Identifier.parse("enderscape:polished_end_stone_slab"),
                        "stairs", Identifier.parse("enderscape:polished_end_stone_stairs"),
                        "wall", Identifier.parse("enderscape:polished_end_stone_wall"))));
        result.add(uniform("enderscape:polished_veradite", Set.of(BlockTags.MINEABLE_WITH_PICKAXE),
                Map.of("slab", Identifier.parse("enderscape:polished_veradite_slab"),
                        "stairs", Identifier.parse("enderscape:polished_veradite_stairs"),
                        "wall", Identifier.parse("enderscape:polished_veradite_wall"))));
        result.add(uniform("enderscape:mirestone_bricks", Set.of(BlockTags.MINEABLE_WITH_PICKAXE),
                Map.of("slab", Identifier.parse("enderscape:mirestone_brick_slab"),
                        "stairs", Identifier.parse("enderscape:mirestone_brick_stairs"),
                        "wall", Identifier.parse("enderscape:mirestone_brick_wall"))));

        for (String path : List.of("chiseled_end_stone", "cracked_end_stone_bricks",
                "chiseled_purpur", "nebulite_block", "chiseled_shadoline", "chiseled_veradite",
                "chiseled_mirestone", "cracked_mirestone_bricks", "chiseled_kurodite",
                "alluring_magnia", "repulsive_magnia", "chiseled_dusk_purpur",
                "blistered_magnia", "celestial_cap", "murublight_cap",
                "end_lamp", "blinklamp", "raw_shadoline_block")) {
            result.add(uniform("enderscape:" + path, Set.of(BlockTags.MINEABLE_WITH_PICKAXE), Map.of()));
        }

        // These exact Enderscape plank parents keep their provider Slab/Stairs while BGE owns
        // the Wall and tail forms.  Their private Beam variants are linked into the same selector
        // family below, never registered as a second public provider source.
        for (String plank : List.of("veiled", "celestial", "murublight")) {
            result.add(uniform("enderscape:" + plank + "_planks", Set.of(BlockTags.MINEABLE_WITH_AXE),
                    standardRoles("enderscape:" + plank + "_planks")));
        }

        // The source's stress/shatter state remains provider-only.  BGE's ordinary geometry uses
        // the real stress-0 side/end contract and never invents block/void_shale.
        result.add(topSideBottom("enderscape:void_shale", "enderscape:block/void_shale_side",
                "enderscape:block/void_shale_end", "enderscape:block/void_shale_end"));

        result.add(leaves("enderscape:veiled_leaves", TintProfile.SOURCE_PROVIDER));
        result.add(fullBlock("enderscape:drift_jelly_block", VisualProfile.SLIME_INSET,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                "enderscape:block/drift_jelly_block", "enderscape:block/drift_jelly_block",
                "enderscape:block/drift_jelly_block", TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.TRANSLUCENT, Set.of(),
                Set.of(BehaviorCapability.SLIME_INTERACTION)));

        // Match Crimson Nylium's BGE face/UV family: its exposed side overlay is structural, not
        // a provider behavior claim.  Only the material textures differ.
        result.add(nylium("enderscape:veiled_end_stone", "enderscape:block/veiled_end_stone_side",
                "enderscape:block/veiled_end_stone_top", "minecraft:block/end_stone"));
        result.add(nylium("enderscape:celestial_overgrowth",
                "enderscape:block/celestial_overgrowth_side", "enderscape:block/celestial_overgrowth_top",
                "minecraft:block/end_stone"));
        result.add(nylium("enderscape:corrupt_overgrowth",
                "enderscape:block/corrupt_overgrowth_side", "enderscape:block/corrupt_overgrowth_top",
                "enderscape:block/mirestone"));

        result.add(path("enderscape:celestial_path", "enderscape:block/celestial_path_side",
                "enderscape:block/celestial_path_top", "minecraft:block/end_stone"));
        result.add(path("enderscape:corrupt_path", "enderscape:block/corrupt_path_side",
                "enderscape:block/corrupt_path_top", "enderscape:block/mirestone"));

        // Exact stripped provider sources, with their own cube-column side/end contracts.  Do
        // not create stripping transitions beyond the canonical stripped source blocks.
        result.add(pillar("enderscape:stripped_veiled_log", "enderscape:block/stripped_veiled_log",
                "enderscape:block/stripped_veiled_log_top"));
        result.add(pillar("enderscape:stripped_veiled_wood", "enderscape:block/stripped_veiled_log",
                "enderscape:block/stripped_veiled_log"));
        result.add(pillar("enderscape:stripped_celestial_stem", "enderscape:block/stripped_celestial_stem",
                "enderscape:block/stripped_celestial_stem_top"));
        result.add(pillar("enderscape:stripped_celestial_hyphae", "enderscape:block/stripped_celestial_stem",
                "enderscape:block/stripped_celestial_stem"));
        result.add(pillar("enderscape:stripped_murublight_stem", "enderscape:block/stripped_murublight_stem",
                "enderscape:block/stripped_murublight_stem_top"));
        result.add(pillar("enderscape:stripped_murublight_hyphae", "enderscape:block/stripped_murublight_stem",
                "enderscape:block/stripped_murublight_stem"));
        result.addAll(PrivateBeamFamilies.specs());
        return List.copyOf(result);
    }

    public static boolean usesWoodenWall(Spec spec) {
        return PrivateBeamFamilies.usesWoodenWall(spec);
    }

    public static String displayNameBase(Spec spec) {
        String path = PrivateBeamFamilies.displayName(spec.id());
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /** External axial sources intentionally do not fabricate an unrequested strip transition. */
    private static Spec pillar(String id, String side, String end) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, Map.of(), VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED, side, end, end, "",
                TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS), Set.of(), List.of());
    }

    private static Spec topSideBottom(String id, String side, String top, String bottom) {
        return fullBlock(id, VisualProfile.TOP_SIDE_BOTTOM,
                NibaruMaterialProfile.OrientationPolicy.UNIFORM, side, top, bottom,
                TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_PICKAXE), Set.of());
    }

    private static Spec nylium(String id, String side, String top, String bottom) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, Map.of(), ExternalMaterialStateBridge.NONE,
                VisualProfile.TOP_SIDE_BOTTOM, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                side, top, bottom, side, "", TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID, Set.of(BlockTags.MINEABLE_WITH_PICKAXE),
                Set.of(), List.of());
    }

    private static Spec path(String id, String side, String top, String bottom) {
        return fullBlock(id, VisualProfile.PATH, NibaruMaterialProfile.OrientationPolicy.UNIFORM,
                side, top, bottom, TintProfile.NONE, NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_PICKAXE), Set.of());
    }

    private static Spec fullBlock(String id, VisualProfile visual,
            NibaruMaterialProfile.OrientationPolicy orientation, String side, String top, String bottom,
            TintProfile tint, NibaruMaterialProfile.RenderLayer renderLayer, Set<TagKey<Block>> tags,
            Set<BehaviorCapability> capabilities) {
        Identifier key = Identifier.parse(id);
        return new Spec(key, key.getNamespace(), key, key, Map.of(), visual, orientation,
                side, top, bottom, "", tint, renderLayer, tags, capabilities, List.of());
    }

    public record Spec(Identifier id, String provider, Identifier providerReference,
            Identifier generatedIdentity, Map<String, Identifier> providerRoles,
            ExternalMaterialStateBridge materialStateBridge, VisualProfile visual,
            NibaruMaterialProfile.OrientationPolicy orientation,
            String side, String top, String bottom, String overlay, String interior,
            TintProfile tint, NibaruMaterialProfile.RenderLayer renderLayer,
            Set<TagKey<Block>> blockTags, Set<BehaviorCapability> capabilities,
            List<MaterialTransition> transitions) {
        public Spec {
            if (!provider.equals(providerReference.getNamespace())) {
                throw new IllegalArgumentException("Provider reference must be owned by " + provider);
            }
            providerRoles = Map.copyOf(providerRoles);
            materialStateBridge = materialStateBridge == null ? ExternalMaterialStateBridge.NONE : materialStateBridge;
            for (Map.Entry<String, Identifier> role : providerRoles.entrySet()) {
                if (!Set.of("slab", "stairs", "wall").contains(role.getKey())) {
                    throw new IllegalArgumentException("Unsupported provider geometry role " + role.getKey());
                }
                if (!provider.equals(role.getValue().getNamespace())) {
                    throw new IllegalArgumentException("Provider geometry must be owned by " + provider);
                }
            }
            blockTags = Set.copyOf(blockTags);
            capabilities = Set.copyOf(capabilities);
            transitions = List.copyOf(transitions);
        }

        /** Existing declaration shape with no explicit overlay. */
        public Spec(Identifier id, String provider, Identifier providerReference,
                Identifier generatedIdentity, Map<String, Identifier> providerRoles,
                ExternalMaterialStateBridge materialStateBridge, VisualProfile visual,
                NibaruMaterialProfile.OrientationPolicy orientation,
                String side, String top, String bottom, String interior,
                TintProfile tint, NibaruMaterialProfile.RenderLayer renderLayer,
                Set<TagKey<Block>> blockTags, Set<BehaviorCapability> capabilities,
                List<MaterialTransition> transitions) {
            this(id, provider, providerReference, generatedIdentity, providerRoles, materialStateBridge,
                    visual, orientation, side, top, bottom, "", interior, tint, renderLayer,
                    blockTags, capabilities, transitions);
        }

        /** Source-compatible constructor for ordinary external profiles with no material bridge. */
        public Spec(Identifier id, String provider, Identifier providerReference,
                Identifier generatedIdentity, Map<String, Identifier> providerRoles, VisualProfile visual,
                NibaruMaterialProfile.OrientationPolicy orientation,
                String side, String top, String bottom, String interior,
                TintProfile tint, NibaruMaterialProfile.RenderLayer renderLayer,
                Set<TagKey<Block>> blockTags, Set<BehaviorCapability> capabilities,
                List<MaterialTransition> transitions) {
            this(id, provider, providerReference, generatedIdentity, providerRoles,
                    ExternalMaterialStateBridge.forSource(id), visual, orientation,
                    side, top, bottom, "", interior, tint, renderLayer, blockTags, capabilities, transitions);
        }
    }

    public record RequestedExclusion(Identifier requestedId, String reason) {}
}
