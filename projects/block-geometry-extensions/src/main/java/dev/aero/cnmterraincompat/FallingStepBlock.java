package dev.aero.cnmterraincompat;
import games.twinhead.moreslabsstairsandwalls.block.falling.FallingSemantics;
import net.minecraft.core.BlockPos; import net.minecraft.core.Direction; import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource; import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level; import net.minecraft.world.level.LevelReader; import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Fallable; import net.minecraft.world.level.block.state.BlockState;
public class FallingStepBlock extends ProviderStepBlock implements Fallable {
 public FallingStepBlock(Properties p){super(p);} @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState o,boolean n){FallingSemantics.schedule(l,p,this);}
 @Override public BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r){FallingSemantics.schedule(t,p,this);return super.updateShape(s,l,t,p,d,np,ns,r);}
 @Override public void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(FallingSemantics.shouldFall(l,p))FallingBlockEntity.fall(l,p,s);}
 @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){FallingSemantics.animateDust(s,l,p,r);}
}
