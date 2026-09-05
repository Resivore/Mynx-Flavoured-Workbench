package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import java.util.*;
import java.util.function.Supplier;

/** Expands only containers already admitted by QSN's native scan and access checks. */
public final class NestedShulkerDiscovery {
    private static final ThreadLocal<IdentityHashMap<Container, Discovered>> ACTIVE = new ThreadLocal<>();
    private NestedShulkerDiscovery() {}
    private record Discovered(Set<QuickStackMoveEngine.StackKey> outer, List<QuickStackMoveEngine.Target> children) {}
    public static <T> T scoped(Supplier<T> action) {
        var previous = ACTIVE.get(); ACTIVE.set(new IdentityHashMap<>());
        try { return action.get(); } finally { if(previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static Set<QuickStackMoveEngine.StackKey> discover(Container parent, Set<QuickStackMoveEngine.StackKey> outer,
            ServerLevel level, ServerPlayer player, List<BlockPos> positions) {
        var active = ACTIVE.get();
        if(active == null || !safeParent(parent)) return outer;
        var anchors = new ArrayList<>(positions.stream().map(BlockPos::immutable).toList());
        Container destination = parent;
        // A connected barrel exposes both inventories through the provider's public wrapper.
        // Its dirty callback persists both physical owners; never dirty only the scanned half.
        BlockPos partnerPosition = null;
        if (parent instanceof BarrelBlockEntity && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("doublebarrels")) {
            try {
                var api = Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess");
                if ((boolean) api.getMethod("isConnected").invoke(parent)) {
                    partnerPosition = (BlockPos) api.getMethod("getConnectionPos").invoke(parent);
                    if (partnerPosition == null || !level.isLoaded(partnerPosition)) return outer;
                    destination = (Container) api.getMethod("getCombinedInventory").invoke(parent);
                    if (destination == null) return outer;
                    if (!anchors.contains(partnerPosition)) anchors.add(partnerPosition.immutable());
                }
            } catch (ReflectiveOperationException | ClassCastException failure) {
                throw new IllegalStateException("Loaded Double Barrels public inventory contract unavailable", failure);
            }
        }
        final Container owner = destination;
        final BlockPos expectedPartner = partnerPosition;
        var blocks = anchors.stream().map(level::getBlockEntity).toList();
        java.util.function.BooleanSupplier valid = () -> {
            if (expectedPartner != null) {
                try {
                    var api = Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess");
                    if (!expectedPartner.equals(api.getMethod("getConnectionPos").invoke(parent))) return false;
                } catch (ReflectiveOperationException failure) { return false; }
            }
            if (!player.isAlive() || player.isSpectator() || !parent.stillValid(player)) return false;
            for (int i = 0; i < anchors.size(); i++) {
                var pos = anchors.get(i); var block = blocks.get(i);
                if (!level.isLoaded(pos) || level.getBlockEntity(pos) != block || block == null || block.isRemoved()
                        || !player.mayInteract(level, pos) || block instanceof BaseContainerBlockEntity storage && !storage.canOpen(player)) return false;
            }
            return true;
        };
        if (!valid.getAsBoolean()) return outer;
        var sources = CsrQuickStackIntegration.activeSourceStacks();
        var children = new ArrayList<QuickStackMoveEngine.Target>();
        var union = new LinkedHashSet<>(outer);
        for(int slot=0;slot<owner.getContainerSize();slot++) {
            ItemStack host = owner.getItem(slot);
            if(!NestedShulkerTarget.supported(host) || !owner.canPlaceItem(slot,host)) continue;
            var child = new NestedShulkerTarget(owner,slot,valid);
            var keys = new LinkedHashSet<QuickStackMoveEngine.StackKey>();
            for(ItemStack incoming:sources) if(child.accepts(incoming)) keys.add(QuickStackMoveEngine.StackKey.of(incoming));
            if(!keys.isEmpty()) { children.add(new QuickStackMoveEngine.Target(child,Set.copyOf(keys))); union.addAll(keys); }
        }
        if(children.isEmpty()) return outer;
        active.put(parent,new Discovered(outer,List.copyOf(children)));
        // Only keeps the native ScannedContainer alive through its empty-set prefilter.
        // expand() removes these child-only keys from the real outer destination.
        return Collections.unmodifiableSet(union);
    }
    private static boolean safeParent(Container parent) {
        return parent instanceof ChestBlockEntity || parent instanceof BarrelBlockEntity
                || parent instanceof DispenserBlockEntity || parent instanceof HopperBlockEntity
                || parent instanceof CompoundContainer;
    }
    public static List<QuickStackMoveEngine.Target> expand(List<QuickStackMoveEngine.Target> targets) {
        var active = ACTIVE.get(); if(active == null || active.isEmpty()) return targets;
        var result = new ArrayList<QuickStackMoveEngine.Target>();
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for(var target:targets) {
            var discovered=active.get(target.container());
            if(discovered==null) {result.add(target);continue;}
            if(!discovered.outer().isEmpty()) result.add(new QuickStackMoveEngine.Target(target.container(),discovered.outer()));
            for(var child:discovered.children()) if(seen.add(((NestedShulkerTarget)child.container()).hostIdentity())) result.add(child);
        }
        return List.copyOf(result);
    }
}
