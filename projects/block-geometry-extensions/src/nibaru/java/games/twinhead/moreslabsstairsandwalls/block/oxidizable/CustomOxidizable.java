package games.twinhead.moreslabsstairsandwalls.block.oxidizable;

import games.twinhead.moreslabsstairsandwalls.api.material.MaterialTransition;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public interface CustomOxidizable extends ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
    WeatheringCopper.WeatherState getAge();

    default NibaruMaterialProfile copperProfile(BlockState state) {
        return NibaruMaterialProfiles.fromBlock(state.getBlock()).orElseThrow();
    }

    default InteractionResult useItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand) {
        return CopperSemantics.interact(state, copperProfile(state), stack, level, pos, player, hand,
                target -> CopperSemantics.nativeGeometry(state.getBlock(), target));
    }

    @Override
    default Optional<BlockState> getNext(BlockState state) {
        return CopperSemantics.transition(state, copperProfile(state), MaterialTransition.Type.NEXT_OXIDATION,
                target -> CopperSemantics.nativeGeometry(state.getBlock(), target));
    }

    @Override
    default float getChanceModifier() {
        return getAge() == WeatheringCopper.WeatherState.UNAFFECTED ? 0.75F : 1.0F;
    }
}
