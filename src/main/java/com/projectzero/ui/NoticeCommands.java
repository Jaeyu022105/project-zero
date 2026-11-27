package com.projectzero.ui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

/** Operator command: {@code /pz notice <type> <text>}. */
public final class NoticeCommands {
    private NoticeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("notice")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(NoticeType.values())
                                                .map(t -> t.name().toLowerCase(Locale.ROOT)),
                                        builder))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String rawType = StringArgumentType.getString(ctx, "type");
                                            NoticeType type = NoticeType.byName(rawType);
                                            if (type == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown notice type: " + rawType));
                                                return 0;
                                            }
                                            String text = StringArgumentType.getString(ctx, "text");
                                            Component msg = Component.literal(text).withStyle(ChatFormatting.WHITE);
                                            SystemNotices.system(player, type, msg);
                                            return 1;
                                        })))));
    }
}
