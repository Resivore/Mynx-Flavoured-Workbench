package dev.resivore.croak;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.ZipFile;

/** A reload-scoped snapshot. Only the three verified entries ever reach the client. */
public final class SoldierPack extends AbstractPackResources {
    static final String NS = "croak_soldier_piglins";
    public static final Identifier MODEL = id("geckolib/models/soldier.geo.json");
    static final Identifier ANIMATION = id("geckolib/animations/soldier.animation.json");
    static final Identifier TEXTURE = id("textures/entity/soldier.png");
    private final Map<Identifier, byte[]> files;

    static Identifier id(String path) { return Identifier.fromNamespaceAndPath(NS, path); }

    private SoldierPack(Map<Identifier, byte[]> files) {
        super(new PackLocationInfo(NS, Component.literal("Croak Soldier reference"), PackSource.BUILT_IN, Optional.empty()));
        this.files = files;
    }

    public static Optional<SoldierPack> load() {
        String reference = System.getProperty("croakSoldier.referenceJar", "");
        if (reference.isBlank()) return Optional.empty();
        try (ZipFile zip = new ZipFile(Path.of(reference).toFile())) {
            return Optional.of(new SoldierPack(Map.of(
                MODEL, read(zip, "geo/croakhelper.geo.json", "0c190875c6d5c8fc138383d6327f49507abc1d40a058d6a5000aaf7111b74783"),
                ANIMATION, normalizeEasings(read(zip, "animations/croakhelper.animation.json", "f960274d68860f2e0deb3656ef06ebab1924095bbbfb174b4ad472bcaa833c18")),
                TEXTURE, read(zip, "textures/entities/croakhelper.png", "726d86230a6cd30a8840af4d8955dd5346352d58bff3bc205d0b75dadf0174d9")
            )));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            LogUtils.getLogger().warn("Could not load Croak Soldier reference; using vanilla piglins", e);
            return Optional.empty();
        }
    }

    // GeckoLib 5 uses lowercase registry keys; unknown spellings silently bake as linear.
    private static byte[] normalizeEasings(byte[] source) {
        var animation = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
        normalizeKeys(animation);
        return animation.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void normalizeKeys(JsonElement node) {
        if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(SoldierPack::normalizeKeys);
        } else if (node.isJsonObject()) {
            var object = node.getAsJsonObject();
            if (object.has("easing")) {
                String key = object.get("easing").getAsString();
                object.addProperty("easing", key.toLowerCase(Locale.ROOT));
            }
            object.asMap().values().forEach(SoldierPack::normalizeKeys);
        }
    }

    private static byte[] read(ZipFile zip, String path, String hash) throws IOException, GeneralSecurityException {
        var entry = zip.getEntry("assets/croaks/" + path);
        if (entry == null) throw new IOException("Missing " + path);
        try (var input = zip.getInputStream(entry)) {
            byte[] bytes = input.readNBytes(1 << 20);
            if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(hash))
                throw new IOException("Unexpected Soldier resource: " + path);
            return bytes;
        }
    }

    @Override public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(NS) : Set.of();
    }
    @Override public IoSupplier<InputStream> getRootResource(String... path) { return null; }
    @Override public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        byte[] bytes = files.get(id);
        return type == PackType.CLIENT_RESOURCES && bytes != null ? () -> new ByteArrayInputStream(bytes) : null;
    }
    @Override public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !NS.equals(namespace)) return;
        files.forEach((id, bytes) -> {
            if (prefix.isEmpty() || id.getPath().startsWith(prefix + "/"))
                output.accept(id, () -> new ByteArrayInputStream(bytes));
        });
    }
    @Override public void close() {}
}
