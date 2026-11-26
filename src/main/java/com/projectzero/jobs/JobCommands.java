package com.projectzero.jobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.projectzero.ProjectZero;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Job testing commands (operators only). Brigadier merges these into the {@code /pz}
 * node that {@code LevelingCommands} registers.
 *   /pz job set &lt;id&gt;   give yourself a job ("duelist" or "projectzero:duelist")
 *   /pz job reroll     draw a different random job
 *   /pz job clear      remove your job (the Job Change Quest triggers again on next level up)
 */
public final class JobCommands {
    private JobCommands() {}

    private static final DynamicCommandExceptionType UNKNOWN_JOB =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown job: " + id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("job")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                JobRegistry.all().stream().map(Job::id), builder))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Job job = find(ResourceLocationArgument.getId(ctx, "id"));
                                            JobService.assign(player, job);
                                            return 1;
                                        })))
                        .then(Commands.literal("reroll")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    if (JobService.reroll(player) == null) {
                                        ctx.getSource().sendFailure(Component.literal("No jobs are loaded."));
                                        return 0;
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    JobService.clear(ctx.getSource().getPlayerOrException());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Job cleared."), false);
                                    return 1;
                                }))));
    }

    /** Looks the id up as typed, then in our namespace, so "duelist" works without a prefix. */
    private static Job find(ResourceLocation typed) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Job job = JobRegistry.get(typed);
        if (job == null && typed.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            job = JobRegistry.get(ProjectZero.id(typed.getPath()));
        }
        if (job == null) throw UNKNOWN_JOB.create(typed);
        return job;
    }
}
