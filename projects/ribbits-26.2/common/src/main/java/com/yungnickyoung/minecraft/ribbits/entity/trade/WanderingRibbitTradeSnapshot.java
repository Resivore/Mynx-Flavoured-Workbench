package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Persisted seed and exact provider/schema-to-offer ranges for one materialized menu. */
public record WanderingRibbitTradeSnapshot(long seed, List<ProviderRange> providers) {
    public static final Codec<WanderingRibbitTradeSnapshot> CODEC =
            RecordCodecBuilder.<WanderingRibbitTradeSnapshot>create(instance -> instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(WanderingRibbitTradeSnapshot::seed),
                    ProviderRange.CODEC.listOf().fieldOf("providers")
                            .forGetter(WanderingRibbitTradeSnapshot::providers)
            ).apply(instance, WanderingRibbitTradeSnapshot::new)).validate(WanderingRibbitTradeSnapshot::validate);

    public WanderingRibbitTradeSnapshot {
        providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    }

    public int totalOfferCount() {
        if (providers.isEmpty()) {
            return 0;
        }
        ProviderRange last = providers.getLast();
        return last.firstOffer() + last.offerCount();
    }

    private static DataResult<WanderingRibbitTradeSnapshot> validate(WanderingRibbitTradeSnapshot snapshot) {
        int expectedFirst = 0;
        Identifier previous = null;
        Set<Identifier> seen = new HashSet<>();
        for (ProviderRange provider : snapshot.providers) {
            if (!seen.add(provider.id())) {
                return DataResult.error(() -> "Duplicate Wandering Ribbit trade provider " + provider.id());
            }
            if (previous != null && previous.compareTo(provider.id()) >= 0) {
                return DataResult.error(() -> "Wandering Ribbit providers are not in stable ID order");
            }
            if (provider.firstOffer() != expectedFirst) {
                return DataResult.error(() -> "Wandering Ribbit provider offer ranges are not contiguous");
            }
            expectedFirst += provider.offerCount();
            previous = provider.id();
        }
        return DataResult.success(snapshot);
    }

    public record ProviderRange(Identifier id, int schemaVersion, int firstOffer, int offerCount) {
        public static final Codec<ProviderRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("id").forGetter(ProviderRange::id),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("schema_version")
                        .forGetter(ProviderRange::schemaVersion),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("first_offer")
                        .forGetter(ProviderRange::firstOffer),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("offer_count")
                        .forGetter(ProviderRange::offerCount)
        ).apply(instance, ProviderRange::new));

        public ProviderRange {
            Objects.requireNonNull(id, "id");
            if (schemaVersion < 1 || firstOffer < 0 || offerCount < 0) {
                throw new IllegalArgumentException("Invalid Wandering Ribbit provider range");
            }
        }
    }
}
