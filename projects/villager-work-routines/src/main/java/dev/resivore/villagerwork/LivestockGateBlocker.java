package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-transient ownership index for gates physically opened by a VWR task.
 * Gate ownership remains explicit; this service never opens or closes a block itself.
 */
public final class LivestockGateBlocker {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkRoutines");
    private static final String COVERED_TYPES = "[sheep,cow,pig,chicken,rabbit]";
    private static final OwnerIndex<ServerLevel, Long, GateIdentity, Owner> INDEX =
            new OwnerIndex<>(new WeakHashMap<>());

    private LivestockGateBlocker() {}

    /** One route lease; the route UUID keeps later work by the same villager owner-distinct. */
    public record Owner(UUID villagerId, UUID routeId) {
        public Owner {
            Objects.requireNonNull(villagerId, "villagerId");
            Objects.requireNonNull(routeId, "routeId");
        }
    }

    /**
     * Claims an already-open gate. The caller must invoke this only after its own successful
     * closed-to-open transition; an ordinary open gate must never be passed here.
     */
    public static boolean activate(ServerLevel level, BlockPos gate, Owner owner) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        Objects.requireNonNull(owner, "owner");
        long packedGate = gate.asLong();
        if (!level.hasChunkAt(gate)) return false;

        BlockState state = level.getBlockState(gate);
        if (!isOpenFenceGate(state)) {
            invalidate(level, packedGate, "external-change");
            return false;
        }
        GateIdentity identity = GateIdentity.from(state);
        Set<Owner> displaced = Set.of();
        OwnerIndex.Activation result;
        synchronized (INDEX) {
            GateIdentity prior = INDEX.identity(level, packedGate);
            if (prior != null && !prior.equals(identity))
                displaced = INDEX.invalidate(level, packedGate);
            result = INDEX.activate(level, packedGate, identity, owner);
        }
        logReleased(gate, displaced, "external-change", 0);
        if (result == OwnerIndex.Activation.ADDED) {
            LOGGER.info("livestock gate blocker activated gate={} owner={} route={} types={}",
                    gate, owner.villagerId(), owner.routeId(), COVERED_TYPES);
        }
        return result != OwnerIndex.Activation.IDENTITY_CONFLICT;
    }

    /** Joins another active VWR lease without ever claiming an ordinary open gate. */
    public static boolean joinActive(ServerLevel level, BlockPos gate, Owner owner) {
        if (!validate(level, gate)) return false;
        return activate(level, gate, owner);
    }

    /** True when closing now would disrupt a different VWR route using this gate. */
    public static boolean hasOtherOwners(ServerLevel level, BlockPos gate, Owner owner) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        Objects.requireNonNull(owner, "owner");
        synchronized (INDEX) {
            int owners = INDEX.ownerCount(level, gate.asLong());
            return owners > (INDEX.containsOwner(level, gate.asLong(), owner) ? 1 : 0);
        }
    }

    /** Releases only this route's ownership; other owners at the same position remain active. */
    public static boolean release(ServerLevel level, BlockPos gate, Owner owner, String reason) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        Objects.requireNonNull(owner, "owner");
        boolean removed;
        int remaining;
        synchronized (INDEX) {
            removed = INDEX.release(level, gate.asLong(), owner);
            remaining = INDEX.ownerCount(level, gate.asLong());
        }
        if (removed) logReleased(gate, List.of(owner), normalizeReason(reason), remaining);
        return removed;
    }

    /** Invalidates every lease after a proven physical close or external identity change. */
    public static void invalidateGate(ServerLevel level, BlockPos gate, String reason) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        invalidate(level, gate.asLong(), normalizeReason(reason));
    }

    /** Releases every gate lease held by one exact task owner. */
    public static int releaseOwner(ServerLevel level, Owner owner, String reason) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        List<Long> positions;
        synchronized (INDEX) {
            positions = INDEX.positionsOwnedBy(level, owner);
        }
        int released = 0;
        for (long packedGate : positions) {
            if (release(level, BlockPos.of(packedGate), owner, reason)) released++;
        }
        return released;
    }

    /**
     * Revalidates a lease against the live block without loading a chunk. A temporarily
     * unloaded chunk retains its lease; a loaded changed/closed gate invalidates all owners.
     */
    public static boolean validate(ServerLevel level, BlockPos gate) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        GateIdentity expected;
        synchronized (INDEX) {
            expected = INDEX.identity(level, gate.asLong());
        }
        if (expected == null) return false;
        if (!level.hasChunkAt(gate)) return true;
        if (expected.matchesOpen(level.getBlockState(gate))) return true;
        invalidate(level, gate.asLong(), "external-change");
        return false;
    }

    public static boolean isActive(ServerLevel level, BlockPos gate) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(gate, "gate");
        synchronized (INDEX) {
            return INDEX.containsPosition(level, gate.asLong());
        }
    }

    /** Called by the WalkNodeEvaluator mixin for each concrete block in a mob's node volume. */
    public static boolean blocks(Mob mob, PathfindingContext context, int x, int y, int z) {
        if (mob == null || context == null || !(mob.level() instanceof ServerLevel level)) return false;
        long packedGate = BlockPos.asLong(x, y, z);
        GateIdentity expected;
        synchronized (INDEX) {
            expected = INDEX.identity(level, packedGate);
        }
        if (expected == null || !LivestockGateRules.isContainedLivestock(mob.getClass())) return false;

        BlockPos gate = BlockPos.of(packedGate);
        BlockState state = context.getBlockState(gate);
        if (!expected.matchesOpen(state)) {
            invalidate(level, packedGate, "external-change");
            return false;
        }
        return LivestockGateRules.shouldTreatAsFence(mob.getClass(), true, true);
    }

    /** Drops all transient state for one unloading server level. */
    public static void clearLevel(ServerLevel level, String reason) {
        Objects.requireNonNull(level, "level");
        Map<Long, Set<Owner>> removed;
        synchronized (INDEX) {
            removed = INDEX.clearWorld(level);
        }
        String normalized = normalizeReason(reason);
        removed.forEach((packedGate, owners) ->
                logReleased(BlockPos.of(packedGate), owners, normalized, 0));
    }

    private static void invalidate(ServerLevel level, long packedGate, String reason) {
        Set<Owner> removed;
        synchronized (INDEX) {
            removed = INDEX.invalidate(level, packedGate);
        }
        logReleased(BlockPos.of(packedGate), removed, reason, 0);
    }

    private static boolean isOpenFenceGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock
                && state.getValue(FenceGateBlock.OPEN);
    }

    private static void logReleased(BlockPos gate, Collection<Owner> owners,
                                    String reason, int remainingOwners) {
        for (Owner owner : owners) {
            LOGGER.info("livestock gate blocker released gate={} owner={} route={} reason={} remainingOwners={}",
                    gate, owner.villagerId(), owner.routeId(), normalizeReason(reason), remainingOwners);
        }
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "unspecified" : reason;
    }

    private record GateIdentity(Block block, Direction facing) {
        static GateIdentity from(BlockState state) {
            return new GateIdentity(state.getBlock(), state.getValue(FenceGateBlock.FACING));
        }

        boolean matchesOpen(BlockState state) {
            return isOpenFenceGate(state) && state.getBlock() == block
                    && state.getValue(FenceGateBlock.FACING) == facing;
        }
    }

    /** Small generic ownership core kept independent of Minecraft world construction for tests. */
    static final class OwnerIndex<W, P, I, O> {
        enum Activation { ADDED, ALREADY_PRESENT, IDENTITY_CONFLICT }

        private final Map<W, Map<P, Entry<I, O>>> worlds;

        OwnerIndex(Map<W, Map<P, Entry<I, O>>> worlds) {
            this.worlds = Objects.requireNonNull(worlds, "worlds");
        }

        Activation activate(W world, P position, I identity, O owner) {
            Map<P, Entry<I, O>> positions = worlds.computeIfAbsent(world, ignored -> new HashMap<>());
            Entry<I, O> entry = positions.get(position);
            if (entry == null) {
                entry = new Entry<>(identity);
                positions.put(position, entry);
            } else if (!entry.identity.equals(identity)) {
                return Activation.IDENTITY_CONFLICT;
            }
            return entry.owners.add(owner) ? Activation.ADDED : Activation.ALREADY_PRESENT;
        }

        I identity(W world, P position) {
            Entry<I, O> entry = entry(world, position);
            return entry == null ? null : entry.identity;
        }

        boolean containsPosition(W world, P position) {
            return entry(world, position) != null;
        }

        boolean containsOwner(W world, P position, O owner) {
            Entry<I, O> entry = entry(world, position);
            return entry != null && entry.owners.contains(owner);
        }

        int ownerCount(W world, P position) {
            Entry<I, O> entry = entry(world, position);
            return entry == null ? 0 : entry.owners.size();
        }

        boolean release(W world, P position, O owner) {
            Map<P, Entry<I, O>> positions = worlds.get(world);
            if (positions == null) return false;
            Entry<I, O> entry = positions.get(position);
            if (entry == null || !entry.owners.remove(owner)) return false;
            if (entry.owners.isEmpty()) positions.remove(position);
            if (positions.isEmpty()) worlds.remove(world);
            return true;
        }

        Set<O> invalidate(W world, P position) {
            Map<P, Entry<I, O>> positions = worlds.get(world);
            if (positions == null) return Set.of();
            Entry<I, O> removed = positions.remove(position);
            if (positions.isEmpty()) worlds.remove(world);
            return removed == null ? Set.of() : Set.copyOf(removed.owners);
        }

        List<P> positionsOwnedBy(W world, O owner) {
            Map<P, Entry<I, O>> positions = worlds.get(world);
            if (positions == null) return List.of();
            List<P> result = new ArrayList<>();
            positions.forEach((position, entry) -> {
                if (entry.owners.contains(owner)) result.add(position);
            });
            return List.copyOf(result);
        }

        Map<P, Set<O>> clearWorld(W world) {
            Map<P, Entry<I, O>> removed = worlds.remove(world);
            if (removed == null) return Map.of();
            Map<P, Set<O>> snapshot = new LinkedHashMap<>();
            removed.forEach((position, entry) -> snapshot.put(position, Set.copyOf(entry.owners)));
            return Map.copyOf(snapshot);
        }

        private Entry<I, O> entry(W world, P position) {
            Map<P, Entry<I, O>> positions = worlds.get(world);
            return positions == null ? null : positions.get(position);
        }

        private static final class Entry<I, O> {
            final I identity;
            final Set<O> owners = new LinkedHashSet<>();

            Entry(I identity) {
                this.identity = Objects.requireNonNull(identity, "identity");
            }
        }
    }
}
