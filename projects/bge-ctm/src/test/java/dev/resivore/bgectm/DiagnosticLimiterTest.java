package dev.resivore.bgectm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DiagnosticLimiterTest {
    @Test
    void deduplicatesAndReportsOneBoundedSuppressionNotice() {
        DiagnosticLimiter limiter = new DiagnosticLimiter(2);
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED, limiter.admit("first"));
        assertEquals(DiagnosticLimiter.Admission.DUPLICATE, limiter.admit("first"));
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED, limiter.admit("second"));
        assertEquals(DiagnosticLimiter.Admission.SUPPRESS_NOTICE, limiter.admit("third"));
        assertEquals(DiagnosticLimiter.Admission.SUPPRESSED, limiter.admit("fourth"));
        assertEquals(DiagnosticLimiter.Admission.DUPLICATE, limiter.admit("second"));
    }

    @Test
    void categoriesHaveIndependentDeduplicationCapsAndSuppressionNotices() {
        DiagnosticBudgets budgets = new DiagnosticBudgets(1, 2, 1, 1, 1, 1);
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED,
                budgets.admit(DiagnosticBudgets.Category.OVERLAY, "overlay-a"));
        assertEquals(DiagnosticLimiter.Admission.SUPPRESS_NOTICE,
                budgets.admit(DiagnosticBudgets.Category.OVERLAY, "overlay-b"));
        assertEquals(DiagnosticLimiter.Admission.SUPPRESSED,
                budgets.admit(DiagnosticBudgets.Category.OVERLAY, "overlay-c"));
        assertEquals(DiagnosticLimiter.Admission.DUPLICATE,
                budgets.admit(DiagnosticBudgets.Category.OVERLAY, "overlay-a"));

        // Overlay exhaustion cannot consume either the independent regular pool or its
        // signature set; the same text is intentionally a new category-local signature.
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED,
                budgets.admit(DiagnosticBudgets.Category.REGULAR, "overlay-a"));
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED,
                budgets.admit(DiagnosticBudgets.Category.REGULAR, "regular-b"));
        assertEquals(DiagnosticLimiter.Admission.SUPPRESS_NOTICE,
                budgets.admit(DiagnosticBudgets.Category.REGULAR, "regular-c"));
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED,
                budgets.admit(DiagnosticBudgets.Category.APPEARANCE, "appearance-a"));
        assertEquals(DiagnosticLimiter.Admission.ACCEPTED,
                budgets.admit(DiagnosticBudgets.Category.OVERLAY_EMIT, "emit-a"));
    }
}
