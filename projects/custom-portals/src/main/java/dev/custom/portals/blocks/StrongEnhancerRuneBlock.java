package dev.custom.portals.blocks;

import com.mojang.serialization.MapCodec;
import dev.custom.portals.CustomPortals;
import dev.custom.portals.data.CustomPortal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class StrongEnhancerRuneBlock extends AbstractRuneBlock {

    public static final MapCodec<StrongEnhancerRuneBlock> CODEC = simpleCodec(StrongEnhancerRuneBlock::new);

    @Override
    public MapCodec<StrongEnhancerRuneBlock> codec() {
        return CODEC;
    }

    public StrongEnhancerRuneBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void registerOnPortal(CustomPortal portal, Level world) {
        portal.addStrongEnhancer();
        CustomPortals.PORTALS.get(world).tryWithAll(portal);
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }
    
    @Override
    public void unregisterOnPortal(CustomPortal portal, Level world) {
        portal.removeStrongEnhancer();
        if (portal.hasLinked())
            CustomPortals.PORTALS.get(world).tryWithAll(portal.getLinked());
        CustomPortals.PORTALS.get(world).tryWithAll(portal);
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }
}
