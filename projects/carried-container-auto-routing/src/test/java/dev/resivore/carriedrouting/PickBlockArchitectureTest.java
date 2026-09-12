package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PickBlockArchitectureTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of(System.getProperty("projectRoot"), "src", "main", "java", path));
    }

    @Test void vanillaLookupIsWrappedAtTheSingleSharedServerPickBlockPath() throws Exception {
        String mixin = source("dev/resivore/carriedrouting/mixin/ServerGamePacketListenerImplMixin.java");
        assertTrue(mixin.contains("method = \"tryPickItem\""));
        assertTrue(mixin.contains("Inventory;findSlotMatchingItem"));
        assertTrue(mixin.indexOf("original.call(inventory, requested)")
                < mixin.indexOf("PickBlockRouting.tryPickFromShulkers"));
        assertTrue(mixin.contains("vanillaSlot == Inventory.NOT_FOUND_INDEX"));
    }

    @Test void creativeIsExplicitlyExcludedAndNoClientPickBlockMixinExists() throws Exception {
        String mixin = source("dev/resivore/carriedrouting/mixin/ServerGamePacketListenerImplMixin.java");
        String config = Files.readString(Path.of(System.getProperty("projectRoot"), "src", "main", "resources",
                "carried_container_auto_routing.mixins.json"));
        assertTrue(mixin.contains("!player.hasInfiniteMaterials()"));
        assertFalse(config.substring(config.indexOf("\"client\"")).contains("PickBlock"));
    }

    @Test void productionUsesOneSharedCarrierOrderForRoutingAndPickBlock() throws Exception {
        String routing = source("dev/resivore/carriedrouting/RoutingService.java");
        String pick = source("dev/resivore/carriedrouting/PickBlockRouting.java");
        assertTrue(routing.contains("CarriedContainerOrder.hosts"));
        assertTrue(pick.contains("CarriedContainerOrder.hosts"));
        assertFalse(pick.contains("BundleContents"));
    }

    @Test void liveOrdinaryStorageSizeIsUsedWithoutAVanilla36SlotCeiling() throws Exception {
        String pick = source("dev/resivore/carriedrouting/PickBlockRouting.java");
        assertTrue(pick.contains("simulated.inventory().size()"));
        assertFalse(pick.contains("< 36"));
        assertFalse(pick.contains("Math.min(36"));
    }
}
