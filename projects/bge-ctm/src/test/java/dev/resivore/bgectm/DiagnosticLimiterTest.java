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
}
