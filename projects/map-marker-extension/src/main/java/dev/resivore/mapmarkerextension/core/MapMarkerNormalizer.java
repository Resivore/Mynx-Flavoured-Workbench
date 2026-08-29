package dev.resivore.mapmarkerextension.core;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public final class MapMarkerNormalizer {
    public static final String EXPLORATION_TARGET_KEY = "+";

    private MapMarkerNormalizer() {
    }

    /**
     * Replaces only a known exploration map's canonical target entry. The
     * returned entry is present only when the stack component changed, so the
     * caller can immediately refresh MapItemSavedData's runtime decoration.
     */
    public static Optional<MapDecorations.Entry> normalize(ItemStack stack) {
        return normalize(stack, MapMarkerDecorationTypes::holder);
    }

    static Optional<MapDecorations.Entry> normalize(
        ItemStack stack,
        Function<MapMarkerIdentity, Holder<MapDecorationType>> decorationTypeLookup
    ) {
        MapMarkerIdentity identity = MapMarkerItemIdentity.find(stack).orElse(null);
        if (identity == null || !identity.customDecoration()) {
            return Optional.empty();
        }

        MapDecorations decorations = stack.getOrDefault(
            DataComponents.MAP_DECORATIONS,
            MapDecorations.EMPTY
        );
        MapDecorations.Entry current = decorations.decorations().get(EXPLORATION_TARGET_KEY);
        Holder<MapDecorationType> desired = decorationTypeLookup.apply(identity);
        MapDecorations.Entry replacement = normalizeEntry(current, identity, desired);
        if (replacement == current) {
            return Optional.empty();
        }

        stack.set(
            DataComponents.MAP_DECORATIONS,
            decorations.withDecoration(EXPLORATION_TARGET_KEY, replacement)
        );
        return Optional.of(replacement);
    }

    public static MapDecorations.Entry normalizeEntry(
        MapDecorations.Entry current,
        MapMarkerIdentity identity,
        Holder<MapDecorationType> desired
    ) {
        if (current == null || !identity.customDecoration()) {
            return current;
        }
        if (current.type().equals(desired)) {
            return current;
        }
        String currentTypeId = current.type().unwrapKey()
            .map(key -> key.identifier().toString())
            .orElse("");
        if (identity.decorationTypeId().equals(currentTypeId)) {
            return current;
        }
        if (!identity.sourceDecorationTypeId().equals(currentTypeId)) {
            return current;
        }
        return new MapDecorations.Entry(desired, current.x(), current.z(), current.rotation());
    }
}
