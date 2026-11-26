package com.projectzero.awakening;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Awakening & Shard Vessel testing commands (operators only). Brigadier merges these into {@code /pz}.
 *   /pz shard give [count] [tier]   give yourself Awakening Shard(s) at charge tier 0..5
 *   /pz vessel charge <xp>          add charge XP to the first active Shard Vessel in inventory
 */
public final class AwakeningCommands {
    private AwakeningCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("shard")
                        .then(Commands.literal("give")
                                .executes(ctx -> giveShard(ctx.getSource(), 1, 0))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveShard(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count"), 0))
                                        .then(Commands.argument("tier", IntegerArgumentType.integer(0, AwakeningRules.MAX_SHARD_TIER))
                                                .executes(ctx -> giveShard(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                        IntegerArgumentType.getInteger(ctx, "tier")))))))
                .then(Commands.literal("vessel")
                        .then(Commands.literal("charge")
                                .then(Commands.argument("xp", LongArgumentType.longArg(1))
                                        .executes(ctx -> chargeVessel(ctx.getSource(),
                                                LongArgumentType.getLong(ctx, "xp")))))));
    }

    private static int giveShard(CommandSourceStack source, int count, int tier) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = ShardVesselService.createShardStack(count, tier);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Gave " + count + " ")
                .append(AwakeningRules.tierDisplayName(tier))
                .append(Component.literal(" Awakening Shard(s).")), false);
        return 1;
    }

    private static int chargeVessel(CommandSourceStack source, long xp) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack vessel = ShardVesselService.findActiveVessel(player);
        if (vessel == null) {
            source.sendFailure(Component.literal("No Shard Vessel holding a shard found in your inventory."));
            return 0;
        }
        ShardCharge updated = ShardVesselService.addCharge(player, vessel, xp);
        source.sendSuccess(() -> Component.literal("Charged Vessel by +" + xp + " XP -> "
                + updated.chargeXp() + " XP (Tier " + updated.tier() + "/" + AwakeningRules.MAX_SHARD_TIER + ")."), false);
        return 1;
    }
}
