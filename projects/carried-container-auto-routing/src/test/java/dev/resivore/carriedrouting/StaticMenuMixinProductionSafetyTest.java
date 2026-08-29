package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticMenuMixinProductionSafetyTest {
    @Test
    void menuSlotsUseNormallyRemappedAccessInsteadOfARefmapDependentShadow() throws IOException {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String mixin = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/carriedrouting/mixin/AbstractContainerMenuMixin.java"
        ));

        assertFalse(mixin.contains("@Shadow"));
        assertFalse(mixin.contains("org.spongepowered.asm.mixin.Shadow"));
        assertTrue(mixin.contains("List<Slot> slots = menu.slots;"));
        assertTrue(mixin.contains("((AbstractContainerMenu) (Object) this).slots"));
    }
}
