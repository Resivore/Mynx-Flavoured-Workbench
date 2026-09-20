package dev.resivore.slabdecorations;

import net.minecraft.world.level.block.BaseTorchBlock;
import net.penumbra.enderscape.block.VoidTorchBlock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact optional-Enderscape behavior contracts used by the narrow compatibility seams. */
final class EnderscapeReferenceContractTest {
    private static final String ENDERSCAPE_SHA256 =
            "9FCC4F59CA88E91F90E7C7D18289F2F859F20C810EEBCCA924764AA15236C40B";

    @Test
    void exactEnderscapeReferenceExposesTheRequiredSharedContracts() throws Exception {
        Path reference = reference();
        assertEquals(ENDERSCAPE_SHA256, sha256(reference), "Enderscape 3.0.2 reference drifted");

        try (JarFile jar = new JarFile(reference.toFile())) {
            for (String entry : new String[]{
                    "net/penumbra/enderscape/block/DirectionalVegetationBlock.class",
                    "net/penumbra/enderscape/block/VeiledLeafPileBlock.class",
                    "net/penumbra/enderscape/block/VeiledSaplingBlock.class",
                    "net/penumbra/enderscape/block/CelestialChanterelleBlock.class",
                    "net/penumbra/enderscape/block/MurublightChanterelleBlock.class",
                    "net/penumbra/enderscape/block/PuruberryVine.class",
                    "net/penumbra/enderscape/block/PuruberryFlowerBlock.class",
                    "net/penumbra/enderscape/block/UnripePuruberryBlock.class",
                    "net/penumbra/enderscape/block/RipePuruberryBlock.class",
                    "net/penumbra/enderscape/block/VoidTorchBlock.class",
                    "net/penumbra/enderscape/block/VoidWallTorchBlock.class",
                    "net/penumbra/enderscape/block/BulbLanternBlock.class"
            }) {
                assertTrue(jar.getJarEntry(entry) != null, () -> "missing Enderscape class " + entry);
            }

            String directional = classBytes(jar, "net/penumbra/enderscape/block/MurublightChanterelleBlock.class");
            String leafPile = classBytes(jar, "net/penumbra/enderscape/block/VeiledLeafPileBlock.class");
            String vine = classBytes(jar, "net/penumbra/enderscape/block/PuruberryVine.class");
            String fruit = classBytes(jar, "net/penumbra/enderscape/block/RipePuruberryBlock.class");
            String registry = classBytes(jar, "net/penumbra/enderscape/registry/block/EnderscapeBlocks.class");

            assertTrue(directional.contains("DirectionalVegetationBlock") && directional.contains("FACING"),
                    "Murublight must remain on Enderscape's shared directional-vegetation contract");
            assertTrue(leafPile.contains("LAYERS") && leafPile.contains("getStateForPlacement"),
                    "Veiled Leaf Pile must retain native eight-layer stacking behavior");
            assertTrue(vine.contains("AbstractVineBlock") && fruit.contains("PURUBERRY_VINE"),
                    "Puruberry fruit must remain structurally distinguishable from detached fruit");
            assertTrue(registry.contains("LanternBlock") && registry.contains("StandingSignBlock")
                            && registry.contains("CeilingHangingSignBlock"),
                    "Enderscape lanterns and signs must retain the generic surface contracts");
        }
    }

    @Test
    void exactEnderscapeClassHierarchyAndProductionDependencyBoundaryStayNarrow() {
        assertEquals(BaseTorchBlock.class, VoidTorchBlock.class.getSuperclass(),
                "Void Torch's BaseTorchBlock contract is the narrow floor-torch exception");
        assertEquals("net.penumbra.enderscape.block.DirectionalVegetationBlock",
                load("net.penumbra.enderscape.block.MurublightChanterelleBlock").getSuperclass().getName(),
                "Murublight must keep the shared directional-vegetation superclass");
        assertEquals("net.penumbra.enderscape.block.AbstractVineBlock",
                load("net.penumbra.enderscape.block.PuruberryVine").getSuperclass().getName(),
                "Puruberry must keep the shared hanging-vine lifecycle superclass");
        assertFalse(resource("/fabric.mod.json").contains("\"enderscape\""),
                "the optional Enderscape test reference must not become a production dependency");
    }

    private static Path reference() {
        String configured = System.getProperty("enderscapeReferenceJar");
        assertTrue(configured != null && !configured.isBlank(), "missing Enderscape reference property");
        Path reference = Path.of(configured);
        assertTrue(Files.isRegularFile(reference), () -> "Enderscape reference is not a file: " + reference);
        return reference;
    }

    private static String classBytes(JarFile jar, String entry) throws IOException {
        try (InputStream input = jar.getInputStream(jar.getJarEntry(entry))) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, EnderscapeReferenceContractTest.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("missing Enderscape test class " + name, failure);
        }
    }

    private static String resource(String path) {
        try (InputStream input = EnderscapeReferenceContractTest.class.getResourceAsStream(path)) {
            assertTrue(input != null, () -> "missing resource " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new AssertionError("could not read resource " + path, failure);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read != 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
