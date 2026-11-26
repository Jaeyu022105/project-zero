package com.projectzero.equipment;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.projectzero.gates.GateRank;
import com.projectzero.jobs.JobRarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Operator command merged into {@code /pz}:
 *   /pz gear roll <slot> <rarity> <rank>
 */
public final class EquipmentCommands {
    private EquipmentCommands() {}

    private static final List<String> SLOTS = List.of("gauntlets", "ring", "amulet", "earrings", "belt", "charm");
    private static final List<String> RARITIES = Arrays.stream(JobRarity.values())
            .map(r -> r.name().toLowerCase(Locale.ROOT)).toList();
    private static final List<String> RANKS = Arrays.stream(GateRank.values())
            .map(r -> r.name().toLowerCase(Locale.ROOT)).toList();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("gear")
                        .then(Commands.literal("roll")
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SLOTS, b))
                                        .then(Commands.argument("rarity", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(RARITIES, b))
                                                .then(Commands.argument("rank", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RANKS, b))
                                                        .executes(ctx -> rollGear(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "slot"),
                                                                StringArgumentType.getString(ctx, "rarity"),
                                                                StringArgumentType.getString(ctx, "rank")))))))));
    }

    private static int rollGear(CommandSourceStack source, String slotRaw, String rarityRaw, String rankRaw)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AccessorySlotType slot = AccessorySlotType.byName(slotRaw);
        if (slot == null || slot == AccessorySlotType.RELIC) {
            source.sendFailure(Component.literal("Unknown accessory slot: " + slotRaw));
            return 0;
        }
        JobRarity rarity = null;
        for (JobRarity r : JobRarity.values()) {
            if (r.name().equalsIgnoreCase(rarityRaw)) {
                rarity = r;
                break;
            }
        }
        if (rarity == null) {
            source.sendFailure(Component.literal("Unknown rarity: " + rarityRaw));
            return 0;
        }
        GateRank rank = GateRank.byName(rankRaw);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown gate rank: " + rankRaw));
            return 0;
        }

        ItemStack rolled = EquipmentRules.rollItem(player.getRandom(), slot, rarity, rank);
        if (!player.getInventory().add(rolled.copy())) {
            player.drop(rolled.copy(), false);
        }
        source.sendSuccess(() -> Component.literal("Rolled ").append(rolled.getHoverName()), false);
        return 1;
    }
}
