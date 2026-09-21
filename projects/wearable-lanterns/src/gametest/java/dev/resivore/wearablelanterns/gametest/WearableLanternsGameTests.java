package dev.resivore.wearablelanterns.gametest;

import dev.resivore.wearablelanterns.WearableLanterns;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.LivingEntityTrinketAttachment;
import eu.pb4.trinkets.impl.TrinketSlot;
import eu.pb4.trinkets.impl.TrinketsMain;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Controlled tag, predicate, slot, and native Trinkets scan validation without a gameplay profile. */
public final class WearableLanternsGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void vanillaLanternBehaviorAndDedicatedSlotRemainUnchanged(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TrinketAttachment attachment = rebuiltAttachment(player);
        TrinketSlotAccess slot = attachment.getSlotAccess(WearableLanterns.LANTERN_SLOT, 0);
        TrinketsApi.TrinketPredicate predicate = lanternPredicate(helper);

        helper.assertTrue(slot.isValid(), "The dedicated legs/lantern slot was not assigned");
        helper.assertTrue(WearableLanterns.LANTERN_SLOT.equals(slot.slotType().getId()),
                "The resolved slot identity changed");
        helper.assertTrue(slot.maxStackSize(new ItemStack(Items.LANTERN)) == 1,
                "The dedicated lantern slot no longer has amount/max one semantics");
        helper.assertTrue(attachment.getSlotAccess(WearableLanterns.LANTERN_SLOT, 1) == null,
                "A second lantern slot unexpectedly exists");
        helper.assertTrue(predicate.test(new ItemStack(Items.LANTERN), slot, player),
                "Vanilla Lantern is no longer accepted");
        helper.assertTrue(predicate.test(new ItemStack(Items.SOUL_LANTERN), slot, player),
                "Vanilla Soul Lantern is no longer accepted");
        helper.assertTrue(!predicate.test(new ItemStack(Items.TORCH), slot, player),
                "An unrelated vanilla light entered the lantern slot");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void absentOptionalProvidersLoadCleanly(GameTestHelper helper) {
        if (ProviderFixtureInitializer.fixturesEnabled()) {
            helper.succeed();
            return;
        }
        for (var id : ProviderFixtureInitializer.APPROVED_PROVIDER_ITEMS) {
            helper.assertTrue(BuiltInRegistries.ITEM.getOptional(id).isEmpty(),
                    "Provider fixture unexpectedly exists in the absent-provider run: " + id);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void approvedProviderItemsUseTheRealSlotStackAndNativeScan(GameTestHelper helper) {
        if (!ProviderFixtureInitializer.fixturesEnabled()) {
            helper.succeed();
            return;
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TrinketAttachment attachment = rebuiltAttachment(player);
        TrinketSlotAccess slot = attachment.getSlotAccess(WearableLanterns.LANTERN_SLOT, 0);
        TrinketsApi.TrinketPredicate predicate = lanternPredicate(helper);

        for (var id : ProviderFixtureInitializer.APPROVED_PROVIDER_ITEMS) {
            Item item = BuiltInRegistries.ITEM.getOptional(id)
                    .orElseThrow(() -> new AssertionError("Missing provider fixture item: " + id));
            ItemStack actualProviderStack = new ItemStack(item);
            helper.assertTrue(actualProviderStack.is(WearableLanterns.LANTERN_SLOT_ITEMS),
                    "Approved provider item did not resolve into the public tag: " + id);
            helper.assertTrue(predicate.test(actualProviderStack, slot, player),
                    "Approved provider item was rejected by the actual Trinkets predicate: " + id);
            helper.assertTrue(TrinketSlot.canInsert(actualProviderStack, slot, player),
                    "Approved provider item was rejected by the actual Trinkets slot validator: " + id);
            helper.assertTrue(slot.set(actualProviderStack),
                    "Approved provider item could not be stored in legs/lantern: " + id);

            ItemStack storedStack = slot.get();
            helper.assertTrue(storedStack == actualProviderStack,
                    "The slot did not preserve the actual provider ItemStack object: " + id);
            TrinketSlotAccess scannedSlot = attachment.allEquipped(false).stream()
                    .filter(access -> access.equals(slot))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Native allEquipped(false) omitted the provider stack: " + id));
            helper.assertTrue(scannedSlot.get() == storedStack,
                    "Native Trinkets scanning did not expose the actual equipped ItemStack: " + id);
            helper.assertTrue(slot.set(ItemStack.EMPTY),
                    "Could not clear the provider fixture stack: " + id);
        }
        helper.succeed();
    }

    private static TrinketsApi.TrinketPredicate lanternPredicate(GameTestHelper helper) {
        TrinketsApi.TrinketPredicate predicate =
                TrinketsMain.PREDICATES.get(WearableLanterns.LANTERN_ONLY_PREDICATE);
        helper.assertTrue(predicate != null, "Wearable Lanterns did not register lantern_only");
        return predicate;
    }

    private static TrinketAttachment rebuiltAttachment(ServerPlayer player) {
        // Fabric's mock player is constructed before its embedded login finishes. Rebuild once
        // after creation so its attachment sees the already-loaded server slot data, matching a
        // normal player login/reload lifecycle.
        LivingEntityTrinketAttachment attachment = LivingEntityTrinketAttachment.get(player);
        attachment.rebuild();
        return attachment;
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method)
            throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
