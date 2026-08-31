package dev.resivore.matchafrost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.PlainTextFunction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class MatchaFreezingWaterFunctionEnforcerTest {
    @Test
    void redirectsOnlyMatchasResolvedConditionFunctionToTheCanonicalGuard() {
        CommandFunction<CommandSourceStack> upstream =
                stubFunction(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_UPSTREAM_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> canonical =
                stubFunction(
                        MatchaFreezingWaterFunctionEnforcer.CANONICAL_FUNCTION,
                        List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> penalty = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_PENALTY_FUNCTION,
                MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_PENALTY_COMMANDS);
        Identifier unrelatedId = Identifier.parse("example:unrelated");
        CommandFunction<CommandSourceStack> unrelated =
                stubFunction(unrelatedId, List.of("say unrelated"));
        Map<Identifier, CommandFunction<CommandSourceStack>> resolved = Map.of(
                upstream.id(), upstream,
                canonical.id(), canonical,
                penalty.id(), penalty,
                unrelated.id(), unrelated);

        assertSame(
                canonical,
                MatchaFreezingWaterFunctionEnforcer.resolve(
                                MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                                resolved)
                        .orElseThrow());
        assertSame(
                unrelated,
                MatchaFreezingWaterFunctionEnforcer.resolve(unrelatedId, resolved)
                        .orElseThrow());
    }

    @Test
    void failsClosedWhenThePinnedMatchaConditionFunctionDidNotResolve() {
        CommandFunction<CommandSourceStack> canonical =
                stubFunction(
                        MatchaFreezingWaterFunctionEnforcer.CANONICAL_FUNCTION,
                        List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> penalty = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_PENALTY_FUNCTION,
                MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_PENALTY_COMMANDS);

        assertThrows(
                IllegalStateException.class,
                () -> MatchaFreezingWaterFunctionEnforcer.resolve(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        Map.of(canonical.id(), canonical, penalty.id(), penalty)));
    }

    @Test
    void failsClosedWhenTheCanonicalGuardDidNotResolve() {
        CommandFunction<CommandSourceStack> upstream =
                stubFunction(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_UPSTREAM_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> penalty = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_PENALTY_FUNCTION,
                MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_PENALTY_COMMANDS);

        assertThrows(
                IllegalStateException.class,
                () -> MatchaFreezingWaterFunctionEnforcer.resolve(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        Map.of(upstream.id(), upstream, penalty.id(), penalty)));
    }

    @Test
    void failsClosedWhenThePlayerLocalPenaltyFunctionDidNotResolve() {
        CommandFunction<CommandSourceStack> upstream = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_UPSTREAM_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> canonical = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_FUNCTION,
                List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_CONDITION_COMMAND));

        assertThrows(
                IllegalStateException.class,
                () -> MatchaFreezingWaterFunctionEnforcer.resolve(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        Map.of(upstream.id(), upstream, canonical.id(), canonical)));
    }

    @Test
    void failsClosedWhenTheResolvedUpstreamBodyChanged() {
        CommandFunction<CommandSourceStack> upstream = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                List.of("say changed"));
        CommandFunction<CommandSourceStack> canonical = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_FUNCTION,
                List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_CONDITION_COMMAND));
        CommandFunction<CommandSourceStack> penalty = stubFunction(
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_PENALTY_FUNCTION,
                MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_PENALTY_COMMANDS);

        assertThrows(
                IllegalStateException.class,
                () -> MatchaFreezingWaterFunctionEnforcer.resolve(
                        MatchaFreezingWaterFunctionEnforcer.TARGET_FUNCTION,
                        Map.of(
                                upstream.id(), upstream,
                                canonical.id(), canonical,
                                penalty.id(), penalty)));
    }

    @Test
    void usesExactFunctionIdentities() {
        assertTrue(MatchaFreezingWaterFunctionEnforcer.targets(
                Identifier.parse("main:environmental/check_freezing_water_conditions")));
        assertEquals(
                "matcha_frost_protection:environmental/check_freezing_water_conditions",
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_FUNCTION.toString());
        assertEquals(
                "matcha_frost_protection:environmental/freezing_water",
                MatchaFreezingWaterFunctionEnforcer.CANONICAL_PENALTY_FUNCTION.toString());
    }

    private static CommandFunction<CommandSourceStack> stubFunction(
            Identifier id,
            List<String> commands) {
        return new PlainTextFunction<>(
                id,
                commands.stream()
                        .map(MatchaFreezingWaterFunctionEnforcerTest::stubCommand)
                        .toList());
    }

    private static UnboundEntryAction<CommandSourceStack> stubCommand(String command) {
        return new UnboundEntryAction<>() {
            @Override
            public void execute(
                    CommandSourceStack source,
                    ExecutionContext<CommandSourceStack> context,
                    Frame frame) {}

            @Override
            public String toString() {
                return command;
            }
        };
    }
}
