package dev.resivore.dragonbound.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonboundWaystoneBlockTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void shapeIsExactlyThreePixelsTall() {
        AABB bounds = DragonboundWaystoneBlock.SHAPE.bounds();

        assertEquals(0.0D, bounds.minX);
        assertEquals(0.0D, bounds.minY);
        assertEquals(0.0D, bounds.minZ);
        assertEquals(1.0D, bounds.maxX);
        assertEquals(3.0D / 16.0D, bounds.maxY);
        assertEquals(1.0D, bounds.maxZ);
        assertEquals(3.0D, DragonboundWaystoneBlock.HEIGHT_PIXELS);
        assertEquals(3.0D / 16.0D, DragonboundWaystoneBlock.HEIGHT_BLOCKS);
    }

    @Test
    void implementationIsAStateFreeBlockEntityBlockNotASlabBlock() {
        assertEquals(BaseEntityBlock.class, DragonboundWaystoneBlock.class.getSuperclass());
        assertFalse(SlabBlock.class.isAssignableFrom(DragonboundWaystoneBlock.class));
        assertEquals(3_600_000.0F, DragonboundWaystoneBlock.EXPLOSION_RESISTANCE);
    }

    @Test
    void sourceFreezesPistonExplosionLootAndLossProtections() throws IOException {
        String block = readMain("block/DragonboundWaystoneBlock.java");
        String protection = readMain("block/WaystoneLossProtection.java");
        String blockEntity = readMain("block/DragonboundWaystoneBlockEntity.java");
        String content = readMain("DragonboundContent.java");

        assertTrue(block.contains(".pushReaction(PushReaction.BLOCK)"));
        assertTrue(block.contains(".noLootTable()"));
        assertTrue(block.contains("protected void onExplosionHit("));
        assertTrue(block.contains("public boolean dropFromExplosion(Explosion explosion)"));
        assertTrue(block.contains("return false;"));

        int directInventory = protection.indexOf("player.addItem(remainder)");
        int entityFallback = protection.indexOf("new ItemEntity(");
        assertTrue(directInventory >= 0 && directInventory < entityFallback);
        assertTrue(protection.contains("fallback.setUnlimitedLifetime()"));
        assertTrue(protection.contains("fallback.setInvulnerable(true)"));
        assertTrue(protection.contains("fallback.setNoPickUpDelay()"));
        assertTrue(content.contains(".fireResistant()"));

        assertTrue(blockEntity.contains("ItemStack.CODEC"));
        assertTrue(blockEntity.contains("stack.copyWithCount(1)"));
        assertTrue(blockEntity.contains("public void preRemoveSideEffects("));
        assertTrue(blockEntity.contains("DragonboundAnchors.clearIfMatching(serverLevel, pos)"));
        assertTrue(block.contains("waystone.copyPlacedStack()"));
    }

    @Test
    void vanillaDragonAndWitherBodyDestructionCannotRemoveWaystone() throws IOException {
        for (String tag : new String[]{"dragon_immune", "wither_immune"}) {
            Path path = Path.of("src/main/resources/data/minecraft/tags/block", tag + ".json");
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            assertFalse(json.get("replace").getAsBoolean());
            assertEquals(1, json.getAsJsonArray("values").size());
            assertEquals(
                    "dragonbound_waystone:dragonbound_waystone",
                    json.getAsJsonArray("values").get(0).getAsString());
        }
    }

    @Test
    void renderResourcesFaithfullyPortTheFourCuboidModelAndExactUvs() throws IOException {
        JsonObject model = readResourceJson("assets/dragonbound_waystone/models/block/dragonbound_waystone.json");

        assertEquals("minecraft:block/block", model.get("parent").getAsString());
        assertTrue(model.get("ambientocclusion").getAsBoolean());
        JsonObject textures = model.getAsJsonObject("textures");
        assertEquals("minecraft:block/end_stone_bricks", textures.get("particle").getAsString());
        assertEquals("minecraft:block/end_stone_bricks", textures.get("end_stone_bricks").getAsString());

        var elements = model.getAsJsonArray("elements");
        assertEquals(4, elements.size());
        assertCuboid(elements.get(0).getAsJsonObject(),
                List.of(0, 0, 0), List.of(16, 2, 16),
                List.of(
                        List.of(0, 14, 16, 16), List.of(0, 14, 16, 16),
                        List.of(0, 14, 16, 16), List.of(0, 14, 16, 16),
                        List.of(0, 0, 16, 16), List.of(0, 0, 16, 16)));
        assertCuboid(elements.get(1).getAsJsonObject(),
                List.of(2, 2, 3), List.of(14, 3, 13),
                List.of(
                        List.of(2, 13, 14, 14), List.of(3, 2, 13, 3),
                        List.of(2, 12, 14, 13), List.of(3, 3, 13, 4),
                        List.of(2, 3, 14, 13), List.of(2, 3, 14, 13)));
        assertCuboid(elements.get(2).getAsJsonObject(),
                List.of(3, 2, 13), List.of(13, 3, 14),
                List.of(
                        List.of(3, 13, 13, 14), List.of(13, 13, 14, 14),
                        List.of(3, 13, 13, 14), List.of(2, 13, 3, 14),
                        List.of(3, 13, 13, 14), List.of(3, 13, 13, 14)));
        assertCuboid(elements.get(3).getAsJsonObject(),
                List.of(3, 2, 2), List.of(13, 3, 3),
                List.of(
                        List.of(3, 13, 13, 14), List.of(13, 13, 14, 14),
                        List.of(3, 13, 13, 14), List.of(2, 13, 3, 14),
                        List.of(3, 13, 13, 14), List.of(3, 13, 13, 14)));

        JsonObject variant = readResourceJson(
                "assets/dragonbound_waystone/blockstates/dragonbound_waystone.json")
                .getAsJsonObject("variants")
                .getAsJsonObject("");
        assertEquals("dragonbound_waystone:block/dragonbound_waystone", variant.get("model").getAsString());

        JsonObject itemModel = readResourceJson(
                "assets/dragonbound_waystone/items/dragonbound_waystone.json")
                .getAsJsonObject("model");
        assertEquals("minecraft:model", itemModel.get("type").getAsString());
        assertEquals("dragonbound_waystone:block/dragonbound_waystone", itemModel.get("model").getAsString());
    }

    private static void assertCuboid(
            JsonObject element,
            List<Integer> expectedFrom,
            List<Integer> expectedTo,
            List<List<Integer>> expectedUvs) {
        assertEquals(expectedFrom, integers(element, "from"));
        assertEquals(expectedTo, integers(element, "to"));

        JsonObject faces = element.getAsJsonObject("faces");
        List<String> directions = List.of("north", "east", "south", "west", "up", "down");
        assertEquals(directions.size(), faces.size());
        for (int index = 0; index < directions.size(); index++) {
            JsonObject face = faces.getAsJsonObject(directions.get(index));
            assertEquals(expectedUvs.get(index), integers(face, "uv"));
            assertEquals("#end_stone_bricks", face.get("texture").getAsString());
        }
    }

    private static List<Integer> integers(JsonObject object, String member) {
        return object.getAsJsonArray(member).asList().stream()
                .map(element -> element.getAsInt())
                .toList();
    }

    private static JsonObject readResourceJson(String relativePath) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources").resolve(relativePath)))
                .getAsJsonObject();
    }

    private static String readMain(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/dev/resivore/dragonbound").resolve(relativePath));
    }
}
