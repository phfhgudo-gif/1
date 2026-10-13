package com.thanpisitkub.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.move.Moves;
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
 * /thanpisitkub cast <move> [players]       (op / Origins power) start a move
 * /thanpisitkub cancel [players]            stop your own running move (players = op only)
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
                .then(Commands.literal("cast")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("move", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Moves.ALL.stream().map(m -> m.id), b))
                                .executes(c -> cast(c, List.of(c.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> cast(c, EntityArgument.getPlayers(c, "targets"))))))
                .then(Commands.literal("cancel")
                        .executes(c -> cancel(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(s -> s.hasPermission(2))
                                .executes(c -> cancel(c, EntityArgument.getPlayers(c, "targets"))))));
    }

    private static int cast(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        MoveDef move = Moves.byId(StringArgumentType.getString(ctx, "move"));
        if (move == null) throw UNKNOWN_MOVE.create();
        int ok = 0;
        for (ServerPlayer p : targets) if (ServerMoveManager.cast(p, move)) ok++;
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
