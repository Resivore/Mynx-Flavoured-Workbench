package tempeststudios.quickstacknearby;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class QuickStackCommands {
    private QuickStackCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(root("quickstacknearby"));
            dispatcher.register(root("qsn"));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String literal) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(QuickStackCommands::requiresOp);

        root.then(Commands.literal("range")
                .executes(ctx -> showRange(ctx.getSource()))
                .then(Commands.argument(
                                "horizontal",
                                IntegerArgumentType.integer(
                                        QuickStackServerConfig.MIN_HORIZONTAL_RADIUS,
                                        QuickStackServerConfig.MAX_HORIZONTAL_RADIUS
                                )
                        )
                        .then(Commands.argument(
                                        "vertical",
                                        IntegerArgumentType.integer(
                                                QuickStackServerConfig.MIN_VERTICAL_RADIUS,
                                                QuickStackServerConfig.MAX_VERTICAL_RADIUS
                                        )
                                )
                                .executes(ctx -> setRange(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "horizontal"),
                                        IntegerArgumentType.getInteger(ctx, "vertical")
                                )))));
        root.then(Commands.literal("reload")
                .executes(ctx -> reload(ctx.getSource())));
        return root;
    }

    private static int showRange(CommandSourceStack source) {
        QuickStackServerConfig config = QuickStackServerConfig.getInstance();
        source.sendSuccess(() -> Component.literal("Quick Stack Nearby range: horizontal "
                + config.horizontalRadius()
                + ", vertical "
                + config.verticalRadius()
                + "."), false);
        return 1;
    }

    private static int setRange(CommandSourceStack source, int horizontalRadius, int verticalRadius) {
        QuickStackServerConfig config = QuickStackServerConfig.getInstance();
        config.setRange(horizontalRadius, verticalRadius);
        source.sendSuccess(() -> Component.literal("Quick Stack Nearby range set to horizontal "
                + config.horizontalRadius()
                + ", vertical "
                + config.verticalRadius()
                + "."), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        QuickStackServerConfig.getInstance().reload();
        return showRange(source);
    }

    private static boolean requiresOp(CommandSourceStack source) {
        return ServerPermissionCompat.hasCommandLevel(source, 2);
    }
}
