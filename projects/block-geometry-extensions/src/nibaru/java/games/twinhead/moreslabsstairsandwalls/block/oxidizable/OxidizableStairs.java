package games.twinhead.moreslabsstairsandwalls.block.oxidizable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public class OxidizableStairs extends BaseStairs implements CustomOxidizable {

    private final WeatheringCopper.WeatherState oxidationLevel;

    public OxidizableStairs(ModBlocks block,BlockState defaultState, WeatheringCopper.WeatherState oxidationLevel, ModBlocks nextBlock, Properties arg) {
        super(block,defaultState, arg);
        this.oxidationLevel = oxidationLevel;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return useItem(stack, state, world, pos, player, hand);
    }

    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        changeOverTime(state, world, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return this.oxidationLevel != WeatheringCopper.WeatherState.OXIDIZED;
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.oxidationLevel;
    }

}
