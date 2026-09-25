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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import static dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily.Category.MASONRY_DETAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Controlled C4 evidence for IBF's literal provider-first C12 matrix. */
final class ArchitecturalMaterialClosureFamiliesTest {
    private static final String AMC = "architectural_material_closure";
    private static final String C4_SHA256 = "B7344EC4B53305CDFEB5CFD514E2570B919ECCA5432D6C9ED11B7E1846A90FD5";
    private static final List<String> MATERIALS = List.of(
            "stone", "andesite", "diorite", "granite", "brick", "mossy_stone",
            "cobbled_deepslate", "deepslate", "mud_brick", "blackstone", "prismarine",
            "dark_prismarine", "sandstone", "red_sandstone", "quartz", "nether_brick", "end_brick");

    @Test
    void c12CoversTheCompleteProviderFirstMasonryMatrixExactlyOnce() {
        List<AuditedShapeFamily> masonry = AuditedShapeFamilies.families(MASONRY_DETAIL);
        assertEquals(17, masonry.size());
        assertEquals(MATERIALS, masonry.stream().map(family -> family.key().getPath()
                .substring("cnm/masonry_detail/".length())).toList());
        assertEquals(646, masonry.stream().mapToInt(family -> family.members().size()).sum());
        assertTrue(masonry.stream().allMatch(family -> family.members().size() == 38));
        Set<Identifier> cells = new HashSet<>();
        masonry.forEach(family -> {
            assertFalse(family.canonicalParent().getNamespace().equals(AMC), family.key().toString());
            family.members().forEach(cell -> assertTrue(cells.add(cell), cell.toString()));
        });
        assertEquals(646, cells.size());
        assertEquals(409, cells.stream().filter(cell -> !cell.getNamespace().equals(AMC)).count());
        assertEquals(237, cells.stream().filter(cell -> cell.getNamespace().equals(AMC)).count());
    }

    @Test
    void c12UsesOnlyC4GapsAndEveryProviderReferenceExists() throws Exception {
        Set<Identifier> cells = AuditedShapeFamilies.families(MASONRY_DETAIL).stream()
                .flatMap(family -> family.members().stream()).collect(java.util.stream.Collectors.toSet());
        Path c4 = Path.of(System.getProperty("amcReferenceJar"));
        assertEquals(C4_SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(c4))));

        Set<Identifier> c4Items;
        try (JarFile jar = new JarFile(c4.toFile())) {
            c4Items = jar.stream().filter(entry -> entry.getName().startsWith("assets/" + AMC + "/items/")
                            && entry.getName().endsWith(".json"))
                    .map(entry -> Identifier.fromNamespaceAndPath(AMC, entry.getName()
                            .substring(("assets/" + AMC + "/items/").length(), entry.getName().length() - 5)))
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(237, c4Items.size());
            assertEquals("0.1.0-canary4", metadata(jar).get("version").getAsString());
        }
        Set<Identifier> catalogAmc = cells.stream().filter(cell -> cell.getNamespace().equals(AMC))
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(c4Items, catalogAmc, "C4 owns precisely the only AMC cells in C12");

        Map<String, String> providerJars = Map.of(
                "minecraft", System.getProperty("minecraftReferenceJar"),
                "bbb", System.getProperty("bbbReferenceJar"),
                "mcwpaths", System.getProperty("macawsPathsReferenceJar"),
                "mcwwindows", System.getProperty("macawsWindowsReferenceJar"));
        for (Identifier providerCell : cells.stream().filter(cell -> !cell.getNamespace().equals(AMC)).toList()) {
            String jarPath = providerJars.get(providerCell.getNamespace());
            assertNotNull(jarPath, providerCell.toString());
            try (JarFile jar = new JarFile(Path.of(jarPath).toFile())) {
                assertNotNull(jar.getJarEntry("assets/" + providerCell.getNamespace()
                        + "/items/" + providerCell.getPath() + ".json"), providerCell.toString());
            }
        }
    }

    @Test
    void runtimeRequiresUnrestrictedAmcPresenceWhileC4IsValidationEvidenceOnly() throws Exception {
        try (var stream = ArchitecturalMaterialClosureFamiliesTest.class.getClassLoader()
                .getResourceAsStream("fabric.mod.json")) {
            JsonObject metadata = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("*", metadata.getAsJsonObject("depends").get(AMC).getAsString());
        }
    }

    private static JsonObject metadata(JarFile jar) throws Exception {
        try (var reader = new InputStreamReader(jar.getInputStream(jar.getJarEntry("fabric.mod.json")),
                StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
