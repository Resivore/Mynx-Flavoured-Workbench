package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class QuickStackNearbySmokeTest {
    private static final String SMOKE_TEST_PROPERTY = "quickstacknearby.smokeTest";
    private static final int PASS_AFTER_TICKS = 20;

    private static int ticks;
    private static boolean complete;
    private static boolean screenCompatChecked;

    private QuickStackNearbySmokeTest() {
    }

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(SMOKE_TEST_PROPERTY)) {
            return;
        }

        System.out.println("[QuickStackNearby] Automated client smoke test armed.");
        ClientTickEvents.END_CLIENT_TICK.register(QuickStackNearbySmokeTest::tick);
    }

    private static void tick(Minecraft client) {
        if (complete) {
            return;
        }

        ticks++;
        if (!screenCompatChecked && ticks >= 5) {
            SmokeScreen smokeScreen = new SmokeScreen();
            ClientScreenCompat.setScreen(client, smokeScreen);
            ClientScreenCompat.setScreen(client, null);
            screenCompatChecked = true;
            System.out.println("QUICKSTACKNEARBY_SCREEN_COMPAT_PASS");
        }
        if (ticks < PASS_AFTER_TICKS) {
            return;
        }

        complete = true;
        System.out.println(
                "QUICKSTACKNEARBY_SMOKE_TEST_PASS minecraftProfile="
                        + System.getProperty("quickstacknearby.smokeMinecraftProfile", "unknown")
                        + " gameVersion="
                        + System.getProperty("quickstacknearby.smokeGameVersion", "unknown")
                        + " releaseProfile="
                        + System.getProperty("quickstacknearby.smokeReleaseProfile", "unknown")
                        + " installSet="
                        + System.getProperty("quickstacknearby.smokeInstallSet", "unknown")
                        + " injectedMods="
                        + System.getProperty("fabric.addMods", "unknown")
        );
        client.stop();
    }

    private static final class SmokeScreen extends QuickStackRulesScreenBase {
        private SmokeScreen() {
            super(Component.literal("Quick Stack Smoke"));
        }

        @Override
        protected void paintScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}
