package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

class RibbitsApiCompatibilityTest {
    private static final String ROOT = "com/yungnickyoung/minecraft/ribbits/";
    @Test void actualRibbitsProvidesUsedApisWithoutClassLoading() {
        var loader = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.yungnickyoung.") || name.startsWith("net.minecraft.") || name.startsWith("com.geckolib."))
                    throw new AssertionError("premature class load: " + name);
                return super.loadClass(name, resolve);
            }
        };
        var result = RibbitsApiCompatibility.verify(loader);
        assertTrue(result.active(), result.reason());
    }
    @Test void missingClassDeclinesWithName() {
        reject("data/RibbitData", null, "missing");
    }
    @Test void missingTicketDeclinesWithName() {
        reject("module/DataTicketModule", node -> node.fields.removeIf(f -> f.name.equals("DT_RIBBIT_DATA")), "DT_RIBBIT_DATA");
    }
    @Test void wrongTicketDescriptorDeclines() {
        reject("module/DataTicketModule", node -> node.fields.stream().filter(f -> f.name.equals("DT_IN_RAIN"))
                .forEach(f -> f.desc = "Ljava/lang/Object;"), "DT_IN_RAIN");
    }
    @Test void instanceTicketDeclines() {
        reject("module/DataTicketModule", node -> node.fields.stream().filter(f -> f.name.equals("DT_IN_RAIN"))
                .forEach(f -> f.access &= ~Opcodes.ACC_STATIC), "DT_IN_RAIN");
    }
    @Test void changedProfessionMethodDeclines() {
        reject("data/RibbitData", node -> node.methods.removeIf(m -> m.name.equals("getProfession")), "getProfession");
    }
    @Test void staticProfessionIdDeclines() {
        reject("data/RibbitProfession", node -> node.methods.stream().filter(m -> m.name.equals("id"))
                .forEach(m -> m.access |= Opcodes.ACC_STATIC), ".id");
    }
    private void reject(String owner, Consumer<ClassNode> mutation, String diagnostic) {
        var loader = new ClassLoader(getClass().getClassLoader()) {
            @Override public InputStream getResourceAsStream(String name) {
                if (!name.equals(ROOT + owner + ".class")) return super.getResourceAsStream(name);
                if (mutation == null) return null;
                try (var input = super.getResourceAsStream(name)) {
                    var node = new ClassNode(); new ClassReader(input).accept(node, 0);
                    mutation.accept(node); var writer = new ClassWriter(0); node.accept(writer);
                    return new ByteArrayInputStream(writer.toByteArray());
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
            }
        };
        var result = RibbitsApiCompatibility.verify(loader);
        assertFalse(result.active());
        assertTrue(result.reason().startsWith("incompatible Ribbits API:"), result.reason());
        assertTrue(result.reason().contains(diagnostic), result.reason());
    }
}
