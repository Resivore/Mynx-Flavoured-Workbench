package dev.resivore.bgectm;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.continuity.OverlayEmissionGeometry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concise, category-bounded diagnostics for client rendering decisions. */
public final class BgeCtmDiagnostics {
    public static final String PREFIX = "[BGE-CTM DIAG]";
    // Opt-in. The legacy disable flag remains an explicit override for existing launch scripts.
    private static final boolean ENABLED = Boolean.getBoolean("bge_ctm.diagnostics")
            && !Boolean.getBoolean("bge_ctm.diagnostics.disable");

    private static final class Reporting {
        private static final Logger LOGGER = LoggerFactory.getLogger("bge-ctm");
        private static final int APPEARANCE_CAP = configuredCap("appearanceCap", 100);
        private static final int REGULAR_CAP = configuredCap("regularCap", 200);
        private static final int RULE_SELECTION_CAP = configuredCap("ruleSelectionCap", 100);
        private static final int OVERLAY_CAP = configuredCap("overlayCap", 100);
        private static final int OVERLAY_BASELINE_CAP = configuredCap("overlayBaselineCap", 10);
        private static final int OVERLAY_EMIT_CAP = configuredCap("overlayEmitCap", 100);
        private static final DiagnosticBudgets BUDGETS = new DiagnosticBudgets(
                APPEARANCE_CAP, REGULAR_CAP, RULE_SELECTION_CAP, OVERLAY_CAP, OVERLAY_BASELINE_CAP,
                OVERLAY_EMIT_CAP);
    }

    private BgeCtmDiagnostics() {}

    /** JIT-friendly guard: callers must test this before diagnostic-only inspection or allocation. */
    public static boolean enabled() { return ENABLED; }

    public static void startup() {
        if (!ENABLED) return;
        Reporting.LOGGER.info("{} STARTUP BGE × CTM=" + version("bge_ctm")
                + " minecraft=" + FabricLoader.getInstance().getModContainer("minecraft")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown")
                + " continuity=" + version("continuity")
                + " bge=" + version("cnm_terrain_slabs_compat")
                + " regularHook=required-exact overlayHook=required-exact"
                + " appearanceCap=" + Reporting.APPEARANCE_CAP + " regularCap=" + Reporting.REGULAR_CAP
                + " ruleSelectionCap=" + Reporting.RULE_SELECTION_CAP + " overlayCap=" + Reporting.OVERLAY_CAP
                + " overlayBaselineCap=" + Reporting.OVERLAY_BASELINE_CAP
                + " overlayEmitCap=" + Reporting.OVERLAY_EMIT_CAP
                + " enabled=" + ENABLED, PREFIX);
    }

    public static void appearance(BlockState physical, CanonicalAppearanceResolver.Resolution resolution) {
        if (!ENABLED) return;
        String key = "APPEARANCE|" + stateKey(physical) + '|' + resolution.policy();
        info(DiagnosticBudgets.Category.APPEARANCE, key, "event=APPEARANCE physical=" + display(physical)
                + " topology=" + resolution.binding().map(binding -> binding.topology().name())
                        .orElse("UNSUPPORTED")
                + " canonical=" + display(resolution.appearance())
                + " reason=" + appearanceReason(resolution));
    }

    public static void regular(BlockState source, BlockPos pos, BlockState sourceAppearance,
            BlockState other, BlockPos otherPos, BlockState otherAppearance, Object predicate,
            boolean nativeSemantic, boolean canonicalSemantic,
            @Nullable SurfaceContactResolver.QuadSurface quad,
            @Nullable SurfaceContactResolver.Decision stateDecision,
            @Nullable SurfaceContactResolver.Decision geometryDecision, boolean result, String reason) {
        if (!ENABLED) return;
        String key = "REGULAR|" + stateKey(source) + '|' + stateKey(other) + '|'
                + pos.subtract(otherPos) + '|' + reason + '|' + result;
        info(DiagnosticBudgets.Category.REGULAR, key, "event=REGULAR source=" + display(source) + " sourceAppearance="
                + display(sourceAppearance) + " neighbor=" + display(other)
                + " neighborAppearance=" + display(otherAppearance) + " delta="
                + otherPos.subtract(pos) + " predicate=" + predicateName(predicate)
                + " nativeSemantic=" + nativeSemantic
                + " canonicalSemantic=" + canonicalSemantic + " quad=" + quad
                + " state=" + (stateDecision == null ? "NOT_EVALUATED" : stateDecision)
                + " geometry=" + (geometryDecision == null ? "NOT_EVALUATED" : geometryDecision) + " result=" + result + " reason=" + reason);
    }

    /** Records the exact Continuity slice chosen for a managed rendered quad. */
    public static void ruleSelection(BlockState physical, BlockPos pos, BlockState appearance,
            Object sprite, int processors, int multipassProcessors) {
        if (!ENABLED) return;
        if (!managed(physical)) return;
        String reason = processors == 0 && multipassProcessors == 0
                ? "NO_PROCESSOR" : "PROCESSOR_SELECTED";
        String key = "RULE_SELECTION|" + stateKey(physical) + '|' + sprite + '|'
                + processors + '|' + multipassProcessors;
        info(DiagnosticBudgets.Category.RULE_SELECTION, key,
                "event=RULE_SELECTION physical=" + display(physical) + " canonical="
                        + display(appearance) + " sprite=" + sprite + " processors=" + processors
                        + " multipassProcessors=" + multipassProcessors + " reason=" + reason);
    }

    public static void overlay(BlockState receiver, BlockPos receiverPos, BlockState receiverAppearance,
            BlockState source, BlockPos sourcePos, BlockState sourceAppearance, boolean nativeFull,
            boolean promoted, boolean nativeConnection, boolean canonicalConnection,
            @Nullable SurfaceContactResolver.QuadSurface quad,
            @Nullable SurfaceContactResolver.Decision geometryDecision, boolean semantic, boolean result,
            String reason) {
        if (!ENABLED) return;
        String key = "OVERLAY|" + stateKey(receiver) + '|' + stateKey(source) + '|'
                + sourcePos.subtract(receiverPos) + '|' + reason + '|' + result;
        DiagnosticBudgets.Category category = managed(receiver) || managed(source)
                ? DiagnosticBudgets.Category.OVERLAY : DiagnosticBudgets.Category.OVERLAY_BASELINE;
        String event = category == DiagnosticBudgets.Category.OVERLAY
                ? "OVERLAY" : "OVERLAY_BASELINE";
        info(category, key, "event=" + event + " receiver=" + display(receiver) + " receiverAppearance="
                + display(receiverAppearance) + " source=" + display(source)
                + " sourceAppearance=" + display(sourceAppearance) + " delta="
                + sourcePos.subtract(receiverPos) + " nativeFull=" + nativeFull
                + " partialPromoted=" + promoted + " nativeConnection=" + nativeConnection
                + " canonicalConnection=" + canonicalConnection
                + " semantic=" + semantic + " quad=" + quad
                + " geometry=" + (geometryDecision == null ? "NOT_EVALUATED" : geometryDecision) + " result=" + result + " reason=" + reason);
    }

    /** Emits only for a BGE-managed receiver; full unrelated overlays never consume this budget. */
    public static void overlayEmit(BlockState receiver, net.minecraft.core.Direction face,
            @Nullable QuadSurface captured, @Nullable OverlayEmissionGeometry.Projection emitted,
            String path, String reason) {
        if (!ENABLED) return;
        if (!managed(receiver)) return;
        String signature = "OVERLAY_EMIT|" + stateKey(receiver) + '|' + face + '|'
                + captured + '|' + path + '|' + reason;
        String emittedDescription = emitted == null ? "none" : "face=" + emitted.surface().normal()
                + ",plane16=" + emitted.surface().plane16() + ",u=" + emitted.surface().uMin16()
                + ".." + emitted.surface().uMax16() + ",v=" + emitted.surface().vMin16()
                + ".." + emitted.surface().vMax16() + ",square=" + emitted.left() + ','
                + emitted.bottom() + ',' + emitted.right() + ',' + emitted.top() + ','
                + emitted.depth() + ",uv0=" + emitted.uvU(0) + ',' + emitted.uvV(0)
                + ",uv2=" + emitted.uvU(2) + ',' + emitted.uvV(2);
        info(DiagnosticBudgets.Category.OVERLAY_EMIT, signature,
                "event=OVERLAY_EMIT receiver=" + display(receiver) + " face=" + face
                        + " captured=" + captured + " emitted=" + emittedDescription
                        + " path=" + path + " reason=" + reason);
    }

    public static String display(BlockState state) {
        if (state == null) return "null";
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + state.toString();
    }

    private static void info(DiagnosticBudgets.Category category, String signature, String message) {
        if (!ENABLED) return;
        switch (Reporting.BUDGETS.admit(category, signature)) {
            case ACCEPTED -> Reporting.LOGGER.info("{} {}", PREFIX, message);
            case SUPPRESS_NOTICE -> Reporting.LOGGER.info("{} category={} cap={} reached; further unique records suppressed",
                    PREFIX, category, cap(category));
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
            case ELIGIBLE_BOUND_SURFACE -> "PROJECTED";
            case NON_BGE_GEOMETRY, CANONICAL_ROOT -> "NO_BINDING";
            case UNSUPPORTED_SURFACE_CONTRACT -> "UNSUPPORTED_GEOMETRY";
            case UNSUPPORTED_VISUAL_PROFILE -> "UNSUPPORTED_VISUAL";
            case UNMAPPABLE_CANONICAL_STATE -> "UNMAPPABLE_CANONICAL_STATE";
        };
    }

    private static String predicateName(Object predicate) {
        return predicate == null ? "unknown" : predicate.getClass().getSimpleName();
    }

    private static boolean managed(BlockState state) {
        return state != null && CanonicalAppearanceResolver.inspect(state).inherited();
    }

    private static int cap(DiagnosticBudgets.Category category) {
        return switch (category) {
            case APPEARANCE -> Reporting.APPEARANCE_CAP;
            case REGULAR -> Reporting.REGULAR_CAP;
            case RULE_SELECTION -> Reporting.RULE_SELECTION_CAP;
            case OVERLAY -> Reporting.OVERLAY_CAP;
            case OVERLAY_BASELINE -> Reporting.OVERLAY_BASELINE_CAP;
            case OVERLAY_EMIT -> Reporting.OVERLAY_EMIT_CAP;
        };
    }

    private static int configuredCap(String name, int defaultValue) {
        try {
            return Math.clamp(Integer.parseInt(System.getProperty(
                    "bge_ctm.diagnostics." + name, Integer.toString(defaultValue))), 1, 300);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
