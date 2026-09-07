package dev.resivore.blockfamilies.cnm.runtime;

import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime bridge between the artifact-bound literal catalog and CNM's Item
 * graph. This class deliberately owns no registry-name inference.
 */
public final class AuditedShapeRuntime {
    public static final int MAPPING_PRIORITY = 1_000;

    private static volatile ResolvedCatalog resolvedCatalog;

    private AuditedShapeRuntime() {
    }

    public static synchronized void addMappings(List<ShapeMap.Mapping> mappings) {
        ResolvedCatalog catalog = resolveCatalog();
        rejectPreexistingClaims(mappings, catalog);

        for (ResolvedFamily family : catalog.families()) {
            Item parent = family.members().get(0);
            for (int index = 1; index < family.members().size(); index++) {
                mappings.add(new ShapeMap.Mapping(
                        parent,
                        family.members().get(index),
                        MAPPING_PRIORITY,
                        family.definition().key()
                ));
            }
        }
        resolvedCatalog = catalog;
    }

    public static synchronized void assertResolvedShapeMap() {
        ResolvedCatalog catalog = resolved();
        for (ResolvedFamily family : catalog.families()) {
            Item parent = family.members().get(0);
            List<Item> actual = ShapeMap.getShapes(parent);
            if (!actual.equals(family.members())) {
                throw new IllegalStateException("CNM component differs from audited IBF family "
                        + family.definition().key() + ": expected=" + ids(family.members())
                        + ", actual=" + ids(actual));
            }
            for (int index = 1; index < family.members().size(); index++) {
                Item member = family.members().get(index);
                if (ShapeMap.getParent(member) != parent) {
                    throw new IllegalStateException("CNM parent differs from audited IBF family "
                            + family.definition().key() + " for "
                            + BuiltInRegistries.ITEM.getKey(member));
                }
            }
        }
    }

    /**
     * Mirrors CNM's disabled ShapeMap state. An empty synchronized map is the
     * exact payload/state produced by CNM 2.0.7 when its startup switch is off.
     */
    public static synchronized void acceptSynchronizedShapeMap() {
        if (ShapeMap.shapesView().isEmpty() && ShapeMap.inverseView().isEmpty()) {
            resolvedCatalog = null;
            return;
        }
        resolvedCatalog = resolveCatalog();
        assertResolvedShapeMap();
    }

    public static synchronized void disable() {
        resolvedCatalog = null;
    }

    public static boolean isAudited(Item item) {
        ResolvedCatalog catalog = resolvedCatalog;
        return catalog != null && catalog.byMember().containsKey(item);
    }

    /**
     * CNM's recipe cleanup must distinguish an acquisition recipe for an IBF
     * parent from a recipe which produces one of the parent\'s alternates.
     * ShapeMap intentionally exposes the parent as the first component member,
     * so callers which are matching cleanup candidates need this explicit
     * ownership check rather than treating every component member alike.
     */
    public static boolean isAuditedCanonicalParent(Item item) {
        ResolvedCatalog catalog = resolvedCatalog;
        if (catalog == null) {
            return false;
        }
        ResolvedFamily family = catalog.byMember().get(item);
        return family != null && family.members().getFirst() == item;
    }

    /**
     * True only for non-parent members of the two audited multi-block door
     * categories. Those members must retain their provider-native loot pass
     * because it is the provider state that identifies the one authoritative
     * physical segment and the exact design item to drop.
     */
    public static boolean isAuditedAlternateDoor(Item item) {
        ResolvedCatalog catalog = resolvedCatalog;
        if (catalog == null) {
            return false;
        }
        ResolvedFamily family = catalog.byMember().get(item);
        if (family == null || family.members().getFirst() == item) {
            return false;
        }
        AuditedShapeFamily.Category category = family.definition().category();
        return category == AuditedShapeFamily.Category.TWO_HIGH_DOOR
                || category == AuditedShapeFamily.Category.THREE_HIGH_DOOR;
    }

    public static boolean inSameFamily(Item first, Item second) {
        ResolvedCatalog catalog = resolvedCatalog;
        if (catalog == null) {
            return false;
        }
        ResolvedFamily family = catalog.byMember().get(first);
        return family != null && family == catalog.byMember().get(second);
    }

    public static int windowStart(int selectedIndex, int totalSize, int visibleSize) {
        if (totalSize <= 0 || visibleSize <= 0 || visibleSize >= totalSize) {
            return 0;
        }
        int selected = Math.max(0, Math.min(selectedIndex, totalSize - 1));
        int preferred = selected - visibleSize / 2;
        return Math.max(0, Math.min(preferred, totalSize - visibleSize));
    }

    private static ResolvedCatalog resolved() {
        ResolvedCatalog current = resolvedCatalog;
        if (current != null) {
            return current;
        }
        synchronized (AuditedShapeRuntime.class) {
            current = resolvedCatalog;
            if (current == null) {
                current = resolveCatalog();
                resolvedCatalog = current;
            }
            return current;
        }
    }

    private static ResolvedCatalog resolveCatalog() {
        List<AuditedShapeFamily> definitions = AuditedShapeFamilies.families();
        if (definitions.size() != AuditedShapeFamilies.EXPECTED_FAMILY_COUNT
                || AuditedShapeFamilies.uniqueMemberCount()
                != AuditedShapeFamilies.EXPECTED_UNIQUE_MEMBER_COUNT
                || AuditedShapeFamilies.largestFamilySize()
                != AuditedShapeFamilies.EXPECTED_LARGEST_FAMILY_SIZE) {
            throw new IllegalStateException("Audited IBF catalog summary does not match its pinned contract");
        }

        ArrayList<ResolvedFamily> families = new ArrayList<>(definitions.size());
        IdentityHashMap<Item, ResolvedFamily> byMember = new IdentityHashMap<>();
        for (AuditedShapeFamily definition : definitions) {
            ArrayList<Item> members = new ArrayList<>(definition.members().size());
            for (Identifier id : definition.members()) {
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    throw new IllegalStateException("Missing audited IBF item: " + id
                            + " (family " + definition.key() + ")");
                }
                members.add(BuiltInRegistries.ITEM.getValue(id));
            }

            ResolvedFamily family = new ResolvedFamily(definition, List.copyOf(members));
            for (Item member : members) {
                ResolvedFamily previous = byMember.put(member, family);
                if (previous != null) {
                    throw new IllegalStateException("Audited IBF item claimed by both "
                            + previous.definition().key() + " and " + definition.key() + ": "
                            + BuiltInRegistries.ITEM.getKey(member));
                }
            }
            families.add(family);
        }
        return new ResolvedCatalog(List.copyOf(families), immutableIdentityMap(byMember));
    }

    private static void rejectPreexistingClaims(
            List<ShapeMap.Mapping> mappings,
            ResolvedCatalog catalog
    ) {
        IdentityHashMap<Item, Item> roots = new IdentityHashMap<>();
        for (ShapeMap.Mapping mapping : mappings) {
            union(roots, mapping.parent(), mapping.shape());
        }

        IdentityHashMap<Item, Set<Item>> components = new IdentityHashMap<>();
        for (ShapeMap.Mapping mapping : mappings) {
            Item root = find(roots, mapping.parent());
            Set<Item> members = components.computeIfAbsent(root,
                    ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
            members.add(mapping.parent());
            members.add(mapping.shape());
        }

        Set<Item> checkedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<Item, ResolvedFamily> claimed : catalog.byMember().entrySet()) {
            Item root = find(roots, claimed.getKey());
            Set<Item> component = components.get(root);
            if (component == null || !checkedRoots.add(root)) {
                continue;
            }
            for (Item member : component) {
                ResolvedFamily actualFamily = catalog.byMember().get(member);
                if (actualFamily != claimed.getValue()) {
                    throw new IllegalStateException("Pre-existing CNM component crosses audited IBF boundary for "
                            + claimed.getValue().definition().key() + ": " + ids(component));
                }
            }
        }
    }

    private static Item find(IdentityHashMap<Item, Item> roots, Item item) {
        Item parent = roots.get(item);
        if (parent == null || parent == item) {
            return item;
        }
        Item root = find(roots, parent);
        roots.put(item, root);
        return root;
    }

    private static void union(IdentityHashMap<Item, Item> roots, Item first, Item second) {
        Item firstRoot = find(roots, first);
        Item secondRoot = find(roots, second);
        if (firstRoot != secondRoot) {
            roots.put(firstRoot, secondRoot);
        }
    }

    private static Map<Item, ResolvedFamily> immutableIdentityMap(
            IdentityHashMap<Item, ResolvedFamily> source
    ) {
        IdentityHashMap<Item, ResolvedFamily> copy = new IdentityHashMap<>();
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }

    private static List<Identifier> ids(Iterable<Item> items) {
        ArrayList<Identifier> result = new ArrayList<>();
        for (Item item : items) {
            result.add(BuiltInRegistries.ITEM.getKey(item));
        }
        return List.copyOf(result);
    }

    private record ResolvedCatalog(
            List<ResolvedFamily> families,
            Map<Item, ResolvedFamily> byMember
    ) {
    }

    private record ResolvedFamily(AuditedShapeFamily definition, List<Item> members) {
    }
}
