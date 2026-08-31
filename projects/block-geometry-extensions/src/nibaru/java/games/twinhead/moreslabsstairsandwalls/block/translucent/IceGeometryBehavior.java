package games.twinhead.moreslabsstairsandwalls.block.translucent;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class IceGeometryBehavior {
    private IceGeometryBehavior() {}

    public static void randomTick(ModBlocks family, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (family == ModBlocks.ICE
                && level.getBrightness(LightLayer.BLOCK, pos) > 11 - state.getLightDampening()) {
            melt(level, pos);
        }
    }

    public static void afterPlayerDestroy(ModBlocks family, Level level, Player player, BlockPos pos, ItemStack tool) {
        if (family != ModBlocks.ICE || EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_ICE_MELTING)) {
            return;
        }
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        if (below.blocksMotion() || below.liquid()) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        }
    }

    private static void melt(Level level, BlockPos pos) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        level.neighborChanged(pos, Blocks.WATER, null);
    }
}
