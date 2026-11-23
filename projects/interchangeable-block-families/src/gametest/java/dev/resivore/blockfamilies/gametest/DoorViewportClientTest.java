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
import net.minecraft.resources.Identifier;
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
            context.runOnClient(client -> {
                boolean previous = ClutterNoMoreClient.showTooltip;
                try {
                    ClutterNoMoreClient.showTooltip = true;
                    for (String parent : List.of("enderscape:veiled_door", "enderscape:celestial_door",
                            "enderscape:murublight_door", "ribbits:mossy_oak_door")) {
                        checkDoor(client, BuiltInRegistries.ITEM.getOptional(Identifier.parse(parent)).orElseThrow());
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

    private static void checkDoor(Minecraft client, Item parent) {
        ItemStack held = new ItemStack(parent, 7);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var overlay = new ShapeSwitcherOverlay(client, held, true);
        List<Item> members = ShapeMap.getShapes(parent);
        if (members.size() != 23) throw new AssertionError("Expected 23 doors for " + parent);
        var stacks = members.stream().map(Item::getDefaultInstance).toList();
        var missing = client.getModelManager().getItemModel(Identifier.parse("interchangeable_block_families:missing"));
        for (int i = 0; i < members.size(); i++) {
            Item expected = members.get(i);
            if (client.getModelManager().getItemModel(BuiltInRegistries.ITEM.getKey(expected)) == missing) {
                throw new AssertionError("Missing client door model: " + expected);
            }
            if (i > 0) overlay.onMouseScrolled(-1);
            var graphics = new DoorGraphics(client);
            overlay.render(graphics, 1.0F);
            if (overlay.selectedIndex != i || Math.abs(graphics.x(expected) - (graphics.guiWidth() / 2 - 8)) > 1) {
                throw new AssertionError("Overlay did not center " + expected);
            }

            var tooltip = new ClientShapeTooltip(new ShapeTooltip(stacks, i));
            graphics = new DoorGraphics(client);
            int width = tooltip.getWidth(client.font);
            tooltip.extractImage(client.font, 0, 0, width, tooltip.getHeight(client.font), graphics);
            if (width != 264 || graphics.icons.size() != 12 || graphics.x(expected) + 16 > width) {
                throw new AssertionError("Tooltip did not display " + expected + " inside its 12-icon viewport");
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("Verified 23 overlay and tooltip positions for {}", parent);
    }

    private static final class DoorGraphics extends GuiGraphicsExtractor {
        final Map<Item, Integer> icons = new LinkedHashMap<>();

        DoorGraphics(Minecraft client) {
            super(client, new GuiRenderState(), 0, 0);
        }

        @Override
        public int guiWidth() {
            return 320;
        }

        @Override
        public void fakeItem(ItemStack stack, int x, int y) {
            if (x < 0 || x + 16 > guiWidth() || icons.put(stack.getItem(), x) != null) {
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
