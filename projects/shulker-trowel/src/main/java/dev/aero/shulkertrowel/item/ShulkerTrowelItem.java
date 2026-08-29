package dev.aero.shulkertrowel.item;

import dev.aero.shulkertrowel.geometry.CnmNibaruGeometryResolver;
import dev.aero.shulkertrowel.geometry.GeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import dev.aero.shulkertrowel.palette.CandidateSelector;
import dev.aero.shulkertrowel.palette.PaletteCandidate;
import dev.aero.shulkertrowel.palette.PaletteCandidateCollector;
import dev.aero.shulkertrowel.palette.QuantityWeightedSelector;
import dev.aero.shulkertrowel.palette.ShulkerPaletteContents;
import dev.aero.shulkertrowel.sound.PlacementSoundBroadcastScope;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class ShulkerTrowelItem extends Item {
    private static final GeometryResolver GEOMETRY_RESOLVER = new CnmNibaruGeometryResolver();
    private static final PaletteCandidateCollector CANDIDATE_COLLECTOR =
            new PaletteCandidateCollector(GEOMETRY_RESOLVER);
    private static final CandidateSelector CANDIDATE_SELECTOR = new QuantityWeightedSelector();

    public ShulkerTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        // The client never predicts a random candidate or changes container data.
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack offhandShulker = player.getOffhandItem();
        if (!ShulkerPaletteContents.isShulker(offhandShulker)) return InteractionResult.FAIL;

        NonNullList<ItemStack> contents = ShulkerPaletteContents.read(offhandShulker);
        TargetGeometry geometry = TrowelGeometryState.get(context.getItemInHand());
        List<PaletteCandidate> candidates = CANDIDATE_COLLECTOR.collect(contents, geometry);
        PaletteCandidate selected = CANDIDATE_SELECTOR
                .select(candidates, context.getLevel().getRandom())
                .orElse(null);
        if (selected == null) return InteractionResult.FAIL;

        ItemStack placementStack = new ItemStack(selected.placementItem(), 1);
        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        BlockPlaceContext placementContext = new BlockPlaceContext(
                player,
                context.getHand(),
                placementStack,
                hitResult
        );
        InteractionResult result;
        try (PlacementSoundBroadcastScope ignored = PlacementSoundBroadcastScope.open()) {
            result = selected.placementItem().place(placementContext);
        }

        if (result.consumesAction()
                && placementStack.isEmpty()
                && !player.getAbilities().instabuild) {
            ItemStack actualSlot = contents.get(selected.slot());
            // Re-check the exact selected block identity before mutating authoritative contents.
            if (actualSlot.getItem() == selected.sourceItem() && !actualSlot.isEmpty()) {
                actualSlot.shrink(1);
                ShulkerPaletteContents.write(offhandShulker, contents);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offhandShulker);
                player.getInventory().setChanged();
            }
        }
        return result;
    }

}
