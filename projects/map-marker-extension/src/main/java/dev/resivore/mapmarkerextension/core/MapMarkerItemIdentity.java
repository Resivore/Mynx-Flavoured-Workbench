package dev.resivore.mapmarkerextension.core;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MapMarkerItemIdentity {
    private MapMarkerItemIdentity() {
    }

    public static Optional<MapMarkerIdentity> find(ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) {
            return Optional.empty();
        }

        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (itemName != null && itemName.getContents() instanceof TranslatableContents translatable) {
            return MapMarkerIdentity.fromItemNameTranslationKey(translatable.getKey());
        }
        return Optional.empty();
    }
}
