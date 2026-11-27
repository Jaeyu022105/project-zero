package com.projectzero.leveling;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.projectzero.jobs.JobService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Testing commands (operators only):
 *   /pz xp <amount>   grant XP to yourself
 *   /pz info          print your level and stats
 *   /pz reset         back to level 1 and clear your job
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
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, LevelingRules.MAX_LEVEL))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                    LevelingService.setLevel(player, lvl);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set level to " + lvl + "."), false);
                                    return 1;
                                })))
                .then(Commands.literal("ceiling")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerStats s = LevelingService.get(player);
                                    boolean monarch = JobService.hasNoCeiling(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Level " + s.level() + "/" + LevelingRules.MAX_LEVEL
                                                    + " · ceilingBroken=" + s.ceilingBroken()
                                                    + " · jobNoCeiling=" + monarch
                                                    + " · atCeiling=" + s.isAtCeiling(monarch)), false);
                                    return 1;
                                }))
                        .then(Commands.literal("break")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean changed = LevelingService.breakCeiling(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            changed ? "Broke the level ceiling." : "Level ceiling was already broken."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    LevelingService.resetCeiling(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Reset level ceiling flag."), false);
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            LevelingService.reset(player);
                            JobService.clear(player);
                            com.projectzero.skills.SkillService.reset(player);
                            com.projectzero.titles.TitleService.reset(player);
                            com.projectzero.equipment.EquipmentService.clearAll(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("Stats, job, skills, titles, and gear reset."), false);
                            return 1;
                        })));
    }
}
