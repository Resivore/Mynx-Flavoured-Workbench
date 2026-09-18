package dev.resivore.bgectm;

import java.util.EnumMap;
import java.util.Objects;

/** Independent bounded admission pools for render-path diagnostic categories. */
public final class DiagnosticBudgets {
    public enum Category { APPEARANCE, REGULAR, RULE_SELECTION, OVERLAY, OVERLAY_BASELINE, OVERLAY_EMIT }

    private final EnumMap<Category, DiagnosticLimiter> limiters = new EnumMap<>(Category.class);

    public DiagnosticBudgets(int appearanceCap, int regularCap, int ruleSelectionCap,
            int overlayCap, int baselineCap, int overlayEmitCap) {
        limiters.put(Category.APPEARANCE, new DiagnosticLimiter(appearanceCap));
        limiters.put(Category.REGULAR, new DiagnosticLimiter(regularCap));
        limiters.put(Category.RULE_SELECTION, new DiagnosticLimiter(ruleSelectionCap));
        limiters.put(Category.OVERLAY, new DiagnosticLimiter(overlayCap));
        limiters.put(Category.OVERLAY_BASELINE, new DiagnosticLimiter(baselineCap));
        limiters.put(Category.OVERLAY_EMIT, new DiagnosticLimiter(overlayEmitCap));
    }

    public DiagnosticLimiter.Admission admit(Category category, String signature) {
        return Objects.requireNonNull(limiters.get(Objects.requireNonNull(category)), "limiter")
                .admit(signature);
    }
}
