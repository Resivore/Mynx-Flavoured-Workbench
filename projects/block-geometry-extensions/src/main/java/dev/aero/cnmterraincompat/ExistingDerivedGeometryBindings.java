package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Save-compatible ownership metadata; never participates in behavior, visual, or tint selection. */
public final class ExistingDerivedGeometryBindings {
    private static final Map<Key, Supplier<Block>> BINDINGS = Map.of(
            new Key(Identifier.withDefaultNamespace("grass_block"), DerivedGeometrySupport.Geometry.VERTICAL_SLAB),
            () -> CnmTerrainCompat.GRASS_VERTICAL_SLAB);

    private ExistingDerivedGeometryBindings() {}

    public static boolean contains(Identifier parent, DerivedGeometrySupport.Geometry geometry) {
        return BINDINGS.containsKey(new Key(parent, geometry));
    }

    public static Optional<Block> resolve(Identifier parent, DerivedGeometrySupport.Geometry geometry) {
        Supplier<Block> binding = BINDINGS.get(new Key(parent, geometry));
        return binding == null ? Optional.empty() : Optional.of(binding.get());
    }

    private record Key(Identifier parent, DerivedGeometrySupport.Geometry geometry) {}
}
