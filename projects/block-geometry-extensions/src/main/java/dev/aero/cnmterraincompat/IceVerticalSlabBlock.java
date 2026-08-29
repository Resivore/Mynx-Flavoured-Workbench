package dev.aero.cnmterraincompat;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks; import games.twinhead.moreslabsstairsandwalls.block.translucent.IceGeometryBehavior;
import net.minecraft.core.*; import net.minecraft.server.level.ServerLevel; import net.minecraft.util.RandomSource; import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.state.BlockState;
public final class IceVerticalSlabBlock extends ProviderVerticalSlabBlock { public IceVerticalSlabBlock(Properties p){super(p.randomTicks());}
 @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){IceGeometryBehavior.randomTick(ModBlocks.ICE,s,l,p,r);}
 @Override public void playerDestroy(Level l,Player x,BlockPos p,BlockState s,BlockEntity e,ItemStack t){super.playerDestroy(l,x,p,s,e,t);IceGeometryBehavior.afterPlayerDestroy(ModBlocks.ICE,l,x,p,t);}
 @Override public boolean skipRendering(BlockState s,BlockState n,Direction d){return NibaruProviderAdapter.cullsTranslucent(this,s,n)||super.skipRendering(s,n,d);}}
