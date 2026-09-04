package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stable deterministic registry for native and later optional Wandering Ribbit trade providers. */
public final class WanderingRibbitTradeProviders {
    public static final Identifier NATIVE_PROVIDER_ID = RibbitsCommon.id("native");

    private static final Map<Identifier, WanderingRibbitTradeProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        registerInternal(new WanderingRibbitNativeTradeProvider());
    }

    private WanderingRibbitTradeProviders() {
    }

    /** Forces native provider bootstrap without freezing later optional registrations. */
    public static void bootstrap() {
        // Class initialization is the bootstrap operation.
    }

    /** Registers an optional provider. Duplicate IDs and attempts to replace the native provider fail closed. */
    public static synchronized void register(WanderingRibbitTradeProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (NATIVE_PROVIDER_ID.equals(provider.id())) {
            throw new IllegalArgumentException("The native Wandering Ribbit provider cannot be replaced");
        }
        registerInternal(provider);
    }

    public static synchronized List<WanderingRibbitTradeProvider> providers() {
        return PROVIDERS.values().stream()
                .sorted(Comparator.comparing(WanderingRibbitTradeProvider::id))
                .toList();
    }

    public static Materialization materialize(ServerLevel level, BlockPos origin, long entitySeed) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(origin, "origin");

        MerchantOffers offers = new MerchantOffers();
        List<WanderingRibbitTradeSnapshot.ProviderRange> ranges = new ArrayList<>();
        for (WanderingRibbitTradeProvider provider : providers()) {
            int firstOffer = offers.size();
            long providerSeed = deriveProviderSeed(entitySeed, provider.id());
            WanderingRibbitTradeContext context = new WanderingRibbitTradeContext(
                    level, origin, entitySeed, providerSeed, RandomSource.create(providerSeed));
            List<MerchantOffer> contributions = new ArrayList<>();
            try {
                provider.contributeOffers(context, offer ->
                        contributions.add(Objects.requireNonNull(offer, "provider offer")));
                offers.addAll(contributions);
            } catch (RuntimeException exception) {
                if (NATIVE_PROVIDER_ID.equals(provider.id())) {
                    throw exception;
                }
                RibbitsCommon.LOGGER.error(
                        "Optional Wandering Ribbit trade provider {} failed; retaining a persisted zero-offer range",
                        provider.id(), exception);
            }
            ranges.add(new WanderingRibbitTradeSnapshot.ProviderRange(
                    provider.id(), provider.schemaVersion(), firstOffer, offers.size() - firstOffer));
        }

        WanderingRibbitTradeSnapshot.ProviderRange nativeRange = ranges.stream()
                .filter(range -> NATIVE_PROVIDER_ID.equals(range.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Native Wandering Ribbit provider is missing"));
        if (nativeRange.offerCount() != WanderingRibbitNativeTradeProvider.NATIVE_OFFER_COUNT) {
            throw new IllegalStateException(
                    "Native Wandering Ribbit provider must materialize exactly "
                            + WanderingRibbitNativeTradeProvider.NATIVE_OFFER_COUNT + " offers");
        }

        return new Materialization(offers, new WanderingRibbitTradeSnapshot(entitySeed, ranges));
    }

    /** Stable provider-isolated seed derivation; adding a provider cannot perturb another provider's choices. */
    public static long deriveProviderSeed(long entitySeed, Identifier providerId) {
        Objects.requireNonNull(providerId, "providerId");
        long mixed = entitySeed ^ 0x9E3779B97F4A7C15L;
        String text = providerId.toString();
        for (int i = 0; i < text.length(); i++) {
            mixed ^= text.charAt(i);
            mixed *= 0x100000001B3L;
            mixed ^= mixed >>> 32;
        }
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        return mixed ^ mixed >>> 31;
    }

    private static void registerInternal(WanderingRibbitTradeProvider provider) {
        Identifier id = Objects.requireNonNull(provider.id(), "provider.id");
        if (provider.schemaVersion() < 1) {
            throw new IllegalArgumentException("Provider schema version must be positive: " + id);
        }
        WanderingRibbitTradeProvider duplicate = PROVIDERS.putIfAbsent(id, provider);
        if (duplicate != null) {
            throw new IllegalArgumentException("Duplicate Wandering Ribbit trade provider ID: " + id);
        }
    }

    public record Materialization(MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot) {
        public Materialization {
            Objects.requireNonNull(offers, "offers");
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }
}
