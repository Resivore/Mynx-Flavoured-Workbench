package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates selector uniqueness by family + canonical source variant + geometry role. */
public final class CanonicalShapeMapAudit {
    private static final List<String> STANDARD_ROLES = List.of("slab", "stairs", "wall");

    private CanonicalShapeMapAudit() {}

    public static Report inspectExternalFamilies() {
        List<Member> members = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int variants = 0;
        for (ExternalMaterialFamilies.Binding binding : ExternalMaterialFamilies.all()) {
            variants++;
            Item source = binding.source().asItem();
            List<Item> component = ShapeMap.getShapes(source);
            Identifier family = itemId(ShapeMap.getParent(source));
            Identifier variant = binding.spec().id();
            for (Map.Entry<String, Block> role : binding.roles().entrySet()) {
                Item selected = role.getValue().asItem();
                int occurrences = (int) component.stream().filter(item -> item == selected).count();
                if (occurrences != 1) {
                    missing.add(variant + " " + role.getKey() + " occurrences=" + occurrences);
                }
                members.add(new Member(new CanonicalKey(family, variant, Role.from(role.getKey())),
                        itemId(selected)));
            }

            // A provider-native standard role and the historical BGE-generated equivalent are
            // semantically the same key. If both ever enter the actual CNM component, report the
            // collision even though their registry IDs and concrete block instances differ.
            for (String role : STANDARD_ROLES) {
                CanonicalKey key = new CanonicalKey(family, variant, Role.from(role));
                addAlternative(component, binding.roles().get(role), key,
                        binding.spec().providerRoles().get(role), members);
                addAlternative(component, binding.roles().get(role), key,
                        ExternalMaterialFamilies.id(binding.spec(), role), members);
            }
        }
        return new Report(variants, List.copyOf(members), duplicates(members), List.copyOf(missing));
    }

    public static void requireExternalFamilies() {
        Report report = inspectExternalFamilies();
        if (!report.missing().isEmpty() || !report.duplicates().isEmpty()) {
            throw new IllegalStateException("Invalid external ShapeMap families: missing="
                    + report.missing() + ", duplicates=" + report.duplicates());
        }
    }

    /** Compares every final, user-visible supported selector component byte-for-byte by item ID. */
    public static ExactReport inspectExplicitFamilies() {
        List<List<Identifier>> expectedGroups = ExplicitShapeMapFamilies.expectedIdGroups();
        List<String> failures = new ArrayList<>();
        int variations = 0;
        for (List<Identifier> expected : expectedGroups) {
            if (expected.isEmpty() || expected.size() % 9 != 0) {
                failures.add("invalid expected group " + expected);
                continue;
            }
            variations += expected.size() / 9;
            Item root = BuiltInRegistries.ITEM.getValue(expected.getFirst());
            List<Identifier> actual = ShapeMap.getShapes(root).stream()
                    .map(CanonicalShapeMapAudit::itemId).toList();
            if (!actual.equals(expected)) {
                failures.add(expected.getFirst() + " expected=" + expected + " actual=" + actual);
            }
            if (new LinkedHashSet<>(actual).size() != actual.size()) {
                failures.add(expected.getFirst() + " contains duplicate items " + actual);
            }
            for (Identifier member : expected) {
                Item item = BuiltInRegistries.ITEM.getValue(member);
                if (ShapeMap.getParent(item) != root) {
                    failures.add(member + " parent=" + itemId(ShapeMap.getParent(item))
                            + " expected=" + expected.getFirst());
                }
            }
        }
        return new ExactReport(expectedGroups.size(), variations, List.copyOf(failures));
    }

    public static void requireExplicitFamilies() {
        requireExternalFamilies();
        ExactReport report = inspectExplicitFamilies();
        if (!report.failures().isEmpty()) {
            throw new IllegalStateException("Invalid explicit ShapeMap selector sequences: "
                    + report.failures());
        }
    }

    /** Pure duplicate detector used by direct regressions as well as the live ShapeMap audit. */
    public static List<Duplicate> duplicates(List<Member> members) {
        Map<CanonicalKey, LinkedHashSet<Identifier>> byKey = new LinkedHashMap<>();
        for (Member member : members) {
            byKey.computeIfAbsent(member.key(), ignored -> new LinkedHashSet<>()).add(member.item());
        }
        List<Duplicate> result = new ArrayList<>();
        byKey.forEach((key, items) -> {
            if (items.size() > 1) result.add(new Duplicate(key, List.copyOf(items)));
        });
        return List.copyOf(result);
    }

    private static void addAlternative(List<Item> component, Block selected, CanonicalKey key,
            Identifier candidateId, List<Member> members) {
        if (candidateId == null) return;
        Block candidate = BuiltInRegistries.BLOCK.getValue(candidateId);
        if (candidate == null || !candidateId.equals(BuiltInRegistries.BLOCK.getKey(candidate))
                || candidate == selected || !component.contains(candidate.asItem())) return;
        members.add(new Member(key, itemId(candidate.asItem())));
    }

    private static Identifier itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalStateException("Unregistered ShapeMap item " + item);
        }
        return id;
    }

    public enum Role {
        BLOCK, SLAB, STAIRS, WALL, VERTICAL_SLAB, STEP, CORNER, QUARTER_COLUMN, LAYER;

        static Role from(String role) {
            return valueOf(role.toUpperCase(java.util.Locale.ROOT));
        }
    }

    public record CanonicalKey(Identifier family, Identifier variant, Role role) {}
    public record Member(CanonicalKey key, Identifier item) {}
    public record Duplicate(CanonicalKey key, List<Identifier> items) {
        public Duplicate { items = List.copyOf(items); }
    }
    public record Report(int variantCount, List<Member> members,
            List<Duplicate> duplicates, List<String> missing) {
        public Report {
            members = List.copyOf(members);
            duplicates = List.copyOf(duplicates);
            missing = List.copyOf(missing);
        }
    }
    public record ExactReport(int groupCount, int variationCount, List<String> failures) {
        public ExactReport { failures = List.copyOf(failures); }
    }
}
