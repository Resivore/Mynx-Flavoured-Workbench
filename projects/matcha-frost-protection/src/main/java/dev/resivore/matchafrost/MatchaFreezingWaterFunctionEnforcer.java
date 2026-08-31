package dev.resivore.matchafrost;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.PlainTextFunction;
import net.minecraft.resources.Identifier;

public final class MatchaFreezingWaterFunctionEnforcer {
    static final Identifier TARGET_FUNCTION =
            Identifier.parse("main:environmental/check_freezing_water_conditions");
    static final Identifier CANONICAL_FUNCTION = Identifier.parse(
            "matcha_frost_protection:environmental/check_freezing_water_conditions");
    static final Identifier CANONICAL_PENALTY_FUNCTION =
            Identifier.parse("matcha_frost_protection:environmental/freezing_water");
    static final String EXPECTED_UPSTREAM_CONDITION_COMMAND =
            "execute at @a[gamemode=!creative] if block ~ ~1 ~ water run execute unless entity "
                    + "@p[nbt={equipment:{chest:{components:{\"minecraft:enchantments\":{"
                    + "\"main:freezing_protection\":3}}}}}] run execute if biome ~ ~ ~ "
                    + "#minecraft:is_frozen run function main:environmental/freezing_water";
    static final String EXPECTED_CANONICAL_CONDITION_COMMAND =
            "execute as @a[gamemode=!creative] at @s if block ~ ~1 ~ water unless entity "
                    + "@s[nbt={equipment:{head:{components:{\"minecraft:enchantments\":{"
                    + "\"main:freezing_protection\":3}}}}}] unless entity "
                    + "@s[nbt={equipment:{chest:{components:{\"minecraft:enchantments\":{"
                    + "\"main:freezing_protection\":3}}}}}] unless entity "
                    + "@s[nbt={equipment:{legs:{components:{\"minecraft:enchantments\":{"
                    + "\"main:freezing_protection\":3}}}}}] unless entity "
                    + "@s[nbt={equipment:{feet:{components:{\"minecraft:enchantments\":{"
                    + "\"main:freezing_protection\":3}}}}}] if biome ~ ~ ~ #minecraft:is_frozen "
                    + "run function matcha_frost_protection:environmental/freezing_water";
    static final List<String> EXPECTED_CANONICAL_PENALTY_COMMANDS = List.of(
            "effect give @s slowness 5 4 true",
            "effect give @s darkness 5 0 true",
            "damage @s 1 freeze");

    private MatchaFreezingWaterFunctionEnforcer() {}

    public static boolean targets(Identifier requested) {
        return TARGET_FUNCTION.equals(requested);
    }

    public static Optional<CommandFunction<CommandSourceStack>> resolve(
            Identifier requested,
            Map<Identifier, CommandFunction<CommandSourceStack>> resolved) {
        if (!targets(requested)) {
            return Optional.ofNullable(resolved.get(requested));
        }

        try {
            CommandFunction<CommandSourceStack> upstream = resolved.get(TARGET_FUNCTION);
            if (upstream == null) {
                throw new IllegalStateException(
                        "Missing resolved Matcha freezing-water condition function");
            }
            requireExactPlainText(
                    upstream,
                    TARGET_FUNCTION,
                    List.of(EXPECTED_UPSTREAM_CONDITION_COMMAND));

            CommandFunction<CommandSourceStack> canonical = resolved.get(CANONICAL_FUNCTION);
            if (canonical == null) {
                throw new IllegalStateException(
                        "Missing canonical Matcha freezing-water condition function");
            }
            requireExactPlainText(
                    canonical,
                    CANONICAL_FUNCTION,
                    List.of(EXPECTED_CANONICAL_CONDITION_COMMAND));

            CommandFunction<CommandSourceStack> canonicalPenalty =
                    resolved.get(CANONICAL_PENALTY_FUNCTION);
            if (canonicalPenalty == null) {
                throw new IllegalStateException(
                        "Missing canonical player-local freezing-water penalty function");
            }
            requireExactPlainText(
                    canonicalPenalty,
                    CANONICAL_PENALTY_FUNCTION,
                    EXPECTED_CANONICAL_PENALTY_COMMANDS);

            return Optional.of(canonical);
        } catch (RuntimeException exception) {
            MatchaFrostProtection.LOGGER.error(
                    "FATAL: could not enforce the Matcha freezing-water guard", exception);
            throw new IllegalStateException(
                    "Unsafe Matcha freezing-water function contract", exception);
        }
    }

    private static void requireExactPlainText(
            CommandFunction<CommandSourceStack> function,
            Identifier expectedId,
            List<String> expectedCommands) {
        if (!expectedId.equals(function.id())) {
            throw new IllegalStateException(
                    "Resolved function identity mismatch for " + expectedId);
        }
        if (!(function instanceof PlainTextFunction<?> plainText)) {
            throw new IllegalStateException(
                    "Resolved function is not plain text: " + expectedId);
        }

        List<String> actualCommands = plainText.entries().stream()
                .map(Object::toString)
                .toList();
        if (!expectedCommands.equals(actualCommands)) {
            throw new IllegalStateException(
                    "Resolved function body mismatch for " + expectedId);
        }
    }
}
