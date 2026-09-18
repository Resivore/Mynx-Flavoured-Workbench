package dev.resivore.slabdecorations.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Test-only registration of the exact optional continuation identity, with no Ribbits linkage. */
public final class SystemicFixtureInitializer implements ModInitializer {
    public static final Identifier TOADSTOOL_STEM_ID =
            Identifier.fromNamespaceAndPath("ribbits", "toadstool_stem");
    public static final Block TOADSTOOL_STEM = new Block(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, TOADSTOOL_STEM_ID)));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK,
                ResourceKey.create(Registries.BLOCK, TOADSTOOL_STEM_ID), TOADSTOOL_STEM);
    }
}
