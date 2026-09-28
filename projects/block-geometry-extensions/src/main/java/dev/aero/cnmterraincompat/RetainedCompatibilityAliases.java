package dev.aero.cnmterraincompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Registered compatibility identities which must not become independent creative/search entries.
 *
 * <p>These are deliberately not ShapeMap children. Their provider registry identities remain
 * valid for existing worlds and references, while the canonical BGE roles retain the material
 * state that the provider aliases cannot represent.</p>
 */
public final class RetainedCompatibilityAliases {
    private static final List<Identifier> BBB_BEAM_SELECTOR_ALIASES = bbbBeamSelectorAliases();
    private static final List<Identifier> BBB_ENDERSCAPE_PRESENTATION_ITEMS =
            bbbEnderscapePresentationItems();

    private RetainedCompatibilityAliases() {}

    /** BBB aliases omitted from canonical ShapeMap selector membership. */
    static List<Identifier> suppressedSelectorAliases() {
        return BBB_BEAM_SELECTOR_ALIASES;
    }

    /** True only for a live BBB Beam compatibility item with its retained registry identity. */
    public static boolean isRetainedButHiddenCompatibilityAlias(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return BBB_BEAM_SELECTOR_ALIASES.contains(id) && BuiltInRegistries.ITEM.getValue(id) == item;
    }

    /**
     * True only for an exact live compatibility item that must not have an independent
     * Creative/search/JEI presentation entry.
     *
     * <p>The Enderscape Beam roots and Walls deliberately live here rather than in
     * {@link #suppressedSelectorAliases()}: they remain members of their existing BGE Planks
     * selectors, while BBB's redundant provider items no longer appear independently.</p>
     */
    public static boolean isPresentationHiddenCompatibilityItem(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return (BBB_BEAM_SELECTOR_ALIASES.contains(id)
                || BBB_ENDERSCAPE_PRESENTATION_ITEMS.contains(id))
                && BuiltInRegistries.ITEM.getValue(id) == item;
    }

    private static List<Identifier> bbbBeamSelectorAliases() {
        List<Identifier> result = new ArrayList<>();
        for (String material : List.of("oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_slab"));
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_stairs"));
        }
        return List.copyOf(result);
    }

    private static List<Identifier> bbbEnderscapePresentationItems() {
        List<Identifier> result = new ArrayList<>();
        for (String family : List.of("veiled", "celestial", "murublight")) {
            result.add(Identifier.fromNamespaceAndPath("bbb", family + "_beam"));
            result.add(Identifier.fromNamespaceAndPath("bbb", family + "_wall"));
        }
        return List.copyOf(result);
    }
}
