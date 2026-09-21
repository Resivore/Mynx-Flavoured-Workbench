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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact selector authority for the explicitly supported BGE material catalog.
 *
 * <p>CNM remains authoritative for every item outside this catalog. BGE removes only its own
 * supported items from CNM-built components and then installs the declared, complete
 * nine-role groups below. No registry-name inference, live-family discovery, or unknown-family
 * completion occurs here.</p>
 */
public final class ExplicitShapeMapFamilies {
    private static final Identifier MAPPING_SOURCE = Identifier.fromNamespaceAndPath(
            CnmTerrainCompat.MOD_ID, "explicit_selector_groups");

    /** Provider-native BBB slab/stair aliases remain registered but are not canonical selector roles. */
    private static final List<Identifier> SUPPRESSED_SELECTOR_ALIASES = bbbSelectorAliases();

    /** A vanilla Plank remains a standalone family when its optional BBB Beam provider is absent. */
    private static final List<List<Identifier>> OPTIONAL_BBB_GROUPS = optionalBbbGroups();

    /** Multi-variation order is semantic and intentionally not lexical. */
    private static final List<List<Identifier>> DECLARED_MULTI_GROUPS = declaredMultiGroups();

    private ExplicitShapeMapFamilies() {}

    /** Adds only the explicit graph edges needed to make each declared group one CNM family. */
    public static void addDeclaredGroupEdges(List<ShapeMap.Mapping> mappings) {
        for (List<NibaruMaterialProfile> group : profileGroups()) {
            if (group.size() < 2) continue;
            Item parent = group.getFirst().canonicalParent().asItem();
            for (int index = 1; index < group.size(); index++) {
                mappings.add(new ShapeMap.Mapping(parent, group.get(index).canonicalParent().asItem(),
                        950, MAPPING_SOURCE));
            }
        }
    }

    /**
     * Rebuilds only the explicitly supported components after CNM has finished its own graph.
     * Unknown items retain their relative membership and order, including when CNM initially
     * placed them beside a supported item.
     */
    public static void rebuildExactPresentation() {
        List<List<Item>> expected = expectedItemGroups();
        Set<Item> supported = Collections.newSetFromMap(new IdentityHashMap<>());
        expected.forEach(supported::addAll);
        for (Identifier alias : SUPPRESSED_SELECTOR_ALIASES) {
            Item item = BuiltInRegistries.ITEM.getValue(alias);
            if (alias.equals(BuiltInRegistries.ITEM.getKey(item))) supported.add(item);
        }

        Map<Item, List<Item>> shapes = new LinkedHashMap<>();
        Set<List<Item>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<Item, List<Item>> entry : ShapeMap.shapesView().entrySet()) {
            if (!visited.add(entry.getValue())) continue;
            List<Item> remaining = entry.getValue().stream()
                    .filter(item -> !supported.contains(item)).toList();
            // ShapeMap represents switchable components, not singleton parents. If separating an
            // explicit BGE component leaves one unrelated item behind, leave that item wholly
            // outside BGE's map instead of manufacturing a new one-item selector for it.
            if (remaining.size() < 2) continue;
            Item parent = remaining.contains(entry.getKey()) ? entry.getKey() : remaining.getFirst();
            putComponent(shapes, parent, remaining, "CNM-owned");
        }
        for (List<Item> group : expected) {
            putComponent(shapes, group.getFirst(), group, "explicit BGE");
        }

        Map<Item, Item> inverse = new LinkedHashMap<>();
        for (Map.Entry<Item, List<Item>> entry : shapes.entrySet()) {
            for (Item item : entry.getValue()) {
                // Match ShapeMap's native contract: the root is a parent, not one of its shapes.
                if (item == entry.getKey()) continue;
                Item previous = inverse.putIfAbsent(item, entry.getKey());
                if (previous != null && previous != entry.getKey()) {
                    throw new IllegalStateException("ShapeMap item belongs to two components: "
                            + itemId(item) + " -> " + itemId(previous) + " / " + itemId(entry.getKey()));
                }
            }
        }
        ShapeMap.setShapeMaps(shapes, inverse);
    }

    /** One complete, exact item sequence for every declared selector family. */
    public static List<List<Item>> expectedItemGroups() {
        return profileGroups().stream().map(group -> group.stream()
                .flatMap(profile -> variationItems(profile).stream()).toList()).toList();
    }

    /** Exposed as registry IDs so GameTests can compare the actual user-visible selector list. */
    public static List<List<Identifier>> expectedIdGroups() {
        return expectedItemGroups().stream()
                .map(group -> group.stream().map(ExplicitShapeMapFamilies::itemId).toList()).toList();
    }

    /** Exactly Root, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, Layer. */
    public static List<Item> variationItems(NibaruMaterialProfile profile) {
        List<Item> result = new ArrayList<>(9);
        result.add(requireItem(profile.canonicalParent(), profile, "root"));
        result.add(requireItem(profile.effectiveSlabSource().orElseThrow(() ->
                missing(profile, "slab")), profile, "slab"));
        result.add(requireItem(profile.effectiveStairSource().orElseThrow(() ->
                missing(profile, "stairs")), profile, "stairs"));
        result.add(requireItem(profile.nativeWall().orElseThrow(() ->
                missing(profile, "wall")), profile, "wall"));
        for (BgeGeometryCatalog.Descriptor descriptor : BgeGeometryCatalog.ordered()) {
            result.add(descriptor.resolveItem(profile).orElseThrow(() ->
                    missing(profile, descriptor.role().name().toLowerCase(java.util.Locale.ROOT))));
        }
        if (result.size() != 9 || new LinkedHashSet<>(result).size() != 9) {
            throw new IllegalStateException("Explicit BGE variation is not nine distinct items: "
                    + profile.canonicalParentId() + " " + result.stream()
                            .map(ExplicitShapeMapFamilies::itemId).toList());
        }
        return List.copyOf(result);
    }

    private static List<List<NibaruMaterialProfile>> profileGroups() {
        Map<Identifier, NibaruMaterialProfile> byId = new LinkedHashMap<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            NibaruMaterialProfile previous = byId.putIfAbsent(profile.canonicalParentId(), profile);
            if (previous != null) {
                throw new IllegalStateException("Duplicate explicit material profile "
                        + profile.canonicalParentId());
            }
        }

        Map<Identifier, List<Identifier>> declaredByMember = new LinkedHashMap<>();
        for (List<Identifier> declaration : activeMultiGroups(byId.keySet())) {
            for (Identifier member : declaration) {
                List<Identifier> previous = declaredByMember.putIfAbsent(member, declaration);
                if (previous != null) {
                    throw new IllegalStateException("Material appears in two selector groups: " + member);
                }
            }
        }

        List<List<NibaruMaterialProfile>> result = new ArrayList<>();
        Set<Identifier> emitted = new LinkedHashSet<>();
        for (NibaruMaterialProfile profile : NibaruMaterialProfiles.all()) {
            Identifier id = profile.canonicalParentId();
            if (!emitted.add(id)) continue;
            List<Identifier> declaration = declaredByMember.get(id);
            if (declaration == null) {
                result.add(List.of(profile));
                continue;
            }
            List<NibaruMaterialProfile> group = declaration.stream().map(member -> {
                emitted.add(member);
                return byId.get(member);
            }).toList();
            result.add(group);
        }
        if (emitted.size() != byId.size()) {
            throw new IllegalStateException("Explicit selector catalog omitted profiles: expected="
                    + byId.size() + " emitted=" + emitted.size());
        }
        return List.copyOf(result);
    }

    /** Resolves optional-tail declarations without weakening required multi-variation groups. */
    static List<List<Identifier>> activeMultiGroups(Set<Identifier> registeredProfiles) {
        List<List<Identifier>> result = new ArrayList<>();
        for (List<Identifier> declaration : DECLARED_MULTI_GROUPS) {
            long present = declaration.stream().filter(registeredProfiles::contains).count();
            if (present == 0) continue;
            if (present == declaration.size()) {
                result.add(declaration);
                continue;
            }
            if (OPTIONAL_BBB_GROUPS.contains(declaration)
                    && registeredProfiles.contains(declaration.getFirst())
                    && !registeredProfiles.contains(declaration.getLast())) {
                continue;
            }
            throw new IllegalStateException("Partially registered explicit selector group "
                    + declaration + "; present=" + present);
        }
        return List.copyOf(result);
    }

    private static List<List<Identifier>> declaredMultiGroups() {
        List<List<Identifier>> result = new ArrayList<>();
        result.addAll(OPTIONAL_BBB_GROUPS);
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "pale_oak")) {
            result.add(ids("minecraft:" + wood + "_log", "minecraft:" + wood + "_wood"));
            result.add(ids("minecraft:stripped_" + wood + "_log",
                    "minecraft:stripped_" + wood + "_wood"));
        }
        for (String fungus : List.of("crimson", "warped")) {
            result.add(ids("minecraft:" + fungus + "_stem", "minecraft:" + fungus + "_hyphae"));
            result.add(ids("minecraft:stripped_" + fungus + "_stem",
                    "minecraft:stripped_" + fungus + "_hyphae"));
        }
        for (String tree : List.of("wisteria", "silver_birch")) {
            result.add(ids("mynx_trees:" + tree + "_log", "mynx_trees:" + tree + "_wood"));
        }
        result.add(ids("enderscape:veiled_log", "enderscape:veiled_wood"));
        result.add(ids("enderscape:stripped_veiled_log", "enderscape:stripped_veiled_wood"));
        for (String fungus : List.of("celestial", "murublight")) {
            result.add(ids("enderscape:" + fungus + "_stem",
                    "enderscape:" + fungus + "_hyphae"));
            result.add(ids("enderscape:stripped_" + fungus + "_stem",
                    "enderscape:stripped_" + fungus + "_hyphae"));
        }
        for (String wood : List.of("veiled", "celestial", "murublight")) {
            result.add(ids("enderscape:" + wood + "_planks",
                    CnmTerrainCompat.MOD_ID + ":enderscape/" + wood + "_beam"));
        }
        return result.stream().map(List::copyOf).toList();
    }

    private static List<List<Identifier>> optionalBbbGroups() {
        List<List<Identifier>> result = new ArrayList<>();
        for (String material : List.of("oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            result.add(ids("minecraft:" + material + "_planks", "bbb:" + material + "_beam"));
        }
        return result.stream().map(List::copyOf).toList();
    }

    private static List<Identifier> bbbSelectorAliases() {
        List<Identifier> result = new ArrayList<>();
        for (String material : List.of("oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_slab"));
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_stairs"));
        }
        return List.copyOf(result);
    }

    private static List<Identifier> ids(String... ids) {
        return java.util.Arrays.stream(ids).map(Identifier::parse).toList();
    }

    private static Item requireItem(Block block, NibaruMaterialProfile profile, String role) {
        Item item = block.asItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("Unregistered explicit " + role + " item for "
                    + profile.canonicalParentId());
        }
        return item;
    }

    private static IllegalStateException missing(NibaruMaterialProfile profile, String role) {
        return new IllegalStateException("Missing explicit " + role + " for "
                + profile.canonicalParentId());
    }

    private static Identifier itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("Unregistered ShapeMap item " + item);
        }
        return id;
    }

    private static void putComponent(Map<Item, List<Item>> shapes, Item parent,
            List<Item> members, String owner) {
        List<Item> distinct = new ArrayList<>(new LinkedHashSet<>(members));
        if (distinct.size() != members.size()) {
            throw new IllegalStateException(owner + " ShapeMap component contains duplicates: "
                    + members.stream().map(ExplicitShapeMapFamilies::itemId).toList());
        }
        List<Item> previous = shapes.putIfAbsent(parent, new ArrayList<>(distinct));
        if (previous != null) {
            throw new IllegalStateException("ShapeMap parent collision at " + itemId(parent));
        }
    }
}
