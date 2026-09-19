package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * BGE's deliberately narrow two-phase integration with CNM's variant and ShapeMap lifecycles.
 *
 * <p>Phase A records only sources that CNM actually admitted while creating one of its own
 * variants. It neither scans the registry nor derives a family from names, block classes, or
 * assets. A typed BGE material profile is useful only to identify BGE's already-registered local
 * roles; it is not exposed as the candidate's ShapeMap parent.</p>
 *
 * <p>Phase B first snapshots CNM's supplied mapping list, then appends BGE's three local roles
 * only when the exact Phase-A anchor was present in that snapshot. CNM resolves the resulting
 * graph and selects its parent. The tail audit accepts a candidate only when CNM selected the
 * profile's exact material parent; every other candidate loses only the edge introduced by this
 * bridge and remains dormant. Established provider-owned profile edges are never displaced.</p>
 */
public final class CnmShapeMapCandidateBridge {
    public static final String PROFILE_VERSION = "bge-c80-cnm-two-phase-v1";
    private static final Identifier SHAPE_MAP_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "cnm_candidate_roles");
    /* Lower than every BGE ownership edge: this edge can join a CNM family but never win its parent. */
    private static final int INJECTION_PRIORITY = Integer.MIN_VALUE;
    private static final Map<Block, Candidate> CANDIDATES = new IdentityHashMap<>();
    private static final Set<Block> RECURSIVE_SOURCES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private CnmShapeMapCandidateBridge() {}

    /**
     * Phase A. Called only from the two CNM constructors after CNM accepted the source's real
     * state contract and created its own role. The generated registry ID is supplied by CNM's
     * exact registerVariants path rather than synthesized from the source's registry name.
     */
    public static synchronized void admit(Block source, BgeGeometryRole role, Block generated,
            Identifier generatedId) {
        if (CanonicalGeometryRegistry.contains(source)) {
            RECURSIVE_SOURCES.add(source);
            return;
        }
        Identifier sourceId = registeredBlockId(source);
        Candidate candidate = CANDIDATES.computeIfAbsent(source, ignored -> new Candidate(source, sourceId));
        candidate.recordCnmRole(role, generated, generatedId);
    }

    /**
     * Completes Phase A at the registry tail. All BGE-owned roles are already registered by the
     * normal BGE catalog pass at this point. Untyped candidates deliberately remain dormant: BGE
     * creates no speculative block, item, resource, or canonical-parent binding for them.
     */
    public static synchronized void finishRegistryAdmission() {
        for (Candidate candidate : CANDIDATES.values()) {
            if (candidate.phase != Phase.REGISTRY_ADMITTED) continue;
            NibaruMaterialProfile profile = NibaruMaterialProfiles.fromBlock(candidate.source).orElse(null);
            // Optional-provider and structural-vanilla profiles already have an independently
            // typed BGE catalog contract. Their existing bridge stays in place, including the
            // deliberate shared Log/Wood ShapeMap components. C80 owns only native BGE profiles
            // whose CNM-created role is part of this registry-time admission lifecycle.
            if (profile == null) {
                candidate.phase = Phase.DORMANT_UNTYPED;
                continue;
            }
            if (profile.family() == null) {
                candidate.phase = Phase.DORMANT_STATIC_PROFILE;
                continue;
            }
            EnumMap<BgeGeometryRole, Block> roles = new EnumMap<>(BgeGeometryRole.class);
            for (BgeGeometryRole role : List.of(BgeGeometryRole.LAYER,
                    BgeGeometryRole.CORNER, BgeGeometryRole.QUARTER_COLUMN)) {
                Block block = NibaruProviderAdapter.derived(profile, role).orElse(null);
                if (block == null) {
                    candidate.phase = Phase.DORMANT_INCOMPLETE;
                    roles.clear();
                    break;
                }
                roles.put(role, block);
            }
            if (candidate.phase == Phase.REGISTRY_ADMITTED) {
                candidate.profile = profile;
                candidate.bgeRoles = roles;
                candidate.phase = Phase.REGISTRY_READY;
            }
        }
    }

    /**
     * Captures membership from the Mapping objects CNM is about to resolve. This intentionally
     * runs before BGE's historical profile-edge pass, so a BGE edge cannot make a candidate look
     * as though a datapack/regex/tag rule admitted it.
     */
    public static synchronized void observeCnmMappings(List<ShapeMap.Mapping> mappings) {
        for (Candidate candidate : CANDIDATES.values()) {
            if (candidate.phase == Phase.REGISTRY_READY
                    && mappingMentions(mappings, candidate.source.asItem())) {
                candidate.cnmMappingPresent = true;
            }
        }
    }

    /** Pure mapping-list predicate used by the non-pillar focused regression fixture. */
    public static boolean mappingMentions(List<ShapeMap.Mapping> mappings, Item anchor) {
        return mappings.stream().anyMatch(mapping -> mapping.parent() == anchor || mapping.shape() == anchor);
    }

    /**
     * Phase B pre-resolution. At most one actual mapped source for a typed profile injects the
     * local BGE roles. The anchor is selected deterministically from CNM-admitted source IDs, not
     * from a path pairing heuristic; it has no authority over CNM's canonical parent selection.
     */
    public static synchronized void injectMappedRoles(List<ShapeMap.Mapping> mappings) {
        Map<NibaruMaterialProfile, List<Candidate>> byProfile = new IdentityHashMap<>();
        for (Candidate candidate : CANDIDATES.values()) {
            if (candidate.phase == Phase.REGISTRY_READY) {
                byProfile.computeIfAbsent(candidate.profile, ignored -> new ArrayList<>()).add(candidate);
            }
        }
        for (List<Candidate> familyCandidates : byProfile.values()) {
            familyCandidates.sort(java.util.Comparator.comparing(candidate -> candidate.sourceId));
            Candidate injector = familyCandidates.stream().filter(candidate -> candidate.cnmMappingPresent)
                    .findFirst().orElse(null);
            for (Candidate candidate : familyCandidates) {
                if (!candidate.cnmMappingPresent) candidate.phase = Phase.DORMANT_UNMAPPED;
            }
            if (injector == null) continue;
            for (Block block : injector.bgeRoles.values()) {
                Item role = block.asItem();
                // Established BGE profiles are registered by the provider bridge. Preserve that
                // durable ownership edge instead of adding a duplicate just because CNM also
                // admitted this source. A future candidate with an absent role receives the
                // narrow Phase-B edge below, and only that new edge can be withdrawn later.
                if (!mappingMentions(mappings, role)) {
                    mappings.add(new ShapeMap.Mapping(injector.source.asItem(), role,
                            INJECTION_PRIORITY, SHAPE_MAP_SOURCE));
                    injector.added.add(role);
                }
                injector.injected.add(role);
            }
            injector.phase = Phase.MAPPING_INJECTED;
        }
    }

    /**
     * Phase B post-resolution. CNM's own resolved views are the only canonical-family authority.
     * Invalid edges created by this bridge are removed from the live views before BGE's
     * presentation-order pass, so an unmatched source cannot leak a candidate item into normal
     * ShapeMap switching. Existing profile edges retain their independent ownership.
     */
    public static synchronized void bindResolvedFamilies() {
        Set<Item> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Item> rejected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Candidate candidate : CANDIDATES.values()) {
            if (candidate.phase != Phase.MAPPING_INJECTED) continue;
            Item resolvedParent = ShapeMap.getParent(candidate.source.asItem());
            List<Item> component = ShapeMap.getShapes(candidate.source.asItem());
            boolean unique = candidate.injected.stream()
                    .allMatch(item -> Collections.frequency(component, item) == 1 && claimed.add(item));
            if (resolvedParent != candidate.profile.canonicalParent().asItem() || !unique) {
                rejected.addAll(candidate.added);
                candidate.phase = resolvedParent != candidate.profile.canonicalParent().asItem()
                        ? Phase.DORMANT_PARENT_MISMATCH : Phase.DORMANT_DUPLICATE;
                continue;
            }
            candidate.resolvedParent = resolvedParent;
            candidate.phase = Phase.BOUND;
        }
        if (!rejected.isEmpty()) removeInjected(rejected);
    }

    /** ShapeMap deliberately exposes immutable maps after resolution; replace them atomically. */
    private static void removeInjected(Set<Item> rejected) {
        Map<Item, List<Item>> shapes = new LinkedHashMap<>();
        ShapeMap.shapesView().forEach((parent, component) -> {
            List<Item> filtered = new ArrayList<>(component);
            filtered.removeIf(rejected::contains);
            shapes.put(parent, filtered);
        });
        Map<Item, Item> inverse = new LinkedHashMap<>(ShapeMap.inverseView());
        rejected.forEach(inverse::remove);
        ShapeMap.setShapeMaps(shapes, inverse);
    }

    /** Immutable diagnostic/test view; before Phase B no candidate reports a canonical parent. */
    public static synchronized List<Snapshot> snapshots() {
        return CANDIDATES.values().stream()
                .sorted(java.util.Comparator.comparing(candidate -> candidate.sourceId))
                .map(Candidate::snapshot).toList();
    }

    public static synchronized boolean rejectedRecursiveSource(Block block) {
        return RECURSIVE_SOURCES.contains(block);
    }

    private static Identifier registeredBlockId(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("CNM admitted an unregistered source block " + block);
        }
        return id;
    }

    public enum Phase {
        REGISTRY_ADMITTED,
        REGISTRY_READY,
        MAPPING_INJECTED,
        BOUND,
        DORMANT_UNTYPED,
        DORMANT_STATIC_PROFILE,
        DORMANT_INCOMPLETE,
        DORMANT_UNMAPPED,
        DORMANT_PARENT_MISMATCH,
        DORMANT_DUPLICATE
    }

    public record Snapshot(Identifier anchor, Map<BgeGeometryRole, Identifier> cnmRoles,
            Set<Identifier> injectedRoles, boolean canonicalBindingDeferred, Phase phase,
            Optional<Identifier> resolvedParent) {
        public Snapshot {
            cnmRoles = Map.copyOf(cnmRoles);
            injectedRoles = Set.copyOf(injectedRoles);
            resolvedParent = resolvedParent == null ? Optional.empty() : resolvedParent;
        }
    }

    private static final class Candidate {
        private final Block source;
        private final Identifier sourceId;
        private final EnumMap<BgeGeometryRole, Identifier> cnmRoles = new EnumMap<>(BgeGeometryRole.class);
        private final Set<Item> injected = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Item> added = Collections.newSetFromMap(new IdentityHashMap<>());
        private Phase phase = Phase.REGISTRY_ADMITTED;
        private NibaruMaterialProfile profile;
        private EnumMap<BgeGeometryRole, Block> bgeRoles = new EnumMap<>(BgeGeometryRole.class);
        private boolean cnmMappingPresent;
        private Item resolvedParent;

        private Candidate(Block source, Identifier sourceId) {
            this.source = source;
            this.sourceId = sourceId;
        }

        private void recordCnmRole(BgeGeometryRole role, Block generated, Identifier generatedId) {
            if (role != BgeGeometryRole.VERTICAL_SLAB && role != BgeGeometryRole.STEP) {
                throw new IllegalArgumentException("CNM cannot admit local BGE role " + role);
            }
            Identifier currentId = BuiltInRegistries.BLOCK.getKey(generated);
            if (!generatedId.equals(currentId)
                    && !BuiltInRegistries.BLOCK.getDefaultKey().equals(currentId)) {
                throw new IllegalStateException("CNM generated role identity drifted for " + sourceId);
            }
            Identifier previous = cnmRoles.putIfAbsent(role, generatedId);
            if (previous != null && !previous.equals(generatedId)) {
                throw new IllegalStateException("CNM admitted duplicate " + role + " roles for " + sourceId);
            }
        }

        private Snapshot snapshot() {
            Set<Identifier> injectedIds = new LinkedHashSet<>();
            injected.forEach(item -> injectedIds.add(BuiltInRegistries.ITEM.getKey(item)));
            return new Snapshot(sourceId, new LinkedHashMap<>(cnmRoles), injectedIds, true, phase,
                    resolvedParent == null ? Optional.empty()
                            : Optional.of(BuiltInRegistries.ITEM.getKey(resolvedParent)));
        }
    }
}
