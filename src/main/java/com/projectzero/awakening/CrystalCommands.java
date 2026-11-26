package com.projectzero.awakening;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Visual testing (operators only): {@code /pz crystal small|large|colossal} builds a formation
 * in front of you, on the ground.
 */
public final class CrystalCommands {
    private CrystalCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> crystal = Commands.literal("crystal");
        for (CrystalFormation.Size size : CrystalFormation.Size.values()) {
            crystal.then(Commands.literal(size.key()).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ServerLevel level = player.serverLevel();
                Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
                double dist = size.footprintRadius() + 3;
                BlockPos target = BlockPos.containing(player.position().add(look.scale(dist)));
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
                CrystalFormation.place(level, ground, size, level.getRandom());
                ctx.getSource().sendSuccess(() -> Component.literal("Built a " + size.key() + " Awakening Stone."), false);
                return 1;
            }));
        }
        dispatcher.register(Commands.literal("pz").requires(source -> source.hasPermission(2)).then(crystal));
    }
}
