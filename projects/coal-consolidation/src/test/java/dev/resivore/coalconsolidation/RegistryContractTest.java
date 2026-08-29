package dev.resivore.coalconsolidation;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RegistryContractTest {
    private static final Identifier CHARCOAL_ID = Identifier.withDefaultNamespace("charcoal");

    @BeforeAll
    static void bootstrapVanillaRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void vanillaCharcoalRegistryIdStillExistsAndIsUnchanged() {
        assertTrue(BuiltInRegistries.ITEM.containsKey(CHARCOAL_ID));
        assertSame(Items.CHARCOAL, BuiltInRegistries.ITEM.getValue(CHARCOAL_ID));
    }
}
