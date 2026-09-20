package dev.resivore.slabdecorations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;

/** Narrow optional contracts with no foreign classes in this mod's linkage graph. */
public final class OptionalSurfaceAdapters {
    private static final String ENDERSCAPE_DIRECTIONAL_VEGETATION =
            "net.penumbra.enderscape.block.DirectionalVegetationBlock";
    private static final String ENDERSCAPE_VEILED_LEAF_PILE =
            "net.penumbra.enderscape.block.VeiledLeafPileBlock";
    private static final String ENDERSCAPE_PURUBERRY_VINE =
            "net.penumbra.enderscape.block.PuruberryVine";
    private static final String ENDERSCAPE_PURUBERRY_FLOWER =
            "net.penumbra.enderscape.block.PuruberryFlowerBlock";
    private static final String ENDERSCAPE_UNRIPE_PURUBERRY =
            "net.penumbra.enderscape.block.UnripePuruberryBlock";
    private static final String ENDERSCAPE_RIPE_PURUBERRY =
            "net.penumbra.enderscape.block.RipePuruberryBlock";
    private static final String ENDERSCAPE_VOID_TORCH =
            "net.penumbra.enderscape.block.VoidTorchBlock";

    private OptionalSurfaceAdapters() {}

    static boolean isRibbitsSwampLantern(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("ribbits")
                && state.hasProperty(BlockStateProperties.HANGING)
                && state.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    /**
     * Enderscape exposes one directional-vegetation superclass.  Checking its class contract
     * avoids a registry-name allowlist while retaining ordinary behaviour for its horizontal
     * brackets and other non-vertical states.
     */
    static Optional<Direction> enderscapeDirectionalFacing(BlockState state) {
        if (!isInstanceOf(state.getBlock(), ENDERSCAPE_DIRECTIONAL_VEGETATION)
                || !state.hasProperty(BlockStateProperties.FACING)) {
            return Optional.empty();
        }
        return Optional.of(state.getValue(BlockStateProperties.FACING));
    }

    static boolean isEnderscapeDirectionalVegetation(BlockState state) {
        return isInstanceOf(state.getBlock(), ENDERSCAPE_DIRECTIONAL_VEGETATION);
    }

    static boolean isEnderscapeVeiledLeafPile(BlockState state) {
        return isInstanceOf(state.getBlock(), ENDERSCAPE_VEILED_LEAF_PILE);
    }

    static boolean isEnderscapeVoidTorch(BlockState state) {
        return isInstanceOf(state.getBlock(), ENDERSCAPE_VOID_TORCH);
    }

    static Optional<PuruberryPart> puruberryPart(BlockState state) {
        Block block = state.getBlock();
        if (isInstanceOf(block, ENDERSCAPE_PURUBERRY_VINE)) return Optional.of(PuruberryPart.VINE);
        if (isInstanceOf(block, ENDERSCAPE_PURUBERRY_FLOWER)) return Optional.of(PuruberryPart.FLOWER);
        if (isInstanceOf(block, ENDERSCAPE_UNRIPE_PURUBERRY)) return Optional.of(PuruberryPart.UNRIPE);
        if (isInstanceOf(block, ENDERSCAPE_RIPE_PURUBERRY)) return Optional.of(PuruberryPart.RIPE);
        return Optional.empty();
    }

    public static boolean isEnderscapeStructureGrowthReady(BlockState state) {
        return state.getProperties().stream()
                .filter(property -> property.getName().equals("stage")
                        && property.getValueClass() == Integer.class)
                .findFirst()
                .map(property -> state.getValue(property) instanceof Integer stage && stage > 0)
                .orElse(false);
    }

    private static boolean isInstanceOf(Block block, String expectedClassName) {
        for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
            if (expectedClassName.equals(type.getName())) return true;
        }
        return false;
    }

    enum PuruberryPart {
        VINE,
        FLOWER,
        UNRIPE,
        RIPE
    }
}
