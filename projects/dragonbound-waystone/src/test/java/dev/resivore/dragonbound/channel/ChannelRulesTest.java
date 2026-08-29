package dev.resivore.dragonbound.channel;

import dev.resivore.dragonbound.config.DragonboundConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChannelRulesTest {
    private static final double TOLERANCE = 0.25D;

    @BeforeAll
    static void bootstrapItems() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(
                                lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
    }

    @Test
    void threeDimensionalDistanceExactlyAtToleranceIsAllowed() {
        assertFalse(ChannelRules.exceedsMovementTolerance(
                10.0D, 64.0D, -5.0D,
                10.25D, 64.0D, -5.0D,
                TOLERANCE));
    }

    @Test
    void tinyThreeAxisJitterIsAllowed() {
        assertFalse(ChannelRules.exceedsMovementTolerance(
                10.0D, 64.0D, -5.0D,
                10.001D, 63.998D, -4.999D,
                TOLERANCE));
    }

    @Test
    void movementBeyondToleranceOnAnyCombinationOfAxesCancels() {
        assertTrue(ChannelRules.exceedsMovementTolerance(
                10.0D, 64.0D, -5.0D,
                10.15D, 64.20D, -4.999D,
                TOLERANCE));
        assertTrue(ChannelRules.exceedsMovementTolerance(
                10.0D, 64.0D, -5.0D,
                10.0D, 64.0D, -4.749D,
                TOLERANCE));
    }

    @Test
    void movementRuleHasNoRotationInput() throws NoSuchMethodException {
        Method rule = ChannelRules.class.getMethod(
                "exceedsMovementTolerance",
                double.class, double.class, double.class,
                double.class, double.class, double.class,
                double.class);

        assertEquals(7, rule.getParameterCount());
        for (Class<?> parameterType : rule.getParameterTypes()) {
            assertEquals(double.class, parameterType);
        }
    }

    @Test
    void onlyAcceptedDamageCancelsWhenDamageCancellationIsEnabled() {
        assertTrue(ChannelRules.acceptedDamageCancels(true, true));
        assertFalse(ChannelRules.acceptedDamageCancels(true, false));
        assertFalse(ChannelRules.acceptedDamageCancels(false, true));
        assertFalse(ChannelRules.acceptedDamageCancels(false, false));
    }

    @Test
    void semanticallyEquivalentReplacementStacksRemainValidWithoutObjectIdentity() {
        ItemStack captured = new ItemStack(Items.ENDER_PEARL);
        ItemStack liveMainHand = captured.copy();

        assertFalse(liveMainHand == captured);
        assertTrue(validSourceItem(liveMainHand, captured));
    }

    @Test
    void trueSwapComponentChangeAndSlotChangeAllInvalidate() {
        ItemStack captured = new ItemStack(Items.ENDER_PEARL);
        ItemStack equivalent = captured.copy();

        assertFalse(validSourceItem(new ItemStack(Items.BLAZE_ROD), captured));

        ItemStack changedComponents = captured.copy();
        changedComponents.set(DataComponents.CUSTOM_NAME, Component.literal("different stack"));
        assertFalse(validSourceItem(changedComponents, captured));

        assertFalse(ChannelRules.sourceItemIsStillValid(
                2, 3, equivalent, captured));
    }

    @Test
    void releaseStateIsNotPartOfTheClickOnceSourceItemContract() throws NoSuchMethodException {
        Method rule = ChannelRules.class.getMethod(
                "sourceItemIsStillValid",
                int.class,
                int.class,
                ItemStack.class,
                ItemStack.class);

        assertEquals(4, rule.getParameterCount());
        for (Class<?> parameterType : rule.getParameterTypes()) {
            assertFalse(parameterType == boolean.class);
        }
        assertTrue(validSourceItem(
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.ENDER_PEARL)));
    }

    @Test
    void pearlAndStaffUseTheSameSemanticClickOnceLifecycle() {
        for (Item item : new Item[] {
                Items.ENDER_PEARL,
                Items.BLAZE_ROD
        }) {
            ItemStack captured = new ItemStack(item);
            assertTrue(validSourceItem(captured.copy(), captured));
        }
    }

    @Test
    void uninterruptedTwoSecondDefaultChannelCanReachItsCompletionTick() {
        ItemStack captured = new ItemStack(Items.ENDER_PEARL);
        long completionTick = DragonboundConfig.DEFAULT_CHANNEL_TICKS;
        assertEquals(40L, completionTick);
        for (long tick = 0L; tick < completionTick; tick++) {
            assertTrue(validSourceItem(captured.copy(), captured));
            assertFalse(ChannelRules.completionTickReached(tick, completionTick));
        }
        assertTrue(validSourceItem(captured.copy(), captured));
        assertTrue(ChannelRules.completionTickReached(completionTick, completionTick));
    }

    @Test
    void itemConsumptionAndCooldownAreSuccessOnlyEffects() {
        assertTrue(ChannelRules.shouldApplySuccessEffect(true));
        assertFalse(ChannelRules.shouldApplySuccessEffect(false));
    }

    private static boolean validSourceItem(
            ItemStack liveMainHand,
            ItemStack captured
    ) {
        return ChannelRules.sourceItemIsStillValid(
                2,
                2,
                liveMainHand,
                captured);
    }
}
