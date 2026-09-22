package dev.resivore.villagerwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the C24 server-side changes to their deliberately narrow production/deposit scope. */
class C24WorkRoutineContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test void everyRealBarrelMutationFollowsPhysicalArrivalAndImmediateRevalidation()
            throws IOException {
        String source = read("src/main/java/dev/resivore/villagerwork/WorkCoordinator.java");
        String wool = method(source, "private static void depositWool(");
        String fisherman = method(source, "private static void fisherman(");
        String arrival = method(source, "private static boolean atBarrelInteractionPosition(");

        assertTrue(wool.contains("if (!approachBarrel(villager, level, plan.barrel(), state, \"wool\")) return;"));
        assertTrue(wool.indexOf("approachBarrel(") < wool.indexOf("OutputStorage.insertTarget("));
        assertTrue(wool.indexOf("atBarrelInteractionPosition(")
                < wool.indexOf("OutputStorage.insertTarget("));
        assertTrue(wool.contains("current.equals(plan)"));
        assertFalse(wool.contains("insertAcross("));

        int returnBranch = fisherman.indexOf("if (containsFish(owned))");
        assertTrue(fisherman.contains("if (!approachBarrel(villager, level, site, state, \"fish\")) return;"));
        int proximity = fisherman.indexOf("atBarrelInteractionPosition(", returnBranch);
        int transfer = fisherman.indexOf("deposit(owned, current, false)", returnBranch);
        assertTrue(returnBranch >= 0 && proximity > returnBranch && transfer > proximity);
        assertTrue(fisherman.contains("claimedBarrel(level, site)"));
        assertFalse(fisherman.substring(returnBranch).contains("moveNearSite("));
        assertTrue(arrival.contains("level.noCollision(villager, villager.getBoundingBox())"));
        assertTrue(arrival.contains("BarrelInteractionRules.withinReach("));
        assertTrue(source.contains("villager.getHeadLookAngle()"));
        assertFalse(source.contains("BarrelInteractionRules.facing(\n                        villager.getLookAngle()"));
    }

    @Test void emptyRetrieveKeepsVisibleCleanupButSkipsTheFishLootCall() throws IOException {
        String source = read("src/main/java/dev/resivore/villagerwork/WorkCoordinator.java");
        String fisherman = method(source, "private static void fisherman(");

        int swing = fisherman.indexOf("villager.swing(InteractionHand.MAIN_HAND)");
        int splash = fisherman.indexOf("ParticleTypes.SPLASH", swing);
        int discard = fisherman.indexOf("state.floatEntity.discard()", splash);
        int gate = fisherman.indexOf("productiveRetrieve(gateRoll)", discard);
        int conditionalLoot = fisherman.indexOf("productive ? rollFish(", gate);
        int cleanup = fisherman.indexOf("state.water = null", conditionalLoot);
        int cooldown = fisherman.indexOf("POST_RETRIEVE_COOLDOWN_TICKS", cleanup);
        assertTrue(swing >= 0 && splash > swing && discard > splash && gate > discard
                && conditionalLoot > gate && cleanup > conditionalLoot && cooldown > cleanup);
        assertTrue(fisherman.contains("productive ? \"loot_attempt\" : \"intentional_empty\""));
        assertTrue(source.contains("BuiltInLootTables.FISHING_FISH"));
        assertTrue(source.contains("stack.is(Items.COD) || stack.is(Items.SALMON)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String method(String source, String declaration) {
        int start = source.indexOf(declaration);
        if (start < 0) throw new AssertionError("Missing source anchor: " + declaration);
        int opening = source.indexOf('{', start);
        int depth = 1;
        for (int index = opening + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(start, index + 1);
        }
        throw new AssertionError("Unclosed method: " + declaration);
    }
}
