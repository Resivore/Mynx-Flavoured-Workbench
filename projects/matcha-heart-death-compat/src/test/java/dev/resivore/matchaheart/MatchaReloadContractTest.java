package dev.resivore.matchaheart;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MatchaReloadContractTest {
    @Test
    void effectiveResourcesRetainOneGenerationAndOnlyTheApprovedHeartOverrides() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path projects = Path.of(System.getProperty("workbenchRoot"), "projects");
        String providerId = "matcha_flavoured_data:matcha_flavoured_1_12_5";
        String overlayId = "matcha_heart_death_compat:heart_progression";
        Path upstream = projects.resolve("matcha-flavoured-data/build/resources/main/resourcepacks/matcha_flavoured_1_12_5");
        Path overlay = projects.resolve("matcha-heart-death-compat/build/resources/main/resourcepacks/heart_progression");
        for (boolean reversed : List.of(false, true)) {
            var data = pack(providerId, upstream);
            var heart = pack(overlayId, overlay);
            try (var manager = new MultiPackResourceManager(PackType.SERVER_DATA,
                    reversed ? List.of(heart, data) : List.of(data, heart))) {
                for (String hook : List.of("load", "tick")) {
                    assertEquals(1, manager.getResourceStack(Identifier.parse("minecraft:tags/function/" + hook + ".json")).size());
                }
                for (String function : List.of("detect_death", "hpdown", "set_max_hp", "set_max_hp.macro",
                        "decrease_minimum_hearts", "use_crystal_heart")) {
                    assertEquals(overlayId, manager.getResource(Identifier.parse(
                            "matcha:function/mechanics/heart_container/" + function + ".mcfunction")).orElseThrow().sourcePackId());
                }
                assertEquals(overlayId, manager.getResource(Identifier.parse("matcha:recipe/crafting/crystal_heart.json"))
                        .orElseThrow().sourcePackId());
                assertEquals(providerId, manager.getResource(Identifier.parse("matcha:function/effects/wither/effect.mcfunction"))
                        .orElseThrow().sourcePackId());
                assertTrue(manager.getResource(Identifier.parse("matcha:recipe/crafting/ominous_bottle.json")).isEmpty());
                assertTrue(manager.getResource(Identifier.parse("matcha:villager_trade/librarian/5/crystal_heart.json")).isEmpty());
            }
        }
    }

    private static PathPackResources pack(String id, Path path) {
        return new PathPackResources(new PackLocationInfo(id, Component.literal(id), PackSource.DEFAULT,
                Optional.empty()), path);
    }
}
