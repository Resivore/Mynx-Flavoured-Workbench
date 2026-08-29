package games.twinhead.moreslabsstairsandwalls.api.material;

import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Exact parent-material trait bridge for geometry registered by companion mods. */
public final class DerivedMaterialTraits {
    private static final Map<Block, Entry> ENTRIES = new IdentityHashMap<>();

    private DerivedMaterialTraits() {}

    public static synchronized void register(Block derived, Block canonicalParent,
            DerivedGeometrySupport.Geometry geometry, int fuelDivisor) {
        Entry entry = new Entry(derived, canonicalParent, geometry, fuelDivisor);
        Entry previous = ENTRIES.putIfAbsent(derived, entry);
        if (previous != null && !previous.equals(entry))
            throw new IllegalStateException("Conflicting derived material traits for " + derived);
    }

    public static synchronized List<Entry> entries() { return new ArrayList<>(ENTRIES.values()); }

    public static synchronized java.util.Optional<Entry> fromBlock(Block block) {
        return java.util.Optional.ofNullable(ENTRIES.get(block));
    }

    public static synchronized java.util.Optional<Block> equivalent(
            Block canonicalParent, DerivedGeometrySupport.Geometry geometry) {
        return ENTRIES.values().stream().filter(entry -> entry.canonicalParent() == canonicalParent
                && entry.geometry() == geometry).map(Entry::derived).findFirst();
    }

    public record Entry(Block derived, Block canonicalParent, DerivedGeometrySupport.Geometry geometry,
            int fuelDivisor) {
        public Entry {
            if (fuelDivisor < 1) throw new IllegalArgumentException("fuelDivisor must be positive");
        }
    }
}
