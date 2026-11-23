package com.fizzware.dramaticdoors.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

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
                    verifyDoorNames(client);
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
                    int amcCount = 0;
                    for (var id : AmcDoorChecks.doors()) {
                        var block = BuiltInRegistries.BLOCK.getOptional(id);
                        if (block.isEmpty()) continue;
                        var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
                        String name = I18n.get(item.getDescriptionId());
                        if (!name.startsWith("Tall ") || models.getItemModel(id) == missingItem
                                || block.get().getStateDefinition().getPossibleStates().stream()
                                .anyMatch(state -> states.get(state) == states.missingModel())
                                || trees.creativeNameSearch().search(name.toLowerCase(Locale.ROOT)).stream().noneMatch(stack -> stack.is(item))) {
                            throw new AssertionError("Missing AMC state, item model, translation or Creative Search entry " + id);
                        }
                        filter.setFilterText(name);
                        if (filter.getFilteredItemStacks().stream().noneMatch(stack -> stack.is(item))) throw new AssertionError("Missing JEI entry " + id);
                        amcCount++;
                    }
                    com.fizzware.dramaticdoors.DramaticDoors.LOGGER.info("Verified {} AMC tall doors in both search indexes and baked models.", amcCount);
                    com.fizzware.dramaticdoors.DramaticDoors.LOGGER.info("Verified all ten native doors in Creative Search, JEI and baked client models.");
                } finally {
                    filter.setFilterText(previous);
                }
            });
        }
    }

    private static void verifyDoorNames(Minecraft client) {
        var filter = runtime.getIngredientFilter();
        var jeiNames = runtime.getIngredientManager().getIngredientHelper(VanillaTypes.ITEM_STACK);
        var creative = client.getConnection().searchTrees().creativeNameSearch();
        var tooltip = Item.TooltipContext.of(client.level);
        var rows = new ArrayList<String>();
        rows.add("registry_id\titem_key\tblock_key\thover_name\ttooltip_name\tjei_name");
        int wrong = 0;
        for (var id : BuiltInRegistries.ITEM.keySet().stream()
                .filter(key -> key.getNamespace().equals("dramaticdoors")).sorted().toList()) {
            var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
            var block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
            var stack = item.getDefaultInstance();
            String key = "block.dramaticdoors." + id.getPath();
            String name = I18n.get(key);
            String hover = stack.getHoverName().getString();
            String firstLine = stack.getTooltipLines(tooltip, client.player, TooltipFlag.NORMAL).getFirst().getString();
            String jei = jeiNames.getDisplayName(stack);
            rows.add(String.join("\t", id.toString(), item.getDescriptionId(), block.getDescriptionId(), hover, firstLine, jei));
            if (!key.equals(item.getDescriptionId()) || !key.equals(block.getDescriptionId())
                    || name.isBlank() || name.equals(key) || !block.getName().getString().equals(name)
                    || !name.equals(hover) || !name.equals(firstLine) || !name.equals(jei)) {
                wrong++;
                continue;
            }
            filter.setFilterText(name);
            if (creative.search(name.toLowerCase(Locale.ROOT)).stream().noneMatch(result -> result.is(item))
                    || filter.getFilteredItemStacks().stream().noneMatch(result -> result.is(item))) {
                throw new AssertionError("Missing search result for " + id + " under " + name);
            }
        }
        try {
            Files.write(Path.of("door-name-audit.tsv"), rows);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (wrong > 0) throw new AssertionError(wrong + " of " + (rows.size() - 1) + " registered DD names failed; see door-name-audit.tsv");
        com.fizzware.dramaticdoors.DramaticDoors.LOGGER.info("Verified {} DD block, inventory, tooltip, Creative Search and JEI names.", rows.size() - 1);
    }
}
