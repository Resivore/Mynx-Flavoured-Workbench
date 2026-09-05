package com.crispytwig.naturalist.lifecycle;

import com.crispytwig.naturalist.Naturalist;
import com.crispytwig.naturalist.server.advancement.NaturalistAdvancementCompatibility;
import com.crispytwig.naturalist.server.recipe.BugNetInteractionRecipe;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.storage.loot.*;
import com.crispytwig.naturalist.registry.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {
    static RegistryAccess.Frozen registries;
    static MultiPackResourceManager resources;
    static RecipeHarness recipes;
    static HolderLookup.Provider lootRegistries;
    static Map<Identifier, Advancement> preparedAdvancements;
    static final Path ROOT = Path.of(System.getProperty("naturalist.stagedResources"));

    @BeforeAll
    static void bootstrapAndPrepareBeforePublishingComponents() {
        Thread.currentThread().setContextClassLoader(Naturalist.class.getClassLoader());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Naturalist.bootstrap();
        Naturalist.createAttributes(FabricDefaultAttributeRegistry::register);
        // Fabric defers the vanilla freeze until mod initialization has completed.
        BuiltInRegistries.REGISTRY.freeze();
        for (var registry : BuiltInRegistries.REGISTRY) {
            ((MappedRegistry<?>) registry).bindAllTagsToEmpty();
            registry.freeze();
        }
        var info = new PackLocationInfo("naturalist-lifecycle-test", Component.literal("Naturalist lifecycle test"),
                PackSource.DEFAULT, Optional.empty());
        resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(
                new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft").build(info),
                new PathPackResources(info, ROOT)));
        var statics = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        TagLoader.loadTagsForExistingRegistries(resources, statics).forEach(Registry.PendingTags::apply);
        var dynamic = RegistryDataLoader.load(resources, statics.listRegistries().toList(),
                net.fabricmc.fabric.api.event.registry.DynamicRegistries.getWorldRegistries(), Runnable::run).join();
        registries = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(statics.registries(), dynamic.registries())).freeze();
        var layers = RegistryLayer.createRegistryAccess().replaceFrom(RegistryLayer.WORLDGEN, dynamic);
        lootRegistries = ReloadableServerRegistries.reload(layers, List.of(), resources, Runnable::run).join().lookupWithUpdatedTags();
        // This is the production order: bake pending components, prepare/apply
        // listeners, then publish components and finalize recipes.
        var pending = BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries);
        recipes = new RecipeHarness(registries);
        recipes.load(resources);
        preparedAdvancements = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resources, FileToIdConverter.registry(Registries.ADVANCEMENT),
                registries.createSerializationContext(JsonOps.INSTANCE), Advancement.CODEC, preparedAdvancements);
        pending.forEach(p -> p.apply());
        recipes.finalizeRecipeLoading(FeatureFlags.VANILLA_SET);
    }

    @AfterAll static void closeResources() { if (resources != null) resources.close(); }

    @Test
    void everyNaturalistRecipeSurvivesManagerPreparationAndFinalization() {
        assertEquals(101, recipes.getRecipes().stream().filter(r -> r.id().identifier().getNamespace().equals("naturalist")).count());
        for (String ownedOverride : List.of("cake", "leather", "pumpkin_pie", "spectral_arrow")) {
            assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(ownedOverride))).isPresent(), ownedOverride);
        }
        var bee = (BugNetInteractionRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE, Naturalist.location("catch_bee"))).orElseThrow().value();
        assertEquals(Identifier.withDefaultNamespace("bee"), BuiltInRegistries.ENTITY_TYPE.getKey(bee.entityType()));
        // Compare with the declared resource result: no substitute result is chosen here.
        try (var reader = java.nio.file.Files.newBufferedReader(ROOT.resolve("data/naturalist/recipe/catch_bee.json"))) {
            var declared = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("result");
            var expected = ItemStackTemplate.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), declared).getOrThrow().create();
            assertTrue(ItemStack.matches(expected, bee.dropStack()));
            assertNotSame(bee.dropStack(), bee.dropStack());
        } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
    }

    @Test
    void allAdvancementsPublishWithVanillaParents() {
        var manager = new AdvancementHarness(registries);
        manager.publish(new HashMap<>(preparedAdvancements));
        assertEquals(40, manager.tree().nodes().stream().filter(n -> n.holder().id().getNamespace().equals("naturalist")).count());
        preparedAdvancements.forEach((id, advancement) -> {
            if (id.getNamespace().equals("naturalist")) {
                var problems = new ProblemReporter.Collector();
                advancement.validate(problems, registries);
                assertTrue(problems.isEmpty(), () -> id + ": " + problems.getReport());
            }
        });
    }

    @Test
    void creativeTabUsesUniqueRegisteredItemsAndPreservesVariants() throws Exception {
        CreativeInventoryContracts.verify(registries);
    }

    @Test void creativeShapeFamilyCollisionKeepsEveryDistinctStack() throws Exception {
        var bricks = NaturalistRegistry.SHELLSTONE_BRICKS.get().asItem();
        var wall = NaturalistRegistry.SHELLSTONE_BRICK_WALL.get().asItem();
        ShapeEqualityFixture.family = Set.of(bricks, wall);
        try {
            assertNotSame(bricks, wall);
            assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(bricks), new ItemStack(wall)),
                    "The fixture must reproduce the verified CNM shape-family RETURN hook");
            var strategyField = ItemStackLinkedSet.class.getDeclaredField("TYPE_AND_TAG");
            strategyField.setAccessible(true);
            @SuppressWarnings("unchecked") var actualStrategy = (it.unimi.dsi.fastutil.Hash.Strategy<ItemStack>) strategyField.get(null);
            var collisions = new it.unimi.dsi.fastutil.Hash.Strategy<ItemStack>() {
                public int hashCode(ItemStack stack) { return 0; } // Deterministic hash-bucket collision.
                public boolean equals(ItemStack a, ItemStack b) { return actualStrategy.equals(a, b); }
            };
            var baseline = new it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet<ItemStack>(collisions);
            assertTrue(baseline.add(new ItemStack(bricks)));
            assertFalse(baseline.add(new ItemStack(wall)), "Unscoped vanilla collection reproduces the false duplicate");
            var emitted = new it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet<ItemStack>(collisions);
            var field = CreativeModeTab.class.getDeclaredField("displayItemsGenerator");
            field.setAccessible(true);
            var generator = (CreativeModeTab.DisplayItemsGenerator) field.get(NaturalistCreativeTab.ITEM_GROUP.get());
            generator.accept(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, true, registries),
                    (stack, visibility) -> assertTrue(emitted.add(stack), "Accidentally adding the same item stack twice [" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "]"));
            assertEquals(198, emitted.size());
            assertTrue(emitted.stream().anyMatch(stack -> stack.getItem() == bricks));
            assertTrue(emitted.stream().anyMatch(stack -> stack.getItem() == wall));
            assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(bricks), new ItemStack(wall)),
                    "Inventory family equality must survive creative generation");
        } finally { ShapeEqualityFixture.family = Set.of(); }
    }

    @Test void creativeIdentityScopeRestoresAfterNestingAndFailure() {
        assertFalse(com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.isActive());
        assertThrows(IllegalStateException.class, () -> com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.generate(() -> {
            com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.generate(() -> assertTrue(com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.isActive()));
            assertTrue(com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.isActive());
            throw new IllegalStateException("fixture");
        }));
        assertFalse(com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity.isActive());
    }

    @TestFactory Stream<DynamicTest> everyRegisteredEntityHasValidConstructorGoalsAndAttributeContracts() {
        return EntityContracts.roster(registries);
    }

    @TestFactory Stream<DynamicTest> immutableAdvancementPublication() {
        return AdvancementPublicationContracts.cases();
    }

    static class RecipeHarness extends RecipeManager {
        RecipeHarness(HolderLookup.Provider registries) { super(registries); }
        void load(ResourceManager resources) {
            apply(prepare(resources, InactiveProfiler.INSTANCE), resources, InactiveProfiler.INSTANCE);
        }
    }

    @Test void matchaFilteredParentsPublishAllNaturalistAdvancementsWithoutChangingCriteria() {
        var filtered = new HashMap<>(preparedAdvancements);
        filtered.keySet().removeIf(id -> id.getNamespace().equals("minecraft") && id.getPath().startsWith("husbandry/"));
        Identifier root = Identifier.fromNamespaceAndPath("main", "tutorial/root");
        filtered.put(root, preparedAdvancements.get(Identifier.withDefaultNamespace("husbandry/root")));
        var before = new HashMap<>(filtered);
        var rejected = new AdvancementTree();
        rejected.addAll(filtered.entrySet().stream().map(e -> new AdvancementHolder(e.getKey(), e.getValue())).toList());
        assertEquals(37, rejected.nodes().stream().filter(n -> n.holder().id().getNamespace().equals("naturalist")).count(), "Raw decoding misses the three unresolved parents");
        var manager = new AdvancementHarness(registries);
        manager.publish(filtered);
        assertEquals(40, manager.tree().nodes().stream().filter(n -> n.holder().id().getNamespace().equals("naturalist")).count());
        for (String name : List.of("feed_bear_honeycomb", "feed_hippo_melon", "ride_giraffe_with_map")) {
            Identifier id = Naturalist.location("husbandry/" + name);
            Advancement old = before.get(id), value = manager.get(id).value();
            assertSame(old, filtered.get(id), "Publication must leave the input unchanged");
            assertEquals(Optional.of(root), value.parent());
            assertEquals(old.criteria(), value.criteria());
            assertEquals(old.requirements(), value.requirements());
            assertEquals(old.rewards(), value.rewards());
            assertEquals(old.display(), value.display());
        }
    }

    @Test void tacticalFishingUsesDecodedCriteriaAndRemainsIdempotent() {
        var values = new HashMap<>(preparedAdvancements);
        Identifier id = Identifier.withDefaultNamespace("husbandry/tactical_fishing");
        var original = values.get(id);
        var published = NaturalistAdvancementCompatibility.prepareForPublication(values);
        var updated = published.get(id);
        assertSame(original, values.get(id));
        assertEquals(original.criteria().size() + 2, updated.criteria().size());
        assertTrue(updated.criteria().entrySet().containsAll(original.criteria().entrySet()));
        assertEquals(original.rewards(), updated.rewards());
        assertTrue(updated.requirements().validate(updated.criteria().keySet()).isSuccess());
        var repeated = NaturalistAdvancementCompatibility.prepareForPublication(published);
        assertEquals(updated, repeated.get(id));
    }

    @Test void everyStagedDynamicRegistryEntrySurvivesMappedRegistryLoading() {
        int count = 0;
        for (var registry : registries.registries().toList()) {
            var converter = FileToIdConverter.registry(registry.key());
            for (var file : converter.listMatchingResources(resources).keySet()) {
                var id = converter.fileToId(file);
                if (!id.getNamespace().equals("naturalist")) continue;
                assertTrue(registry.value().containsKey(id), "Missing final registry entry " + registry.key() + "/" + id);
                count++;
            }
        }
        assertEquals(107, count, "99 variants plus damage, songs, painting and worldgen entries");
        assertEquals(48, registries.registries().filter(r -> r.key().identifier().getNamespace().equals("naturalist")).count());
        System.out.println("Final Naturalist dynamic registry entries=" + count);
    }

    @Test void allLootTablesSurviveReloadAndReferenceValidation() {
        var tables = lootRegistries.lookupOrThrow(Registries.LOOT_TABLE);
        assertEquals(78, tables.listElements().filter(h -> h.key().identifier().getNamespace().equals("naturalist")).count());
        var problems = new ProblemReporter.Collector();
        var context = new ValidationContextSource(problems, lootRegistries);
        LootDataType.values().forEach(type -> validateLoot(context, type));
        // Include resolved vanilla references, so missing dependencies cannot hide behind an individual codec pass.
        assertTrue(problems.isEmpty(), problems::getReport);
    }

    private static <T extends Validatable> void validateLoot(ValidationContextSource context, LootDataType<T> type) {
        type.runValidation(context, lootRegistries.lookupOrThrow(type.registryKey()));
    }

    static class AdvancementHarness extends ServerAdvancementManager {
        AdvancementHarness(HolderLookup.Provider registries) { super(registries); }
        void publish(Map<Identifier, Advancement> prepared) {
            apply(prepared, resources, InactiveProfiler.INSTANCE);
        }
    }
}
