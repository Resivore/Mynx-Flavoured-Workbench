package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Read-only contract checks for the separately owned Naturalist and Ribbits companions. */
class CompanionCoexistenceTest {
    private static final Path PROJECTS = Path.of(System.getProperty("projectRoot")).getParent();

    @Test
    void c10KeepsTheExistingSingleEmfTraceRedirectAndCompanionSeamsDisjoint() throws Exception {
        String ours = Files.readString(Path.of(System.getProperty("projectRoot"))
                .resolve("src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconModelPartPrerendererMixin.java"));
        String naturalist = Files.readString(PROJECTS.resolve(
                "naturalist-xaero-entity-icons-compat/src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconAdapter.java"));
        String ribbitsConfig = Files.readString(PROJECTS.resolve(
                "ribbits-xaero-entity-icons-compat/src/main/resources/ribbits_xaero_entity_icons_compat.mixins.json"));
        String ribbitsCreator = Files.readString(PROJECTS.resolve(
                "ribbits-xaero-entity-icons-compat/src/main/java/dev/resivore/ribbitsxaeroicons/mixin/RadarIconCreatorMixin.java"));

        assertEquals(2, occurrences(ours, "@Redirect")); // trace color plus adapter draw; unchanged C9 seam.
        assertFalse(naturalist.contains("@Redirect"));
        assertFalse(ribbitsConfig.contains("RadarIconModelPartPrerendererMixin"));
        assertTrue(ribbitsCreator.contains("@Mixin(value = RadarIconCreator.class"));
        assertFalse(ribbitsCreator.contains("RadarIconModelPartPrerenderer"));
    }

    @Test
    void successorPolicyDoesNotContainCompanionNamespacesOrMutableSharedSelection() throws Exception {
        String policy = Files.readString(Path.of(System.getProperty("projectRoot"))
                .resolve("src/main/java/dev/resivore/xaeroemfcompat/IconTargetPolicy.java"));
        assertFalse(policy.contains("naturalist:"));
        assertFalse(policy.contains("ribbits:"));
        assertFalse(policy.contains("ThreadLocal"));
        assertTrue(policy.contains("Map.of"));
    }

    private static int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }
}
