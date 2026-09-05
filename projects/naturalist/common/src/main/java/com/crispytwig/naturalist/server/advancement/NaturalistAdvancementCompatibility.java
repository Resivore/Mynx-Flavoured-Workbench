package com.crispytwig.naturalist.server.advancement;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.registry.NaturalistRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.FilledBucketTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adjusts decoded advancements immediately before the server builds their tree. */
public final class NaturalistAdvancementCompatibility {
    private static final Identifier HUSBANDRY_ROOT = Identifier.withDefaultNamespace("husbandry/root");
    private static final Identifier MATCHA_ROOT = Identifier.fromNamespaceAndPath("main", "tutorial/root");
    private static final Identifier TACTICAL_FISHING = Identifier.withDefaultNamespace("husbandry/tactical_fishing");

    private NaturalistAdvancementCompatibility() { }

    public static Map<Identifier, Advancement> prepareForPublication(Map<Identifier, Advancement> incoming) {
        // Reload listeners may supply immutable maps. Keep their entries and nested
        // collections intact, and return the copy consumed by the publication hook.
        Map<Identifier, Advancement> advancements = new LinkedHashMap<>(incoming);
        // Matcha filters the vanilla husbandry tree and provides this root.
        // Keep vanilla parenting whenever available; never rewrite another mod.
        if (!advancements.containsKey(HUSBANDRY_ROOT) && advancements.containsKey(MATCHA_ROOT)
                && advancements.get(MATCHA_ROOT).parent().isEmpty()) {
            for (String name : List.of("feed_bear_honeycomb", "feed_hippo_melon", "ride_giraffe_with_map")) {
                Identifier id = Naturalist.location("husbandry/" + name);
                Advancement value = advancements.get(id);
                if (value != null && value.parent().equals(Optional.of(HUSBANDRY_ROOT))) {
                    advancements.put(id, new Advancement(Optional.of(MATCHA_ROOT), value.display(), value.rewards(),
                            value.criteria(), value.requirements(), value.sendsTelemetryEvent(), value.name()));
                }
            }
        }

        Advancement fishing = advancements.get(TACTICAL_FISHING);
        if (fishing == null || fishing.requirements().isEmpty()) return advancements;
        Map<String, Criterion<?>> criteria = new LinkedHashMap<>(fishing.criteria());
        var requirements = new ArrayList<>(fishing.requirements().requirements());
        var alternatives = new ArrayList<>(requirements.getFirst());
        addBucket(criteria, alternatives, "catfish_bucket", NaturalistRegistry.CATFISH_BUCKET.get());
        addBucket(criteria, alternatives, "bass_bucket", NaturalistRegistry.BASS_BUCKET.get());
        requirements.set(0, alternatives);
        advancements.put(TACTICAL_FISHING, new Advancement(fishing.parent(), fishing.display(), fishing.rewards(),
                criteria, new AdvancementRequirements(requirements), fishing.sendsTelemetryEvent(), fishing.name()));
        return advancements;
    }

    private static void addBucket(Map<String, Criterion<?>> criteria, List<String> alternatives, String name, Item item) {
        if (criteria.containsKey(name)) return;
        criteria.put(name, FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, item)));
        alternatives.add(name);
    }
}
