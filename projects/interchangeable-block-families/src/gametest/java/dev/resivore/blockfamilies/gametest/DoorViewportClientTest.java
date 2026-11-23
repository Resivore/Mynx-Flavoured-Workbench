package dev.resivore.blockfamilies.gametest;

import dev.tazer.clutternomore.ClutterNoMoreClient;
import dev.tazer.clutternomore.client.ClientShapeTooltip;
import dev.tazer.clutternomore.client.ShapeSwitcherOverlay;
import dev.tazer.clutternomore.common.inventory.ShapeTooltip;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DoorViewportClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player != null && client.level != null);
            var server = world.getServer();
            var families = new LinkedHashMap<String, Integer>();
            families.put("architectural_material_closure:mossy_oak_classic_trapdoor", 17);
            families.put("minecraft:bamboo_button", 9);
            families.put("minecraft:bamboo_trapdoor", 18);
            for (String wood : List.of("veiled", "celestial", "murushroom", "mossy_oak")) {
                families.put("dramaticdoors:tall_" + wood + "_door", 23);
            }
            var before = server.computeOnServer(mc -> {
                var snapshot = new LinkedHashMap<Item, List<Item>>();
                families.forEach((id, size) -> {
                    var parent = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElseThrow();
                    var members = List.copyOf(ShapeMap.getShapes(parent));
                    if (members.size() != size || members.getFirst() != parent) throw new AssertionError(id);
                    snapshot.put(parent, members);
                });
                return snapshot;
            });
            var reload = server.computeOnServer(mc -> mc.reloadResources(mc.getPackRepository().getSelectedIds()));
            context.waitFor(client -> reload.isDone(), 1200);
            reload.join();
            world.getConnection().waitForClientboundPackets();
            server.runOnServer(mc -> before.forEach((parent, members) -> {
                if (!members.equals(ShapeMap.getShapes(parent))) throw new AssertionError("Reload changed " + parent);
                for (Item item : members) {
                    var id = BuiltInRegistries.ITEM.getKey(item);
                    boolean hasRecipe = mc.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent();
                    if (hasRecipe != (item == parent)) throw new AssertionError("Wrong recipe after reload: " + id);
                }
            }));
            context.runOnClient(client -> {
                boolean previous = ClutterNoMoreClient.showTooltip;
                try {
                    ClutterNoMoreClient.showTooltip = true;
                    before.forEach((parent, members) -> {
                        if (!members.equals(ShapeMap.getShapes(parent))) throw new AssertionError("Client reload order differs for " + parent);
                        checkFamily(client, BuiltInRegistries.ITEM.getKey(parent).toString(), members.size());
                    });
                    for (String parent : List.of("enderscape:veiled_door", "enderscape:celestial_door",
                            "enderscape:murublight_door", "ribbits:mossy_oak_door")) {
                        checkFamily(client, parent, 23);
                    }
                    for (String wood : List.of("veiled", "celestial", "murublight")) {
                        checkFamily(client, "enderscape:" + wood + "_trapdoor", 18);
                    }
                } finally {
                    ClutterNoMoreClient.showTooltip = previous;
                }
            });
            // CNM sends the final debounced scroll packet on the first tick after 50 ms.
            long flushAfter = System.nanoTime() + java.time.Duration.ofMillis(50).toNanos();
            context.waitFor(client -> System.nanoTime() >= flushAfter);
            context.waitTick();
            world.getConnection().waitForClientboundPackets();
        }
    }

    private static void checkFamily(Minecraft client, String parentId, int expectedSize) {
        Item parent = BuiltInRegistries.ITEM.getOptional(Identifier.parse(parentId)).orElseThrow();
        ItemStack held = new ItemStack(parent, 7);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var overlay = new ShapeSwitcherOverlay(client, held, true);
        List<Item> members = ShapeMap.getShapes(parent);
        if (members.size() != expectedSize) throw new AssertionError("Wrong selector size for " + parent);
        var stacks = members.stream().map(Item::getDefaultInstance).toList();
        var missing = client.getModelManager().getItemModel(Identifier.parse("interchangeable_block_families:missing"));
        for (int i = 0; i < members.size(); i++) {
            Item expected = members.get(i);
            if (client.getModelManager().getItemModel(BuiltInRegistries.ITEM.getKey(expected)) == missing) {
                throw new AssertionError("Missing client door model: " + expected);
            }
            if (i > 0) overlay.onMouseScrolled(-1);
            var graphics = new DoorGraphics(client, expectedSize > 12);
            overlay.render(graphics, 1.0F);
            if (overlay.selectedIndex != i || Math.abs(graphics.x(expected) - (graphics.guiWidth() / 2 - 8)) > 1) {
                throw new AssertionError("Overlay did not center " + expected);
            }

            var tooltip = new ClientShapeTooltip(new ShapeTooltip(stacks, i));
            graphics = new DoorGraphics(client, true);
            int width = tooltip.getWidth(client.font);
            tooltip.extractImage(client.font, 0, 0, width, tooltip.getHeight(client.font), graphics);
            int visible = Math.min(12, expectedSize);
            if (width != 22 * visible || graphics.icons.size() != visible || graphics.x(expected) + 16 > width) {
                throw new AssertionError("Tooltip did not display " + expected + " inside its viewport");
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("Verified {} overlay and tooltip positions for {}", expectedSize, parent);
    }

    private static final class DoorGraphics extends GuiGraphicsExtractor {
        final Map<Item, Integer> icons = new LinkedHashMap<>();

        final boolean checkAllBounds;

        DoorGraphics(Minecraft client, boolean checkAllBounds) {
            super(client, new GuiRenderState(), 0, 0);
            this.checkAllBounds = checkAllBounds;
        }

        @Override
        public int guiWidth() {
            return 320;
        }

        @Override
        public void fakeItem(ItemStack stack, int x, int y) {
            if ((checkAllBounds && (x < 0 || x + 16 > guiWidth())) || icons.put(stack.getItem(), x) != null) {
                throw new AssertionError("Clipped or duplicate door icon: " + stack);
            }
            super.fakeItem(stack, x, y);
        }

        int x(Item item) {
            Integer coordinate = icons.get(item);
            if (coordinate == null) throw new AssertionError("Missing selected icon: " + item);
            return coordinate;
        }
    }
}
