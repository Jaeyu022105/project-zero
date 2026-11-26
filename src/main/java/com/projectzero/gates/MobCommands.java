package com.projectzero.gates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.projectzero.leveling.LevelingRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Operator command merged into {@code /pz}:
 *   /pz mob spawn <entity> <rank> [level]
 */
public final class MobCommands {
    private MobCommands() {}

    private static final List<String> RANKS = Arrays.stream(GateRank.values())
            .map(r -> r.name().toLowerCase(Locale.ROOT)).toList();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("mob")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                                ForgeRegistries.ENTITY_TYPES.getKeys(), b))
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(RANKS, b))
                                                .executes(ctx -> spawnMob(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "entity"),
                                                        StringArgumentType.getString(ctx, "rank"),
                                                        -1))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, LevelingRules.MAX_LEVEL))
                                                        .executes(ctx -> spawnMob(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "entity"),
                                                                StringArgumentType.getString(ctx, "rank"),
                                                                IntegerArgumentType.getInteger(ctx, "level")))))))));
    }

    private static int spawnMob(CommandSourceStack source, ResourceLocation entityId, String rankRaw, int explicitLevel)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityId));
            return 0;
        }
        GateRank rank = GateRank.byName(rankRaw);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown gate rank: " + rankRaw));
            return 0;
        }

        Entity created = type.create(level);
        if (!(created instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not a LivingEntity: " + entityId));
            return 0;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 pos = player.position().add(look.x * 2.5, 0, look.z * 2.5);
        living.moveTo(pos.x, player.getY(), pos.z, player.getYRot() + 180.0F, 0.0F);

        if (living instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.COMMAND, null);
        }

        int mobLevel = explicitLevel > 0 ? explicitLevel : rank.randomLevel(player.getRandom());
        MobScaling.apply(living, rank, mobLevel);
        level.addFreshEntity(living);

        source.sendSuccess(() -> Component.literal("Spawned ").append(living.getDisplayName())
                .append(Component.literal(" (HP " + Math.round(living.getMaxHealth()) + ")")), false);
        return 1;
    }
}
