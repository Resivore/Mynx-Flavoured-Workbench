package com.starfish_studios.bbb.gametest;

import com.starfish_studios.bbb.compat.EnderscapeWoodIntegration;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Controlled server-start proof that Enderscape absence leaves BBB's accepted registry/data intact. */
public final class StandaloneBbbGameTests implements CustomTestMethodInvoker {
    private static final List<String> BASE_MATERIALS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak"
    );
    private static final List<String> PROVIDER_MATERIALS = List.of("veiled", "celestial", "murublight");

    @GameTest(maxTicks = 40)
    public void standaloneStartupKeepsExactAcceptedRegistryAndData(GameTestHelper helper) {
        List<String> materials = BBBContent.WOOD_FAMILIES.stream()
                .map(BBBContent.WoodFamily::material)
                .toList();
        helper.assertTrue(BBBContent.BLOCKS.size() == 171
                        && BBBContent.ITEMS.size() == 172
                        && BBBContent.WOOD_FAMILIES.size() == 12
                        && BBBContent.BEAM_FAMILIES.size() == 12
                        && BBBContent.LATTICES.size() == 12
                        && materials.equals(BASE_MATERIALS),
                "Standalone BBB registry changed: blocks=" + BBBContent.BLOCKS.size()
                        + ", items=" + BBBContent.ITEMS.size() + ", materials=" + materials);
        helper.assertTrue(!BBBContent.enderscapeFamiliesRegistered()
                        && !EnderscapeWoodIntegration.familiesRegistered(),
                "Enderscape families registered while the provider was absent");

        for (String material : PROVIDER_MATERIALS) {
            for (String form : BBBContent.WOOD_FORMS) {
                Identifier id = Identifier.fromNamespaceAndPath("bbb", material + "_" + form);
                helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(id)
                                && !BuiltInRegistries.ITEM.containsKey(id),
                        "Standalone startup created optional registry ID " + id);
            }
        }
        Set<String> providerRecipes = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .map(holder -> holder.id().identifier())
                .filter(id -> id.getNamespace().equals("bbb"))
                .map(Identifier::getPath)
                .filter(path -> PROVIDER_MATERIALS.stream().anyMatch(path::startsWith))
                .collect(Collectors.toSet());
        helper.assertTrue(providerRecipes.isEmpty(),
                "Standalone data reload activated optional provider recipes: " + providerRecipes);
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
