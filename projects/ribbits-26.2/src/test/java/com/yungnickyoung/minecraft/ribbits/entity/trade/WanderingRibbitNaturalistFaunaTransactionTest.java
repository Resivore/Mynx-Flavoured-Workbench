package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises Minecraft's production MerchantMenu, MerchantContainer, and MerchantResultSlot path. */
class WanderingRibbitNaturalistFaunaTransactionTest {
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
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
        registryOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @Test
    void realMenuQuickMoveAndOrdinaryClickCannotTakeAStaleFaunaResultTwice() {
        MerchantOffer faunaA = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 4, new ItemStack(Items.EGG));
        MerchantOffer faunaB = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 6, new ItemStack(Items.SLIME_BALL));
        MerchantOffers offers = offers(faunaA, faunaB);
        WanderingRibbitTradeSnapshot snapshot = naturalistSnapshot(0, 2);
        MenuFixture fixture = menu(offers, snapshot);

        fixture.menu().setSelectionHint(0);
        fixture.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 12));
        assertTrue(fixture.menu().getSlot(2).hasItem());

        fixture.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, fixture.player());

        assertEquals(1, faunaA.getUses());
        assertTrue(faunaA.isOutOfStock());
        assertEquals(8, fixture.menu().getSlot(0).getItem().getCount());
        assertEquals(1, inventoryCount(fixture.inventory(), Items.EGG));
        assertFalse(faunaB.isOutOfStock(), "fauna B remains independently available");

        // Recreate the stale-result condition that defeats C23: vanilla has already handed this
        // stack to quickMoveStack before MerchantOffer.take can reject an exhausted offer.
        fixture.menu().getSlot(2).set(faunaA.assemble());
        fixture.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, fixture.player());

        assertEquals(1, faunaA.getUses(), "rejected quick-move cannot advance beyond exhaustion");
        assertEquals(8, fixture.menu().getSlot(0).getItem().getCount(),
                "rejected quick-move consumes no additional payment");
        assertEquals(1, inventoryCount(fixture.inventory(), Items.EGG),
                "rejected quick-move produces no second fauna item");

        fixture.menu().setSelectionHint(1);
        fixture.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 6));
        assertTrue(fixture.menu().getSlot(2).hasItem(), "fauna B remains purchasable");
        fixture.menu().clicked(2, 0, ContainerInput.PICKUP, fixture.player());

        assertEquals(1, faunaB.getUses());
        assertTrue(faunaB.isOutOfStock());
        assertEquals(1, fixture.menu().getCarried().getCount());
        assertTrue(fixture.menu().getCarried().is(Items.SLIME_BALL));
        assertTrue(fixture.menu().getSlot(0).getItem().isEmpty());

        fixture.menu().setCarried(ItemStack.EMPTY);
        fixture.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 6));
        fixture.menu().getSlot(2).set(faunaB.assemble());
        fixture.menu().clicked(2, 0, ContainerInput.PICKUP, fixture.player());

        assertTrue(fixture.menu().getCarried().isEmpty(),
                "ordinary result click cannot receive a stale second result");
        assertEquals(6, fixture.menu().getSlot(0).getItem().getCount());
        assertEquals(1, faunaB.getUses());
    }

    @Test
    void closeReopenAndCodecReloadKeepEachFaunaOfferIndependentlyExhausted() {
        MerchantOffer faunaA = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 4, new ItemStack(Items.EGG));
        MerchantOffer faunaB = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 6, new ItemStack(Items.SLIME_BALL));
        faunaA.increaseUses();
        MerchantOffers offers = offers(faunaA, faunaB);
        WanderingRibbitTradeSnapshot snapshot = naturalistSnapshot(0, 2);

        MenuFixture reopened = menu(offers, snapshot);
        reopened.menu().setSelectionHint(0);
        reopened.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 10));
        assertFalse(reopened.menu().getSlot(2).hasItem(), "reopen keeps fauna A exhausted");
        reopened.menu().setSelectionHint(1);
        reopened.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 6));
        assertTrue(reopened.menu().getSlot(2).hasItem(), "reopen leaves fauna B available");

        MerchantOffers reloaded = offers(roundTrip(faunaA), roundTrip(faunaB));
        WanderingRibbitTradeProviders.restoreNaturalistFaunaOneShotOffers(reloaded, snapshot);
        assertInstanceOf(WanderingRibbitOneShotOffer.class, reloaded.get(0));
        assertInstanceOf(WanderingRibbitOneShotOffer.class, reloaded.get(1));
        assertEquals(1, reloaded.get(0).getUses());
        assertEquals(0, reloaded.get(1).getUses());

        MenuFixture restarted = menu(reloaded, snapshot);
        restarted.menu().setSelectionHint(0);
        restarted.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 10));
        assertFalse(restarted.menu().getSlot(2).hasItem(), "serialization/reload keeps A exhausted");
        restarted.menu().setSelectionHint(1);
        restarted.menu().getSlot(0).set(new ItemStack(Items.GLOW_BERRIES, 6));
        restarted.menu().clicked(2, 0, ContainerInput.QUICK_MOVE, restarted.player());
        assertEquals(1, reloaded.get(1).getUses(), "B still succeeds exactly once after reload");
        assertEquals(1, inventoryCount(restarted.inventory(), Items.SLIME_BALL));
    }

    @Test
    void migrationPreservesUsedFaunaAndOrdinaryRestockStillTouchesOnlyOrdinaryRange() {
        MerchantOffer ordinary = new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(Items.STICK), 2, 0, 0.0F);
        ordinary.increaseUses();
        MerchantOffer faunaA = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 4, new ItemStack(Items.EGG));
        MerchantOffer faunaB = WanderingRibbitNaturalistFaunaTradeProvider.faunaOffer(
                Items.GLOW_BERRIES, 6, new ItemStack(Items.SLIME_BALL));
        faunaA.increaseUses();
        faunaA.increaseUses();
        faunaB.increaseUses();
        faunaA.setSpecialPriceDiff(3);
        faunaA.updateDemand();
        int savedDemand = faunaA.getDemand();

        MerchantOffers offers = offers(ordinary, roundTrip(faunaA), roundTrip(faunaB));
        WanderingRibbitTradeSnapshot snapshot = new WanderingRibbitTradeSnapshot(73L, List.of(
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 4, 0, 1,
                        WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY),
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID, 1, 1, 2,
                        WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK)
        ));

        WanderingRibbitTradeProviders.restoreNaturalistFaunaOneShotOffers(offers, snapshot);
        assertEquals(2, offers.get(1).getUses(), "migration never resets an already-used offer");
        assertEquals(3, offers.get(1).getSpecialPriceDiff());
        assertEquals(savedDemand, offers.get(1).getDemand());
        assertFalse(WanderingRibbitTradeProviders.mayTakeMerchantResult(
                offers, snapshot, offers.get(1)));
        assertTrue(WanderingRibbitTradeProviders.mayTakeMerchantResult(
                offers, snapshot, offers.getFirst()), "ordinary offers keep vanilla transaction eligibility");

        WanderingRibbitTradeProviders.resetOrdinaryProviderUses(offers, snapshot);
        assertEquals(0, offers.getFirst().getUses(), "ordinary provider range still restocks");
        assertEquals(2, offers.get(1).getUses(), "fauna A never restocks or rewinds");
        assertEquals(1, offers.get(2).getUses(), "fauna B never restocks");
    }

    private static MenuFixture menu(MerchantOffers offers, WanderingRibbitTradeSnapshot snapshot) {
        Player player = mock(Player.class);
        Inventory inventory = new Inventory(player, new EntityEquipment());
        when(player.getInventory()).thenReturn(inventory);
        WanderingRibbitEntity merchant = mock(WanderingRibbitEntity.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);
        when(level.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
        when(merchant.getOffers()).thenReturn(offers);
        when(merchant.getTradingPlayer()).thenReturn(player);
        when(merchant.isClientSide()).thenReturn(false);
        when(merchant.level()).thenReturn(level);
        when(merchant.getNotifyTradeSound()).thenReturn(SoundEvents.VILLAGER_YES);
        doAnswer(invocation -> {
            invocation.<MerchantOffer>getArgument(0).increaseUses();
            return null;
        }).when(merchant).notifyTrade(org.mockito.ArgumentMatchers.any(MerchantOffer.class));
        WanderingRibbitMerchantMenu menu = new WanderingRibbitMerchantMenu(
                1,
                inventory,
                merchant,
                offer -> WanderingRibbitTradeProviders.mayTakeMerchantResult(offers, snapshot, offer)
        );
        return new MenuFixture(menu, player, inventory);
    }

    private static WanderingRibbitTradeSnapshot naturalistSnapshot(int firstOffer, int offerCount) {
        return new WanderingRibbitTradeSnapshot(73L, List.of(
                new WanderingRibbitTradeSnapshot.ProviderRange(
                        WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID,
                        WanderingRibbitNaturalistFaunaTradeProvider.SCHEMA_VERSION,
                        firstOffer,
                        offerCount,
                        WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK)
        ));
    }

    private static MerchantOffers offers(MerchantOffer... values) {
        MerchantOffers offers = new MerchantOffers();
        offers.addAll(List.of(values));
        return offers;
    }

    private static int inventoryCount(Inventory inventory, Item item) {
        return inventory.getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static MerchantOffer roundTrip(MerchantOffer offer) {
        JsonElement encoded = MerchantOffer.CODEC.encodeStart(registryOps, offer).getOrThrow();
        return MerchantOffer.CODEC.parse(registryOps, encoded).getOrThrow();
    }

    private record MenuFixture(WanderingRibbitMerchantMenu menu, Player player, Inventory inventory) {
    }
}
