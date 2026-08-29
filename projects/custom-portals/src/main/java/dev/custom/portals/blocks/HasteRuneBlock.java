package dev.custom.portals.blocks;

import com.mojang.serialization.MapCodec;
import dev.custom.portals.CustomPortals;
import dev.custom.portals.data.CustomPortal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class HasteRuneBlock extends AbstractRuneBlock {

    public static final MapCodec<HasteRuneBlock> CODEC = simpleCodec(HasteRuneBlock::new);

    @Override
    public MapCodec<HasteRuneBlock> codec() {
        return CODEC;
    }

    public HasteRuneBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void registerOnPortal(CustomPortal portal, Level world) {
        portal.addHaste();
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }

    @Override
    public void unregisterOnPortal(CustomPortal portal, Level world) {
        portal.removeHaste();
        if (!world.isClientSide())
            CustomPortals.PORTALS.get(world).syncWithAll(((ServerLevel)world).getServer());
    }
}
