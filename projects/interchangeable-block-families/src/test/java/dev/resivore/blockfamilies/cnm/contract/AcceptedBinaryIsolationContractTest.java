package dev.resivore.blockfamilies.cnm.contract;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptedBinaryIsolationContractTest {
    private static final String CNM_JAR_PROPERTY = "cnmUpstreamReferenceJar";
    private static final String NIBARU_INTEGRATION_JAR_PROPERTY = "cnmIntegrationReferenceJar";
    private static final long ACCEPTED_NIBARU_INTEGRATION_SIZE = 189071L;
    private static final String ACCEPTED_NIBARU_INTEGRATION_SHA256 =
            "0E84FB7B8C69E31C3C22A592D0667DB66C2918C8FD9F461BD2C216D377722E69";

    @Test
    void exactCnmBinaryStillOwnsShapeAffinityAndTransmuteCopyWhenProvided() throws Exception {
        Path jarPath = configuredPath(CNM_JAR_PROPERTY);
        Properties contract = AuditFixtures.contract();
        assertExactArtifact(jarPath,
                Long.parseLong(contract.getProperty("cnm.jar.size")),
                contract.getProperty("cnm.jar.sha256"));

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String shapeMap = classConstants(jar,
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap.class");
            assertTrue(shapeMap.contains("inSameShapeSet"));
            assertTrue(shapeMap.contains("transmuteCopy"));
            assertTrue(shapeMap.contains("setMappings"));

            String itemStackMixin = classConstants(jar,
                    "dev/tazer/clutternomore/common/mixin/item/ItemStackMixin.class");
            assertTrue(itemStackMixin.contains("inSameShapeSet"));
            assertTrue(itemStackMixin.contains("isSameItemSameComponents"));
        }
    }

    @Test
    void acceptedNibaruShapeMapAdapterRemainsByteForByteUntouchedWhenProvided() throws Exception {
        Path jarPath = configuredPath(NIBARU_INTEGRATION_JAR_PROPERTY);
        assertExactArtifact(jarPath,
                ACCEPTED_NIBARU_INTEGRATION_SIZE,
                ACCEPTED_NIBARU_INTEGRATION_SHA256);

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String adapter = classConstants(jar,
                    "dev/aero/cnmterraincompat/NibaruProviderAdapter.class");
            for (String required : new String[]{
                    "addExactShapeMapEdges",
                    "canonicalParent",
                    "effectiveSlabSource",
                    "effectiveStairSource",
                    "nativeWall",
                    "VERTICAL_SLAB",
                    "STEP",
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap$Mapping"
            }) {
                assertTrue(adapter.contains(required), "Accepted adapter lost " + required);
            }
            for (String forbidden : new String[]{
                    "interchangeable_block_families",
                    "mcwdoors",
                    "mcwpaths",
                    "mcwtrpdoors",
                    "mcwwindows",
                    "dramaticdoors"
            }) {
                assertFalse(adapter.contains(forbidden),
                        "Accepted Nibaru adapter unexpectedly absorbed IBF mapping ownership: " + forbidden);
            }
        }
    }

    private static Path configuredPath(String property) {
        String value = System.getProperty(property);
        Assumptions.assumeTrue(value != null && !value.isBlank(),
                () -> "Set -D" + property + " to run the optional exact-binary contract");
        Path path = Path.of(value);
        Assumptions.assumeTrue(Files.isRegularFile(path), () -> "Missing configured JAR " + path);
        return path;
    }

    private static void assertExactArtifact(Path path, long expectedSize, String expectedSha256) throws Exception {
        assertEquals(expectedSize, Files.size(path), path.toString());
        assertEquals(expectedSha256, AuditFixtures.sha256(Files.readAllBytes(path)), path.toString());
    }

    private static String classConstants(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
