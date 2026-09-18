package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.BgeCtmDiagnostics;
import dev.resivore.bgectm.SurfaceContactResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Thread-confined metadata used only to make a single overlay diagnostic explainable. */
public final class OverlayAttemptContext {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    private OverlayAttemptContext() {}

    public static void begin(BlockPos sourcePos, BlockState sourceAppearance, BlockState source,
            BlockPos receiverPos, BlockState receiverAppearance, BlockState receiver, Direction face) {
        if (!BgeCtmDiagnostics.enabled()) return;
        CURRENT.set(new Attempt(sourcePos.immutable(), sourceAppearance, source, receiverPos.immutable(),
                receiverAppearance, receiver, face));
    }

    @Nullable public static Attempt end() {
        if (!BgeCtmDiagnostics.enabled()) return null;
        Attempt attempt = CURRENT.get();
        CURRENT.remove();
        return attempt;
    }

    @Nullable public static Attempt current() { return BgeCtmDiagnostics.enabled() ? CURRENT.get() : null; }

    public static void gate(boolean nativeFull, boolean promoted) {
        if (!BgeCtmDiagnostics.enabled()) return;
        Attempt attempt = CURRENT.get();
        if (attempt != null) {
            attempt.nativeFull = nativeFull;
            attempt.promoted = promoted;
            if (!nativeFull && !promoted) attempt.reason = "PARTIAL_SOURCE_GATE_REJECT";
            if (promoted) attempt.reason = "PARTIAL_SOURCE_PROMOTED";
        }
    }

    public static void connectBlocks(boolean result) {
        if (!BgeCtmDiagnostics.enabled()) return;
        Attempt attempt = CURRENT.get();
        if (attempt != null && !result) attempt.reason = "CONNECT_BLOCKS_REJECT";
    }

    public static final class Attempt {
        public final BlockPos sourcePos;
        public final BlockState sourceAppearance;
        public final BlockState source;
        public final BlockPos receiverPos;
        public final BlockState receiverAppearance;
        public final BlockState receiver;
        public final Direction face;
        public boolean nativeFull;
        public boolean promoted;
        public boolean semantic;
        public boolean result;
        public String reason = "NATIVE_SEMANTIC_REJECT";
        @Nullable public SurfaceContactResolver.QuadSurface quad;
        public SurfaceContactResolver.Decision geometry = SurfaceContactResolver.Decision.BYPASS_UNRELATED;

        private Attempt(BlockPos sourcePos, BlockState sourceAppearance, BlockState source,
                BlockPos receiverPos, BlockState receiverAppearance, BlockState receiver, Direction face) {
            this.sourcePos = sourcePos;
            this.sourceAppearance = sourceAppearance;
            this.source = source;
            this.receiverPos = receiverPos;
            this.receiverAppearance = receiverAppearance;
            this.receiver = receiver;
            this.face = face;
        }
    }

}
