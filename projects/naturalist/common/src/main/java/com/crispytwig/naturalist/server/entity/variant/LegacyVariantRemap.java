package com.crispytwig.naturalist.server.entity.variant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LegacyVariantRemap {
    private static final Map<String, String> BIRD_VARIANTS = Map.of(
            "naturalist:bluejay", "naturalist:blue_jay",
            "naturalist:cardinal", "naturalist:northern_cardinal",
            "naturalist:robin", "naturalist:american_robin",
            "naturalist:sparrow", "naturalist:white_throated_sparrow",
            "naturalist:canary", "naturalist:american_robin",
            "naturalist:finch", "naturalist:carolina_chickadee"
    );
    private static final Set<String> LEGACY_SNAKES = Set.of("naturalist:coral_snake", "naturalist:rattlesnake");

    private LegacyVariantRemap() {
    }

    public static boolean isLegacyBirdEntityId(String id) {
        return BIRD_VARIANTS.containsKey(id);
    }

    public static boolean isLegacySnakeEntityId(String id) {
        return LEGACY_SNAKES.contains(id);
    }

    public static Optional<Identifier> variantForLegacyEntityId(String id) {
        String birdVariant = BIRD_VARIANTS.get(id);
        String variant = birdVariant != null ? birdVariant : LEGACY_SNAKES.contains(id) ? id : null;
        return Optional.ofNullable(variant).map(Identifier::tryParse);
    }

    public static void apply(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        String id = tag.getString("id").orElse(null);
        if (id == null) {
            return;
        }
        if (!id.startsWith("naturalist:")) {
            return;
        }
        if (isLegacyBirdEntityId(id)) {
            tag.putString("id", "naturalist:bird");
        } else if (isLegacySnakeEntityId(id)) {
            tag.putString("id", "naturalist:snake");
        } else {
            return;
        }
        variantForLegacyEntityId(id).ifPresent(variant -> tag.putString(DataDrivenVariantAnimal.VARIANT_TAG, variant.toString()));
    }
}
