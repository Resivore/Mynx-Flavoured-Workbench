package dev.resivore.blockfamilies.cnm.catalog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.BUILDING_ACCESSORY;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.MASONRY_DETAIL;
import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.WINDOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** C11's controlled-artifact contract for the local-only AMC C3 provider. */
final class ArchitecturalMaterialClosureFamiliesTest {
    private static final String NAMESPACE = "architectural_material_closure";
    private static final List<String> MATERIALS = List.of(
            "stone", "andesite", "diorite", "granite", "brick", "mossy_stone_brick",
            "cobbled_deepslate", "deepslate", "mud_brick", "blackstone", "prismarine",
            "dark_prismarine", "sandstone", "red_sandstone", "quartz", "nether_brick", "end_brick");
    private static final List<String> DETAILS = List.of("column", "urn", "moulding", "fence", "frame");
    private static final List<String> ACCESSORIES = List.of(
            "running_bond_path", "strewn_rocky_path", "windmill_weave_path", "flagstone_path", "crystal_floor_path",
            "diamond_paving", "basket_weave_paving", "square_paving", "honeycomb_paving", "clover_paving",
            "dumble_paving", "parapet");
    private static final List<String> WINDOWS = List.of(
            "window", "window2", "four_window", "pane_window", "gothic", "arrow_slit", "louvered_shutter");
    private static final String C3_SHA256 = "5C74F1C3442D93D0877067195596344A826D118AA894E339DB7E07B1244E7417";

    @Test
    void c11IntegratesEveryC3MaterialProfileIntoLiteralCategoryFamilies() {
        assertFamilies(MASONRY_DETAIL, "cnm/masonry_detail/", DETAILS);
        assertFamilies(BUILDING_ACCESSORY, "cnm/building_accessory/masonry/", ACCESSORIES);
        assertFamilies(WINDOW, "cnm/window/masonry/", WINDOWS);
    }

    @Test
    void controlledC3ArtifactContainsEveryCatalogedLocalMaterialization() throws Exception {
        Path artifact = Path.of(System.getProperty("amcReferenceJar"));
        assertTrue(Files.isRegularFile(artifact), artifact.toString());
        assertEquals(C3_SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        try (JarFile jar = new JarFile(artifact.toFile())) {
            JarEntry metadataEntry = jar.getJarEntry("fabric.mod.json");
            assertNotNull(metadataEntry);
            JsonObject metadata;
            try (var reader = new InputStreamReader(jar.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                metadata = JsonParser.parseReader(reader).getAsJsonObject();
            }
            assertEquals(NAMESPACE, metadata.get("id").getAsString());
            assertEquals("0.1.0-canary3", metadata.get("version").getAsString());
            for (String material : MATERIALS) for (String form : forms()) {
                String item = material + "_" + form;
                assertNotNull(jar.getJarEntry("assets/" + NAMESPACE + "/items/" + item + ".json"), item);
                assertNotNull(jar.getJarEntry("assets/" + NAMESPACE + "/blockstates/" + item + ".json"), item);
                assertNotNull(jar.getJarEntry("data/" + NAMESPACE + "/loot_table/blocks/" + item + ".json"), item);
            }
        }
    }

    @Test
    void runtimeRequiresOnlyAnInstalledAmcProviderWhileValidationPinsTheControlledC3Artifact() throws Exception {
        try (var stream = ArchitecturalMaterialClosureFamiliesTest.class.getClassLoader()
                .getResourceAsStream("fabric.mod.json")) {
            assertNotNull(stream);
            JsonObject metadata = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("*", metadata.getAsJsonObject("depends")
                    .get("architectural_material_closure").getAsString());
        }
    }

    private static void assertFamilies(AuditedShapeFamily.Category category, String keyPrefix, List<String> forms) {
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families(category).stream()
                .filter(family -> family.key().getPath().startsWith(keyPrefix)).toList();
        assertEquals(MATERIALS.size(), families.size());
        for (String material : MATERIALS) {
            AuditedShapeFamily family = families.stream()
                    .filter(candidate -> candidate.key().equals(ibfId(keyPrefix + material))).findFirst().orElseThrow();
            assertEquals(forms.stream().map(form -> id(material + "_" + form)).toList(), family.members());
        }
    }

    private static List<String> forms() {
        return java.util.stream.Stream.of(DETAILS, ACCESSORIES, WINDOWS).flatMap(List::stream).toList();
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(NAMESPACE, path); }
    private static Identifier ibfId(String path) { return Identifier.fromNamespaceAndPath("interchangeable_block_families", path); }
}
