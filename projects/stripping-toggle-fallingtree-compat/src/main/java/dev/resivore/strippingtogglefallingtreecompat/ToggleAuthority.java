package dev.resivore.strippingtogglefallingtreecompat;

import net.minecraft.world.entity.player.Player;

import java.util.Set;

public final class ToggleAuthority {
    public static final String LEGACY_DISABLED_TAG = "fallingtree-disabled";
    public static final String ENABLED_TRANSLATION_KEY = "command.fallingtree.toggle.enabled";
    public static final String DISABLED_TRANSLATION_KEY = "command.fallingtree.toggle.disabled";

    private ToggleAuthority() { }

    public static boolean shouldHaveDisabledTag(boolean strippingToggleEnabled) {
        return !strippingToggleEnabled;
    }

    public static void synchronizeNativeState(Player player, boolean strippingToggleEnabled) {
        if (shouldHaveDisabledTag(strippingToggleEnabled)) {
            player.addTag(LEGACY_DISABLED_TAG);
            if (!player.entityTags().contains(LEGACY_DISABLED_TAG)) {
                throw new IllegalStateException(
                        "Cannot disarm FallingTree because the player entity-tag limit was reached");
            }
        } else {
            player.removeTag(LEGACY_DISABLED_TAG);
        }
    }

    public static void clearNativeState(Player player) {
        player.removeTag(LEGACY_DISABLED_TAG);
    }

    public static boolean isEnabled(Set<String> nativeTags) {
        return !nativeTags.contains(LEGACY_DISABLED_TAG);
    }

    public static String statusTranslationKey(boolean strippingToggleEnabled) {
        return strippingToggleEnabled ? ENABLED_TRANSLATION_KEY : DISABLED_TRANSLATION_KEY;
    }
}
