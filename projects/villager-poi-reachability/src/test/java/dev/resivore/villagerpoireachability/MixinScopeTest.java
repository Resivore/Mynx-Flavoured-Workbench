package dev.resivore.villagerpoireachability;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinScopeTest {
    @Test
    void hookIsRestrictedToVillagerAcquirePoiJobSitesAndRealPoiTargets() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/resivore/villagerpoireachability/mixin/AcquirePoiMixin.java"));
        assertTrue(source.contains("@Mixin(AcquirePoi.class)"));
        assertTrue(source.contains("mob instanceof Villager"));
        assertTrue(source.contains("PoiTypeTags.ACQUIRABLE_JOB_SITE"));
        assertTrue(source.contains("createPath(standing, 0)"));
        assertTrue(source.contains("new Path(nodes, poi, true)"));
        assertFalse(source.contains("@Mixin(Path.class)"), "the patch must not alter global Path semantics");
    }
}
