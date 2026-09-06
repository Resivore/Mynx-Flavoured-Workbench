package com.crispytwig.naturalist.data;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.client.NaturalistRenderEntityLookup;
import com.crispytwig.naturalist.client.NaturalistParrotRenderStateLookup;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.world.entity.Marker;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;

class ResourceCodecTest {
    private static RegistryOps<JsonElement> ops;
    private static final Path ROOT = Path.of(System.getProperty("naturalist.stagedResources"));

    @BeforeAll
    static void bootstrapRegistries() throws Exception {
        Thread.currentThread().setContextClassLoader(Naturalist.class.getClassLoader());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Naturalist.bootstrap();
        var vanilla = VanillaRegistries.createLookup();
        // Production item defaults resolve the private song from the data registry.
        var songs = new MappedRegistry<JukeboxSong>(Registries.JUKEBOX_SONG, Lifecycle.stable());
        vanilla.lookupOrThrow(Registries.JUKEBOX_SONG).listElements().forEach(
                song -> Registry.register(songs, song.key(), song.value()));
        try (var files = Files.list(ROOT.resolve("data/naturalist/jukebox_song"))) {
            for (var path : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (var reader = Files.newBufferedReader(path)) {
                    Registry.register(songs, Naturalist.location(path.getFileName().toString().replace(".json", "")),
                            JukeboxSong.DIRECT_CODEC.parse(vanilla.createSerializationContext(JsonOps.INSTANCE),
                                    JsonParser.parseReader(reader)).getOrThrow());
                }
            }
        }
        songs.freeze();
        var registries = HolderLookup.Provider.create(Stream.concat(vanilla.listRegistries()
                .filter(registry -> !registry.key().equals(Registries.JUKEBOX_SONG)), Stream.of(songs)));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
        var info = new PackLocationInfo("codec-test", Component.literal("Codec test"), PackSource.DEFAULT, Optional.empty());
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(
                new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft").build(info),
                new PathPackResources(info, ROOT)))) {
            TagLoader.loadTagsForRegistry(resources, (MappedRegistry<Item>) BuiltInRegistries.ITEM);
        }
        ops = registries.createSerializationContext(JsonOps.INSTANCE);
    }

    private Stream<DynamicTest> resources(String folder, int expectedCount, Codec<?> codec) throws Exception {
        List<Path> paths;
        try (var files = Files.walk(ROOT.resolve("data/naturalist/" + folder))) {
            paths = files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
        assertEquals(expectedCount, paths.size(), folder + " corpus");
        return paths.stream().map(path -> DynamicTest.dynamicTest(ROOT.relativize(path).toString(), () -> {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                var result = codec.parse(ops, JsonParser.parseReader(reader));
                assertTrue(result.isSuccess(), () -> result.error().orElseThrow().message());
            }
        }));
    }

    @TestFactory Stream<DynamicTest> recipes() throws Exception { return resources("recipe", 7, Recipe.CODEC); }
    @TestFactory Stream<DynamicTest> advancements() throws Exception { return resources("advancement", 8, Advancement.CODEC); }
    @TestFactory Stream<DynamicTest> lootTables() throws Exception { return resources("loot_table", 56, LootTable.DIRECT_CODEC); }

    @Test
    void renderLookupKeepsEntityAndPartialTickTogetherAcrossStateReuse() {
        var state = new EntityRenderState();
        var otherState = new EntityRenderState();
        // The bridge only stores identity; these test tokens never execute entity
        // behavior. Avoid starting or faking a world solely to obtain two objects.
        var identities = new ObjenesisStd().getInstantiatorOf(Marker.class);
        var first = identities.newInstance();
        var second = identities.newInstance();
        assertNull(NaturalistRenderEntityLookup.source(state));
        assertEquals(0.0F, NaturalistRenderEntityLookup.partialTick(state));
        NaturalistRenderEntityLookup.remember(null, first, 0.8F);
        assertNull(NaturalistRenderEntityLookup.source(null));
        NaturalistRenderEntityLookup.remember(state, first, 0.25F);
        NaturalistRenderEntityLookup.remember(otherState, second, 0.75F);
        assertSame(first, NaturalistRenderEntityLookup.source(state));
        assertEquals(0.25F, NaturalistRenderEntityLookup.partialTick(state));
        NaturalistRenderEntityLookup.remember(state, second, 0.5F);
        assertSame(second, NaturalistRenderEntityLookup.source(state));
        assertEquals(0.5F, NaturalistRenderEntityLookup.partialTick(state));
        assertSame(second, NaturalistRenderEntityLookup.source(otherState));
        assertEquals(0.75F, NaturalistRenderEntityLookup.partialTick(otherState));
    }

    @Test
    void shoulderFlightMarkerStaysWithItsQueuedParrotState() {
        var flying = new ParrotRenderState();
        var perched = new ParrotRenderState();
        assertFalse(NaturalistParrotRenderStateLookup.isFlyingShoulder(flying));
        NaturalistParrotRenderStateLookup.markFlyingShoulder(flying);
        assertTrue(NaturalistParrotRenderStateLookup.isFlyingShoulder(flying));
        assertFalse(NaturalistParrotRenderStateLookup.isFlyingShoulder(perched));
    }

}
