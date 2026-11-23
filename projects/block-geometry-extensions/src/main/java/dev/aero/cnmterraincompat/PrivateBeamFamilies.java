package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.TintProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.VisualProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Set;

/**
 * The private Beam roots are BGE code identities whose texture bytes are assembled only into a
 * local private build.  This class intentionally contains identifiers and generation policy, not
 * any BBB model or texture bytes.
 */
public final class PrivateBeamFamilies {
    private static final List<Definition> DEFINITIONS = List.of(
            definition("enderscape", "veiled"), definition("enderscape", "celestial"),
            definition("enderscape", "murublight"), definition("ribbits", "mossy_oak"),
            definition("mynx_trees", "silver_birch"), definition("mynx_trees", "wisteria"));

    private PrivateBeamFamilies() {}

    /** Runs only after the provider has registered the exact plank parents. */
    static void registerRoots(String provider) {
        for (Definition definition : DEFINITIONS) {
            if (!definition.planks().getNamespace().equals(provider)) continue;
            if (provider.equals("mynx_trees") && !BuiltInRegistries.BLOCK.containsKey(definition.planks())) continue;
            Block existing = BuiltInRegistries.BLOCK.getValue(definition.root());
            if (definition.root().equals(BuiltInRegistries.BLOCK.getKey(existing))) continue;
            Block plank = BuiltInRegistries.BLOCK.getValue(definition.planks());
            if (!definition.planks().equals(BuiltInRegistries.BLOCK.getKey(plank))) {
                throw new IllegalStateException("Provider did not register private Beam plank parent "
                        + definition.planks());
            }
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(plank)
                    .setId(ResourceKey.create(Registries.BLOCK, definition.root()));
            CnmTerrainCompat.register(definition.root(), new RotatedPillarBlock(properties));
        }
    }

    static List<ExternalMaterialCatalog.Spec> specs() {
        return DEFINITIONS.stream().map(definition -> new ExternalMaterialCatalog.Spec(
                definition.root(), definition.planks().getNamespace(), definition.planks(),
                Identifier.fromNamespaceAndPath(definition.planks().getNamespace(), definition.family() + "_beam"),
                java.util.Map.of(), VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED,
                texture(definition, ""), texture(definition, "_top"),
                texture(definition, "_top"), "", TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS), Set.of(), List.of())).toList();
    }

    static boolean usesWoodenWall(ExternalMaterialCatalog.Spec spec) {
        // BBB's Pale Oak Beam wall and Nibaru's Pale Oak Planks wall are deliberately
        // different contracts.  Only the private Beam roots use BBB's thin post/arm wall;
        // Enderscape planks retain the ordinary vanilla WallBlock topology.
        return DEFINITIONS.stream().anyMatch(definition -> definition.root().equals(spec.id()));
    }

    public static boolean isPrivateBeam(Identifier source) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.root().equals(source));
    }

    static String displayName(Identifier source) {
        return DEFINITIONS.stream().filter(definition -> definition.root().equals(source))
                .map(definition -> definition.family() + "_beam").findFirst().orElse(source.getPath());
    }

    static List<Definition> definitions() { return DEFINITIONS; }

    private static Definition definition(String provider, String family) {
        return new Definition(family, Identifier.fromNamespaceAndPath(provider, family + "_planks"),
                Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                        provider + "/" + family + "_beam"));
    }

    private static String texture(Definition definition, String suffix) {
        return CnmTerrainCompat.MOD_ID + ":block/private/" + definition.planks().getNamespace() + "/" + definition.family() + "_beam" + suffix;
    }

    record Definition(String family, Identifier planks, Identifier root) {}
}
