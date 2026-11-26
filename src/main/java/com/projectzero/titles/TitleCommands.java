package com.projectzero.titles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.projectzero.ProjectZero;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Operator commands for titles and milestones merged into {@code /pz}:
 *   /pz title list
 *   /pz title equip <id|none>
 *   /pz milestone add <stat> <n>
 */
public final class TitleCommands {
    private TitleCommands() {}

    private static final DynamicCommandExceptionType BAD_TITLE =
            new DynamicCommandExceptionType(id -> Component.literal("Title not unlocked or unknown: " + id));

    private static final List<String> MILESTONE_STATS = List.of(
            "total_kills", "boss_kills", "deaths", "highest_level", "gates_cleared", "rerolls",
            "wolf_kills", "undead_kills", "wolf", "zombie"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("title")
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerTitles data = TitleService.get(player);
                                    if (data.unlocked().isEmpty()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("No titles unlocked."), false);
                                        return 1;
                                    }
                                    MutableComponent msg = Component.literal("Unlocked titles (" + data.unlocked().size() + "): ");
                                    boolean first = true;
                                    for (ResourceLocation id : data.unlocked()) {
                                        if (!first) msg.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                                        first = false;
                                        boolean eq = id.equals(data.equipped());
                                        msg.append(Title.displayName(id).copy()
                                                .withStyle(eq ? ChatFormatting.GOLD : ChatFormatting.AQUA));
                                        if (eq) msg.append(Component.literal(" [Equipped]").withStyle(ChatFormatting.YELLOW));
                                    }
                                    ctx.getSource().sendSuccess(() -> msg, false);
                                    return 1;
                                }))
                        .then(Commands.literal("equip")
                                .then(Commands.literal("none")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            TitleService.equip(player, null);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Unequipped title."), false);
                                            return 1;
                                        }))
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                TitleService.get(ctx.getSource().getPlayerOrException()).unlocked(), builder))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ResourceLocation id = resolveTitleId(player, ResourceLocationArgument.getId(ctx, "id"));
                                            if (!TitleService.equip(player, id)) {
                                                throw BAD_TITLE.create(id);
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Equipped title: ")
                                                    .append(Title.displayName(id).copy().withStyle(ChatFormatting.GOLD)), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("milestone")
                        .then(Commands.literal("add")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MILESTONE_STATS, builder))
                                        .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                                .executes(ctx -> addMilestone(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        IntegerArgumentType.getInteger(ctx, "n"))))))));
    }

    private static int addMilestone(CommandSourceStack source, String stat, int n) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MilestoneTracker m = MilestoneTracker.get(player);
        switch (stat) {
            case "total_kills", "kills" -> m.addTotalKills(n);
            case "boss_kills", "boss" -> m.addBossKills(n);
            case "deaths" -> m.addDeaths(n);
            case "highest_level", "level" -> m.updateHighestLevel(m.highestLevel() + n);
            case "gates_cleared", "gates" -> m.addGatesCleared(n);
            case "rerolls", "reroll" -> m.addRerolls(n);
            case "wolf_kills", "wolves", "wolf" -> m.addEntityKills(ResourceLocation.withDefaultNamespace("wolf"), n, false);
            case "undead_kills", "undead" -> m.addEntityKills(ResourceLocation.withDefaultNamespace("zombie"), n, false);
            default -> {
                ResourceLocation entityId = stat.contains(":")
                        ? ResourceLocation.tryParse(stat)
                        : ResourceLocation.withDefaultNamespace(stat);
                if (entityId == null) {
                    source.sendFailure(Component.literal("Unknown milestone stat: " + stat));
                    return 0;
                }
                m.addEntityKills(entityId, n, false);
            }
        }
        MilestoneTracker.set(player, m);
        source.sendSuccess(() -> Component.literal("Added +" + n + " to milestone '" + stat + "'."), false);
        return 1;
    }

    private static ResourceLocation resolveTitleId(ServerPlayer player, ResourceLocation typed) {
        if (!TitleService.get(player).hasUnlocked(typed) && typed.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            ResourceLocation pz = ProjectZero.id(typed.getPath());
            if (TitleService.get(player).hasUnlocked(pz)) return pz;
        }
        return typed;
    }
}
