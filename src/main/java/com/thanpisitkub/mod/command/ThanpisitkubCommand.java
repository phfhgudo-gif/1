package com.thanpisitkub.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.move.Moves;
import com.thanpisitkub.mod.power.PowerData;
import com.thanpisitkub.mod.server.ServerMoveManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * /thanpisitkub power <on|off> [players]    (op) give / remove the power
 * /thanpisitkub cast <move> [players]       (op) start a move, ignores power + cooldown (used by the Origins power)
 * /thanpisitkub cancel [players]            (op) stop a running move
 * Moves: galaxy_impact, galaxy_divide
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID)
public final class ThanpisitkubCommand {
    private ThanpisitkubCommand() {
    }

    private static final SimpleCommandExceptionType UNKNOWN_MOVE =
            new SimpleCommandExceptionType(Component.literal("Unknown move. Use galaxy_impact or galaxy_divide."));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("thanpisitkub")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("power")
                        .then(Commands.literal("on")
                                .executes(c -> power(c, List.of(c.getSource().getPlayerOrException()), true))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> power(c, EntityArgument.getPlayers(c, "targets"), true))))
                        .then(Commands.literal("off")
                                .executes(c -> power(c, List.of(c.getSource().getPlayerOrException()), false))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> power(c, EntityArgument.getPlayers(c, "targets"), false)))))
                .then(Commands.literal("cast")
                        .then(Commands.argument("move", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Moves.ALL.stream().map(m -> m.id), b))
                                .executes(c -> cast(c, List.of(c.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> cast(c, EntityArgument.getPlayers(c, "targets"))))))
                .then(Commands.literal("cancel")
                        .executes(c -> cancel(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> cancel(c, EntityArgument.getPlayers(c, "targets"))))));
    }

    private static int power(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean on) {
        for (ServerPlayer p : targets) {
            PowerData.set(p, on);
            p.displayClientMessage(Component.translatable(
                    on ? "message.thanpisitkub.power_on" : "message.thanpisitkub.power_off"), true);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                on ? "commands.thanpisitkub.power.on" : "commands.thanpisitkub.power.off", targets.size()), true);
        return targets.size();
    }

    private static int cast(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        MoveDef move = Moves.byId(StringArgumentType.getString(ctx, "move"));
        if (move == null) throw UNKNOWN_MOVE.create();
        int ok = 0;
        for (ServerPlayer p : targets) if (ServerMoveManager.forceCast(p, move)) ok++;
        final int n = ok;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.thanpisitkub.cast", move.id, n), true);
        return ok;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) ServerMoveManager.cancel(p);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.thanpisitkub.cancel", targets.size()), true);
        return targets.size();
    }
}
