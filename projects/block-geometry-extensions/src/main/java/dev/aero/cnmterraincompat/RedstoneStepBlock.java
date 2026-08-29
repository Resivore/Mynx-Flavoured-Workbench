package dev.aero.cnmterraincompat;
import games.twinhead.moreslabsstairsandwalls.block.redstone.RedstoneSemantics;
import net.minecraft.core.BlockPos; import net.minecraft.core.Direction; import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
public final class RedstoneStepBlock extends ProviderStepBlock {
 public RedstoneStepBlock(Properties p){super(p);}
 @Override public boolean isSignalSource(BlockState s){return RedstoneSemantics.isSignalSource();}
 @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return RedstoneSemantics.signal();}
}
