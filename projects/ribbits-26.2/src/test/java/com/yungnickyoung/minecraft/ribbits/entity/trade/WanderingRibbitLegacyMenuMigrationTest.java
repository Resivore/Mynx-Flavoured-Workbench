package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.network.chat.Component;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WanderingRibbitLegacyMenuMigrationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        RegistryAccess builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        HolderLookup.Provider vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
    }

    @Test
    void exactC17MenuDropsOnlyCuriositiesAndRetainsOptionalChoicesAndOfferState() {
        MerchantOffers offers = new MerchantOffers();
        MerchantOffer chute = offer(Items.EMERALD, Items.APPLE);
        MerchantOffer map = offer(Items.EMERALD, Items.BOOK);
        offers.add(chute);
        offers.add(map);
        offers.add(offer(Items.EMERALD, Items.RED_MUSHROOM));
        offers.add(offer(Items.EMERALD, Items.BROWN_MUSHROOM));
        offers.add(offer(Items.EMERALD, Items.LILY_PAD));
        MerchantOffer compass = offer(Items.COMPASS, Items.EMERALD);
        compass.increaseUses();
        compass.getResult().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("kept"));
        offers.add(compass);
        MerchantOffer matcha = offer(Items.COPPER_INGOT, Items.EMERALD);
        MerchantOffer naturalist = offer(Items.EGG, Items.SLIME_BALL);
        offers.add(matcha);
        offers.add(naturalist);

        WanderingRibbitTradeSnapshot legacy = new WanderingRibbitTradeSnapshot(123L, List.of(
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 3, 0, 6),
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_MATCHA_COMPASSES_PROVIDER_ID, 1, 6, 1),
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID, 1, 7, 1,
                        WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK)
        ));

        WanderingRibbitTradeSnapshot migrated =
                WanderingRibbitTradeProviders.migrateC17NativeMenu(offers, legacy);

        assertEquals(5, offers.size());
        assertSame(chute, offers.get(0));
        assertSame(map, offers.get(1));
        assertSame(compass, offers.get(2));
        assertSame(matcha, offers.get(3));
        assertSame(naturalist, offers.get(4));
        assertEquals(1, compass.getUses());
        assertEquals(Component.literal("kept"), compass.getResult().getHoverName());
        assertEquals(List.of(0, 3, 4), migrated.providers().stream()
                .map(WanderingRibbitTradeSnapshot.ProviderRange::firstOffer).toList());
        assertEquals(List.of(3, 1, 1), migrated.providers().stream()
                .map(WanderingRibbitTradeSnapshot.ProviderRange::offerCount).toList());
        assertEquals(WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK,
                migrated.providers().getLast().restockPolicy());
    }

    @Test
    void malformedOrUnrecognizedSnapshotsAreNotRewritten() {
        MerchantOffers offers = new MerchantOffers();
        for (int index = 0; index < 6; index++) offers.add(offer(Items.EMERALD, Items.STICK));
        WanderingRibbitTradeSnapshot malformed = new WanderingRibbitTradeSnapshot(1L, List.of(
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 3, 0, 6),
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_MATCHA_COMPASSES_PROVIDER_ID, 1, 8, 0)
        ));

        assertSame(malformed, WanderingRibbitTradeProviders.migrateC17NativeMenu(offers, malformed));
        assertEquals(6, offers.size());
    }

    private static MerchantOffer offer(net.minecraft.world.item.Item cost, net.minecraft.world.item.Item result) {
        return new MerchantOffer(new ItemCost(cost, 1), new ItemStack(result), 4, 0, 0.0F);
    }
}
