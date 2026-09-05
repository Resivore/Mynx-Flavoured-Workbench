package dev.resivore.ribbitsxaeroicons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

/** Verifies dependency gates once before mixin application, without loading game classes. */
public final class RuntimeCompatibility {
    private static final String XAEROLIB_NESTED_PATH =
            "META-INF/jars/xaerolib-fabric-26.2-1.7.1.jar";
    private static final Set<String> XAEROLIB_PARENTS =
            Set.of("xaerominimap", "xaeroworldmap");

    private RuntimeCompatibility() {
    }

    public static CompatibilityActivation.Decision evaluateLoadedMods() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            // Ribbits and GeckoLib metadata are not archive fingerprints or version allowlists.
            CompatibilityActivation.Decision identities = CompatibilityActivation.evaluate(
                    identity(loader, CompatibilityActivation.SUPPORTED_XAERO.modId()),
                    nestedXaeroLibIdentity(loader),
                    loader.getModContainer("geckolib").map(container ->
                            new CompatibilityActivation.DependencyIdentity(
                                    "geckolib", version(container), 0L, "")).orElse(null),
                    loader.getModContainer("ribbits").map(container ->
                            new CompatibilityActivation.DependencyIdentity(
                                    "ribbits", version(container), 0L, "")).orElse(null));
            if (!identities.active()) return identities;
            ClassLoader classes = RuntimeCompatibility.class.getClassLoader();
            var gecko = verifyGeckoApi(classes);
            return gecko.active() ? RibbitsApiCompatibility.verify(classes) : gecko;
        } catch (Throwable failure) {
            return new CompatibilityActivation.Decision(
                    false, "could not verify required dependency origins: "
                            + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    /** Read only the used API signatures: loading game classes during mixin setup is unsafe. */
    static CompatibilityActivation.Decision verifyGeckoApi(ClassLoader loader) {
        try {
            String model = "Lcom/geckolib/model/GeoModel;";
            String state = "Lcom/geckolib/renderer/base/GeoRenderState;";
            String ticket = "Lcom/geckolib/constant/dataticket/DataTicket;";
            String id = "Lnet/minecraft/resources/Identifier;";
            requireGeckoMethod(loader, "renderer/GeoEntityRenderer", "getGeoModel", "()" + model);
            requireGeckoMethod(loader, "renderer/GeoEntityRenderer", "getRenderType",
                    "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;" + id
                            + ")Lnet/minecraft/client/renderer/rendertype/RenderType;");
            requireGeckoMethod(loader, "model/GeoModel", "getModelResource", "(" + state + ")" + id);
            requireGeckoMethod(loader, "model/GeoModel", "getTextureResource", "(" + state + ")" + id);
            requireGeckoMethod(loader, "model/GeoModel", "getBakedModel",
                    "(" + id + ")Lcom/geckolib/cache/model/BakedGeoModel;");
            requireGeckoMethod(loader, "renderer/base/GeoRenderState", "getGeckolibData",
                    "(" + ticket + ")Ljava/lang/Object;");
            requireGeckoMethod(loader, "renderer/base/GeoRenderState", "hasGeckolibData", "(" + ticket + ")Z");
            requireGeckoMethod(loader, "cache/model/BakedGeoModel", "isMissingno", "()Z");
            requireGeckoMethod(loader, "cache/model/BakedGeoModel", "topLevelBones",
                    "()[Lcom/geckolib/cache/model/GeoBone;");
            ClassNode cuboid = geckoClass(loader, "cache/model/cuboid/CuboidGeoBone");
            if (cuboid.fields.stream().noneMatch(field -> field.name.equals("cubes")
                    && field.desc.equals("[Lcom/geckolib/cache/model/cuboid/GeoCube;")
                    && (field.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC)) {
                throw new IOException("CuboidGeoBone.cubes requires public instance GeoCube[]");
            }
            requireGeckoMethod(loader, "cache/model/cuboid/GeoCube", "render",
                    "(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V");
            return new CompatibilityActivation.Decision(true, "required GeckoLib APIs available; dependency gates matched");
        } catch (IOException | RuntimeException | LinkageError failure) {
            return new CompatibilityActivation.Decision(false, "incompatible GeckoLib API: "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    private static ClassNode geckoClass(ClassLoader loader, String name) throws IOException {
        String path = "com/geckolib/" + name + ".class";
        try (InputStream input = loader.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing " + path);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static void requireGeckoMethod(ClassLoader loader, String owner, String name, String descriptor)
            throws IOException {
        ClassNode node = geckoClass(loader, owner);
        if (node.methods.stream().noneMatch(method -> method.name.equals(name)
                && method.desc.equals(descriptor)
                && (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC)) {
            throw new IOException("missing public instance GeckoLib method " + owner + "." + name + descriptor);
        }
    }

    private static CompatibilityActivation.DependencyIdentity identity(
            FabricLoader loader, String modId) throws IOException, NoSuchAlgorithmException {
        ModContainer container = loader.getModContainer(modId)
                .orElseThrow(() -> new IOException("missing Fabric mod " + modId));
        Path origin = regularOrigin(container, modId);
        return new CompatibilityActivation.DependencyIdentity(
                modId, version(container), Files.size(origin), sha256(origin));
    }

    private static CompatibilityActivation.DependencyIdentity nestedXaeroLibIdentity(
            FabricLoader loader) throws IOException, NoSuchAlgorithmException {
        ModContainer xaeroLib = loader.getModContainer("xaerolib")
                .orElseThrow(() -> new IOException("missing Fabric mod xaerolib"));
        ModOrigin origin = xaeroLib.getOrigin();
        if (origin.getKind() != ModOrigin.Kind.NESTED
                || !XAEROLIB_PARENTS.contains(origin.getParentModId())
                || !XAEROLIB_NESTED_PATH.equals(origin.getParentSubLocation())) {
            throw new IOException("xaerolib is not the exact audited nested dependency origin");
        }

        ModContainer parent = loader.getModContainer(origin.getParentModId())
                .orElseThrow(() -> new IOException(
                        "missing XaeroLib parent mod " + origin.getParentModId()));
        Path parentOrigin = regularOrigin(parent, origin.getParentModId());
        try (ZipFile archive = new ZipFile(parentOrigin.toFile())) {
            ZipEntry entry = archive.getEntry(XAEROLIB_NESTED_PATH);
            if (entry == null || entry.isDirectory()) {
                throw new IOException("loaded XaeroLib nested entry is missing from " + parentOrigin);
            }
            try (InputStream input = archive.getInputStream(entry)) {
                Fingerprint fingerprint = fingerprint(input);
                return new CompatibilityActivation.DependencyIdentity(
                        "xaerolib", version(xaeroLib), fingerprint.size(), fingerprint.sha256());
            }
        }
    }

    private static Path regularOrigin(ModContainer container, String modId) throws IOException {
        if (container.getOrigin().getKind() != ModOrigin.Kind.PATH) {
            throw new IOException(modId + " origin is not one regular archive");
        }
        List<Path> originPaths = container.getOrigin().getPaths();
        if (originPaths.size() != 1) {
            throw new IOException(modId + " has " + originPaths.size() + " origin paths");
        }
        Path origin = originPaths.getFirst().toRealPath();
        if (!Files.isRegularFile(origin)) {
            throw new IOException(modId + " origin is not one regular archive: " + origin);
        }
        return origin;
    }

    private static String version(ModContainer container) {
        return container.getMetadata().getVersion().getFriendlyString();
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        try (InputStream input = Files.newInputStream(path)) {
            return fingerprint(input).sha256();
        }
    }

    private static Fingerprint fingerprint(InputStream input)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long size = 0;
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                digest.update(buffer, 0, read);
                size += read;
            }
        }
        return new Fingerprint(size, HexFormat.of().formatHex(digest.digest()));
    }

    private record Fingerprint(long size, String sha256) {
    }
}
