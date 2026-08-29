package dev.resivore.matchajei;

import java.util.Set;

public final class MatchaNamespaces {
    public static final Set<String> RECIPE_NAMESPACES = Set.of(
            "blasting", "blessings", "crafting", "custom_music", "debug", "food",
            "smelting", "smithing_table", "smoking", "stonecutting"
    );

    private MatchaNamespaces() {
    }

    public static boolean contains(String namespace) {
        return RECIPE_NAMESPACES.contains(namespace);
    }
}
