package dev.resivore.matchaheart;

/** Pure health-point rules. One Minecraft heart is two health points. */
public final class HeartRules {
    public static final int FLOOR = 10;
    public static final int BASELINE = 20;
    public static final int MATCHA_CAP = 60;

    private HeartRules() {}

    public static int sanitize(int healthPoints) {
        if (healthPoints < FLOOR || healthPoints > MATCHA_CAP || (healthPoints & 1) != 0) {
            return BASELINE;
        }
        return healthPoints;
    }

    public static int afterDeath(int before) {
        int safe = sanitize(before);
        return Math.max(FLOOR, safe - 2);
    }

    public static int afterCrystal(int before) {
        int safe = sanitize(before);
        return safe >= BASELINE && safe < MATCHA_CAP ? safe + 2 : safe;
    }

    public static int afterReinforced(int before) {
        int safe = sanitize(before);
        return safe < BASELINE ? Math.min(BASELINE, safe + 2) : safe;
    }
}
