package com.crispytwig.naturalist.server.entity.persistence;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * Shared 26.2 persistence adapters for Naturalist-owned entity state.
 *
 * <p>This class intentionally contains serialization mechanics only. It does
 * not select variants, alter inventory contents, or apply gameplay policy.</p>
 */
public final class NaturalistEntityPersistence {
    public static final String VARIANT_TAG = "Variant";
    private static final String NATURALIST_NAMESPACE = "naturalist";

    private NaturalistEntityPersistence() {
    }

    public static void saveVariant(ValueOutput output, Identifier variant) {
        output.putString(VARIANT_TAG, variant.toString());
    }

    public static Optional<Identifier> readVariant(ValueInput input, String[] legacyNames) {
        Optional<String> serialized = input.getString(VARIANT_TAG);
        if (serialized.isPresent()) {
            return Optional.ofNullable(Identifier.tryParse(serialized.get()));
        }
        if (legacyNames == null || legacyNames.length == 0) {
            return Optional.empty();
        }
        return input.getInt(VARIANT_TAG).map(index -> Identifier.fromNamespaceAndPath(
                NATURALIST_NAMESPACE,
                legacyNames[Math.floorMod(index, legacyNames.length)]));
    }

    public static <T extends UniquelyIdentifyable> void saveReference(
            ValueOutput output, String key, EntityReference<T> reference) {
        EntityReference.store(reference, output, key);
    }

    public static <T extends UniquelyIdentifyable> EntityReference<T> readReference(ValueInput input, String key) {
        return EntityReference.read(input, key);
    }

    /**
     * Completes the current neutral-mob load path for Naturalist's old
     * {@code AngryAt} UUID key, including the target resolution performed by
     * the normal 26.2 {@code angry_at} reader.
     */
    public static void loadLegacyAngerTarget(ValueInput input, NeutralMob mob) {
        if (mob.getPersistentAngerTarget() != null) {
            return;
        }
        EntityReference<LivingEntity> reference = readReference(input, "AngryAt");
        if (reference == null) {
            return;
        }
        mob.setPersistentAngerTarget(reference);
        if (mob.level() instanceof ServerLevel serverLevel) {
            mob.setTarget(EntityReference.getLivingEntity(reference, serverLevel));
        }
    }

    /** Preserves fixed slot indices using vanilla's current {@code Items} list format. */
    public static void saveFixedInventory(ValueOutput output, SimpleContainer inventory) {
        ContainerHelper.saveAllItems(output, inventory.getItems());
    }

    public static void loadFixedInventory(ValueInput input, SimpleContainer inventory) {
        inventory.clearContent();
        ContainerHelper.loadAllItems(input, inventory.getItems());
        inventory.setChanged();
    }

    /** Preserves Naturalist's compact, insertion-ordered carried-item list semantics. */
    public static void savePackedInventory(ValueOutput output, String key, SimpleContainer inventory) {
        inventory.storeAsItemList(output.list(key, ItemStack.CODEC));
    }

    public static void loadPackedInventory(ValueInput input, String key, SimpleContainer inventory) {
        input.list(key, ItemStack.CODEC).ifPresent(inventory::fromItemList);
    }
}
