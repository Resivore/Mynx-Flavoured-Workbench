package dev.resivore.dragonbound.client;

import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Resolves only unanimous, cull-specific exterior face materials. */
final class DirectionalMaterialResolver {
    private DirectionalMaterialResolver() {
    }

    static <T> Optional<EnumMap<Direction, T>> resolve(
            EnumMap<Direction, List<T>> candidates,
            Predicate<T> isSafe) {
        EnumMap<Direction, T> resolved = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            List<T> exterior = candidates.get(direction);
            if (exterior == null || exterior.isEmpty()) {
                return Optional.empty();
            }

            T material = exterior.getFirst();
            if (!isSafe.test(material) || exterior.stream().anyMatch(candidate ->
                    !isSafe.test(candidate) || !material.equals(candidate))) {
                return Optional.empty();
            }
            resolved.put(direction, material);
        }
        return Optional.of(resolved);
    }
}
