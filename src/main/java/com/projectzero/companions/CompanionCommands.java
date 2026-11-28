package com.projectzero.companions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.projectzero.ProjectZero;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CompanionCommands {
    private CompanionCommands() {}

    private static final DynamicCommandExceptionType UNKNOWN_TYPE =
            new DynamicCommandExceptionType(id -> Component.translatable("command.projectzero.companion.unknown_type", id));
    private static final DynamicCommandExceptionType UNKNOWN_COMPANION =
            new DynamicCommandExceptionType(name -> Component.translatable("command.projectzero.companion.unknown", name));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz").requires(source -> source.hasPermission(2))
                .then(Commands.literal("companion")
                        .then(Commands.literal("list").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            var entries = CompanionService.get(player).entries();
                            if (entries.isEmpty()) ctx.getSource().sendSuccess(() -> Component.translatable("command.projectzero.companion.empty"), false);
                            for (CompanionEntry entry : entries) {
                                ctx.getSource().sendSuccess(() -> Component.translatable("command.projectzero.companion.entry",
                                        entry.name(), entry.level(), entry.xp()), false);
                            }
                            return entries.size();
                        }))
                        .then(Commands.literal("add")
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                                        .then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "entity");
                                            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw UNKNOWN_TYPE.create(id);
                                            CompanionEntry entry = CompanionService.add(player, CompanionKind.SUMMON, id,
                                                    StringArgumentType.getString(ctx, "name"));
                                            ctx.getSource().sendSuccess(() -> Component.translatable("command.projectzero.companion.added", entry.name()), false);
                                            return 1;
                                        }))))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1, 1_000_000_000L))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    CompanionEntry entry = CompanionService.findByName(CompanionService.get(player), name);
                                                    if (entry == null) throw UNKNOWN_COMPANION.create(name);
                                                    CompanionService.gainXp(player, entry.id(), LongArgumentType.getLong(ctx, "amount"));
                                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.projectzero.companion.xp_added", entry.name()), false);
                                                    return 1;
                                                }))))));
    }
}
