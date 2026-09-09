package dev.resivore.wearablelanterns;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import java.lang.reflect.Method;
import java.util.OptionalInt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Selects the stack that Iris will evaluate for its secondary handheld-light uniforms. This is
 * deliberately called only by the Iris uniform supplier mixin: it never changes the player's
 * actual offhand, inventory, packets, or any world lighting state.
 */
public final class WearableLanternsIrisBridge {
    private static final String IRIS_ITEM_LIGHT_PROVIDER =
            "net.irisshaders.iris.api.v0.item.IrisItemLightProvider";

    private static volatile boolean irisProviderResolved;
    private static Class<?> irisProviderType;
    private static Method irisLightEmission;

    private WearableLanternsIrisBridge() {}

    public static ItemStack selectSecondaryHandLightStack(LocalPlayer player, ItemStack realOffhand) {
        ItemStack wornLantern = wornLantern(player);
        if (wornLantern.isEmpty() || !wornLantern.is(WearableLanterns.LANTERN_SLOT_ITEMS)) {
            return realOffhand;
        }

        OptionalInt wearableEmission = irisLightEmission(player, wornLantern);
        OptionalInt offhandEmission = irisLightEmission(player, realOffhand);
        return shouldUseWearable(offhandEmission, wearableEmission) ? wornLantern : realOffhand;
    }

    static boolean shouldUseWearable(OptionalInt offhandEmission, OptionalInt wearableEmission) {
        return offhandEmission.isPresent()
                && wearableEmission.isPresent()
                && wearableEmission.getAsInt() > 0
                && wearableEmission.getAsInt() > offhandEmission.getAsInt();
    }

    private static ItemStack wornLantern(LocalPlayer player) {
        try {
            TrinketSlotAccess slot = TrinketsApi.getAttachment(player)
                    .getSlotAccess(WearableLanterns.LANTERN_SLOT, 0);
            return slot.isValid() ? slot.get() : ItemStack.EMPTY;
        } catch (RuntimeException ignored) {
            // A missing/not-yet-synchronized Trinkets attachment must not alter Iris hand uniforms.
            return ItemStack.EMPTY;
        }
    }

    private static OptionalInt irisLightEmission(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalInt.of(0);
        }

        Method emission = irisLightEmissionMethod();
        if (emission == null || !irisProviderType.isInstance(stack.getItem())) {
            return OptionalInt.empty();
        }

        try {
            Object value = emission.invoke(stack.getItem(), player, stack);
            return value instanceof Integer integer && integer >= 0
                    ? OptionalInt.of(integer)
                    : OptionalInt.empty();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return OptionalInt.empty();
        }
    }

    private static Method irisLightEmissionMethod() {
        if (!irisProviderResolved) {
            synchronized (WearableLanternsIrisBridge.class) {
                if (!irisProviderResolved) {
                    try {
                        irisProviderType = Class.forName(
                                IRIS_ITEM_LIGHT_PROVIDER,
                                false,
                                WearableLanternsIrisBridge.class.getClassLoader());
                        irisLightEmission = irisProviderType.getMethod(
                                "getLightEmission", Player.class, ItemStack.class);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        irisProviderType = null;
                        irisLightEmission = null;
                    }
                    irisProviderResolved = true;
                }
            }
        }
        return irisLightEmission;
    }
}
