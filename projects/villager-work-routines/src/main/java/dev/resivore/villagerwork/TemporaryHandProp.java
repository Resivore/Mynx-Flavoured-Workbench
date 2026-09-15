package dev.resivore.villagerwork;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;
import java.util.UUID;

/** A reversible, semantically owned visual hand-item overlay. */
final class TemporaryHandProp {
    static final float SYNTHETIC_DROP_CHANCE = 0.0F;

    private static final String MARKER_KEY = "villager_work_routines:temporary_hand_prop";
    private static final String VWR_MARKER = "vwr_owned";
    private static final String OWNER_KEY = "owner";
    private static final String ACTION_KEY = "action";
    private static final String ITEM_KEY = "item";

    private final UUID ownerId;
    private final UUID actionId;
    private final Item intendedItem;
    private final String intendedItemId;
    private final HandState originalState;
    private HandState restorationState;
    private int rebaseCount;

    private TemporaryHandProp(UUID ownerId, UUID actionId, Item intendedItem,
                              ItemStack originalStack, float originalDropChance) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.actionId = Objects.requireNonNull(actionId, "actionId");
        this.intendedItem = Objects.requireNonNull(intendedItem, "intendedItem");
        this.intendedItemId = BuiltInRegistries.ITEM.getKey(intendedItem).toString();
        this.originalState = legitimateState(originalStack, originalDropChance);
        this.restorationState = this.originalState;
    }

    static TemporaryHandProp begin(UUID ownerId, UUID actionId, Item intendedItem,
                                   ItemStack originalStack, float originalDropChance) {
        return new TemporaryHandProp(ownerId, actionId, intendedItem, originalStack, originalDropChance);
    }

    UUID ownerId() { return ownerId; }

    UUID actionId() { return actionId; }

    Item intendedItem() { return intendedItem; }

    int rebaseCount() { return rebaseCount; }

    HandState originalState() { return originalState.copy(); }

    HandState restorationState() { return restorationState.copy(); }

    Overlay overlay() {
        ItemStack presentation = new ItemStack(intendedItem);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(VWR_MARKER, true);
        marker.putString(OWNER_KEY, ownerId.toString());
        marker.putString(ACTION_KEY, actionId.toString());
        marker.putString(ITEM_KEY, intendedItemId);
        CompoundTag customData = new CompoundTag();
        customData.put(MARKER_KEY, marker);
        presentation.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return new Overlay(presentation, SYNTHETIC_DROP_CHANCE);
    }

    /** True for copies or reconstructed stacks carrying this exact owner/action/item marker. */
    boolean owns(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(intendedItem)) return false;
        CompoundTag marker = marker(stack);
        return marker != null
                && marker.getBooleanOr(VWR_MARKER, false)
                && ownerId.toString().equals(marker.getStringOr(OWNER_KEY, ""))
                && actionId.toString().equals(marker.getStringOr(ACTION_KEY, ""))
                && intendedItemId.equals(marker.getStringOr(ITEM_KEY, ""));
    }

    /**
     * Keeps the overlay visible. A legitimate replacement or clear becomes the new restoration
     * state before a fresh marked presentation is returned.
     */
    Reconcile reconcile(ItemStack currentStack, float currentDropChance) {
        Objects.requireNonNull(currentStack, "currentStack");
        if (owns(currentStack)) {
            if (currentDropChance == SYNTHETIC_DROP_CHANCE)
                return new Reconcile(ReconcileKind.PRESENTATION_INTACT, false, overlay(), restorationState());
            return new Reconcile(ReconcileKind.SYNTHETIC_DROP_CHANCE_CORRECTED,
                    true, overlay(), restorationState());
        }
        if (hasVwrMarker(currentStack)) {
            // Never adopt another/stale synthetic presentation as legitimate restoration state;
            // overwrite it with this live transaction so it cannot survive or become droppable.
            return new Reconcile(ReconcileKind.FOREIGN_VWR_PRESENTATION,
                    true, overlay(), restorationState());
        }
        restorationState = new HandState(currentStack, currentDropChance);
        rebaseCount++;
        ReconcileKind kind = currentStack.isEmpty()
                ? ReconcileKind.EXTERNAL_CLEAR_REBASED
                : ReconcileKind.EXTERNAL_REPLACEMENT_REBASED;
        return new Reconcile(kind, true, overlay(), restorationState());
    }

    /**
     * Restores only over this transaction's marked presentation. An unobserved external hand
     * change and its drop chance are left untouched.
     */
    Restore restore(ItemStack currentStack, float currentDropChance) {
        Objects.requireNonNull(currentStack, "currentStack");
        if (hasVwrMarker(currentStack)) {
            RestoreKind kind = owns(currentStack)
                    ? (rebaseCount == 0 ? RestoreKind.ORIGINAL_RESTORED : RestoreKind.REBASED_STATE_RESTORED)
                    : RestoreKind.FOREIGN_VWR_PRESENTATION_QUARANTINED;
            return new Restore(kind, true, restorationState());
        }
        return new Restore(RestoreKind.EXTERNAL_STATE_LEFT_UNTOUCHED,
                false, new HandState(currentStack, currentDropChance));
    }

    private static HandState legitimateState(ItemStack stack, float dropChance) {
        Objects.requireNonNull(stack, "stack");
        // A stale VWR presentation is synthetic, never a legitimate item to resurrect later.
        return hasVwrMarker(stack)
                ? new HandState(ItemStack.EMPTY, dropChance)
                : new HandState(stack, dropChance);
    }

    private static boolean hasVwrMarker(ItemStack stack) {
        CompoundTag marker = marker(stack);
        return marker != null && marker.getBooleanOr(VWR_MARKER, false);
    }

    private static CompoundTag marker(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return customData.get(MARKER_KEY) instanceof CompoundTag marker ? marker : null;
    }

    enum ReconcileKind {
        PRESENTATION_INTACT,
        SYNTHETIC_DROP_CHANCE_CORRECTED,
        EXTERNAL_REPLACEMENT_REBASED,
        EXTERNAL_CLEAR_REBASED,
        FOREIGN_VWR_PRESENTATION
    }

    enum RestoreKind {
        ORIGINAL_RESTORED,
        REBASED_STATE_RESTORED,
        FOREIGN_VWR_PRESENTATION_QUARANTINED,
        EXTERNAL_STATE_LEFT_UNTOUCHED
    }

    record HandState(ItemStack stack, float dropChance) {
        HandState {
            Objects.requireNonNull(stack, "stack");
            stack = stack.copy();
        }

        @Override public ItemStack stack() { return stack.copy(); }

        HandState copy() { return new HandState(stack, dropChance); }
    }

    record Overlay(ItemStack stack, float dropChance) {
        Overlay {
            Objects.requireNonNull(stack, "stack");
            stack = stack.copy();
        }

        @Override public ItemStack stack() { return stack.copy(); }
    }

    record Reconcile(ReconcileKind kind, boolean applyOverlay,
                     Overlay overlay, HandState restorationState) {}

    record Restore(RestoreKind kind, boolean applyRestoration, HandState handState) {}
}
