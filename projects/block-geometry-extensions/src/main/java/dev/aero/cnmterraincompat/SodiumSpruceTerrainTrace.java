package dev.aero.cnmterraincompat;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded C98 observations from Sodium 0.9.1's actual terrain meshing path.
 *
 * <p>The optional mixin captures Sodium's resolved provider colors immediately after
 * {@code ColorProvider#getColors}, then the ABGR values copied into its chunk vertices. It
 * neither selects a provider nor mutates a quad, vertex, material, or render layer.</p>
 */
public final class SodiumSpruceTerrainTrace {
    private static final Logger LOGGER = LoggerFactory.getLogger("BGE/SodiumSpruceTrace");
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private static final Set<Block> TINT_EMITTED = ConcurrentHashMap.newKeySet();
    private static final Set<Block> VERTEX_EMITTED = ConcurrentHashMap.newKeySet();

    private SodiumSpruceTerrainTrace() {}

    public static void beginModel(BlockState state, BlockPos pos) {
        if (isTarget(state.getBlock())) CONTEXT.set(new Context(state, pos.immutable()));
    }

    public static void endModel() {
        CONTEXT.remove();
    }

    public static void afterColorProvider(Object provider, int tintIndex, int[] resolvedArgb,
            int[] modelArgb) {
        Context context = CONTEXT.get();
        if (context == null || tintIndex < 0 || !TINT_EMITTED.add(context.state().getBlock())) return;
        LOGGER.info("BGE_SODIUM_SPRUCE_TINT_TRACE|stage=sodium_color_provider_after_getColors"
                        + "|familyRole={}|block={}|state={}|pos={}|canonicalParent={}"
                        + "|canonicalState={}|tintIndex={}|provider={}|resolvedArgb={}|modelArgb={}",
                context.familyRole(), blockId(context.state().getBlock()), context.state(), context.pos(),
                context.canonicalParent(), context.canonicalState(), tintIndex,
                provider.getClass().getName(), colors(resolvedArgb), colors(modelArgb));
    }

    public static void afterVertexWrite(int tintIndex, int[] packedAbgr) {
        Context context = CONTEXT.get();
        if (context == null || tintIndex < 0 || !VERTEX_EMITTED.add(context.state().getBlock())) return;
        LOGGER.info("BGE_SODIUM_SPRUCE_TINT_TRACE|stage=sodium_vertex_after_bufferQuad"
                        + "|familyRole={}|block={}|state={}|pos={}|canonicalParent={}"
                        + "|canonicalState={}|tintIndex={}|packedVertexAbgr={}",
                context.familyRole(), blockId(context.state().getBlock()), context.state(), context.pos(),
                context.canonicalParent(), context.canonicalState(), tintIndex, colors(packedAbgr));
    }

    private static boolean isTarget(Block block) {
        return block == Blocks.SPRUCE_LEAVES || FoliageTintContract.isSpruceGeometry(block);
    }

    private static String blockId(Block block) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static String colors(int[] colors) {
        return Arrays.stream(colors).mapToObj(value -> String.format("0x%08X", value))
                .toList().toString();
    }

    private record Context(BlockState state, BlockPos pos) {
        private String familyRole() {
            return state.getBlock() == Blocks.SPRUCE_LEAVES ? "canonical" : "derived";
        }

        private String canonicalParent() {
            return binding().map(binding -> blockId(binding.canonicalMaterial()))
                    .orElseGet(() -> blockId(state.getBlock()));
        }

        private String canonicalState() {
            return binding().flatMap(binding -> binding.canonicalState(state))
                    .map(Object::toString).orElseGet(state::toString);
        }

        private Optional<BgeMaterialBindings.Binding> binding() {
            return BgeMaterialBindings.all().stream()
                    .filter(binding -> binding.physicalBlock() == state.getBlock()).findFirst();
        }
    }
}
