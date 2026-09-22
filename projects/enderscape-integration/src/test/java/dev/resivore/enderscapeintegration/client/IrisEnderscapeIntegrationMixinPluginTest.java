package dev.resivore.enderscapeintegration.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IrisEnderscapeIntegrationMixinPluginTest {
    @Test
    void absentOrDifferentIrisVersionDoesNotActivateTheOptionalCompatibilityCallback() {
        assertTrue(IrisEnderscapeIntegrationMixinPlugin.activationAllowed(true));
        assertFalse(IrisEnderscapeIntegrationMixinPlugin.activationAllowed(false));
    }

    @Test
    void irisIsNotACommonRuntimeRequirement() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
        String metadata = Files.readString(projectRoot.resolve("src/main/resources/fabric.mod.json"));
        String plugin = Files.readString(projectRoot.resolve(
                "src/main/java/dev/resivore/enderscapeintegration/client/IrisEnderscapeIntegrationMixinPlugin.java"));

        assertTrue(metadata.contains("\"suggests\""));
        assertFalse(metadata.substring(metadata.indexOf("\"depends\""), metadata.indexOf("\"recommends\""))
                .contains("iris"));
        assertTrue(plugin.contains("hasExactVersion(\"iris\", IRIS_VERSION)"));
        assertFalse(plugin.contains("net.irisshaders"));

        try (var stream = IrisEnderscapeIntegrationMixinPlugin.class
                .getResourceAsStream("IrisEnderscapeIntegrationMixinPlugin.class")) {
            assertTrue(stream != null);
            String constants = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(constants.contains("net/irisshaders"));
            assertFalse(constants.contains("VeiledLeavesShaderMaterialBridge"));
        }
    }
}
