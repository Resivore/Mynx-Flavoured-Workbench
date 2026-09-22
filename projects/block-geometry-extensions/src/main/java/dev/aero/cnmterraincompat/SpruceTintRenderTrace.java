package dev.aero.cnmterraincompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded runtime evidence at the final Java-side terrain tint seam.
 *
 * <p>{@code ModelBlockRenderer} invokes this immediately before it multiplies a tinted quad into
 * its {@code QuadInstance}. The trace intentionally observes only native spruce and the frozen,
 * exact eight-role BGE spruce family. It makes no rendering decision, does not register a color,
 * and never identifies a target from an ID or name.</p>
 */
public final class SpruceTintRenderTrace {
    private static final Logger LOGGER = LoggerFactory.getLogger("BGE/SpruceTintTrace");
    private static final Set<Block> EMITTED = ConcurrentHashMap.newKeySet();

    private SpruceTintRenderTrace() {}

    /** Called from the actual vanilla {@code ModelBlockRenderer} submission path. */
    public static void beforeQuadColorMultiply(BlockAndTintGetter level, BlockState state,
            BlockPos pos, BakedQuad quad) {
        Block block = state.getBlock();
        boolean canonical = block == Blocks.SPRUCE_LEAVES;
        boolean derived = FoliageTintContract.isSpruceGeometry(block);
        if (!canonical && !derived) return;

        int tintIndex = quad.materialInfo().tintIndex();
        if (tintIndex < 0 || !EMITTED.add(block)) return;

        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, tintIndex);
        int argb = source == null ? -1 : source.colorInWorld(state, level, pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        Identifier spriteId = quad.materialInfo().sprite() == null
                ? null : quad.materialInfo().sprite().contents().name();
        LOGGER.info("BGE_SPRUCE_TINT_TRACE|stage=model_block_renderer_before_multiply"
                        + "|familyRole={}|block={}|state={}|pos={}|quadDirection={}|sprite={}"
                        + "|tintIndex={}|source={}|argb={}|expected={}",
                canonical ? "canonical" : "derived",
                blockId, state, pos, quad.direction(), spriteId, tintIndex,
                source == null ? "null" : source.getClass().getName(), argbHex(argb),
                argbHex(FoliageTintContract.SPRUCE_FIXED_ARGB));
    }

    private static String argbHex(int argb) {
        return String.format("0x%08X", argb);
    }
}
