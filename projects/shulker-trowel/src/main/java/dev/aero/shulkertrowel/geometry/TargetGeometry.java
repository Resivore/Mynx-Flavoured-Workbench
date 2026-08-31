package dev.aero.shulkertrowel.geometry;

import dev.aero.cnmterraincompat.BgeGeometryCatalog;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One Trowel selector mode.
 *
 * <p>The four native modes remain Trowel-owned. Every BGE-derived mode is projected from
 * {@link BgeGeometryCatalog}; adding a catalog descriptor therefore adds a Trowel mode without a
 * geometry-name change here. Saved/network identity and selector order are intentionally separate.</p>
 */
public final class TargetGeometry {
    public static final TargetGeometry FULL = nativeMode(
            "full_block", 0, 0, "Full Block", NativeRole.FULL);
    public static final TargetGeometry SLAB = nativeMode(
            "slab", 1, 1, "Slab", NativeRole.SLAB);
    public static final TargetGeometry STAIR = nativeMode(
            "stair", 2, 2, "Stair", NativeRole.STAIR);
    public static final TargetGeometry WALL = nativeMode(
            "wall", 3, 3, "Wall", NativeRole.WALL);

    private static final List<TargetGeometry> ORDERED;
    private static final Map<Integer, TargetGeometry> BY_PERSISTENCE_ID;
    private static final Map<Identifier, TargetGeometry> BY_KEY;

    static {
        List<TargetGeometry> modes = new ArrayList<>(List.of(FULL, SLAB, STAIR, WALL));
        BgeGeometryCatalog.ordered().stream().map(TargetGeometry::bgeMode).forEach(modes::add);
        modes.sort(Comparator.comparingInt(TargetGeometry::selectorOrder));

        Map<Integer, TargetGeometry> byPersistenceId = new LinkedHashMap<>();
        Map<Identifier, TargetGeometry> byKey = new LinkedHashMap<>();
        for (int index = 0; index < modes.size(); index++) {
            TargetGeometry mode = modes.get(index);
            if (mode.selectorOrder != index) {
                throw new IllegalStateException("Non-contiguous Trowel selector order at " + mode.key
                        + ": expected " + index + ", got " + mode.selectorOrder);
            }
            if (byPersistenceId.putIfAbsent(mode.persistenceId, mode) != null) {
                throw new IllegalStateException("Duplicate Trowel persistence ID " + mode.persistenceId);
            }
            if (byKey.putIfAbsent(mode.key, mode) != null) {
                throw new IllegalStateException("Duplicate Trowel geometry key " + mode.key);
            }
        }
        ORDERED = List.copyOf(modes);
        BY_PERSISTENCE_ID = Map.copyOf(byPersistenceId);
        BY_KEY = Map.copyOf(byKey);
    }

    private final Identifier key;
    private final int persistenceId;
    private final int selectorOrder;
    private final String displayName;
    private final NativeRole nativeRole;
    private final BgeGeometryCatalog.Descriptor bgeDescriptor;

    private TargetGeometry(Identifier key, int persistenceId, int selectorOrder,
            String displayName, NativeRole nativeRole,
            BgeGeometryCatalog.Descriptor bgeDescriptor) {
        this.key = Objects.requireNonNull(key, "key");
        this.persistenceId = persistenceId;
        this.selectorOrder = selectorOrder;
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.nativeRole = nativeRole;
        this.bgeDescriptor = bgeDescriptor;
        if ((nativeRole == null) == (bgeDescriptor == null)) {
            throw new IllegalArgumentException("A Trowel mode must have exactly one owner");
        }
    }

    private static TargetGeometry nativeMode(String path, int persistenceId,
            int selectorOrder, String displayName, NativeRole nativeRole) {
        return new TargetGeometry(
                Identifier.fromNamespaceAndPath("shulker_trowel", path),
                persistenceId, selectorOrder, displayName, nativeRole, null);
    }

    private static TargetGeometry bgeMode(BgeGeometryCatalog.Descriptor descriptor) {
        return new TargetGeometry(descriptor.key(), descriptor.persistenceId(),
                descriptor.selectorOrder(), descriptor.displayName(), null, descriptor);
    }

    public Identifier key() {
        return key;
    }

    /** Stable saved/network identity; never use selector index for persistence. */
    public int persistenceId() {
        return persistenceId;
    }

    /** Compatibility name for the existing integer payload. */
    public int networkId() {
        return persistenceId;
    }

    public int selectorOrder() {
        return selectorOrder;
    }

    public int selectorIndex() {
        return ORDERED.indexOf(this);
    }

    public String displayName() {
        return displayName;
    }

    public Optional<NativeRole> nativeRole() {
        return Optional.ofNullable(nativeRole);
    }

    public Optional<BgeGeometryCatalog.Descriptor> bgeDescriptor() {
        return Optional.ofNullable(bgeDescriptor);
    }

    /** Stable selector order, independent of persistent/network decoding. */
    public static List<TargetGeometry> ordered() {
        return ORDERED;
    }

    public static Optional<TargetGeometry> fromSelectorIndex(int index) {
        return index >= 0 && index < ORDERED.size()
                ? Optional.of(ORDERED.get(index))
                : Optional.empty();
    }

    public static TargetGeometry byNetworkId(int id) {
        return fromNetworkId(id).orElse(FULL);
    }

    public static Optional<TargetGeometry> fromNetworkId(int id) {
        return Optional.ofNullable(BY_PERSISTENCE_ID.get(id));
    }

    public static Optional<TargetGeometry> byKey(Identifier key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    @Override
    public String toString() {
        return displayName + " [" + key + "]";
    }

    public enum NativeRole { FULL, SLAB, STAIR, WALL }
}
