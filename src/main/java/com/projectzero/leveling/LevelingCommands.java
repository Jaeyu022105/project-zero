package com.projectzero.leveling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Testing commands (operators only):
 *   /pz xp <amount>   grant XP to yourself
 *   /pz info          print your level and stats
 *   /pz reset         back to level 1
 */
public final class LevelingCommands {
    private LevelingCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xp")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    LevelingService.grantXp(player, amount);
                                    return 1;
                                })))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerStats s = LevelingService.get(player);
                            StringBuilder sb = new StringBuilder("Level " + s.level() + " (" + s.xp() + "/"
                                    + s.xpToNext() + " XP), points: " + s.unspentPoints());
                            for (Stat stat : Stat.values()) {
                                sb.append(", ").append(stat.shortName()).append(' ').append(s.get(stat));
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            LevelingService.reset(ctx.getSource().getPlayerOrException());
                            ctx.getSource().sendSuccess(() -> Component.literal("Stats reset."), false);
                            return 1;
                        })));
    }
}
