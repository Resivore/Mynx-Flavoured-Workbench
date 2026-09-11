package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One-level, server-side sources for carried vanilla shulkers and bundles.  This is deliberately
 * a source adapter: QSN still owns target order, acceptance, insertion, and source accounting.
 */
public final class CarriedContainerSources {
    private static final ThreadLocal<List<CarrierSource>> ACTIVE = new ThreadLocal<>();

    private CarriedContainerSources() {}

    public static <T> T scoped(net.minecraft.server.level.ServerPlayer player,
            QuickStackMoveEngine.SourceRules rules, Supplier<T> action) {
        List<CarrierSource> previous = ACTIVE.get();
        ACTIVE.set(snapshot(player, rules));
        try { return action.get(); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }

    public static List<ItemStack> discoveryStacks() {
        List<CarrierSource> sources = ACTIVE.get();
        if (sources == null || sources.isEmpty()) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (CarrierSource source : sources) result.addAll(source.contents());
        return result;
    }

    public static QuickStackMoveEngine.Result drain(List<QuickStackMoveEngine.Target> targets) {
        List<CarrierSource> sources = ACTIVE.get();
        if (sources == null || sources.isEmpty() || targets == null || targets.isEmpty()) {
            return QuickStackMoveEngine.Result.empty();
        }
        int moved = 0, touched = 0, targetTouches = 0;
        for (CarrierSource source : sources) {
            QuickStackMoveEngine.Result result = QuickStackMoveEngine.moveMatchingItems(
                    source.view(), 0, source.view().getContainerSize(), targets, QuickStackMoveEngine.SourceRules.EMPTY);
            if (result.itemsMoved() > 0) {
                source.commit();
                moved += result.itemsMoved();
                touched++;
                targetTouches += result.targetContainersTouched();
            }
        }
        return new QuickStackMoveEngine.Result(moved, touched, targetTouches);
    }

    private static List<CarrierSource> snapshot(net.minecraft.server.level.ServerPlayer player,
            QuickStackMoveEngine.SourceRules originalRules) {
        Inventory inventory = player.getInventory();
        QuickStackMoveEngine.SourceRules rules = originalRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY : originalRules;
        List<CarrierSource> sources = new ArrayList<>();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            ItemStack carrier = inventory.getItem(slot);
            // QSN only has meaningful source rules for ordinary storage; a locked/fully-kept
            // outer stack is conservative exclusion for its direct contents too.
            if (slot >= Inventory.SELECTION_SIZE && (rules.isLocked(slot)
                    || rules.movableCount(slot, carrier.getCount()) <= 0)) continue;
            addIfEligible(sources, carrier);
        }
        addIfEligible(sources, player.getOffhandItem());
        return List.copyOf(sources);
    }

    private static void addIfEligible(List<CarrierSource> sources, ItemStack carrier) {
        if (carrier.isEmpty() || !RoutingLockResolver.mayDrain(carrier)) return;
        CarrierSource source = isShulker(carrier) ? CarrierSource.shulker(carrier)
                : carrier.getItem() instanceof BundleItem ? CarrierSource.bundle(carrier) : null;
        if (source != null && !source.view().isEmpty()) sources.add(source);
    }

    private static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
    }

    private enum Kind { SHULKER, BUNDLE }

    private record CarrierSource(ItemStack carrier, Kind kind, SimpleContainer view) {
        static CarrierSource shulker(ItemStack carrier) {
            NonNullList<ItemStack> physical = NonNullList.withSize(27, ItemStack.EMPTY);
            carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(physical);
            SimpleContainer view = new SimpleContainer(27);
            for (int i = 0; i < 27; i++) view.setItem(i, physical.get(i).copy());
            return new CarrierSource(carrier, Kind.SHULKER, view);
        }
        static CarrierSource bundle(ItemStack carrier) {
            List<ItemStack> physical = carrier.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
                    .itemCopyStream().toList();
            SimpleContainer view = new SimpleContainer(physical.size());
            for (int i = 0; i < physical.size(); i++) view.setItem(i, physical.get(i).copy());
            return new CarrierSource(carrier, Kind.BUNDLE, view);
        }
        List<ItemStack> contents() {
            List<ItemStack> result = new ArrayList<>();
            for (int slot = 0; slot < view.getContainerSize(); slot++) {
                ItemStack stack = view.getItem(slot);
                if (!stack.isEmpty()) result.add(stack);
            }
            return result;
        }
        void commit() {
            if (kind == Kind.SHULKER) {
                List<ItemStack> physical = new ArrayList<>(27);
                for (int i = 0; i < 27; i++) physical.add(view.getItem(i));
                carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(physical));
            } else {
                List<ItemStackTemplate> physical = new ArrayList<>(view.getContainerSize());
                for (int i = 0; i < view.getContainerSize(); i++) {
                    ItemStack stack = view.getItem(i);
                    if (!stack.isEmpty()) physical.add(ItemStackTemplate.fromNonEmptyStack(stack));
                }
                carrier.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(physical));
            }
        }
    }
}
