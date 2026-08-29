package dev.aero.shulkertrowel.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Canary1GameplayFreezeTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void selectionPlacementConsumptionAndFailureContractsRemainIntact() throws IOException {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/item/ShulkerTrowelItem.java"
        )).replace("\r\n", "\n");

        assertTrue(source.contains(
                "if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;"
        ));
        assertTrue(source.contains("ShulkerPaletteContents.isShulker(offhandShulker)"));
        assertTrue(source.contains("CANDIDATE_SELECTOR\n                .select(candidates, context.getLevel().getRandom())"));
        assertTrue(source.contains("new ItemStack(selected.placementItem(), 1)"));
        assertTrue(source.contains("new BlockPlaceContext("));
        assertTrue(source.contains("PlacementSoundBroadcastScope.open()"));
        assertTrue(source.contains("result = selected.placementItem().place(placementContext);"));
        assertTrue(source.contains("if (result.consumesAction()\n"
                + "                && placementStack.isEmpty()\n"
                + "                && !player.getAbilities().instabuild)"));
        assertTrue(source.contains("actualSlot.getItem() == selected.sourceItem()"));
        assertEquals(1, occurrences(source, "actualSlot.shrink(1);"));
        assertTrue(source.contains("ShulkerPaletteContents.write(offhandShulker, contents);"));
        assertTrue(source.contains("player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhandShulker);"));
        assertTrue(source.contains("player.getInventory().setChanged();"));
        assertTrue(source.indexOf("selected.placementItem().place(placementContext)") <
                source.indexOf("actualSlot.shrink(1)"));
        assertTrue(source.trim().endsWith("}"));
    }

    @Test
    void quantityWeightingAndExactFullBlockIdentityRemainIntact() throws IOException {
        String selector = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/palette/QuantityWeightedSelector.java"
        ));
        String resolver = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/geometry/CnmNibaruGeometryResolver.java"
        ));
        String collector = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/dev/aero/shulkertrowel/palette/PaletteCandidateCollector.java"
        ));

        assertTrue(selector.contains("candidate.weight()"));
        assertTrue(selector.contains("random.nextInt(totalWeight)"));
        assertTrue(resolver.contains("targetGeometry == TargetGeometry.FULL"));
        assertTrue(resolver.contains("blockItem.getBlock() == block"));
        assertTrue(collector.contains("sourceStack.getCount()"));
        assertTrue(collector.indexOf("resolveGeometry") < collector.indexOf("candidates.add"));
        assertTrue(collector.contains("sourceItem,") && collector.contains("placementItem.get()"));
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
