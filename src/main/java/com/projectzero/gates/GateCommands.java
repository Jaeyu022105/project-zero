package com.projectzero.gates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Operator commands merged into {@code /pz}:
 *   /pz gate spawn <rank> <size>
 *   /pz gate leave
 */
public final class GateCommands {
    private GateCommands() {}

    private static final List<String> RANKS = Arrays.stream(GateRank.values())
            .map(r -> r.name().toLowerCase(Locale.ROOT)).toList();
    private static final List<String> SIZES = Arrays.stream(GateSize.values())
            .map(GateSize::key).toList();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("gate")
                        .then(Commands.literal("leave")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    GateService.returnFromDungeon(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Returned from dungeon."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RANKS, b))
                                        .then(Commands.argument("size", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(SIZES, b))
                                                .executes(ctx -> spawnGate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "rank"),
                                                        StringArgumentType.getString(ctx, "size"))))))));
    }

    private static int spawnGate(CommandSourceStack source, String rankRaw, String sizeRaw)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GateRank rank = GateRank.byName(rankRaw);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown gate rank: " + rankRaw));
            return 0;
        }
        GateSize size = GateSize.byName(sizeRaw);
        if (size == null) {
            source.sendFailure(Component.literal("Unknown gate size: " + sizeRaw));
            return 0;
        }

        Direction facing = player.getDirection();
        boolean alongX = (facing.getAxis() == Direction.Axis.Z);
        BlockPos base = player.blockPosition().relative(facing, 4).above(1);

        var rec = GateService.spawnGate(player.serverLevel(), base, rank, size, alongX);
        if (rec == null) {
            source.sendFailure(Component.literal("Failed to spawn gate (missing projectzero:dungeon dimension)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned " + rank.name() + "-Rank (" + size.name()
                + ") Gate at " + base.toShortString() + " [slot " + rec.slot + "]."), false);
        return 1;
    }
}
