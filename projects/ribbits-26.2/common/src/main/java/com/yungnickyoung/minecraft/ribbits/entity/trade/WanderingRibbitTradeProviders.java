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
    public static final Identifier OPTIONAL_MATCHA_COMPASSES_PROVIDER_ID =
            RibbitsCommon.id("optional_matcha_compasses");
    public static final Identifier OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID =
            RibbitsCommon.id("optional_naturalist_fauna");

    private static final Map<Identifier, WanderingRibbitTradeProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        registerInternal(new WanderingRibbitNativeTradeProvider());
        registerInternal(new WanderingRibbitMatchaCompassTradeProvider());
        registerInternal(new WanderingRibbitNaturalistFaunaTradeProvider());
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
                    provider.id(), provider.schemaVersion(), firstOffer, offers.size() - firstOffer,
                    provider.restockPolicy()));
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

    /**
     * Converts only the exact C17 native snapshot layout in place. The retained offer
     * objects are deliberately moved rather than recreated, preserving their uses and
     * components and leaving every optional provider choice untouched.
     */
    public static WanderingRibbitTradeSnapshot migrateC17NativeMenu(
            MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot
    ) {
        Objects.requireNonNull(offers, "offers");
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.totalOfferCount() != offers.size() || snapshot.providers().isEmpty()) return snapshot;

        WanderingRibbitTradeSnapshot.ProviderRange nativeRange = snapshot.providers().getFirst();
        if (!NATIVE_PROVIDER_ID.equals(nativeRange.id())
                || nativeRange.schemaVersion() != 3
                || nativeRange.firstOffer() != 0
                || nativeRange.offerCount() != 6) {
            return snapshot;
        }

        // Snapshot validation guarantees contiguity. Still reject a malformed later range
        // instead of deleting arbitrary merchant offers from an ambiguous saved entity.
        int expectedFirst = 6;
        for (int index = 1; index < snapshot.providers().size(); index++) {
            WanderingRibbitTradeSnapshot.ProviderRange range = snapshot.providers().get(index);
            if (range.firstOffer() != expectedFirst) return snapshot;
            expectedFirst += range.offerCount();
        }
        if (expectedFirst != offers.size()) return snapshot;

        offers.remove(4);
        offers.remove(3);
        offers.remove(2);
        List<WanderingRibbitTradeSnapshot.ProviderRange> migrated = new ArrayList<>();
        migrated.add(new WanderingRibbitTradeSnapshot.ProviderRange(
                NATIVE_PROVIDER_ID, WanderingRibbitNativeTradeProvider.SCHEMA_VERSION, 0,
                WanderingRibbitNativeTradeProvider.NATIVE_OFFER_COUNT, nativeRange.restockPolicy()));
        for (int index = 1; index < snapshot.providers().size(); index++) {
            WanderingRibbitTradeSnapshot.ProviderRange range = snapshot.providers().get(index);
            migrated.add(new WanderingRibbitTradeSnapshot.ProviderRange(
                    range.id(), range.schemaVersion(), range.firstOffer() - 3,
                    range.offerCount(), range.restockPolicy()));
        }
        return new WanderingRibbitTradeSnapshot(snapshot.seed(), migrated);
    }

    /**
     * Reattaches the server-only one-shot transaction guard after the ordinary MerchantOffer
     * codec has decoded a persisted Naturalist fauna range.  The exact range shape is required so
     * malformed snapshots and unrelated optional providers remain untouched.
     */
    public static void restoreNaturalistFaunaOneShotOffers(
            MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot
    ) {
        Objects.requireNonNull(offers, "offers");
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.totalOfferCount() != offers.size()) return;

        WanderingRibbitTradeSnapshot.ProviderRange range = snapshot.providers().stream()
                .filter(candidate -> OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID.equals(candidate.id()))
                .findFirst().orElse(null);
        if (range == null
                || range.schemaVersion() != WanderingRibbitNaturalistFaunaTradeProvider.SCHEMA_VERSION
                || range.offerCount() != WanderingRibbitNaturalistFaunaTradeProvider.OFFER_COUNT
                || range.restockPolicy() != WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK) {
            return;
        }
        for (int index = range.firstOffer(); index < range.firstOffer() + range.offerCount(); index++) {
            if (offers.get(index).getMaxUses() != 1) return;
        }
        for (int index = range.firstOffer(); index < range.firstOffer() + range.offerCount(); index++) {
            offers.set(index, WanderingRibbitOneShotOffer.restore(offers.get(index)));
        }
    }

    /**
     * Authorizes result removal for the exact active offer object owned by this persisted merchant.
     * Naturalist fauna ranges are one-shot even when a stale menu result survives long enough to
     * reach a second server click; ordinary provider offers retain vanilla behavior.
     */
    public static boolean mayTakeMerchantResult(
            MerchantOffers offers,
            WanderingRibbitTradeSnapshot snapshot,
            MerchantOffer activeOffer
    ) {
        Objects.requireNonNull(offers, "offers");
        if (activeOffer == null) return false;

        int activeIndex = -1;
        for (int index = 0; index < offers.size(); index++) {
            if (offers.get(index) == activeOffer) {
                activeIndex = index;
                break;
            }
        }
        if (activeIndex < 0) return false;
        if (snapshot == null || snapshot.totalOfferCount() != offers.size()) return true;

        WanderingRibbitTradeSnapshot.ProviderRange range = snapshot.providers().stream()
                .filter(candidate -> OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID.equals(candidate.id()))
                .findFirst().orElse(null);
        if (range == null
                || range.schemaVersion() != WanderingRibbitNaturalistFaunaTradeProvider.SCHEMA_VERSION
                || range.offerCount() != WanderingRibbitNaturalistFaunaTradeProvider.OFFER_COUNT
                || range.restockPolicy() != WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK) {
            return true;
        }
        boolean fauna = activeIndex >= range.firstOffer()
                && activeIndex < range.firstOffer() + range.offerCount();
        return !fauna || (activeOffer.getUses() == 0 && !activeOffer.isOutOfStock());
    }

    /** Shared range calculation used by Wandering Ribbit's ordinary restock cadence. */
    public static List<Integer> ordinaryOfferIndexes(
            MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot
    ) {
        Objects.requireNonNull(offers, "offers");
        if (snapshot == null || snapshot.totalOfferCount() != offers.size()) return List.of();
        List<Integer> indexes = new ArrayList<>();
        for (WanderingRibbitTradeSnapshot.ProviderRange range : snapshot.providers()) {
            if (range.restockPolicy() != WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY) continue;
            for (int index = range.firstOffer(); index < range.firstOffer() + range.offerCount(); index++) {
                indexes.add(index);
            }
        }
        return List.copyOf(indexes);
    }

    /** Resets only exact persisted ranges whose provider explicitly permits ordinary restocking. */
    public static void resetOrdinaryProviderUses(
            MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot
    ) {
        for (int index : ordinaryOfferIndexes(offers, snapshot)) {
            offers.get(index).resetUses();
        }
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
