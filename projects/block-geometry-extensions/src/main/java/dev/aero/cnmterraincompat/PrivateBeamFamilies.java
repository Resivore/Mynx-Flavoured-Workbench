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
import java.util.Optional;
import java.util.Set;

/**
 * The three C91 Beam roots are BGE code identities whose texture bytes are assembled only into a
 * local private build.  This class intentionally contains identifiers and generation policy, not
 * any BBB model or texture bytes.
 */
public final class PrivateBeamFamilies {
    private static final List<Definition> DEFINITIONS = List.of(
            definition("veiled"), definition("celestial"), definition("murublight"));

    private PrivateBeamFamilies() {}

    /** Runs only after Enderscape has registered the exact plank parents. */
    static void registerRoots() {
        for (Definition definition : DEFINITIONS) {
            Block existing = BuiltInRegistries.BLOCK.getValue(definition.root());
            if (definition.root().equals(BuiltInRegistries.BLOCK.getKey(existing))) continue;
            Block plank = BuiltInRegistries.BLOCK.getValue(definition.planks());
            if (!definition.planks().equals(BuiltInRegistries.BLOCK.getKey(plank))) {
                throw new IllegalStateException("Enderscape did not register private Beam plank parent "
                        + definition.planks());
            }
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(plank)
                    .setId(ResourceKey.create(Registries.BLOCK, definition.root()));
            CnmTerrainCompat.register(definition.root(), new RotatedPillarBlock(properties));
        }
    }

    static List<ExternalMaterialCatalog.Spec> specs() {
        return DEFINITIONS.stream().map(definition -> new ExternalMaterialCatalog.Spec(
                definition.root(), "enderscape", definition.planks(),
                Identifier.fromNamespaceAndPath("enderscape", definition.family() + "_beam"),
                java.util.Map.of(), VisualProfile.PILLAR,
                NibaruMaterialProfile.OrientationPolicy.AXIS_ALIGNED,
                texture(definition.family(), ""), texture(definition.family(), "_top"),
                texture(definition.family(), "_top"), "", TintProfile.NONE,
                NibaruMaterialProfile.RenderLayer.SOLID,
                Set.of(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS), Set.of(), List.of())).toList();
    }

    static Optional<Identifier> selectorParent(Identifier source) {
        return DEFINITIONS.stream().filter(definition -> definition.root().equals(source))
                .map(Definition::planks).findFirst();
    }

    static boolean usesWoodenWall(ExternalMaterialCatalog.Spec spec) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.root().equals(spec.id())
                || definition.planks().equals(spec.id()));
    }

    /** Keeps each private Beam immediately after its corresponding public plank family. */
    static String selectorOrderKey(Identifier canonicalParent) {
        for (Definition definition : DEFINITIONS) {
            if (definition.planks().equals(canonicalParent)) return definition.planks() + "/0";
            if (definition.root().equals(canonicalParent)) return definition.planks() + "/1";
        }
        return canonicalParent.toString() + "/0";
    }

    public static boolean isPrivateBeam(Identifier source) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.root().equals(source));
    }

    static String displayName(Identifier source) {
        return DEFINITIONS.stream().filter(definition -> definition.root().equals(source))
                .map(definition -> definition.family() + "_beam").findFirst().orElse(source.getPath());
    }

    static List<Definition> definitions() { return DEFINITIONS; }

    private static Definition definition(String family) {
        return new Definition(family, Identifier.fromNamespaceAndPath("enderscape", family + "_planks"),
                Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                        "enderscape/" + family + "_beam"));
    }

    private static String texture(String family, String suffix) {
        return CnmTerrainCompat.MOD_ID + ":block/private/enderscape/" + family + "_beam" + suffix;
    }

    record Definition(String family, Identifier planks, Identifier root) {}
}
