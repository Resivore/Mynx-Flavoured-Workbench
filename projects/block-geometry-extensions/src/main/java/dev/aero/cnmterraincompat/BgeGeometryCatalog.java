package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable, ordered public catalog of BGE-derived geometry.
 *
 * <p>Persistence identity is deliberately independent from selector order. Consumers must use
 * {@link Descriptor#persistenceId()} or {@link Descriptor#key()} for saved/network identity and
 * {@link Descriptor#selectorOrder()} only for presentation. This keeps Layer's established ID
 * while presenting newly added geometry before it.</p>
 */
public final class BgeGeometryCatalog {
    private static final List<Descriptor> ORDERED = List.of(
            descriptor("vertical_slab", BgeGeometryRole.VERTICAL_SLAB, 4, 4, "Vertical Slab"),
            descriptor("step", BgeGeometryRole.STEP, 5, 5, "Step"),
            descriptor("corner", BgeGeometryRole.CORNER, 7, 6, "Corner"),
            descriptor("quarter_column", BgeGeometryRole.QUARTER_COLUMN, 8, 7, "Quarter Column"),
            descriptor("layer", BgeGeometryRole.LAYER, 6, 8, "Layer"));

    private BgeGeometryCatalog() {}

    /** BGE-derived selector entries in deterministic presentation order. */
    public static List<Descriptor> ordered() {
        return ORDERED;
    }

    /** Resolves a stable saved/network identity without relying on enum ordinal. */
    public static Optional<Descriptor> byPersistenceId(int persistenceId) {
        return ORDERED.stream().filter(entry -> entry.persistenceId() == persistenceId).findFirst();
    }

    /** Resolves a stable namespaced role key. */
    public static Optional<Descriptor> byKey(Identifier key) {
        Objects.requireNonNull(key, "key");
        return ORDERED.stream().filter(entry -> entry.key().equals(key)).findFirst();
    }

    private static Descriptor descriptor(String path, BgeGeometryRole role,
            int persistenceId, int selectorOrder, String displayName) {
        return new Descriptor(Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID, path),
                role, persistenceId, selectorOrder, displayName);
    }

    public record Descriptor(Identifier key, BgeGeometryRole role, int persistenceId,
            int selectorOrder, String displayName) {
        public Descriptor {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(displayName, "displayName");
            if (persistenceId < 0) throw new IllegalArgumentException("Negative persistence ID");
            if (selectorOrder < 0) throw new IllegalArgumentException("Negative selector order");
            if (displayName.isBlank()) throw new IllegalArgumentException("Blank display name");
        }

        /** Whether this exact role has a registered generated block for the material profile. */
        public boolean isAvailable(NibaruMaterialProfile profile) {
            return resolveBlock(profile).isPresent();
        }

        /** Resolves the exact canonical-material geometry block registered by BGE. */
        public Optional<Block> resolveBlock(NibaruMaterialProfile profile) {
            Objects.requireNonNull(profile, "profile");
            return NibaruProviderAdapter.derived(profile, role);
        }

        /** Short alias for consumers whose placement path is block-oriented. */
        public Optional<Block> resolve(NibaruMaterialProfile profile) {
            return resolveBlock(profile);
        }

        /** Resolves the registered BlockItem used for selector icons and normal placement. */
        public Optional<Item> resolveItem(NibaruMaterialProfile profile) {
            return resolveBlock(profile).map(Block::asItem);
        }
    }
}
