package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class GeckoApiActivationTest {
    private static final String RENDERER = "com.geckolib.renderer.GeoEntityRenderer";

    @Test
    void actualGeckoBinaryProvidesRequiredApis() {
        var decision = RuntimeCompatibility.verifyGeckoApi(getClass().getClassLoader());
        assertTrue(decision.active(), decision.reason());
    }

    @Test
    void apiProbeNeverLoadsGeckoOrMinecraftClasses() {
        var loader = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.geckolib.") || name.startsWith("net.minecraft.")) {
                    throw new AssertionError("premature class loading: " + name);
                }
                return super.loadClass(name, resolve);
            }
        };
        var decision = RuntimeCompatibility.verifyGeckoApi(loader);
        assertTrue(decision.active(), decision.reason());
    }

    @Test
    void missingGeckoClassFailsClosedWithItsName() {
        var loader = new ClassLoader(getClass().getClassLoader()) {
            @Override public InputStream getResourceAsStream(String name) {
                if (name.equals(RENDERER.replace('.', '/') + ".class")) return null;
                return super.getResourceAsStream(name);
            }
        };
        assertRejected(loader, "missing", "GeoEntityRenderer.class");
    }

    @Test
    void missingRendererMethodFailsClosedWithItsName() {
        assertRejected(rendererFixture(null, false), "missing public instance GeckoLib method", "getGeoModel");
    }

    @Test
    void changedReturnTypeFailsClosed() {
        assertRejected(rendererFixture("Ljava/lang/Object;", false),
                "missing public instance GeckoLib method", "getGeoModel");
    }

    @Test
    void staticReplacementFailsClosed() {
        assertRejected(rendererFixture("Lcom/geckolib/model/GeoModel;", true),
                "missing public instance GeckoLib method", "getGeoModel");
    }

    @Test
    void missingFabricGeckoModHasClearDiagnostic() {
        var decision = CompatibilityActivation.evaluate(CompatibilityActivation.SUPPORTED_XAERO,
                CompatibilityActivation.SUPPORTED_XAEROLIB, null, CompatibilityActivation.SUPPORTED_RIBBITS);
        assertFalse(decision.active());
        assertEquals("missing Fabric mod geckolib", decision.reason());
    }

    // Synthetic rejection fixtures test diagnostics only; positive evidence uses the actual input JAR.
    private ClassLoader rendererFixture(String result, boolean isStatic) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, RENDERER.replace('.', '/'),
                null, "java/lang/Object", null);
        if (result != null) {
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                    "getGeoModel", "()" + result, null, null);
            method.visitCode();
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(1, isStatic ? 0 : 1);
            method.visitEnd();
        }
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(getClass().getClassLoader()) {
            @Override public InputStream getResourceAsStream(String name) {
                if (name.equals(RENDERER.replace('.', '/') + ".class")) {
                    return new ByteArrayInputStream(bytes);
                }
                return super.getResourceAsStream(name);
            }
        };
    }

    private static void assertRejected(ClassLoader loader, String kind, String member) {
        var decision = RuntimeCompatibility.verifyGeckoApi(loader);
        assertFalse(decision.active(), decision.reason());
        assertTrue(decision.reason().startsWith("incompatible GeckoLib API:"), decision.reason());
        assertTrue(decision.reason().contains(kind), decision.reason());
        assertTrue(decision.reason().contains(member), decision.reason());
    }
}
