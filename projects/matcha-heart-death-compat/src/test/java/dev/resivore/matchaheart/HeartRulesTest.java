package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class HeartRulesTest {
    @Test void crystalMatrix() {
        assertEquals(22, HeartRules.afterCrystal(20));
        assertEquals(32, HeartRules.afterCrystal(30));
        assertEquals(18, HeartRules.afterCrystal(18));
        assertEquals(60, HeartRules.afterCrystal(60));
    }

    @Test void reinforcedMatrix() {
        assertEquals(20, HeartRules.afterReinforced(18));
        assertEquals(16, HeartRules.afterReinforced(14));
        assertEquals(20, HeartRules.afterReinforced(20));
        assertEquals(24, HeartRules.afterReinforced(24));
    }

    @Test void deathMatrix() {
        assertEquals(28, HeartRules.afterDeath(30));
        assertEquals(20, HeartRules.afterDeath(22));
        assertEquals(18, HeartRules.afterDeath(20));
        assertEquals(16, HeartRules.afterDeath(18));
        assertEquals(10, HeartRules.afterDeath(12));
        assertEquals(10, HeartRules.afterDeath(10));
    }

    @Test void malformedStateFailsClosedToBaseline() {
        assertEquals(20, HeartRules.sanitize(19));
        assertEquals(20, HeartRules.sanitize(100));
    }
}
