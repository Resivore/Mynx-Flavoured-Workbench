package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Separates CNM registry admission from its later ShapeMap parent resolution.
 *
 * <p>Phase A records only CNM's real Vertical-Slab/Step admission and registers a BGE tail before
 * registry freeze. Phase B observes the resolved ShapeMap graph, elects one candidate per actual
 * component, and attaches the missing normal Wall plus Corner, Quarter Column, and Layer to the component CNM selected. A
 * temporary admission source is never treated as a material parent.</p>
 */
public final class CnmShapeMapCandidateBridge {
    public static final String PROFILE_VERSION = "bge-c87-cnm-rebuild-closure-v1";
    private static final Identifier SHAPE_MAP_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "cnm_resolved_candidate_roles");
    private static final int INJECTION_PRIORITY = Integer.MIN_VALUE;
    private static final List<BgeGeometryRole> TAIL = List.of(BgeGeometryRole.CORNER,
            BgeGeometryRole.QUARTER_COLUMN, BgeGeometryRole.LAYER);
    private static final Map<Block, Candidate> CANDIDATES = new IdentityHashMap<>();
    private static final Set<Block> RECURSIVE_SOURCES = Collections.newSetFromMap(new IdentityHashMap<>());

    private CnmShapeMapCandidateBridge() {}

    /** Phase A: called only by CNM's exact constructors after it admitted a real geometry source. */
    public static synchronized void admit(Block source, BgeGeometryRole role, Block generated,
            Identifier generatedId) {
        if (CanonicalGeometryRegistry.contains(source)) {
            RECURSIVE_SOURCES.add(source);
            return;
        }
        Candidate candidate = CANDIDATES.computeIfAbsent(source,
                ignored -> new Candidate(source, registeredBlockId(source)));
        candidate.recordCnmRole(role, generated, generatedId);
    }

    /**
     * Registry tail, still before freeze. Known BGE profiles reuse their registered tail. An
     * untyped CNM admission receives three inert, geometry-only candidates whose identities are
     * anchored to the admitted source; no canonical parent is fabricated.
     */
    public static synchronized void finishRegistryAdmission() {
        for (Candidate candidate : orderedCandidates()) {
            if (candidate.registryReady) continue;
            candidate.profile = NibaruMaterialProfiles.fromBlock(candidate.source).orElse(null);
            if (candidate.profile != null) {
                for (BgeGeometryRole role : TAIL) {
                    Block block = NibaruProviderAdapter.derived(candidate.profile, role).orElseThrow(() ->
                            new IllegalStateException("Missing registered BGE " + role + " for "
                                    + candidate.profile.canonicalParentId()));
                    candidate.bgeRoles.put(role, block);
                }
            } else {
                candidate.deferred = true;
                for (BgeGeometryRole role : TAIL) {
                    Identifier id = deferredId(candidate.sourceId, role);
                    Block block = DeferredCnmGeometryBlock.create(role, BlockBehaviour.Properties
                            .ofFullCopy(candidate.source)
                            .setId(ResourceKey.create(Registries.BLOCK, id)));
                    CnmTerrainCompat.registerDeferredCandidate(id, block);
                    candidate.bgeRoles.put(role, block);
                }
                Identifier wallId = deferredWallId(candidate.sourceId);
                candidate.deferredWall = new DeferredCnmWallBlock(BlockBehaviour.Properties
                        .ofFullCopy(candidate.source)
                        .setId(ResourceKey.create(Registries.BLOCK, wallId)));
                CnmTerrainCompat.registerDeferredCandidate(wallId, candidate.deferredWall);
            }
            candidate.registryReady = true;
            candidate.phase = Phase.REGISTRY_READY;
        }
    }

    /** Captures actual CNM mapping membership before BGE adds its deliberately low-priority edges. */
    public static synchronized void observeCnmMappings(List<ShapeMap.Mapping> mappings) {
        for (Candidate candidate : orderedCandidates()) {
            if (!candidate.registryReady) continue;
            // ShapeMap.setMappings receives a new graph on every legitimate reconstruction.
            // Admission is permanent, but every contribution below belongs only to this graph.
            candidate.beginMappingPass(mappingMentions(mappings, candidate.source.asItem()));
        }
    }

    /** Production Mapping predicate retained as a focused regression seam. */
    public static boolean mappingMentions(List<ShapeMap.Mapping> mappings, Item anchor) {
        return mappings.stream().anyMatch(mapping -> mapping.parent() == anchor || mapping.shape() == anchor);
    }

    /**
     * Adds registry-time tail edges only for a real CNM mapping. There may be more than one
     * admission anchor for one eventual component; Phase B removes every non-elected tail before
     * the component becomes visible.
     */
    public static synchronized void injectMappedRoles(List<ShapeMap.Mapping> mappings) {
        for (Candidate candidate : orderedCandidates()) {
            if (!candidate.registryReady || candidate.phase != Phase.REGISTRY_READY) continue;
            if (!candidate.cnmMappingPresent) {
                candidate.phase = Phase.DORMANT_UNMAPPED;
                candidate.exemptDeferredRoles("CNM did not retain the Phase-A admission in its mapping graph.");
                continue;
            }
            candidate.bgeRoles.values().forEach(block -> candidate.inject(mappings, block));
            // A normal profile already owns a Wall at registry time. A profile-free component
            // gets a carrier only when this fresh CNM graph has no provider/stock Wall anywhere
            // in the anchor component; no registry path or name participates in that decision.
            if (candidate.deferredWall != null && !componentHasWall(mappings, candidate.source.asItem())) {
                candidate.inject(mappings, candidate.deferredWall);
                candidate.contributesDeferredWall = true;
            }
            candidate.phase = Phase.MAPPING_INJECTED;
        }
    }

    /**
     * Phase B: CNM has selected all components. The actual resolved component is now the shared
     * authority for known profiles and formerly untyped candidates alike.
     */
    public static synchronized void bindResolvedFamilies() {
        Map<Item, List<Candidate>> candidatesByParent = new LinkedHashMap<>();
        for (Candidate candidate : orderedCandidates()) {
            if (candidate.phase != Phase.MAPPING_INJECTED) continue;
            Item parent = ShapeMap.getParent(candidate.source.asItem());
            if (ShapeMap.getShapes(candidate.source.asItem()).isEmpty()) {
                candidate.phase = Phase.DORMANT_UNRESOLVED;
                candidate.exemptDeferredRoles("CNM did not resolve a ShapeMap component for this admission.");
                continue;
            }
            candidatesByParent.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(candidate);
        }

        Map<Item, List<Item>> shapes = copyShapes();
        Map<Item, Item> inverse = new LinkedHashMap<>(ShapeMap.inverseView());
        Set<Item> rejected = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Map.Entry<Item, List<Candidate>> entry : candidatesByParent.entrySet()) {
            Item parent = entry.getKey();
            List<Candidate> family = entry.getValue();
            family.sort(java.util.Comparator.comparing(candidate -> candidate.sourceId));
            Candidate winner = family.getFirst();
            for (Candidate candidate : family) {
                if (candidate == winner) continue;
                rejected.addAll(candidate.added);
                candidate.phase = Phase.DORMANT_DUPLICATE_COMPONENT;
                candidate.exemptDeferredRoles("CNM candidate: a deterministic sibling admission owns this resolved component.");
            }
            List<Item> component = shapes.get(parent);
            if (component == null) throw new IllegalStateException("CNM resolved parent without component: " + parent);
            winner.bind(parent, component, inverse, rejected);
        }

        removeRejected(shapes, inverse, rejected);
        completeKnownProfileFamilies(shapes, inverse);
        ShapeMap.setShapeMaps(shapes, inverse);
        reportResolvedFamilies();
        CnmTerrainCompat.finalizeResolvedCnmFamilies();
    }

    /**
     * C80 only injected through candidate anchors. Re-check every actual resolved component for
     * every BGE profile so a known material such as Moss cannot remain a six-role CNM family.
     */
    private static void completeKnownProfileFamilies(Map<Item, List<Item>> shapes,
            Map<Item, Item> inverse) {
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Item material = profile.canonicalParent().asItem();
            Item parent = ShapeMap.getParent(material);
            List<Item> component = shapes.get(parent);
            if (component == null || component.isEmpty()) continue;
            for (BgeGeometryRole role : TAIL) {
                Block block = NibaruProviderAdapter.derived(profile, role).orElseThrow(() ->
                        new IllegalStateException("Missing BGE " + role + " for "
                                + profile.canonicalParentId()));
                ensureComponentMember(component, inverse, parent, block.asItem());
            }
            requireExactlyOne(component, profile.canonicalParentId(), TAIL.stream()
                    .map(role -> NibaruProviderAdapter.derived(profile, role).orElseThrow().asItem()).toList());
        }
    }

    /** Identity-graph check used before CNM resolves its real parent; never a name heuristic. */
    static boolean componentHasWall(List<ShapeMap.Mapping> mappings, Item anchor) {
        Map<Item, Set<Item>> graph = new IdentityHashMap<>();
        for (ShapeMap.Mapping mapping : mappings) {
            graph.computeIfAbsent(mapping.parent(), ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(mapping.shape());
            graph.computeIfAbsent(mapping.shape(), ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(mapping.parent());
        }
        Set<Item> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Item> pending = new ArrayList<>();
        pending.add(anchor);
        while (!pending.isEmpty()) {
            Item item = pending.removeLast();
            if (!visited.add(item)) continue;
            if (Block.byItem(item) instanceof net.minecraft.world.level.block.WallBlock) return true;
            pending.addAll(graph.getOrDefault(item, Set.of()));
        }
        return false;
    }

    private static void reportResolvedFamilies() {
        for (Candidate candidate : orderedCandidates()) {
            if (candidate.phase != Phase.BOUND) continue;
            List<Item> component = ShapeMap.getShapes(candidate.source.asItem());
            List<Identifier> before = candidate.cnmRoles.values().stream().toList();
            List<Identifier> finalRoles = component.stream().map(CnmShapeMapCandidateBridge::itemId).toList();
            System.out.println("BGE_CNM_RESOLVED_FAMILY|parent=" + itemId(candidate.resolvedParent)
                    + "|anchor=" + candidate.sourceId + "|cnm_roles=" + before
                    + "|added=" + candidate.injected.stream().map(CnmShapeMapCandidateBridge::itemId).toList()
                    + "|final=" + finalRoles);
        }
    }

    private static void requireExactlyOne(List<Item> component, Identifier parent, List<Item> expected) {
        for (Item item : expected) {
            int occurrences = Collections.frequency(component, item);
            if (occurrences != 1) {
                throw new IllegalStateException("Incomplete BGE tail for resolved CNM family " + parent
                        + ": " + itemId(item) + " occurrences=" + occurrences + " component="
                        + component.stream().map(CnmShapeMapCandidateBridge::itemId).toList());
            }
        }
    }

    private static void ensureComponentMember(List<Item> component, Map<Item, Item> inverse,
            Item parent, Item member) {
        int occurrences = Collections.frequency(component, member);
        if (occurrences > 1) throw new IllegalStateException("Duplicate ShapeMap role " + itemId(member)
                + " in component " + itemId(parent));
        if (occurrences == 0) component.add(member);
        if (member != parent) inverse.put(member, parent);
    }

    private static void removeRejected(Map<Item, List<Item>> shapes, Map<Item, Item> inverse,
            Set<Item> rejected) {
        if (rejected.isEmpty()) return;
        shapes.values().forEach(component -> component.removeIf(rejected::contains));
        rejected.forEach(inverse::remove);
    }

    private static Map<Item, List<Item>> copyShapes() {
        Map<Item, List<Item>> result = new LinkedHashMap<>();
        ShapeMap.shapesView().forEach((parent, component) -> result.put(parent, new ArrayList<>(component)));
        return result;
    }

    /** Immutable diagnostic/test view; a candidate has no canonical parent until Phase B. */
    public static synchronized List<Snapshot> snapshots() {
        return orderedCandidates().stream().map(Candidate::snapshot).toList();
    }

    public static synchronized boolean rejectedRecursiveSource(Block block) {
        return RECURSIVE_SOURCES.contains(block);
    }

    /**
     * The small set of genuinely profile-free families which CNM resolved after registry
     * admission.  The parent and registered roles are facts from the resolved ShapeMap; client
     * and server generators may consume them, but must never use them to elect a parent.
     */
    public static synchronized List<ResolvedGenericFamily> resolvedGenericFamilies() {
        return orderedCandidates().stream()
                .filter(candidate -> candidate.phase == Phase.BOUND && candidate.deferred)
                .map(Candidate::resolvedGenericFamily)
                .toList();
    }

    /**
     * First-bake visual closure.  The admitted CNM source is a real registered carrier and is
     * therefore safe as a temporary resource reference, but it is never a canonical parent.
     * ShapeMap resolution replaces this with {@link #resolvedGenericFamilies()} on later passes.
     */
    public static synchronized List<ProvisionalGenericFamily> provisionalGenericFamilies() {
        return orderedCandidates().stream()
                .filter(candidate -> candidate.registryReady && candidate.deferred)
                .map(Candidate::provisionalGenericFamily)
                .toList();
    }

    private static List<Candidate> orderedCandidates() {
        return CANDIDATES.values().stream().sorted(java.util.Comparator.comparing(candidate -> candidate.sourceId))
                .toList();
    }

    private static Identifier deferredId(Identifier anchor, BgeGeometryRole role) {
        String suffix = switch (role) {
            case CORNER -> "corner";
            case QUARTER_COLUMN -> "quarter_column";
            case LAYER -> "layer";
            default -> throw new IllegalArgumentException("Not a BGE tail role: " + role);
        };
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                "deferred/" + anchor.getNamespace() + "/" + anchor.getPath() + "_" + suffix);
    }

    private static Identifier deferredWallId(Identifier anchor) {
        return Identifier.fromNamespaceAndPath(CnmTerrainCompat.MOD_ID,
                "deferred/" + anchor.getNamespace() + "/" + anchor.getPath() + "_wall");
    }

    private static Identifier registeredBlockId(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
            throw new IllegalStateException("CNM admitted an unregistered source block " + block);
        }
        return id;
    }

    private static Identifier itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("Unregistered ShapeMap item " + item);
        }
        return id;
    }

    public enum Phase {
        REGISTRY_ADMITTED,
        REGISTRY_READY,
        MAPPING_INJECTED,
        BOUND,
        DORMANT_UNMAPPED,
        DORMANT_UNRESOLVED,
        DORMANT_DUPLICATE_COMPONENT
    }

    public record Snapshot(Identifier anchor, Map<BgeGeometryRole, Identifier> cnmRoles,
            Set<Identifier> injectedRoles, boolean canonicalBindingDeferred, boolean deferredMaterial,
            Phase phase, Optional<Identifier> resolvedParent) {
        public Snapshot {
            cnmRoles = Map.copyOf(cnmRoles);
            injectedRoles = Set.copyOf(injectedRoles);
            resolvedParent = resolvedParent == null ? Optional.empty() : resolvedParent;
        }
    }

    /** A post-resolution resource/data contract for one otherwise-untyped CNM component. */
    public record ResolvedGenericFamily(Identifier canonicalParent,
            Map<BgeGeometryRole, Identifier> roles, Optional<Identifier> wall) {
        public ResolvedGenericFamily {
            Objects.requireNonNull(canonicalParent, "canonicalParent");
            roles = Map.copyOf(roles);
            wall = wall == null ? Optional.empty() : wall;
            if (!roles.keySet().containsAll(TAIL)) {
                throw new IllegalArgumentException("Resolved generic CNM family is missing a BGE tail: "
                        + canonicalParent + " " + roles);
            }
        }
    }

    /** Resource-only pre-resolution contract; {@code visualSource} is not family identity. */
    public record ProvisionalGenericFamily(Identifier visualSource,
            Map<BgeGeometryRole, Identifier> roles, Optional<Identifier> wall) {
        public ProvisionalGenericFamily {
            Objects.requireNonNull(visualSource, "visualSource");
            roles = Map.copyOf(roles);
            wall = wall == null ? Optional.empty() : wall;
            if (!roles.keySet().containsAll(TAIL)) {
                throw new IllegalArgumentException("Provisional CNM family is missing a BGE tail: "
                        + visualSource + " " + roles);
            }
        }
    }

    private static final class Candidate {
        private final Block source;
        private final Identifier sourceId;
        private final EnumMap<BgeGeometryRole, Identifier> cnmRoles = new EnumMap<>(BgeGeometryRole.class);
        private final EnumMap<BgeGeometryRole, Block> bgeRoles = new EnumMap<>(BgeGeometryRole.class);
        private final Set<Item> injected = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Item> added = Collections.newSetFromMap(new IdentityHashMap<>());
        private Phase phase = Phase.REGISTRY_ADMITTED;
        private NibaruMaterialProfile profile;
        private boolean deferred;
        private boolean registryReady;
        private boolean cnmMappingPresent;
        /** Persistent resolved-family fact; diagnostics themselves remain per mapping pass. */
        private boolean contributesDeferredWall;
        private Item resolvedParent;
        private DeferredCnmWallBlock deferredWall;

        private Candidate(Block source, Identifier sourceId) {
            this.source = source;
            this.sourceId = sourceId;
        }

        private void recordCnmRole(BgeGeometryRole role, Block generated, Identifier generatedId) {
            if (role != BgeGeometryRole.VERTICAL_SLAB && role != BgeGeometryRole.STEP) {
                throw new IllegalArgumentException("CNM cannot admit local BGE role " + role);
            }
            Identifier current = BuiltInRegistries.BLOCK.getKey(generated);
            if (!generatedId.equals(current) && !BuiltInRegistries.BLOCK.getDefaultKey().equals(current)) {
                throw new IllegalStateException("CNM generated role identity drifted for " + sourceId);
            }
            Identifier previous = cnmRoles.putIfAbsent(role, generatedId);
            if (previous != null && !previous.equals(generatedId)) {
                throw new IllegalStateException("CNM admitted duplicate " + role + " roles for " + sourceId);
            }
        }

        private void beginMappingPass(boolean mappingPresent) {
            cnmMappingPresent = mappingPresent;
            injected.clear();
            added.clear();
            phase = Phase.REGISTRY_READY;
        }

        private void inject(List<ShapeMap.Mapping> mappings, Block block) {
            Item role = block.asItem();
            if (!mappingMentions(mappings, role)) {
                mappings.add(new ShapeMap.Mapping(source.asItem(), role,
                        INJECTION_PRIORITY, SHAPE_MAP_SOURCE));
                added.add(role);
            }
            injected.add(role);
        }

        private void bind(Item parent, List<Item> component, Map<Item, Item> inverse,
                Set<Item> rejected) {
            Block canonical = Block.byItem(parent);
            NibaruMaterialProfile resolvedProfile = NibaruMaterialProfiles.fromBlock(canonical).orElse(null);
            if (deferred && resolvedProfile != null) {
                // A provider can register after CNM's Phase-A scan. Its resolved canonical profile
                // now owns the same roles, so withdraw the temporary carrier instead of creating
                // a parallel family. The component, not a namespace or name heuristic, decides.
                for (Block deferredBlock : bgeRoles.values()) rejected.add(deferredBlock.asItem());
                if (deferredWall != null) rejected.add(deferredWall.asItem());
                exemptDeferredRoles("CNM candidate: a resolved BGE/provider material profile owns this component.");
                bgeRoles.clear();
                for (BgeGeometryRole role : TAIL) {
                    Block block = NibaruProviderAdapter.derived(resolvedProfile, role).orElseThrow();
                    bgeRoles.put(role, block);
                }
                deferred = false;
                deferredWall = null;
            }
            for (Block block : bgeRoles.values()) ensureComponentMember(component, inverse, parent, block.asItem());
            requireExactlyOne(component, itemId(parent), bgeRoles.values().stream().map(Block::asItem).toList());
            if (deferred) {
                for (Map.Entry<BgeGeometryRole, Block> entry : bgeRoles.entrySet()) {
                    BgeMaterialBindings.bindResolvedCnmCandidate(entry.getValue(), canonical, entry.getKey());
                }
                if (deferredWall != null && contributesDeferredWall) {
                    ensureComponentMember(component, inverse, parent, deferredWall.asItem());
                    requireExactlyOne(component, itemId(parent), List.of(deferredWall.asItem()));
                    BgeMaterialBindings.bindResolvedCnmWallCandidate(deferredWall, canonical);
                }
            }
            if (resolvedParent != null && resolvedParent != parent) {
                throw new IllegalStateException("CNM elected a different parent on a ShapeMap rebuild for "
                        + sourceId + ": " + itemId(resolvedParent) + " then " + itemId(parent));
            }
            resolvedParent = parent;
            phase = Phase.BOUND;
        }

        private void exemptDeferredRoles(String reason) {
            if (!deferred) return;
            for (Block block : bgeRoles.values()) exemptIfUnbound(block, reason);
            if (deferredWall != null) exemptIfUnbound(deferredWall, reason);
        }

        private static void exemptIfUnbound(Block block, String reason) {
            // A later map may legitimately omit an already-resolved component. Its immutable
            // canonical binding remains valid; only never-bound dormant carriers need exemption.
            if (BgeMaterialBindings.fromBlock(block).isEmpty() && !BgeMaterialBindings.isValidated())
                BgeMaterialBindings.registerExemption(block, reason);
        }

        private Snapshot snapshot() {
            Set<Identifier> injectedIds = new LinkedHashSet<>();
            injected.forEach(item -> injectedIds.add(itemId(item)));
            return new Snapshot(sourceId, new LinkedHashMap<>(cnmRoles), injectedIds, true, deferred,
                    phase, resolvedParent == null ? Optional.empty() : Optional.of(itemId(resolvedParent)));
        }

        private ResolvedGenericFamily resolvedGenericFamily() {
            if (phase != Phase.BOUND || !deferred || resolvedParent == null) {
                throw new IllegalStateException("Candidate is not a resolved generic family: " + sourceId);
            }
            Map<BgeGeometryRole, Identifier> roles = new EnumMap<>(BgeGeometryRole.class);
            bgeRoles.forEach((role, block) -> roles.put(role, registeredBlockId(block)));
            return new ResolvedGenericFamily(itemId(resolvedParent), roles,
                    deferredWall == null || !contributesDeferredWall ? Optional.empty()
                            : Optional.of(registeredBlockId(deferredWall)));
        }

        private ProvisionalGenericFamily provisionalGenericFamily() {
            Map<BgeGeometryRole, Identifier> roles = new EnumMap<>(BgeGeometryRole.class);
            bgeRoles.forEach((role, block) -> roles.put(role, registeredBlockId(block)));
            // The wall carrier is a preregistered visual candidate at first bake.  Emitting its
            // resources here is harmless when CNM later supplies a Wall, and prevents a genuine
            // no-Wall component from reaching the initial model bake without its eventual item
            // or blockstate assets.  Its functional ShapeMap membership remains governed only by
            // contributesDeferredWall after resolution.
            return new ProvisionalGenericFamily(sourceId, roles,
                    deferredWall == null ? Optional.empty() : Optional.of(registeredBlockId(deferredWall)));
        }
    }
}
