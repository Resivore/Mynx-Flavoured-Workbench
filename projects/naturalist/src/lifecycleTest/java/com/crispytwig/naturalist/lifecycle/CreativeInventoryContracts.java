package com.crispytwig.naturalist.lifecycle;

import com.crispytwig.naturalist.registry.*;
import com.crispytwig.naturalist.server.item.NaturalistBucketItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

final class CreativeInventoryContracts {
    static void verify(HolderLookup.Provider registries) throws Exception {
        Set<Item> registered = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var entry : NaturalistRegistry.ITEMS.getEntries()) {
            Item item = entry.get();
            assertTrue(registered.add(item), "Repeated deferred holder identity: " + BuiltInRegistries.ITEM.getKey(item));
            if (item instanceof BlockItem blockItem) {
                assertSame(item, blockItem.getBlock().asItem());
                assertEquals(BuiltInRegistries.ITEM.getKey(item), BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
            }
        }
        assertEquals(141, registered.size());
        assertEquals(registered, BuiltInRegistries.ITEM.stream().filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals("naturalist")).collect(Collectors.toSet()));
        var tab = NaturalistCreativeTab.ITEM_GROUP.get();
        var params = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, true, registries);
        for (int rebuild = 0; rebuild < 3; rebuild++) {
            tab.buildContents(params); // Vanilla's strict duplicate-rejecting builder.
            verifyStacks(registered, tab.getDisplayItems());
            assertEquals(tab.getDisplayItems().size(), tab.getSearchTabDisplayItems().size());
        }
        String externalOutput = System.getProperty("naturalist.creativeOutputClass");
        if (externalOutput != null) {
            // Optional forensic input: executes the exact retained third-party Output
            // class with the actual Naturalist generator, without launching a client.
            var outputClass = Class.forName(externalOutput);
            var generatorField = CreativeModeTab.class.getDeclaredField("displayItemsGenerator");
            generatorField.setAccessible(true);
            var generator = (CreativeModeTab.DisplayItemsGenerator) generatorField.get(tab);
            for (int rebuild = 0; rebuild < 3; rebuild++) {
                var output = (CreativeModeTab.Output) outputClass.getConstructor(CreativeModeTab.class, net.minecraft.world.flag.FeatureFlagSet.class).newInstance(tab, FeatureFlags.VANILLA_SET);
                generator.accept(params, output);
                @SuppressWarnings("unchecked") var stacks = (Collection<ItemStack>) outputClass.getField("parentTabStacks").get(output);
                verifyStacks(registered, stacks);
            }
            System.out.println("Exact external output class passed: " + externalOutput);
        }
        System.out.println("Creative inventory: 141 registry identities, 198 unique component-distinct stacks; 3 rebuilds");
    }

    private static void verifyStacks(Set<Item> registered, Collection<ItemStack> stacks) {
        assertEquals(198, stacks.size());
        var exact = ItemStackLinkedSet.createTypeAndComponentsSet();
        Map<Item, List<ItemStack>> byItem = new IdentityHashMap<>();
        for (var stack : stacks) {
            assertEquals(1, stack.getCount());
            assertTrue(exact.add(stack), "Exact duplicate: " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
            byItem.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>()).add(stack);
        }
        assertEquals(registered, byItem.keySet());
        for (Item item : registered) {
            List<ItemStack> variants = byItem.get(item);
            if (item == NaturalistRegistry.SNAIL.get() || item == NaturalistRegistry.SNAIL_SHELL.get()) {
                assertEquals(16, variants.size());
                Set<Integer> colors = variants.stream().map(s -> s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("Color", -1)).collect(Collectors.toSet());
                assertEquals(java.util.stream.IntStream.range(0,16).boxed().collect(Collectors.toSet()), colors);
            } else if (item instanceof NaturalistBucketItem bucket && bucket.getLegacyVariantNames() != null) {
                Set<String> expected = Arrays.stream(bucket.getLegacyVariantNames()).map(n -> "naturalist:" + n).collect(Collectors.toSet());
                Set<String> actual = variants.stream().map(s -> s.get(DataComponents.BUCKET_ENTITY_DATA).copyTag().getStringOr("Variant", "")).collect(Collectors.toSet());
                assertEquals(expected.size(), variants.size());
                assertEquals(expected, actual, BuiltInRegistries.ITEM.getKey(item).toString());
            } else assertEquals(1, variants.size(), BuiltInRegistries.ITEM.getKey(item).toString());
            if (item instanceof NaturalistBucketItem bucket && bucket.getLegacyVariantNames() != null) for (var stack : variants) {
                assertEquals(stack.get(DataComponents.CUSTOM_DATA), stack.get(DataComponents.BUCKET_ENTITY_DATA));
            }
        }
    }
}
