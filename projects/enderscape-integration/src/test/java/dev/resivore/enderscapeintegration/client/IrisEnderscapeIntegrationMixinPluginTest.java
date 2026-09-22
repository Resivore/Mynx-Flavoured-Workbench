package dev.resivore.enderscapeintegration.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IrisEnderscapeIntegrationMixinPluginTest {
    @Test
    void onlyTheOptionalIrisCallbackIsVersionGated() {
        String irisMixin = IrisEnderscapeIntegrationMixinPlugin.IRIS_MIXIN_CLASS;
        assertTrue(IrisEnderscapeIntegrationMixinPlugin.activationAllowed(irisMixin, true));
        assertFalse(IrisEnderscapeIntegrationMixinPlugin.activationAllowed(irisMixin, false));

        for (String commonMixin : List.of(
                "dev.resivore.enderscapeintegration.mixin.ChorusCakeRollMixin",
                "dev.resivore.enderscapeintegration.mixin.EnchantmentHelperMixin",
                "dev.resivore.enderscapeintegration.mixin.FoodPropertiesMixin",
                "dev.resivore.enderscapeintegration.mixin.RecipeManagerMixin",
                "dev.resivore.enderscapeintegration.mixin.ServerAdvancementManagerMixin",
                "dev.resivore.enderscapeintegration.mixin.TagLoaderMixin")) {
            assertTrue(IrisEnderscapeIntegrationMixinPlugin.activationAllowed(commonMixin, false), commonMixin);
        }
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
        assertTrue(plugin.contains("if (!IRIS_MIXIN_CLASS.equals(mixinClassName))"));
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
