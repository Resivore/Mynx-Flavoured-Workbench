package dev.custom.portals.blocks;

import com.mojang.serialization.MapCodec;
import dev.custom.portals.CustomPortals;
import dev.custom.portals.data.CustomPortal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class InfinityRuneBlock extends AbstractRuneBlock {

    public static final MapCodec<InfinityRuneBlock> CODEC = simpleCodec(InfinityRuneBlock::new);

    @Override
    public MapCodec<InfinityRuneBlock> codec() {
        return CODEC;
    }

    public InfinityRuneBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void registerOnPortal(CustomPortal portal, Level world) {
        portal.addInfinity();
        CustomPortals.PORTALS.get(world).tryWithAll(portal);
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }

    @Override
    public void unregisterOnPortal(CustomPortal portal, Level world) {
        portal.removeInfinity();
        if (portal.hasLinked())
            CustomPortals.PORTALS.get(world).tryWithAll(portal.getLinked());
        CustomPortals.PORTALS.get(world).tryWithAll(portal);
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }
}
