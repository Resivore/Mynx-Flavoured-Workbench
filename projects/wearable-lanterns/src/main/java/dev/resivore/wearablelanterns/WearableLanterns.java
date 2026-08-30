package dev.resivore.wearablelanterns;

import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class WearableLanterns implements ModInitializer {
    public static final String MOD_ID = "wearable_lanterns";
    public static final String LANTERN_SLOT = "legs/lantern";

    public static final TagKey<Item> LANTERN_SLOT_ITEMS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("trinkets", LANTERN_SLOT));

    public static final Identifier LANTERN_ONLY_PREDICATE =
            Identifier.fromNamespaceAndPath(MOD_ID, "lantern_only");

    @Override
    public void onInitialize() {
        TrinketsApi.registerTrinketPredicate(
                LANTERN_ONLY_PREDICATE,
                (stack, slot, entity) ->
                        LANTERN_SLOT.equals(slot.slotType().getId())
                                && stack.is(LANTERN_SLOT_ITEMS));
    }
}
