package com.fizzware.dramaticdoors.test;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.List;
import java.util.Locale;

@mezz.jei.api.JeiPlugin
public final class DoorSearchTest implements IModPlugin, FabricClientGameTest {
    private static volatile IJeiRuntime runtime;

    @Override public Identifier getPluginUid() { return Identifier.parse("dramaticdoors-tests:door_search"); }
    @Override public void onRuntimeAvailable(IJeiRuntime jei) { runtime = jei; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.level != null && runtime != null, 1800);
            context.runOnClient(client -> {
                CreativeModeTabs.tryRebuildTabContents(client.level.enabledFeatures(), false, client.level.registryAccess());
                var trees = client.getConnection().searchTrees();
                trees.updateCreativeTooltips(client.level.registryAccess(), List.copyOf(CreativeModeTabs.searchTab().getDisplayItems()));
                var models = client.getModelManager();
                var states = models.getBlockStateModelSet();
                var missingItem = models.getItemModel(Identifier.parse("dramaticdoors-tests:missing"));
                var filter = runtime.getIngredientFilter();
                String previous = filter.getFilterText();
                try {
                    for (String material : List.of("Pale Oak", "Veiled", "Celestial", "Murublight", "Mossy Oak")) {
                        String path = material.equals("Murublight") ? "murushroom" : material.toLowerCase(Locale.ROOT).replace(' ', '_');
                        for (String height : List.of("Short", "Tall")) {
                            String name = height + " " + material + " Door";
                            Identifier id = Identifier.parse("dramaticdoors:" + height.toLowerCase(Locale.ROOT) + "_" + path + "_door");
                            var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
                            if (!I18n.get(item.getDescriptionId()).equals(name)
                                    || trees.creativeNameSearch().search(name.toLowerCase(Locale.ROOT)).stream().noneMatch(stack -> stack.is(item))) {
                                throw new AssertionError("Creative Search name or item missing: " + id);
                            }
                            var block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
                            if (models.getItemModel(id) == missingItem || block.getStateDefinition().getPossibleStates().stream()
                                    .anyMatch(state -> states.get(state) == states.missingModel())) {
                                throw new AssertionError("Missing baked model for " + id);
                            }
                            filter.setFilterText(name);
                            if (filter.getFilteredItemStacks().stream().noneMatch(stack -> stack.is(item))) {
                                throw new AssertionError("JEI search for " + name + " did not find " + id);
                            }
                        }
                    }
                    com.fizzware.dramaticdoors.DramaticDoors.LOGGER.info("Verified all ten native doors in Creative Search, JEI and baked client models.");
                } finally {
                    filter.setFilterText(previous);
                }
            });
        }
    }
}
