package com.yungnickyoung.minecraft.ribbits.chute;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChuteStaticContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void itemIsUnlimitedStackOneAndAppearsOnceInCreative() throws IOException {
        String items = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");
        String item = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/item/ChuteLeafItem.java");
        String creative = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");

        assertEquals(1, occurrences(items, "@AutoRegister(\"chute_leaf\")"));
        assertTrue(items.contains("new Item.Properties().stacksTo(1).setId(RegisterHelper.itemKey(\"chute_leaf\"))"));
        assertEquals(1, occurrences(creative, "CreativeEntry.of(\"chute_leaf\", ItemModule.CHUTE_LEAF::get)"));
        assertTrue(item.contains("item.ribbits.chute_leaf.tooltip"));
        assertFalse((items + item).contains("durability("));
        assertFalse((items + item).contains("DataComponents.GLIDER"));
        assertFalse((items + item).contains("DataComponents.EQUIPPABLE"));
    }

    @Test
    void onlyExactAcceptedChestCapeEligibilityIsAdded() throws IOException {
        JsonObject tag = JsonParser.parseString(read(
                "common/src/main/resources/data/trinkets/tags/item/chest/cape.json")).getAsJsonObject();
        assertFalse(tag.get("replace").getAsBoolean());
        JsonArray values = tag.getAsJsonArray("values");
        assertEquals(1, values.size());
        assertEquals("ribbits:chute_leaf", values.get(0).getAsString());
        assertFalse(Files.exists(ROOT.resolve("common/src/main/resources/data/trinkets/slots/chest/cape.json")));
        assertFalse(Files.exists(ROOT.resolve("common/src/main/resources/data/trinkets/tags/item/chest/back.json")));
    }

    @Test
    void translationIsSourceSafeAndPrivatePathIsNotDuplicated() throws IOException {
        JsonObject language = JsonParser.parseString(read(
                "common/src/publicResources/assets/ribbits/lang/en_us.json")).getAsJsonObject();
        assertEquals("Chute Leaf", language.get("item.ribbits.chute_leaf").getAsString());
        assertEquals("Jump again while airborne to deploy.",
                language.get("item.ribbits.chute_leaf.tooltip").getAsString());
        assertFalse(Files.exists(ROOT.resolve("common/src/main/resources/assets/ribbits/lang/en_us.json")));
    }

    @Test
    void movementAndRenderingRemainNarrowAndSeparated() throws IOException {
        String serverMixin = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/mixin/mixins/chute/PlayerChuteMovementMixin.java");
        String clientMixin = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/mixin/mixins/client/chute/ClientPlayerChuteMovementMixin.java");
        String renderer = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/ChuteLeafRenderer.java");
        String controller = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/chute/ChuteServerController.java");

        assertTrue(serverMixin.contains("@At(\"HEAD\")"));
        assertTrue(serverMixin.contains("@At(\"RETURN\")"));
        assertTrue(clientMixin.contains("instanceof LocalPlayer"));
        assertEquals(1, occurrences(renderer, "TrinketRendererRegistry.registerRenderer"));
        assertTrue(renderer.contains("RibbitsCommon.id(\"chute_leaf_open\")"));
        assertTrue(renderer.contains("renderState.isInvisible"));
        assertTrue(renderer.contains("if (!isMainHand"));
        String deployed = renderer.substring(renderer.indexOf("if (deployed) {"), renderer.indexOf("} else {"));
        assertTrue(deployed.contains("ItemDisplayContext.NONE"));
        assertTrue(deployed.contains("applyDeployedPose(poseStack)"));
        assertFalse(deployed.contains("translateToChest"));
        assertFalse(deployed.contains("ItemDisplayContext.FIXED"));
        assertFalse(renderer.contains("WingsTrinketElement"));
        assertFalse(renderer.contains("minecraft:elytra"));
        assertFalse(controller.contains("addEffect"));
        assertFalse(controller.contains("setFallFlying"));
        assertFalse(controller.contains("hurtAndBreak"));
    }

    @Test
    void exactDependencyAndAllConflictEntryPointsAreRequired() throws IOException {
        String gradle = read("build.gradle");
        String metadata = read("fabric/src/main/resources/fabric.mod.json");
        String equipment = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/chute/ChuteEquipment.java");
        String mixins = read("common/src/main/resources/ribbits.mixins.json");

        assertTrue(gradle.contains("trinkets-4.1.0-beta.3+26.2-inventory-compat-canary5.jar"));
        assertTrue(gradle.contains("4c1fa6ac36c0457483fd0d395b99bbd94c9334aad6defece7633bbf0552d1724"));
        assertTrue(metadata.contains("\"trinkets_updated\": \">=4.1.0-beta.3\""));
        assertTrue(equipment.contains("SLOT_ID = \"chest/cape\""));
        assertTrue(equipment.contains("TrinketCanEquipCallback.EVENT.register"));
        assertTrue(equipment.contains("slot.slotType().validatorCheck(stack, slot, player)"));
        assertTrue(mixins.contains("chute.ArmorSlotChuteMixin"));
        assertTrue(mixins.contains("chute.EquippableChuteMixin"));
        assertTrue(mixins.contains("chute.PlayerChuteDataMixin"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath)).replace("\r\n", "\n");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
