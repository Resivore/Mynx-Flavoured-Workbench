package games.twinhead.moreslabsstairsandwalls.block.oxidizable;

import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CustomWaxedCopper {
    default InteractionResult useWaxedItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand) {
        var profile = NibaruMaterialProfiles.fromBlock(state.getBlock()).orElseThrow();
        return CopperSemantics.interact(state, profile, stack, level, pos, player, hand,
                target -> CopperSemantics.nativeGeometry(state.getBlock(), target));
    }
}
