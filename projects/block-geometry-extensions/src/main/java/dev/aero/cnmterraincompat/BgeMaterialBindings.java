package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

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
 * Authoritative canonical-material identity for every BGE geometry.
 *
 * <p>This registry is deliberately backed by {@link NibaruMaterialProfile}; it does not infer
 * material identity from registry names. A normal catalog profile contributes exactly one primary
 * owner for all nine standard roles. Retained registry identities are explicit aliases, while
 * limited forms such as Farmland Slab are explicit special bindings that do not expand the
 * generated catalog.</p>
 */
public final class BgeMaterialBindings {
    private static final Map<Block, Binding> BY_BLOCK = new IdentityHashMap<>();
    private static final Map<Block, String> EXEMPTIONS = new IdentityHashMap<>();
    private static final Set<Block> OWNED_BLOCKS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Block> DERIVED_GEOMETRY = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<NibaruMaterialProfile, EnumMap<Role, Block>> PRIMARY =
            new IdentityHashMap<>();
    private static final Map<NibaruMaterialProfile, EnumMap<Role, CatalogExclusion>> EXCLUSIONS =
            new IdentityHashMap<>();
    private static boolean normalCatalogBound;
    private static boolean validated;

    private BgeMaterialBindings() {}

    /** Complete role contract implied by adding a normal material to the BGE catalog. */
    public enum Role {
        CANONICAL_BLOCK,
        HORIZONTAL_SLAB,
        STAIR,
        WALL,
        VERTICAL_SLAB,
        STEP,
        LAYER,
        CORNER,
        QUARTER_COLUMN;

        static Role from(BgeGeometryRole role) {
            return switch (role) {
                case VERTICAL_SLAB -> VERTICAL_SLAB;
                case STEP -> STEP;
                case LAYER -> LAYER;
                case CORNER -> CORNER;
                case QUARTER_COLUMN -> QUARTER_COLUMN;
            };
        }
    }

    public enum CatalogMembership {
        NORMAL_CATALOG,
        SPECIAL_CANONICAL_BOUND
    }

    public enum Ownership {
        PRIMARY,
        RETAINED_ALIAS,
        SPECIAL
    }

    /** Typed topology identity; physical surfaces are supplied separately by each binding. */
    public enum Topology {
        CANONICAL_ROOT,
        HORIZONTAL_SLAB,
        STAIR,
        WALL,
        VERTICAL_SLAB,
        STEP,
        LAYER,
        CORNER,
        QUARTER_COLUMN,
        FARMLAND_SLAB;

        public boolean isFullOccupancy(BlockState state) {
            return switch (this) {
                case CANONICAL_ROOT -> true;
                case HORIZONTAL_SLAB -> state.hasProperty(SlabBlock.TYPE)
                        && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
                case VERTICAL_SLAB -> state.hasProperty(VerticalSlabBlock.DOUBLE)
                        && state.getValue(VerticalSlabBlock.DOUBLE);
                case LAYER -> state.hasProperty(BgeLayerBlock.LAYERS)
                        && state.getValue(BgeLayerBlock.LAYERS) == 4;
                // A DOUBLE Step is two opposed quarter-cell members, not a full cube. Corner is
                // three quarters and Quarter Column tops out at two diagonal quarters.
                case STAIR, WALL, STEP, CORNER, QUARTER_COLUMN, FARMLAND_SLAB -> false;
            };
        }

        /** A single exact cuboid where the topology has one; compound forms return empty. */
        public Optional<Bounds> bounds(BlockState state) {
            return switch (this) {
                case CANONICAL_ROOT -> Optional.of(new Bounds(0, 0, 0, 16, 16, 16));
                case HORIZONTAL_SLAB -> Optional.of(switch (state.getValue(SlabBlock.TYPE)) {
                    case BOTTOM -> new Bounds(0, 0, 0, 16, 8, 16);
                    case TOP -> new Bounds(0, 8, 0, 16, 16, 16);
                    case DOUBLE -> new Bounds(0, 0, 0, 16, 16, 16);
                });
                case VERTICAL_SLAB -> Optional.of(verticalBounds(state));
                case LAYER -> Optional.of(layerBounds(state));
                case FARMLAND_SLAB -> Optional.of(switch (state.getValue(FarmlandSlabBlock.TYPE)) {
                    case BOTTOM -> new Bounds(0, 0, 0, 16, 7, 16);
                    case TOP -> new Bounds(0, 7, 0, 16, 15, 16);
                    case DOUBLE -> new Bounds(0, 0, 0, 16, 15, 16);
                });
                case STAIR, WALL, STEP, CORNER, QUARTER_COLUMN -> Optional.empty();
            };
        }

        private static Bounds verticalBounds(BlockState state) {
            if (state.getValue(VerticalSlabBlock.DOUBLE)) return new Bounds(0, 0, 0, 16, 16, 16);
            return switch (state.getValue(VerticalSlabBlock.FACING)) {
                case NORTH -> new Bounds(0, 0, 0, 16, 16, 8);
                case EAST -> new Bounds(8, 0, 0, 16, 16, 16);
                case SOUTH -> new Bounds(0, 0, 8, 16, 16, 16);
                case WEST -> new Bounds(0, 0, 0, 8, 16, 16);
                default -> throw new IllegalStateException("Vertical Slab facing is not horizontal");
            };
        }

        private static Bounds layerBounds(BlockState state) {
            int depth = state.getValue(BgeLayerBlock.LAYERS) * 4;
            return switch (state.getValue(BgeLayerBlock.FACING)) {
                case UP -> new Bounds(0, 0, 0, 16, depth, 16);
                case DOWN -> new Bounds(0, 16 - depth, 0, 16, 16, 16);
                case NORTH -> new Bounds(0, 0, 0, 16, 16, depth);
                case EAST -> new Bounds(16 - depth, 0, 0, 16, 16, 16);
                case SOUTH -> new Bounds(0, 0, 16 - depth, 16, 16, 16);
                case WEST -> new Bounds(0, 0, 0, depth, 16, 16);
            };
        }
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Bounds {
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 16 || maxZ > 16
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("Invalid block-pixel bounds");
            }
        }
    }

    @FunctionalInterface
    public interface StateProjection {
        Optional<BlockState> project(BlockState geometryState);
    }

    public record Binding(Block physicalBlock, Block canonicalMaterial,
            Optional<NibaruMaterialProfile> materialProfile, Role role, Ownership ownership,
            CatalogMembership membership, StateProjection stateProjection, Topology topology,
            BgeSurfaceGeometry.SurfaceProvider surfaceProvider,
            boolean additionalCatalogGenerationExpected, boolean fullOccupancyNormalizationEnabled,
            boolean normallyObtainable, Optional<String> limitationReason) {
        public Binding {
            Objects.requireNonNull(physicalBlock, "physicalBlock");
            Objects.requireNonNull(canonicalMaterial, "canonicalMaterial");
            materialProfile = Objects.requireNonNull(materialProfile, "materialProfile");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(ownership, "ownership");
            Objects.requireNonNull(membership, "membership");
            Objects.requireNonNull(stateProjection, "stateProjection");
            Objects.requireNonNull(topology, "topology");
            Objects.requireNonNull(surfaceProvider, "surfaceProvider");
            limitationReason = Objects.requireNonNull(limitationReason, "limitationReason");
            if (membership == CatalogMembership.SPECIAL_CANONICAL_BOUND
                    && limitationReason.filter(reason -> !reason.isBlank()).isEmpty()) {
                throw new IllegalArgumentException("Special canonical binding needs a limitation reason");
            }
            if (membership == CatalogMembership.NORMAL_CATALOG && limitationReason.isPresent()) {
                throw new IllegalArgumentException("Normal catalog binding cannot have a limitation reason");
            }
        }

        public Optional<BlockState> canonicalState(BlockState state) {
            if (!state.is(physicalBlock)) return Optional.empty();
            return stateProjection.project(state);
        }

        /** Complete BGE-owned rendered surface data for this exact physical state. */
        public BgeSurfaceGeometry.SurfaceModel surfaceModel(BlockState state) {
            if (!state.is(physicalBlock)) {
                return BgeSurfaceGeometry.SurfaceModel.unsupported(
                        "Surface query state does not belong to the canonical binding.");
            }
            return surfaceProvider.describe(state);
        }
    }

    public record CatalogExclusion(NibaruMaterialProfile profile, Role role, String technicalReason) {
        public CatalogExclusion {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(role, "role");
            if (technicalReason == null || technicalReason.isBlank()) {
                throw new IllegalArgumentException("Catalog exclusion needs a technical reason");
            }
        }
    }

    public static synchronized Optional<Binding> fromBlock(Block block) {
        return Optional.ofNullable(BY_BLOCK.get(block));
    }

    public static synchronized Optional<BlockState> projectToCanonical(BlockState state) {
        Binding binding = BY_BLOCK.get(state.getBlock());
        return binding == null ? Optional.empty() : binding.canonicalState(state);
    }

    public static synchronized List<Binding> all() {
        return List.copyOf(BY_BLOCK.values());
    }

    public static synchronized List<CatalogExclusion> exclusions() {
        return EXCLUSIONS.values().stream().flatMap(entries -> entries.values().stream()).toList();
    }

    /** Diagnostic view used by controlled completeness tests. */
    public static synchronized Set<Block> ownedBlocks() {
        return Set.copyOf(OWNED_BLOCKS);
    }

    /** Documented non-material classifications; empty for the current catalog. */
    public static synchronized Map<Block, String> exemptions() {
        return Map.copyOf(EXEMPTIONS);
    }

    public static synchronized void requireValid() {
        validateAndFreeze();
    }

    public static synchronized void noteOwnedRegistration(Block block) {
        requireMutable();
        OWNED_BLOCKS.add(block);
    }

    static synchronized void noteDerivedGeometry(Block block) {
        requireMutable();
        OWNED_BLOCKS.add(block);
        DERIVED_GEOMETRY.add(block);
    }

    static synchronized boolean isDerivedGeometry(Block block) {
        return DERIVED_GEOMETRY.contains(block);
    }

    /** Structured escape hatch for a truly non-material BGE block. No current block uses it. */
    static synchronized void registerExemption(Block block, String reason) {
        requireMutable();
        noteOwnedRegistration(block);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Blank exemption reason");
        if (BY_BLOCK.containsKey(block)) throw new IllegalStateException("Bound block cannot be exempt");
        String previous = EXEMPTIONS.putIfAbsent(block, reason);
        if (previous != null && !previous.equals(reason)) {
            throw new IllegalStateException("Conflicting BGE exemption for " + id(block));
        }
    }

    /**
     * Derives the normal nine-role catalog from the authoritative material profiles and provider
     * adapter. This method is called only after CNM has completed generated registration.
     */
    static synchronized void bindNormalCatalog() {
        requireMutable();
        if (normalCatalogBound) return;
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            bindPrimary(profile, Role.CANONICAL_BLOCK, profile.canonicalParent());
            bindPrimary(profile, Role.HORIZONTAL_SLAB, profile.effectiveSlabSource()
                    .orElseThrow(() -> missing(profile, Role.HORIZONTAL_SLAB)));
            bindPrimary(profile, Role.STAIR, profile.effectiveStairSource()
                    .orElseThrow(() -> missing(profile, Role.STAIR)));
            bindPrimary(profile, Role.WALL, profile.nativeWall()
                    .orElseThrow(() -> missing(profile, Role.WALL)));
            bindPrimary(profile, Role.VERTICAL_SLAB, derived(profile, BgeGeometryRole.VERTICAL_SLAB));
            bindPrimary(profile, Role.STEP, derived(profile, BgeGeometryRole.STEP));
            bindPrimary(profile, Role.LAYER, derived(profile, BgeGeometryRole.LAYER));
            bindPrimary(profile, Role.CORNER, derived(profile, BgeGeometryRole.CORNER));
            bindPrimary(profile, Role.QUARTER_COLUMN, derived(profile, BgeGeometryRole.QUARTER_COLUMN));
        }
        normalCatalogBound = true;
    }

    static synchronized void bindRetainedAlias(Block alias, NibaruMaterialProfile profile, Role role) {
        requireMutable();
        bind(new Binding(alias, profile.canonicalParent(), Optional.of(profile), role,
                Ownership.RETAINED_ALIAS, CatalogMembership.NORMAL_CATALOG,
                projection(profile, role), topology(role), surfaceProvider(profile, topology(role)), true,
                normalizes(role), alias.asItem() != Items.AIR, Optional.empty()));
    }

    static synchronized void bindFarmlandSpecial(Block farmlandSlab) {
        requireMutable();
        bind(new Binding(net.minecraft.world.level.block.Blocks.FARMLAND,
                net.minecraft.world.level.block.Blocks.FARMLAND, Optional.empty(),
                Role.CANONICAL_BLOCK, Ownership.SPECIAL,
                CatalogMembership.SPECIAL_CANONICAL_BOUND, state -> Optional.of(state),
                Topology.CANONICAL_ROOT, BgeSurfaceGeometry.provider(Topology.CANONICAL_ROOT, true),
                false, false, true,
                Optional.of("Canonical terrain root metadata for the special Farmland Slab binding.")));
        bind(new Binding(farmlandSlab, net.minecraft.world.level.block.Blocks.FARMLAND,
                Optional.empty(), Role.HORIZONTAL_SLAB, Ownership.SPECIAL,
                CatalogMembership.SPECIAL_CANONICAL_BOUND,
                state -> Optional.of(net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState()
                        .setValue(BlockStateProperties.MOISTURE,
                                state.getValue(FarmlandSlabBlock.MOISTURE))),
                Topology.FARMLAND_SLAB, BgeSurfaceGeometry.provider(Topology.FARMLAND_SLAB, true),
                false, false, false,
                Optional.of("State-only horizontal farmland form created by tilling; no other catalog geometry is intentional.")));
    }

    /** Adds a validated role exclusion only for a demonstrated technical incompatibility. */
    static synchronized void exclude(NibaruMaterialProfile profile, Role role, String reason) {
        requireMutable();
        CatalogExclusion exclusion = new CatalogExclusion(profile, role, reason);
        CatalogExclusion previous = EXCLUSIONS.computeIfAbsent(profile,
                ignored -> new EnumMap<>(Role.class)).putIfAbsent(role, exclusion);
        if (previous != null && !previous.equals(exclusion)) {
            throw new IllegalStateException("Conflicting catalog exclusion for "
                    + profile.canonicalParentId() + " " + role);
        }
    }

    /** Build-time/runtime-bootstrap invariant with material-and-role diagnostics. */
    static synchronized void validateAndFreeze() {
        if (validated) return;
        List<String> problems = new ArrayList<>();
        if (!normalCatalogBound) problems.add("normal catalog bindings were not built");

        for (Block block : OWNED_BLOCKS) {
            boolean bound = BY_BLOCK.containsKey(block);
            boolean exempt = EXEMPTIONS.containsKey(block);
            if (bound == exempt) {
                problems.add(id(block) + (bound
                        ? " is both canonical-bound and exempt"
                        : " has no canonical binding or documented exemption"));
            }
        }
        for (Map.Entry<Block, Binding> entry : BY_BLOCK.entrySet()) {
            Binding binding = entry.getValue();
            if (entry.getKey() != binding.physicalBlock()) {
                problems.add(id(entry.getKey()) + " binding physical-block mismatch");
            }
            binding.materialProfile().ifPresent(profile -> {
                if (profile.canonicalParent() != binding.canonicalMaterial()) {
                    problems.add(id(binding.physicalBlock()) + " points outside profile canonical material");
                }
            });
        }

        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            EnumMap<Role, Block> roles = PRIMARY.get(profile);
            for (Role role : Role.values()) {
                Block owner = roles == null ? null : roles.get(role);
                CatalogExclusion exclusion = Optional.ofNullable(EXCLUSIONS.get(profile))
                        .map(entries -> entries.get(role)).orElse(null);
                if ((owner == null) == (exclusion == null)) {
                    problems.add(profile.canonicalParentId() + " " + role
                            + (owner == null ? " is missing without a technical exclusion"
                                    : " has both a primary owner and an exclusion"));
                } else if (owner != null) {
                    Binding binding = BY_BLOCK.get(owner);
                    if (binding == null || binding.ownership() != Ownership.PRIMARY
                            || binding.membership() != CatalogMembership.NORMAL_CATALOG
                            || binding.role() != role
                            || binding.canonicalMaterial() != profile.canonicalParent()) {
                        problems.add(profile.canonicalParentId() + " " + role
                                + " has an invalid primary binding at " + id(owner));
                    }
                }
            }
            validateNormalizableProjection(profile, Role.HORIZONTAL_SLAB, problems);
            validateNormalizableProjection(profile, Role.VERTICAL_SLAB, problems);
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("BGE canonical binding validation failed:\n - "
                    + String.join("\n - ", problems));
        }
        validated = true;
    }

    private static void validateNormalizableProjection(NibaruMaterialProfile profile, Role role,
            List<String> problems) {
        Block block = Optional.ofNullable(PRIMARY.get(profile)).map(roles -> roles.get(role)).orElse(null);
        if (block == null) return;
        Binding binding = BY_BLOCK.get(block);
        if (binding == null || !binding.fullOccupancyNormalizationEnabled()) return;
        BlockState sample = fullState(block.defaultBlockState(), role);
        Optional<BlockState> projected = binding.canonicalState(sample);
        if (projected.isEmpty() || projected.get().getBlock() != profile.canonicalParent()) {
            problems.add(profile.canonicalParentId() + " " + role + " has no canonical projection");
        }
        for (Property<?> property : materialPropertiesFor(profile, role, sample)) {
            if (!projected.map(state -> sameValue(sample, state, property)).orElse(false)) {
                problems.add(profile.canonicalParentId() + " " + role
                        + " loses material property " + property.getName());
            }
        }
        for (Property<?> property : profile.canonicalParent().defaultBlockState().getProperties()) {
            if (!sample.hasProperty(property)) continue;
            boolean explicitlyCopied = materialPropertiesFor(profile, role, sample).contains(property);
            boolean independentlyProjectedPattern = property == BlockStateProperties.HORIZONTAL_FACING
                    && sample.hasProperty(GlazedPatternState.PATTERN_FACING);
            if (!explicitlyCopied && !independentlyProjectedPattern) {
                problems.add(profile.canonicalParentId() + " " + role
                        + " has an unclassified shared canonical property " + property.getName());
            }
        }
    }

    private static BlockState fullState(BlockState state, Role role) {
        return switch (role) {
            case HORIZONTAL_SLAB -> state.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
            case VERTICAL_SLAB -> state.setValue(VerticalSlabBlock.DOUBLE, true);
            default -> state;
        };
    }

    private static Block derived(NibaruMaterialProfile profile, BgeGeometryRole role) {
        return NibaruProviderAdapter.derived(profile, role)
                .orElseThrow(() -> missing(profile, Role.from(role)));
    }

    private static IllegalStateException missing(NibaruMaterialProfile profile, Role role) {
        return new IllegalStateException("Missing normal BGE catalog role " + role
                + " for " + profile.canonicalParentId());
    }

    private static void bindPrimary(NibaruMaterialProfile profile, Role role, Block block) {
        EnumMap<Role, Block> roles = PRIMARY.computeIfAbsent(profile,
                ignored -> new EnumMap<>(Role.class));
        Block previous = roles.putIfAbsent(role, block);
        if (previous != null && previous != block) {
            throw new IllegalStateException("Duplicate primary owner for "
                    + profile.canonicalParentId() + " " + role + ": "
                    + id(previous) + " and " + id(block));
        }
        bind(new Binding(block, profile.canonicalParent(), Optional.of(profile), role,
                Ownership.PRIMARY, CatalogMembership.NORMAL_CATALOG,
                projection(profile, role), topology(role), surfaceProvider(profile, topology(role)), true,
                normalizes(role), block.asItem() != Items.AIR, Optional.empty()));
    }

    private static void bind(Binding binding) {
        if (EXEMPTIONS.containsKey(binding.physicalBlock())) {
            throw new IllegalStateException("Exempt block cannot gain a binding: " + id(binding.physicalBlock()));
        }
        Binding previous = BY_BLOCK.putIfAbsent(binding.physicalBlock(), binding);
        if (previous != null && !equivalent(previous, binding)) {
            throw new IllegalStateException("Conflicting canonical material identities for "
                    + id(binding.physicalBlock()) + ": " + id(previous.canonicalMaterial())
                    + " and " + id(binding.canonicalMaterial()));
        }
    }

    private static boolean equivalent(Binding left, Binding right) {
        return left.physicalBlock() == right.physicalBlock()
                && left.canonicalMaterial() == right.canonicalMaterial()
                && left.materialProfile().equals(right.materialProfile())
                && left.role() == right.role()
                && left.ownership() == right.ownership()
                && left.membership() == right.membership()
                && left.topology() == right.topology()
                && left.additionalCatalogGenerationExpected() == right.additionalCatalogGenerationExpected()
                && left.fullOccupancyNormalizationEnabled() == right.fullOccupancyNormalizationEnabled()
                && left.normallyObtainable() == right.normallyObtainable()
                && left.limitationReason().equals(right.limitationReason());
    }

    private static StateProjection projection(NibaruMaterialProfile profile, Role role) {
        if (role == Role.CANONICAL_BLOCK) return state -> Optional.of(state);
        return state -> {
            BlockState projected = profile.canonicalParent().defaultBlockState();
            for (Property<?> property : materialPropertiesFor(profile, role, state)) {
                projected = copy(state, projected, property, property);
            }
            if (state.hasProperty(GlazedPatternState.PATTERN_FACING)
                    && projected.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                projected = projected.setValue(BlockStateProperties.HORIZONTAL_FACING,
                        state.getValue(GlazedPatternState.PATTERN_FACING));
            }
            return Optional.of(projected);
        };
    }

    private static List<Property<?>> materialPropertiesFor(NibaruMaterialProfile profile, Role role,
            BlockState source) {
        BlockState canonical = profile.canonicalParent().defaultBlockState();
        List<Property<?>> result = new ArrayList<>();
        addShared(result, source, canonical, BlockStateProperties.AXIS);
        addShared(result, source, canonical, BlockStateProperties.DISTANCE);
        addShared(result, source, canonical, BlockStateProperties.PERSISTENT);
        addShared(result, source, canonical, BlockStateProperties.SNOWY);
        addShared(result, source, canonical, BlockStateProperties.WATERLOGGED);
        // Horizontal Slabs use facing as glazed-pattern material state. Every other standard
        // geometry uses its ordinary facing as topology and, where required, carries pattern state
        // independently through GlazedPatternState.PATTERN_FACING.
        if (role == Role.HORIZONTAL_SLAB) {
            addShared(result, source, canonical, BlockStateProperties.HORIZONTAL_FACING);
        }
        return List.copyOf(result);
    }

    private static void addShared(List<Property<?>> result, BlockState source, BlockState target,
            Property<?> property) {
        if (source.hasProperty(property) && target.hasProperty(property)) result.add(property);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copy(BlockState source, BlockState target,
            Property sourceProperty, Property targetProperty) {
        return target.setValue(targetProperty, source.getValue(sourceProperty));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean sameValue(BlockState source, BlockState target, Property property) {
        return source.hasProperty(property) && target.hasProperty(property)
                && source.getValue(property).equals(target.getValue(property));
    }

    private static Topology topology(Role role) {
        return switch (role) {
            case CANONICAL_BLOCK -> Topology.CANONICAL_ROOT;
            case HORIZONTAL_SLAB -> Topology.HORIZONTAL_SLAB;
            case STAIR -> Topology.STAIR;
            case WALL -> Topology.WALL;
            case VERTICAL_SLAB -> Topology.VERTICAL_SLAB;
            case STEP -> Topology.STEP;
            case LAYER -> Topology.LAYER;
            case CORNER -> Topology.CORNER;
            case QUARTER_COLUMN -> Topology.QUARTER_COLUMN;
        };
    }

    private static boolean normalizes(Role role) {
        return role == Role.HORIZONTAL_SLAB || role == Role.VERTICAL_SLAB;
    }

    private static BgeSurfaceGeometry.SurfaceProvider surfaceProvider(
            NibaruMaterialProfile profile, Topology topology) {
        boolean terrainHeightInset = profile.capabilities().contains(BehaviorCapability.PATH_CONVERSION);
        return BgeSurfaceGeometry.provider(topology, terrainHeightInset);
    }

    private static Identifier id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private static void requireMutable() {
        if (validated) throw new IllegalStateException("BGE material bindings are already validated");
    }
}
