package com.projectzero.domain;

import com.mojang.brigadier.CommandDispatcher;
import com.projectzero.ProjectZero;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side events for the Library domain. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LibraryEvents {
    private LibraryEvents() {}

    /** Runs before JobEvents' login check, so a stuck player is home before a new ceremony opens. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LibraryDomain.onLogin(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ReawakeningVision.onLogout(player);
            LibraryDomain.onLogout(player);
        }
    }

    /** Nothing can hurt you inside the domain or during the Reawakening vision. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (LibraryDomain.isInDomain(player) || ReawakeningVision.isInVision(player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    /** /pz domain and /pz reawaken (testing). /pz domain: open the Library domain now (testing; works even if you already have a job). */
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("domain").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!LibraryDomain.open(player)) {
                        ctx.getSource().sendFailure(Component.literal("The library could not open (see log)."));
                        return 0;
                    }
                    return 1;
                }))
                // /pz reawaken: run a real Awakening Stone reroll with the vision, no stone or shard needed.
                .then(Commands.literal("reawaken").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (ReawakeningVision.isInVision(player) || LibraryDomain.isInDomain(player)) {
                        ctx.getSource().sendFailure(Component.literal("Already in a vision or the library."));
                        return 0;
                    }
                    var result = com.projectzero.jobs.JobService.awakeningRerollQuiet(player);
                    if (result == null) {
                        ctx.getSource().sendFailure(Component.literal("You need a job first (/pz job set ...), and another job must exist."));
                        return 0;
                    }
                    if (!ReawakeningVision.begin(player, result)) {
                        com.projectzero.jobs.JobService.announceAwakening(player, result);
                        ReawakeningVision.auraBurst(player, result.job().rarity());
                    }
                    return 1;
                })));
    }
}
