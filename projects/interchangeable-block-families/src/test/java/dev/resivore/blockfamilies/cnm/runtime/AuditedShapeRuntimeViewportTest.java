package dev.resivore.blockfamilies.cnm.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuditedShapeRuntimeViewportTest {
    @Test
    void twelveItemWindowKeepsEveryLargeFamilySelectionVisible() {
        int familySize = 22;
        int visibleSize = 12;

        for (int selected = 0; selected < familySize; selected++) {
            int start = AuditedShapeRuntime.windowStart(selected, familySize, visibleSize);
            assertTrue(start >= 0 && start <= familySize - visibleSize,
                    "Window start escaped the 22-member family at selection " + selected);
            assertTrue(selected >= start && selected < start + visibleSize,
                    "Selected item left the tooltip viewport at index " + selected);
        }

        assertEquals(0, AuditedShapeRuntime.windowStart(0, familySize, visibleSize));
        assertEquals(5, AuditedShapeRuntime.windowStart(11, familySize, visibleSize));
        assertEquals(10, AuditedShapeRuntime.windowStart(21, familySize, visibleSize));
        assertTrue(visibleSize * 22 < 320,
                "The audited tooltip viewport no longer fits the common 320-pixel scaled width");
    }

    @Test
    void viewportHelperClampsOutOfRangeSelectionsAndSmallFamilies() {
        assertEquals(0, AuditedShapeRuntime.windowStart(-10, 22, 12));
        assertEquals(10, AuditedShapeRuntime.windowStart(99, 22, 12));
        assertEquals(0, AuditedShapeRuntime.windowStart(3, 5, 12));
        assertEquals(0, AuditedShapeRuntime.windowStart(0, 0, 12));
    }
}
