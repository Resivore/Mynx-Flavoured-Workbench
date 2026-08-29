package com.crispytwig.naturalist.client.model.item;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Parsed form of the protected 1.21.1 item-model override table. */
public record ItemModelOverrides(List<Entry> entries) {
    public static final ItemModelOverrides EMPTY = new ItemModelOverrides(List.of());

    public ItemModelOverrides {
        entries = List.copyOf(entries);
    }

    public Optional<Identifier> resolve(Function<Identifier, Float> propertyGetter) {
        // Vanilla's legacy override list gave the last matching entry priority.
        Map<Identifier, Float> propertyValues = new HashMap<>();
        Function<Identifier, Float> cachedPropertyGetter = id ->
                propertyValues.computeIfAbsent(id, propertyGetter);
        for (int index = this.entries.size() - 1; index >= 0; --index) {
            Entry entry = this.entries.get(index);
            if (entry.matches(cachedPropertyGetter)) {
                return Optional.of(entry.model());
            }
        }
        return Optional.empty();
    }

    public record Entry(Identifier model, Map<Identifier, Float> predicates) {
        public Entry {
            predicates = Map.copyOf(predicates);
        }

        private boolean matches(Function<Identifier, Float> propertyGetter) {
            for (Map.Entry<Identifier, Float> predicate : this.predicates.entrySet()) {
                if (propertyGetter.apply(predicate.getKey()) < predicate.getValue()) {
                    return false;
                }
            }
            return true;
        }
    }
}
