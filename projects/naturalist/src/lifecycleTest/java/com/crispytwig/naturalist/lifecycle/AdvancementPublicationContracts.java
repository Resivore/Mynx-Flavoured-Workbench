package com.crispytwig.naturalist.lifecycle;

import com.crispytwig.naturalist.Naturalist;
import net.minecraft.advancements.*;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DynamicTest;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Runs every case through the production-transformed manager, including its final tree. */
final class AdvancementPublicationContracts {
    private static final Identifier VANILLA = Identifier.withDefaultNamespace("husbandry/root");
    private static final Identifier MATCHA = Identifier.parse("main:tutorial/root");
    private static final Identifier FISHING = Identifier.withDefaultNamespace("husbandry/tactical_fishing");
    private static final Identifier OTHER = Identifier.parse("other:root");
    private static final Identifier CHILD = Identifier.parse("other:child");
    private static final Identifier UNTOUCHED = Naturalist.location("husbandry/not_a_fallback_target");
    private static final List<Identifier> TARGETS = Stream.of("feed_bear_honeycomb", "feed_hippo_melon", "ride_giraffe_with_map")
            .map(name -> Naturalist.location("husbandry/" + name)).toList();
    private enum Input {
        COPY, WRAPPER, MUTABLE;
        Map<Identifier, Advancement> wrap(Map<Identifier, Advancement> values) {
            return switch (this) {
                case COPY -> Map.copyOf(values);
                case WRAPPER -> Collections.unmodifiableMap(values);
                case MUTABLE -> values;
            };
        }
    }
    private enum Parenting { MATCHA, VANILLA, ABSENT, INVALID, ALREADY_REPARENTED }
    private enum Fishing { ABSENT, FRESH, ONE_BUCKET, BUCKET_IN_LATER_GROUP, BOTH_BUCKETS, EMPTY_REQUIREMENTS }

    static Stream<DynamicTest> cases() {
        return Arrays.stream(Input.values()).flatMap(input -> Stream.concat(
                Stream.concat(Arrays.stream(Parenting.values()).map(parenting ->
                        DynamicTest.dynamicTest(input + " parenting " + parenting, () -> parenting(input, parenting))),
                        Arrays.stream(Fishing.values()).map(fishing ->
                                DynamicTest.dynamicTest(input + " fishing " + fishing, () -> fishing(input, fishing)))),
                Stream.of(DynamicTest.dynamicTest(input + " prior immutable hook and fresh reload", () -> precedingHook(input)))));
    }

    private static Map<Identifier, Advancement> base() {
        var values = new LinkedHashMap<Identifier, Advancement>();
        var root = LifecycleTest.preparedAdvancements.get(VANILLA);
        values.put(OTHER, root);
        values.put(CHILD, parent(root, OTHER));
        TARGETS.forEach(id -> values.put(id, LifecycleTest.preparedAdvancements.get(id)));
        // Same namespace and original parent as the targets, but outside the exact allowlist.
        values.put(UNTOUCHED, values.get(TARGETS.getFirst()));
        return values;
    }

    private static void parenting(Input kind, Parenting parenting) {
        var values = base();
        var root = values.get(OTHER);
        if (parenting != Parenting.ABSENT) values.put(MATCHA, parenting == Parenting.INVALID ? parent(root, OTHER) : root);
        if (parenting == Parenting.VANILLA) values.put(VANILLA, root);
        if (parenting == Parenting.ALREADY_REPARENTED) values.put(TARGETS.getFirst(), parent(values.get(TARGETS.getFirst()), OTHER));
        var input = kind.wrap(values);
        var snapshot = new Snapshot(input);
        var manager = new LifecycleTest.AdvancementHarness(LifecycleTest.registries);
        manager.publish(input);
        snapshot.unchanged(input);
        assertEquals(input.keySet(), published(manager).keySet());
        var nodeIds = nodeIds(manager);
        for (var id : TARGETS) {
            var old = input.get(id);
            var expected = old.parent().orElseThrow();
            if (expected.equals(VANILLA) && (parenting == Parenting.MATCHA || parenting == Parenting.ALREADY_REPARENTED)) expected = MATCHA;
            var actual = manager.get(id).value();
            assertEquals(Optional.of(expected), actual.parent(), id.toString());
            assertEquals(parent(old, expected), actual, "All other record fields must survive");
            assertEquals(input.containsKey(expected), nodeIds.contains(id), "Final tree parent resolution: " + id);
        }
        for (var id : input.keySet()) if (!TARGETS.contains(id)) assertSame(input.get(id), manager.get(id).value(), id.toString());
        assertTrue(nodeIds.containsAll(List.of(OTHER, CHILD)));
        assertEquals(parenting == Parenting.VANILLA, nodeIds.contains(UNTOUCHED), "Only the three named entries receive fallback parenting");
        manager.publish(input);
        snapshot.unchanged(input);
        assertEquals(input.keySet(), published(manager).keySet());
    }

    private static void fishing(Input kind, Fishing mode) {
        var values = base();
        values.put(VANILLA, values.get(OTHER));
        var original = parent(LifecycleTest.preparedAdvancements.get(FISHING), VANILLA);
        var criteria = new LinkedHashMap<String, Criterion<?>>(original.criteria());
        var alternatives = new ArrayList<>(original.requirements().requirements().getFirst());
        var existingCriterion = criteria.values().iterator().next();
        criteria.put("prior_extra", existingCriterion);
        if (mode == Fishing.ONE_BUCKET || mode == Fishing.BOTH_BUCKETS) {
            criteria.put("catfish_bucket", existingCriterion);
            alternatives.add("catfish_bucket");
        }
        if (mode == Fishing.BOTH_BUCKETS) {
            criteria.put("bass_bucket", existingCriterion);
            alternatives.add("bass_bucket");
        }
        var groups = new ArrayList<>(original.requirements().requirements());
        groups.set(0, List.copyOf(alternatives));
        groups.add(List.of("prior_extra"));
        if (mode == Fishing.BUCKET_IN_LATER_GROUP) {
            criteria.put("catfish_bucket", existingCriterion);
            groups.add(List.of("catfish_bucket"));
        }
        var requirements = new AdvancementRequirements(mode == Fishing.EMPTY_REQUIREMENTS ? List.of() : List.copyOf(groups));
        if (mode != Fishing.ABSENT) values.put(FISHING, new Advancement(original.parent(), original.display(), original.rewards(),
                Map.copyOf(criteria), requirements, original.sendsTelemetryEvent(), original.name()));
        var input = kind.wrap(values);
        var snapshot = new Snapshot(input);
        var manager = new LifecycleTest.AdvancementHarness(LifecycleTest.registries);
        manager.publish(input);
        snapshot.unchanged(input);
        if (mode == Fishing.ABSENT) {
            assertNull(manager.get(FISHING));
        } else {
            var before = input.get(FISHING);
            var actual = manager.get(FISHING).value();
            assertTrue(nodeIds(manager).contains(FISHING));
            assertEquals(before.parent(), actual.parent());
            assertEquals(before.display(), actual.display());
            assertEquals(before.rewards(), actual.rewards());
            assertEquals(before.name(), actual.name());
            assertEquals(before.sendsTelemetryEvent(), actual.sendsTelemetryEvent());
            if (mode == Fishing.EMPTY_REQUIREMENTS) {
                assertSame(before, actual);
            } else {
                var expectedCriteria = new HashSet<>(before.criteria().keySet());
                expectedCriteria.addAll(List.of("catfish_bucket", "bass_bucket"));
                assertEquals(expectedCriteria, actual.criteria().keySet());
                before.criteria().forEach((name, criterion) -> assertSame(criterion, actual.criteria().get(name)));
                var expectedGroups = new ArrayList<>(before.requirements().requirements());
                var expectedAlternatives = new ArrayList<>(expectedGroups.getFirst());
                for (String bucket : List.of("catfish_bucket", "bass_bucket")) {
                    if (!before.criteria().containsKey(bucket)) expectedAlternatives.add(bucket);
                }
                expectedGroups.set(0, expectedAlternatives);
                assertEquals(expectedGroups, actual.requirements().requirements());
                assertTrue(actual.requirements().validate(actual.criteria().keySet()).isSuccess());
            }
        }
        for (var id : input.keySet()) if (!id.equals(FISHING)) assertSame(input.get(id), manager.get(id).value());
        var first = published(manager);
        manager.publish(kind.wrap(new LinkedHashMap<>(first)));
        assertEquals(first, published(manager), "Reloading adjusted records must not accumulate alternatives");
        snapshot.unchanged(input);
    }

    private static void precedingHook(Input kind) {
        var values = base();
        values.put(FISHING, LifecycleTest.preparedAdvancements.get(FISHING));
        var input = kind.wrap(values);
        var original = new Snapshot(input);
        var incomingSnapshots = new ArrayList<Snapshot>();
        var replacements = new ArrayList<Map<Identifier, Advancement>>();
        var ownId = Identifier.parse("main:mechanics/heart_container_obtained");
        AdvancementReloadFixture.replacement = incoming -> {
            assertSame(input, incoming, "The preceding hook receives the original argument");
            var changed = new LinkedHashMap<>(incoming);
            changed.put(MATCHA, incoming.get(OTHER));
            changed.put(ownId, parent(incoming.get(OTHER), MATCHA));
            changed.put(TARGETS.getFirst(), parent(incoming.get(TARGETS.getFirst()), ownId));
            var immutable = Map.copyOf(changed);
            replacements.add(immutable);
            incomingSnapshots.add(new Snapshot(immutable));
            return immutable;
        };
        var manager = new LifecycleTest.AdvancementHarness(LifecycleTest.registries);
        try {
            manager.publish(input);
        } finally {
            AdvancementReloadFixture.replacement = UnaryOperator.identity();
        }
        assertEquals(1, replacements.size(), "The transformed preceding hook must execute");
        original.unchanged(input);
        incomingSnapshots.getFirst().unchanged(replacements.getFirst());
        assertEquals(replacements.getFirst().keySet(), published(manager).keySet());
        assertSame(replacements.getFirst().get(ownId), manager.get(ownId).value());
        assertSame(replacements.getFirst().get(TARGETS.getFirst()), manager.get(TARGETS.getFirst()).value());
        for (var id : TARGETS.subList(1, TARGETS.size())) assertEquals(Optional.of(MATCHA), manager.get(id).value().parent());
        assertTrue(nodeIds(manager).containsAll(TARGETS));
        assertTrue(nodeIds(manager).contains(ownId));
        assertTrue(manager.get(FISHING).value().criteria().keySet().containsAll(List.of("catfish_bucket", "bass_bucket")));
        // A new reload removes the prior hook's records and uses the vanilla parent again.
        var fresh = base();
        fresh.put(VANILLA, fresh.get(OTHER));
        fresh.remove(TARGETS.getLast());
        manager.publish(kind.wrap(fresh));
        assertEquals(fresh, published(manager), "No state from the preceding publication may leak");
        assertFalse(nodeIds(manager).contains(ownId));
        for (var id : TARGETS.subList(0, 2)) assertEquals(Optional.of(VANILLA), manager.get(id).value().parent());
        assertNull(manager.get(TARGETS.getLast()));
    }

    private static Advancement parent(Advancement value, Identifier parent) {
        return new Advancement(Optional.of(parent), value.display(), value.rewards(), value.criteria(),
                value.requirements(), value.sendsTelemetryEvent(), value.name());
    }
    private static Map<Identifier, Advancement> published(LifecycleTest.AdvancementHarness manager) {
        var result = new LinkedHashMap<Identifier, Advancement>();
        manager.getAllAdvancements().forEach(holder -> result.put(holder.id(), holder.value()));
        return result;
    }
    private static Set<Identifier> nodeIds(LifecycleTest.AdvancementHarness manager) {
        var result = new HashSet<Identifier>();
        manager.tree().nodes().forEach(node -> result.add(node.holder().id()));
        return result;
    }
    private record Nested(Map<String, Criterion<?>> criteria, List<List<String>> requirements) {
        Nested(Advancement value) {
            this(new LinkedHashMap<>(value.criteria()), value.requirements().requirements().stream().map(List::copyOf).toList());
        }
    }
    private record Snapshot(Map<Identifier, Advancement> entries, Map<Identifier, Nested> nested) {
        Snapshot(Map<Identifier, Advancement> input) {
            this(new LinkedHashMap<>(input), new LinkedHashMap<>());
            input.forEach((id, value) -> nested.put(id, new Nested(value)));
        }
        void unchanged(Map<Identifier, Advancement> input) {
            assertEquals(entries.keySet(), input.keySet());
            entries.forEach((id, value) -> {
                assertSame(value, input.get(id), "Input record replaced: " + id);
                assertEquals(nested.get(id), new Nested(input.get(id)), "Input nested collections modified: " + id);
            });
        }
    }
}
