package dev.resivore.matchajei;

import java.util.Set;

public final class MatchaNamespaces {
    public static final Set<String> RECIPE_NAMESPACES = Set.of("matcha", "debug");

    private MatchaNamespaces() {
    }

    public static boolean contains(String namespace) {
        return RECIPE_NAMESPACES.contains(namespace);
    }
}
