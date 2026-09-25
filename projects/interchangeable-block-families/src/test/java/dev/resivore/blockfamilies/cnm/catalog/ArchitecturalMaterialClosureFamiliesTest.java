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
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.MASONRY_DETAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact C10 integration contract for the separately retained AMC C1 provider. */
final class ArchitecturalMaterialClosureFamiliesTest {
    private static final String NAMESPACE = "architectural_material_closure";
    private static final List<String> MATERIALS = List.of(
            "andesite", "diorite", "granite", "brick", "mossy_stone_brick",
            "cobbled_deepslate", "mud_brick", "dark_prismarine");
    private static final List<String> FORMS = List.of("column", "urn", "moulding", "fence", "frame");
    private static final String C1_SHA256 = "49AE754AFDA6A1FAD959F11E1C51027A5B2DBC00CB5108CF69A70D3264C63999";

    @Test
    void c10AddsOnlyTheEightLiteralC1MasonryDetailFamilies() {
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families(MASONRY_DETAIL);
        assertEquals(8, families.size());
        for (int index = 0; index < MATERIALS.size(); index++) {
            String material = MATERIALS.get(index);
            AuditedShapeFamily family = families.get(index);
            assertEquals(ibfId("cnm/masonry_detail/" + material), family.key());
            assertEquals(ids(material), family.members());
            assertEquals(id(material + "_column"), family.canonicalParent());
        }
    }

    @Test
    void retainedC1ArtifactContainsEveryCatalogedItemAndNativeStonecuttingRecipe() throws Exception {
        Path artifact = Path.of(System.getProperty("amcReferenceJar"));
        assertTrue(Files.isRegularFile(artifact), artifact.toString());
        assertEquals(C1_SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));

        try (JarFile jar = new JarFile(artifact.toFile())) {
            JsonObject metadata;
            JarEntry metadataEntry = jar.getJarEntry("fabric.mod.json");
            assertNotNull(metadataEntry);
            try (var reader = new InputStreamReader(jar.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                metadata = JsonParser.parseReader(reader).getAsJsonObject();
            }
            assertEquals(NAMESPACE, metadata.get("id").getAsString());
            assertEquals("0.1.0-canary1", metadata.get("version").getAsString());

            for (String material : MATERIALS) {
                for (String form : FORMS) {
                    String item = material + "_" + form;
                    assertNotNull(jar.getJarEntry("assets/" + NAMESPACE + "/items/" + item + ".json"), item);
                    assertNotNull(jar.getJarEntry("assets/" + NAMESPACE + "/blockstates/" + item + ".json"), item);
                    assertNotNull(jar.getJarEntry("data/" + NAMESPACE + "/loot_table/blocks/" + item + ".json"), item);
                    assertNotNull(jar.getJarEntry("data/" + NAMESPACE + "/recipe/" + item + "_from_stonecutting.json"), item);
                }
            }
        }
    }

    private static List<Identifier> ids(String material) {
        return FORMS.stream().map(form -> id(material + "_" + form)).toList();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    private static Identifier ibfId(String path) {
        return Identifier.fromNamespaceAndPath("interchangeable_block_families", path);
    }
}
