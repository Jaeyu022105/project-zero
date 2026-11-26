package com.projectzero.awakening;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.projectzero.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Awakening testing commands (operators only). Brigadier merges these into {@code /pz}.
 *   /pz shard give [count]   give yourself Awakening Shard(s) until gate boss loot drops them
 */
public final class AwakeningCommands {
    private AwakeningCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("shard")
                        .then(Commands.literal("give")
                                .executes(ctx -> give(ctx.getSource(), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> give(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))));
    }

    private static int give(CommandSourceStack source, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = new ItemStack(ModItems.AWAKENING_SHARD.get(), count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Gave " + count + " Awakening Shard(s)."), false);
        return 1;
    }
}
