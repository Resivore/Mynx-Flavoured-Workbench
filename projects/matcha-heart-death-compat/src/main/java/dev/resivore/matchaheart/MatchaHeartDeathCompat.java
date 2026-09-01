package dev.resivore.matchaheart;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatchaHeartDeathCompat implements ModInitializer {
    public static final String MOD_ID = "matcha_heart_death_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Identifier MATCHA_MODEL = Identifier.withDefaultNamespace("heart_container");
    private static final Identifier REINFORCED_MODEL = Identifier.fromNamespaceAndPath(MOD_ID, "reinforced_crystal_heart");

    public static final AttachmentType<Integer> MAX_HEALTH = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT)
            .copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "max_health_points"));

    @Override
    public void onInitialize() {
        HeartItems.register();
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            String resourcePath = HeartDataContract.LOOT_TABLE_RESOURCES.get(key.identifier().toString());
            if (resourcePath == null) {
                return null;
            }
            try {
                var replacement = AuthoritativeData.decodeLootTable(resourcePath, registries);
                LOGGER.info("Enforced authoritative Echo Shard loot contract for {} during data reload",
                        key.identifier());
                return replacement;
            } catch (RuntimeException exception) {
                LOGGER.error("FATAL: could not enforce Echo Shard loot contract for {}; aborting data reload",
                        key.identifier(), exception);
                throw new IllegalStateException("Unsafe Echo Shard loot contract for " + key.identifier(), exception);
            }
        });
        UseItemCallback.EVENT.register(this::onUse);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                int before = state(oldPlayer);
                setState(newPlayer, HeartRules.afterDeath(before));
            }
        });
        LOGGER.info("Matcha heart compatibility canary active; reload enforcement owns recipes={}, "
                        + "advancements={}, loot tables={}, functions={}",
                HeartDataContract.RECIPE_RESOURCES.keySet(), HeartDataContract.ADVANCEMENT_RESOURCES.keySet(),
                HeartDataContract.LOOT_TABLE_RESOURCES.keySet(), HeartDataContract.BLOCKED_FUNCTIONS);
    }

    private InteractionResult onUse(net.minecraft.world.entity.player.Player player,
                                    net.minecraft.world.level.Level level,
                                    InteractionHand hand) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean crystal = isCrystalHeart(stack);
        boolean reinforced = isReinforcedHeart(stack);
        if (!crystal && !reinforced) {
            return InteractionResult.PASS;
        }

        int before = state(serverPlayer);
        int after = crystal ? HeartRules.afterCrystal(before) : HeartRules.afterReinforced(before);
        if (after == before) {
            Component message = crystal && before < HeartRules.BASELINE
                    ? Component.literal("Damaged baseline hearts require a Reinforced Crystal Heart")
                    : reinforced
                        ? Component.literal("Reinforced Crystal Hearts only repair damaged baseline hearts")
                        : Component.literal("Maximum Crystal Heart capacity reached");
            serverPlayer.sendOverlayMessage(message);
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        setState(serverPlayer, after);
        HeartUseFeedback.emitSuccessful(
                serverPlayer,
                crystal ? HeartUseFeedback.Kind.CRYSTAL : HeartUseFeedback.Kind.REINFORCED,
                before,
                after);
        return InteractionResult.SUCCESS;
    }

    static boolean isCrystalHeart(ItemStack stack) {
        return stack.is(Items.POISONOUS_POTATO) && MATCHA_MODEL.equals(stack.get(DataComponents.ITEM_MODEL));
    }

    static boolean isReinforcedHeart(ItemStack stack) {
        return stack.is(Items.POISONOUS_POTATO) && REINFORCED_MODEL.equals(stack.get(DataComponents.ITEM_MODEL));
    }

    static int state(ServerPlayer player) {
        Integer stored = player.getAttached(MAX_HEALTH);
        if (stored != null) return HeartRules.sanitize(stored);
        double actual = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
        int adopted = HeartRules.sanitize((int) Math.round(actual));
        player.setAttached(MAX_HEALTH, adopted);
        return adopted;
    }

    static void setState(ServerPlayer player, int value) {
        int safe = HeartRules.sanitize(value);
        player.setAttached(MAX_HEALTH, safe);
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(safe);
        if (player.getHealth() > safe) player.setHealth(safe);
    }
}
