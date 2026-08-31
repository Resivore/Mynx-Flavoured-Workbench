package dev.resivore.matchavillagerestoration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class VillagePackPrecedenceTest {
    private static final List<String> VILLAGE_IDS = List.of(
            "village_plains",
            "village_desert",
            "village_savanna",
            "village_snowy",
            "village_taiga");

    @Test
    @Order(1)
    void exactBuiltInPackWinsOverALaterConflictingMatchaLikePack(@TempDir Path tempDir)
            throws IOException {
        Path conflictingRoot = conflictingPack(tempDir.resolve("matcha-like"));

        try (var ordinaryOrder = manager(
                pack("ordinary-fabric-builtin", builtInRoot()),
                pack("file/Matcha_Flavoured_1_12.zip", conflictingRoot))) {
            for (String villageId : VILLAGE_IDS) {
                assertEquals(
                        "file/Matcha_Flavoured_1_12.zip",
                        ordinaryOrder.getResource(resourceId(villageId)).orElseThrow().sourcePackId(),
                        "Fabric's ordinary built-in-before-user order must reproduce the conflict");
            }
        }

        try (var effective = manager(
                pack(MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID, builtInRoot()),
                pack("file/Matcha_Flavoured_1_12.zip", conflictingRoot))) {
            for (String villageId : VILLAGE_IDS) {
                var resource = effective.getResource(resourceId(villageId)).orElseThrow();
                assertEquals(
                        MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                        resource.sourcePackId());
                assertEquals(
                        Files.readString(
                                builtInRoot().resolve("data/minecraft/worldgen/structure/"
                                        + villageId + ".json"),
                                UTF_8),
                        readResource(resource));
            }
        }

        List<PackResources> selected = new ArrayList<>();
        selected.add(pack(
                MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                builtInRoot()));
        selected.add(pack("file/Matcha_Flavoured_1_12.zip", conflictingRoot));
        assertEquals(
                MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                VillagePackPrecedence.prioritize(selected).getLast().packId());
    }

    @Test
    @Order(2)
    void promotionPreservesEveryOtherPacksRelativeOrder(@TempDir Path tempDir) throws IOException {
        Path empty = Files.createDirectories(tempDir.resolve("empty"));
        PackResources first = pack("vanilla", empty);
        PackResources restoration = pack(
                MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                builtInRoot());
        PackResources second = pack("mod-resources", empty);
        PackResources matcha = pack("file/Matcha_Flavoured_1_12.zip", empty);
        PackResources last = pack("another-user-pack", empty);

        List<PackResources> prioritized = VillagePackPrecedence.prioritize(
                List.of(first, restoration, second, matcha, last));
        assertEquals(
                List.of(
                        "vanilla",
                        "mod-resources",
                        "file/Matcha_Flavoured_1_12.zip",
                        "another-user-pack",
                        MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID),
                prioritized.stream().map(PackResources::packId).toList());
    }

    @Test
    @Order(3)
    void alreadyHighestOrAbsentPackNeedsNoMutation(@TempDir Path tempDir) throws IOException {
        Path empty = Files.createDirectories(tempDir.resolve("empty"));
        List<PackResources> absent = List.of(pack("vanilla", empty), pack("user", empty));
        assertSame(absent, VillagePackPrecedence.prioritize(absent));

        List<PackResources> alreadyHighest = List.of(
                pack("user", empty),
                pack(MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID, builtInRoot()));
        assertSame(alreadyHighest, VillagePackPrecedence.prioritize(alreadyHighest));
    }

    @Test
    @Order(4)
    void duplicateRestorationPackIdsFailClosed() {
        assertThrows(IllegalStateException.class, () -> VillagePackPrecedence.prioritize(List.of(
                pack(MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID, builtInRoot()),
                pack(MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID, builtInRoot()))));
    }

    @Test
    @Order(5)
    void fabricRegistrationIsDiscoverableRequiredAndEffective(@TempDir Path tempDir)
            throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        new MatchaVanillaVillageRestoration().onInitialize();

        List<Pack> discovered = new ArrayList<>();
        ResourceLoaderImpl.registerBuiltinResourcePacks(PackType.SERVER_DATA, discovered::add);
        List<Pack> matches = discovered.stream()
                .filter(pack -> MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID
                        .toString().equals(pack.getId()))
                .toList();
        assertEquals(1, matches.size(), "Fabric must discover the registered built-in pack exactly once");
        Pack profile = matches.getFirst();
        assertEquals(net.minecraft.server.packs.repository.Pack.Position.TOP,
                profile.getDefaultPosition());
        assertEquals(true, profile.isRequired(), "ALWAYS_ENABLED must create a required pack");
        assertEquals(true, profile.getPackSource().shouldAddAutomatically(),
                "Fabric's built-in mod pack source must add the pack automatically");

        Path conflictingRoot = conflictingPack(tempDir.resolve("matcha-like"));
        try (var effective = manager(
                profile.open(),
                pack("file/Matcha_Flavoured_1_12.zip", conflictingRoot))) {
            assertEquals(
                    MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                    effective.listPacks().toList().getLast().packId());
            for (String villageId : VILLAGE_IDS) {
                var resource = effective.getResource(resourceId(villageId)).orElseThrow();
                assertEquals(
                        MatchaVanillaVillageRestoration.BUILTIN_PACK_RESOURCE_ID,
                        resource.sourcePackId());
                assertEquals(
                        Files.readString(
                                builtInRoot().resolve("data/minecraft/worldgen/structure/"
                                        + villageId + ".json"),
                                UTF_8),
                        readResource(resource));
            }
        }
    }

    private static MultiPackResourceManager manager(PackResources... packs) {
        return new MultiPackResourceManager(PackType.SERVER_DATA, List.of(packs));
    }

    private static PathPackResources pack(String id, Path root) {
        return new PathPackResources(
                new PackLocationInfo(
                        id,
                        Component.literal(id),
                        PackSource.DEFAULT,
                        Optional.empty()),
                root);
    }

    private static Identifier resourceId(String villageId) {
        return Identifier.fromNamespaceAndPath(
                "minecraft", "worldgen/structure/" + villageId + ".json");
    }

    private static String readResource(net.minecraft.server.packs.resources.Resource resource)
            throws IOException {
        try (var reader = resource.openAsReader()) {
            return reader.lines().collect(Collectors.joining("\n", "", "\n"));
        }
    }

    private static Path builtInRoot() {
        return projectRoot()
                .resolve("src/main/resources/resourcepacks/vanilla_villages")
                .toAbsolutePath()
                .normalize();
    }

    private static Path conflictingPack(Path root) throws IOException {
        Path structures = Files.createDirectories(
                root.resolve("data/minecraft/worldgen/structure"));
        for (String villageId : VILLAGE_IDS) {
            Files.writeString(
                    structures.resolve(villageId + ".json"),
                    "{\"matcha_like_conflict\":\"" + villageId + "\"}\n",
                    UTF_8);
        }
        return root;
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }
}
