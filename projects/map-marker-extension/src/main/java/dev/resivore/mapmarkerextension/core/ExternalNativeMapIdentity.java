package dev.resivore.mapmarkerextension.core;

import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * A map identity whose native decoration holder and artwork are owned by another mod.
 *
 * <p>MME recognizes the stack only through a stable boolean custom-data marker. It never
 * registers, normalizes, or supplies the external decoration or its assets.</p>
 */
public record ExternalNativeMapIdentity(
    String id,
    String stackMarkerKey,
    String decorationTypeId
) {
    public ExternalNativeMapIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stackMarkerKey, "stackMarkerKey");
        Objects.requireNonNull(decorationTypeId, "decorationTypeId");
    }

    public boolean matches(ItemStack stack) {
        return stack.is(Items.FILLED_MAP)
            && stack.has(DataComponents.MAP_ID)
            && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBooleanOr(stackMarkerKey, false);
    }

    public boolean admitsDecorationType(String candidateTypeId) {
        return decorationTypeId.equals(candidateTypeId);
    }
}
