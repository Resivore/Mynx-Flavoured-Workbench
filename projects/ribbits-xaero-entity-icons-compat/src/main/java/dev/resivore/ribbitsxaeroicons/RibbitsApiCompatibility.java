package dev.resivore.ribbitsxaeroicons;

import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Probe only directly linked APIs, without loading game classes or pinning a release. */
final class RibbitsApiCompatibility {
    private static final String ROOT = "com/yungnickyoung/minecraft/ribbits/";
    private RibbitsApiCompatibility() {}

    static CompatibilityActivation.Decision verify(ClassLoader loader) {
        try {
            ClassNode tickets = read(loader, "module/DataTicketModule");
            for (String name : new String[] {"DT_RIBBIT_DATA", "DT_PLAYING_INSTRUMENT",
                    "DT_UMBRELLA_FALLING", "DT_IN_RAIN", "DT_IS_PRIDE_RIBBIT"}) {
                if (tickets.fields.stream().noneMatch(f -> f.name.equals(name)
                        && f.desc.equals("Lcom/geckolib/constant/dataticket/DataTicket;")
                        && (f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC))
                            == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)))
                    throw new IOException("missing public static DataTicketModule." + name);
            }
            requireMethod(loader, "data/RibbitData", "getProfession", "()L" + ROOT + "data/RibbitProfession;");
            requireMethod(loader, "data/RibbitProfession", "id", "()Lnet/minecraft/resources/Identifier;");
            // Exact entity/renderer eligibility, populated state, active resources and selected
            // geometry are checked at capture time. No version/size/hash/archive-layout allowlist.
            return new CompatibilityActivation.Decision(true, "required Ribbits and GeckoLib APIs available; Xaero gates matched");
        } catch (IOException | RuntimeException | LinkageError failure) {
            return new CompatibilityActivation.Decision(false, "incompatible Ribbits API: " + failure.getMessage());
        }
    }

    private static ClassNode read(ClassLoader loader, String name) throws IOException {
        String path = ROOT + name + ".class";
        try (var input = loader.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing " + path);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static void requireMethod(ClassLoader loader, String owner, String name, String descriptor) throws IOException {
        var node = read(loader, owner);
        if (node.methods.stream().noneMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)
                && (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC))
            throw new IOException("missing public instance " + owner + "." + name + descriptor);
    }
}
