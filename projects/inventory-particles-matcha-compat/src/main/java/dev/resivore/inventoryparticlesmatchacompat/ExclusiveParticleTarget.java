package dev.resivore.inventoryparticlesmatchacompat;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** The six exact stack identities owned by this compatibility companion. */
enum ExclusiveParticleTarget {
    GREEN_CURRY("mynx_ipmc_green_curry"),
    RAMEN("mynx_ipmc_ramen"),
    CRYSTAL_HEART("mynx_ipmc_crystal_heart"),
    GLOWCAP("mynx_ipmc_glowcap"),
    TOADSTOOL_HEART("mynx_ipmc_toadstool_heart"),
    RIBBIT_VILLAGE_EXPLORER_MAP("mynx_ipmc_ribbit_village_map");

    static final Identifier GREEN_CURRY_MODEL = Identifier.withDefaultNamespace("green_curry");
    static final Identifier RAMEN_MODEL = Identifier.withDefaultNamespace("ramen");
    static final Identifier CRYSTAL_HEART_MODEL = Identifier.withDefaultNamespace("heart_container");
    static final Identifier GLOWCAP_ITEM = Identifier.parse("ribbits:glowcap");
    static final Identifier TOADSTOOL_HEART_ITEM = Identifier.parse("ribbits:toadstool_heart");
    static final String RIBBIT_VILLAGE_MARKER = "ribbits:ribbit_village_explorer_map";

    private final String compatHolderName;

    ExclusiveParticleTarget(String compatHolderName) {
        this.compatHolderName = compatHolderName;
    }

    String compatHolderName() {
        return compatHolderName;
    }

    static Optional<ExclusiveParticleTarget> find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        boolean hasMapId = stack.has(DataComponents.MAP_ID);
        boolean explorerMarker = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr(RIBBIT_VILLAGE_MARKER, false);
        return find(new StackIdentity(itemId, model, hasMapId, explorerMarker));
    }

    /** Package-visible for focused tests and for keeping the registry extraction separate. */
    static Optional<ExclusiveParticleTarget> find(StackIdentity stack) {
        if (stack.itemId().equals(Identifier.withDefaultNamespace("poisonous_potato"))) {
            if (GREEN_CURRY_MODEL.equals(stack.itemModel())) {
                return Optional.of(GREEN_CURRY);
            }
            if (RAMEN_MODEL.equals(stack.itemModel())) {
                return Optional.of(RAMEN);
            }
            if (CRYSTAL_HEART_MODEL.equals(stack.itemModel())) {
                return Optional.of(CRYSTAL_HEART);
            }
            return Optional.empty();
        }
        if (GLOWCAP_ITEM.equals(stack.itemId())) {
            return Optional.of(GLOWCAP);
        }
        if (TOADSTOOL_HEART_ITEM.equals(stack.itemId())) {
            return Optional.of(TOADSTOOL_HEART);
        }
        if (stack.itemId().equals(Identifier.withDefaultNamespace("filled_map"))
                && stack.hasMapId() && stack.explorerMarker()) {
            return Optional.of(RIBBIT_VILLAGE_EXPLORER_MAP);
        }
        return Optional.empty();
    }

    record StackIdentity(Identifier itemId, Identifier itemModel, boolean hasMapId, boolean explorerMarker) {
    }
}
