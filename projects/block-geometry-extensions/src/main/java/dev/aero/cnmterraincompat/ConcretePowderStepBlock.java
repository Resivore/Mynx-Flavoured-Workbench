package dev.aero.cnmterraincompat;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSemantics;
import net.minecraft.core.BlockPos; import net.minecraft.core.Direction; import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource; import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level; import net.minecraft.world.level.LevelReader; import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block; import net.minecraft.world.level.block.state.BlockState;
public final class ConcretePowderStepBlock extends FallingStepBlock {
 public ConcretePowderStepBlock(Properties p){super(p);} private Block hardened(){return NibaruProviderAdapter.concreteHardening(this);}
 @Override public BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r){return ConcretePowderSemantics.shouldHarden(l,p,s)?ConcretePowderSemantics.hardenedState(s,hardened()):super.updateShape(s,l,t,p,d,np,ns,r);}
 @Override public void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(ConcretePowderSemantics.shouldHarden(l,p,s)){l.setBlock(p,ConcretePowderSemantics.hardenedState(s,hardened()),Block.UPDATE_ALL);return;}super.tick(s,l,p,r);}
 @Override public void onLand(Level l,BlockPos p,BlockState falling,BlockState current,FallingBlockEntity e){if(ConcretePowderSemantics.shouldHarden(l,p,current))l.setBlock(p,ConcretePowderSemantics.hardenedState(falling,hardened()),Block.UPDATE_ALL);}
}
