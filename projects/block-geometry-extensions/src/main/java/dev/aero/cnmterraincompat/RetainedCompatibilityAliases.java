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

    private static List<Identifier> bbbBeamSelectorAliases() {
        List<Identifier> result = new ArrayList<>();
        for (String material : List.of("oak", "spruce", "birch", "jungle", "acacia",
                "dark_oak", "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_slab"));
            result.add(Identifier.fromNamespaceAndPath("bbb", material + "_beam_stairs"));
        }
        return List.copyOf(result);
    }
}
