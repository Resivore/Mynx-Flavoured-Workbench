package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class ProductionArtifactContractTest {
    private static final Path PATCH = Path.of(Objects.requireNonNull(
            System.getProperty("patchJar"), "patchJar"));

    @Test
    void packagedMetadataIsExactAndClientOnly() throws Exception {
        assertEquals("ribbits-xaero-entity-icons-compat-0.1.0-canary3.jar",
                PATCH.getFileName().toString());
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            JsonObject metadata = json(zip, "fabric.mod.json");
            assertEquals("ribbits_xaero_entity_icons_compat", metadata.get("id").getAsString());
            assertEquals("0.1.0-canary3", metadata.get("version").getAsString());
            assertEquals("client", metadata.get("environment").getAsString());
            assertFalse(metadata.has("entrypoints"));

            JsonObject depends = metadata.getAsJsonObject("depends");
            assertNotNull(depends);
            assertEquals("=26.2", depends.get("minecraft").getAsString());
            assertEquals(">=25", depends.get("java").getAsString());
            assertTrue(depends.has("fabricloader"));

            assertEquals(1, metadata.getAsJsonArray("mixins").size());
            assertEquals("ribbits_xaero_entity_icons_compat.mixins.json",
                    metadata.getAsJsonArray("mixins").get(0).getAsString());

            JsonObject suggests = metadata.getAsJsonObject("suggests");
            assertEquals("*", suggests.get("xaerominimap").getAsString());
            assertEquals("*", suggests.get("geckolib").getAsString());
            assertEquals("*", suggests.get("ribbits").getAsString());
        }
    }

    @Test
    void packagedMixinConfigurationNamesTheExactPluginAccessorAndClientTargets()
            throws Exception {
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            JsonObject mixins = json(zip, "ribbits_xaero_entity_icons_compat.mixins.json");
            assertTrue(mixins.get("required").getAsBoolean());
            assertEquals("0.8", mixins.get("minVersion").getAsString());
            assertEquals("dev.resivore.ribbitsxaeroicons.mixin",
                    mixins.get("package").getAsString());
            assertEquals("JAVA_25", mixins.get("compatibilityLevel").getAsString());
            assertEquals("dev.resivore.ribbitsxaeroicons.mixin.RibbitsXaeroIconsMixinPlugin",
                    mixins.get("plugin").getAsString());
            assertEquals(List.of(
                            "RadarIconCacheAccessor",
                            "RadarIconCreatorMixin",
                "RadarIconEntityCacheMixin",
                            "RadarIconManagerMixin",
                            "RadarIconVariantHandlerMixin"),
                    mixins.getAsJsonArray("client").asList().stream()
                            .map(element -> element.getAsString())
                            .toList());
            assertEquals(1, mixins.getAsJsonObject("injectors")
                    .get("defaultRequire").getAsInt());
            assertFalse(mixins.has("mixins"));
            assertFalse(mixins.has("server"));
            assertFalse(mixins.has("refmap"));
        }
    }

    @Test
    void artifactContainsOnlyProjectCodeAndMetadata() throws Exception {
        Set<String> requiredClasses = Set.of(
                "dev/resivore/ribbitsxaeroicons/CompatibilityActivation.class",
                "dev/resivore/ribbitsxaeroicons/RibbitHeadSelector.class",
                "dev/resivore/ribbitsxaeroicons/RibbitCacheVariant.class",
                "dev/resivore/ribbitsxaeroicons/CacheIdentity.class",
                "dev/resivore/ribbitsxaeroicons/ReloadGeneration.class",
                "dev/resivore/ribbitsxaeroicons/TransformSafety.class",
                "dev/resivore/ribbitsxaeroicons/RenderStateGuard.class",
                "dev/resivore/ribbitsxaeroicons/GeoAwareFormPrerenderer.class",
                "dev/resivore/ribbitsxaeroicons/RibbitGeoIconProvider.class",
                "dev/resivore/ribbitsxaeroicons/mixin/RibbitsXaeroIconsMixinPlugin.class",
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconCacheAccessor.class",
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconCreatorMixin.class",
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconVariantHandlerMixin.class",
                "dev/resivore/ribbitsxaeroicons/mixin/RadarIconManagerMixin.class");

        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            List<String> names = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName().replace('\\', '/'))
                    .toList();
            Set<String> classes = new TreeSet<>();
            List<String> forbidden = new ArrayList<>();
            for (String name : names) {
                String folded = name.toLowerCase(Locale.ROOT);
                if (name.endsWith(".class")) {
                    classes.add(name);
                    if (!name.startsWith("dev/resivore/ribbitsxaeroicons/")) {
                        forbidden.add(name);
                    }
                }
                if (name.startsWith("xaero/")
                        || name.startsWith("com/geckolib/")
                        || name.startsWith("com/yungnickyoung/")
                        || name.startsWith("traben/")
                        || name.startsWith("net/minecraft/")
                        || name.startsWith("assets/")
                        || name.startsWith("data/")
                        || name.startsWith("META-INF/jars/")
                        || folded.endsWith(".jar")
                        || folded.endsWith(".geo.json")
                        || folded.endsWith(".png")
                        || folded.endsWith(".refmap.json")) {
                    forbidden.add(name);
                }
            }

            assertTrue(classes.containsAll(requiredClasses),
                    () -> "missing required production classes: " + difference(requiredClasses, classes));
            assertTrue(forbidden.isEmpty(), () -> "forbidden packaged payload: " + forbidden);
        }
    }

    private static Set<String> difference(Set<String> required, Set<String> present) {
        Set<String> result = new TreeSet<>(required);
        result.removeAll(present);
        return result;
    }

    private static JsonObject json(ZipFile zip, String entryName) throws IOException {
        var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
        try (InputStream input = zip.getInputStream(entry);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
