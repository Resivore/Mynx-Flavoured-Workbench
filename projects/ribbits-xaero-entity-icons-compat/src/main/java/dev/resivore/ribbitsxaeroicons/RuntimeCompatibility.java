package dev.resivore.ribbitsxaeroicons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

/** Resolves and hashes the actual Fabric origins once, before any unsafe mixin can apply. */
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
            return CompatibilityActivation.evaluate(
                    identity(loader, CompatibilityActivation.SUPPORTED_XAERO.modId()),
                    nestedXaeroLibIdentity(loader),
                    identity(loader, CompatibilityActivation.SUPPORTED_GECKOLIB.modId()),
                    identity(loader, CompatibilityActivation.SUPPORTED_RIBBITS.modId()));
        } catch (Throwable failure) {
            return new CompatibilityActivation.Decision(
                    false, "could not verify exact dependency origins: "
                            + failure.getClass().getSimpleName() + ": " + failure.getMessage());
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
