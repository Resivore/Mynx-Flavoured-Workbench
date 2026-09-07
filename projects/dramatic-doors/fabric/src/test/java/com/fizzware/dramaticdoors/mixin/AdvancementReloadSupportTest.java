package com.fizzware.dramaticdoors.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import com.fizzware.dramaticdoors.compat.AdvancementReloadSupport;
import org.junit.jupiter.api.Test;

class AdvancementReloadSupportTest {
    @Test
    void createsAMutableReloadMapFromAnImmutableInputWithoutChangingExistingEntries() {
        Map<String, String> input = Map.of(
                "minecraft:story/root", "vanilla",
                "provider:recipes/root", "provider");

        Map<String, String> reloadMap = AdvancementReloadSupport.copyForGeneratedEntries(input, true);
        reloadMap.put("dramaticdoors:recipes/redstone/short_oak_door", "generated");

        assertEquals("vanilla", reloadMap.get("minecraft:story/root"));
        assertEquals("provider", reloadMap.get("provider:recipes/root"));
        assertEquals("generated", reloadMap.get("dramaticdoors:recipes/redstone/short_oak_door"));
        assertThrows(UnsupportedOperationException.class, () -> input.put("unexpected", "value"));
        assertEquals(2, input.size());
    }

    @Test
    void eachReloadStartsFromACleanCopyAndDoesNotAccumulateGeneratedEntries() {
        Map<String, String> input = Map.of("minecraft:story/root", "vanilla");

        Map<String, String> firstReload = AdvancementReloadSupport.copyForGeneratedEntries(input, true);
        firstReload.put("dramaticdoors:recipes/redstone/short_oak_door", "generated");
        Map<String, String> secondReload = AdvancementReloadSupport.copyForGeneratedEntries(input, true);
        secondReload.put("dramaticdoors:recipes/redstone/short_oak_door", "generated");

        assertEquals(firstReload, secondReload);
        assertEquals(2, secondReload.size());
        assertEquals("vanilla", secondReload.get("minecraft:story/root"));
    }

    @Test
    void leavesTheVanillaMapUntouchedWhenThereAreNoGeneratedEntries() {
        Map<String, String> input = Map.of("minecraft:story/root", "vanilla");

        assertSame(input, AdvancementReloadSupport.copyForGeneratedEntries(input, false));
    }
}
