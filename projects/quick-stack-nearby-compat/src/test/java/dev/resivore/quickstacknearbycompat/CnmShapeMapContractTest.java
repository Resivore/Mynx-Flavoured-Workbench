package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CnmShapeMapContractTest {
    private static final Artifact NIBARU = new Artifact(
            "nibaruReferenceJar",
            3_863_790L,
            "5792A7539E467C2718F5C0D73E4F56D7DD5C589078B8ED66FEF0A194C52226D1",
            "more_slabs_stairs_and_walls",
            "4.2.0+26.2-port-canary40-pale-coverage"
    );
    private static final Artifact CNM_INTEGRATION = new Artifact(
            "cnmIntegrationReferenceJar",
            159_760L,
            "04EB0E22E7F5D9F37EB9BA9E72C1FA3D767BDDED5BF7A7CAA4C060CAA7ACFCF2",
            "cnm_terrain_slabs_compat",
            "0.5.46-nibaru-cnm-canary1.36-pale-coverage"
    );
    private static final Artifact CNM_UPSTREAM = new Artifact(
            "cnmUpstreamReferenceJar",
            759_419L,
            "41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE",
            "clutternomore",
            "2.0.7+26.2"
    );

    @Test
    void exactAcceptedShapeMapRuntimeSetIsUsed() throws Exception {
        assertArtifact(NIBARU);
        assertArtifact(CNM_INTEGRATION);
        assertArtifact(CNM_UPSTREAM);
    }

    @Test
    void exactCnmItemStackHookDelegatesToShapeMapEquivalence() throws Exception {
        try (JarFile jar = new JarFile(CNM_UPSTREAM.path().toFile())) {
            ClassNode shapeMap = readClass(
                    jar,
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap.class"
            );
            assertTrue(hasMethod(
                    shapeMap,
                    "inSameShapeSet",
                    "(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/Item;)Z"
            ));

            ClassNode itemStackMixin = readClass(
                    jar,
                    "dev/tazer/clutternomore/common/mixin/item/ItemStackMixin.class"
            );
            MethodNode equalityHook = method(itemStackMixin, "cnm$isSameItemSameComponents");
            assertTrue(hasCall(
                    equalityHook,
                    "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
                    "getReturnValue"
            ));
            assertTrue(hasCall(
                    equalityHook,
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap",
                    "inSameShapeSet"
            ));
            assertTrue(hasCall(
                    equalityHook,
                    "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
                    "setReturnValue"
            ));
        }
    }

    @Test
    void acceptedCompanionAddsProviderGeometriesAsExactShapeMapEdges() throws Exception {
        try (JarFile jar = new JarFile(CNM_INTEGRATION.path().toFile())) {
            ClassNode orderMixin = readClass(jar, "dev/aero/cnmterraincompat/mixin/ShapeMapOrderMixin.class");
            assertTrue(orderMixin.methods.stream().anyMatch(method -> hasCall(
                    method,
                    "dev/aero/cnmterraincompat/NibaruProviderAdapter",
                    "addExactShapeMapEdges"
            )), "Accepted companion no longer injects its exact provider edges into CNM ShapeMap mappings");

            ClassNode adapter = readClass(jar, "dev/aero/cnmterraincompat/NibaruProviderAdapter.class");
            assertTrue(hasMethod(adapter, "addExactShapeMapEdges", "(Ljava/util/List;)V"));
            assertTrue(adapter.methods.stream().anyMatch(method -> hasCall(
                    method,
                    "dev/tazer/clutternomore/common/shape_map/ShapeMap$Mapping",
                    "<init>"
            )), "Accepted companion no longer constructs exact CNM ShapeMap mappings");
        }
    }

    private static void assertArtifact(Artifact expected) throws Exception {
        Path path = expected.path();
        assertEquals(expected.size(), Files.size(path));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(path));
        assertEquals(expected.sha256(), HexFormat.of().withUpperCase().formatHex(digest.digest()));

        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry("fabric.mod.json");
            assertNotNull(entry, "Missing fabric.mod.json in " + path);
            try (InputStream input = jar.getInputStream(entry)) {
                String metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(metadata.contains("\"id\": \"" + expected.modId() + "\""));
                assertTrue(metadata.contains("\"version\": \"" + expected.version() + "\""));
            }
        }
    }

    private static ClassNode readClass(JarFile jar, String entryName) throws Exception {
        var entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + owner.name + "." + name));
    }

    private static boolean hasMethod(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }

    private static boolean hasCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private record Artifact(
            String systemProperty,
            long size,
            String sha256,
            String modId,
            String version) {
        private Path path() {
            String configured = System.getProperty(systemProperty);
            assertNotNull(configured, "Gradle must provide " + systemProperty);
            return Path.of(configured);
        }
    }
}
