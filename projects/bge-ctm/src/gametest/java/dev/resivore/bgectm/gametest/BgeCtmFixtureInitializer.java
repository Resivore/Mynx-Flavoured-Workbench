package dev.resivore.bgectm.gametest;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registers one BGE-looking ID without a BGE binding to catch registry-name inference. */
public final class BgeCtmFixtureInitializer implements ModInitializer {
    private static final Identifier UNBOUND_ID = Identifier.fromNamespaceAndPath(
            "bge_ctm_gametest", "more_slabs_stairs_and_walls/stone_layer");
    private static final ResourceKey<Block> UNBOUND_KEY = ResourceKey.create(Registries.BLOCK, UNBOUND_ID);
    public static final VerticalSlabBlock UNBOUND_BGE_LOOKING_VERTICAL = new VerticalSlabBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(UNBOUND_KEY));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, UNBOUND_KEY, UNBOUND_BGE_LOOKING_VERTICAL);
    }
}
