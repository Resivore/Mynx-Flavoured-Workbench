package dev.resivore.mapmarkerextension.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.MinecraftTestBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class MapMarkerItemIdentityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void allKnownStableItemNameKeysResolve() {
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            assertEquals(identity, MapMarkerItemIdentity.find(namedMap(identity)).orElseThrow());
        }
    }

    @Test
    void anvilCustomNameDoesNotDefeatTheStableItemNameIdentity() {
        ItemStack stack = namedMap(MapMarkerIdentity.PAPAL_OUTPOST);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("My Expedition"));

        assertEquals(
            MapMarkerIdentity.PAPAL_OUTPOST,
            MapMarkerItemIdentity.find(stack).orElseThrow()
        );
    }

    @Test
    void ordinaryLiteralAndNonMapStacksRemainUnknown() {
        ItemStack ordinary = new ItemStack(Items.FILLED_MAP);
        ItemStack literal = new ItemStack(Items.FILLED_MAP);
        literal.set(DataComponents.ITEM_NAME, Component.literal("Papal Outpost"));
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.ITEM_NAME, Component.translatable(
            MapMarkerIdentity.PAPAL_OUTPOST.itemNameTranslationKey()
        ));

        assertTrue(MapMarkerItemIdentity.find(ordinary).isEmpty());
        assertTrue(MapMarkerItemIdentity.find(literal).isEmpty());
        assertTrue(MapMarkerItemIdentity.find(paper).isEmpty());
    }

    static ItemStack namedMap(MapMarkerIdentity identity) {
        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.ITEM_NAME, Component.translatable(
            identity.itemNameTranslationKey()
        ));
        return stack;
    }
}
