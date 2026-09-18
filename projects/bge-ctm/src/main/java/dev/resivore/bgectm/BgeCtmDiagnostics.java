package dev.resivore.bgectm;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concise bounded diagnostics for Canary 4's client rendering decisions. */
public final class BgeCtmDiagnostics {
    public static final String PREFIX = "[BGE-CTM DIAG]";
    private static final Logger LOGGER = LoggerFactory.getLogger("bge-ctm");
    private static final int CAP = configuredCap();
    private static final boolean ENABLED = !Boolean.getBoolean("bge_ctm.diagnostics.disable");
    private static final DiagnosticLimiter LIMITER = new DiagnosticLimiter(CAP);

    private BgeCtmDiagnostics() {}

    public static void startup() {
        info("STARTUP", "BGE × CTM=" + version("bge_ctm")
                + " minecraft=" + FabricLoader.getInstance().getModContainer("minecraft")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown")
                + " continuity=" + version("continuity")
                + " bge=" + version("cnm_terrain_slabs_compat")
                + " regularHook=required-exact overlayHook=required-exact"
                + " cap=" + CAP + " enabled=" + ENABLED);
    }

    public static void appearance(BlockState physical, CanonicalAppearanceResolver.Resolution resolution) {
        String key = "APPEARANCE|" + stateKey(physical) + '|' + resolution.policy();
        info(key, "event=APPEARANCE physical=" + display(physical)
                + " carrier=" + resolution.binding().map(binding -> binding.carrier().name())
                        .orElse("UNSUPPORTED")
                + " canonical=" + display(resolution.appearance())
                + " reason=" + appearanceReason(resolution));
    }

    public static void regular(BlockState source, BlockPos pos, BlockState sourceAppearance,
            BlockState other, BlockPos otherPos, BlockState otherAppearance, Object predicate,
            boolean upstream, @Nullable SurfaceContactResolver.QuadSurface quad,
            SurfaceContactResolver.Decision stateDecision,
            SurfaceContactResolver.Decision geometryDecision, boolean result, String reason) {
        String key = "REGULAR|" + stateKey(source) + '|' + stateKey(other) + '|'
                + pos.subtract(otherPos) + '|' + reason + '|' + result;
        info(key, "event=REGULAR source=" + display(source) + " sourceAppearance="
                + display(sourceAppearance) + " neighbor=" + display(other)
                + " neighborAppearance=" + display(otherAppearance) + " delta="
                + otherPos.subtract(pos) + " predicate=" + predicateName(predicate)
                + " upstream=" + upstream + " quad=" + quad + " state=" + stateDecision
                + " geometry=" + geometryDecision + " result=" + result + " reason=" + reason);
    }

    public static void overlay(BlockState receiver, BlockPos receiverPos, BlockState receiverAppearance,
            BlockState source, BlockPos sourcePos, BlockState sourceAppearance, boolean nativeFull,
            boolean promoted, @Nullable SurfaceContactResolver.QuadSurface quad,
            SurfaceContactResolver.Decision geometryDecision, boolean semantic, boolean result,
            String reason) {
        String key = "OVERLAY|" + stateKey(receiver) + '|' + stateKey(source) + '|'
                + sourcePos.subtract(receiverPos) + '|' + reason + '|' + result;
        info(key, "event=OVERLAY receiver=" + display(receiver) + " receiverAppearance="
                + display(receiverAppearance) + " source=" + display(source)
                + " sourceAppearance=" + display(sourceAppearance) + " delta="
                + sourcePos.subtract(receiverPos) + " nativeFull=" + nativeFull
                + " partialPromoted=" + promoted + " semantic=" + semantic + " quad=" + quad
                + " geometry=" + geometryDecision + " result=" + result + " reason=" + reason);
    }

    public static String display(BlockState state) {
        if (state == null) return "null";
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + state.toString();
    }

    private static void info(String signature, String message) {
        if (!ENABLED) return;
        switch (LIMITER.admit(signature)) {
            case ACCEPTED -> LOGGER.info("{} {}", PREFIX, message);
            case SUPPRESS_NOTICE -> LOGGER.info("{} cap={} reached; further unique records suppressed", PREFIX, CAP);
            case DUPLICATE, SUPPRESSED -> { }
        }
    }

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unavailable");
    }

    private static String stateKey(BlockState state) {
        return state == null ? "null" : display(state);
    }

    private static String appearanceReason(CanonicalAppearanceResolver.Resolution resolution) {
        return switch (resolution.policy()) {
            case ELIGIBLE_ORDINARY_SLAB, ELIGIBLE_LAYER, ELIGIBLE_VERTICAL_SLAB -> "PROJECTED";
            case NON_PROFILE_GEOMETRY -> "NO_PROFILE";
            case STEP_GEOMETRY, CORNER_GEOMETRY, QUARTER_COLUMN_GEOMETRY, UNKNOWN_GEOMETRY -> "UNSUPPORTED_GEOMETRY";
            case UNSUPPORTED_VISUAL_PROFILE -> "UNSUPPORTED_VISUAL";
            case UNMAPPABLE_CANONICAL_STATE -> "UNMAPPABLE_CANONICAL_STATE";
        };
    }

    private static String predicateName(Object predicate) {
        return predicate == null ? "unknown" : predicate.getClass().getSimpleName();
    }

    private static int configuredCap() {
        try {
            return Math.clamp(Integer.parseInt(System.getProperty("bge_ctm.diagnostics.cap", "200")), 1, 300);
        } catch (NumberFormatException ignored) {
            return 200;
        }
    }
}
