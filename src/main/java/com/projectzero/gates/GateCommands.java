package com.projectzero.gates;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.projectzero.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Operator commands merged into {@code /pz}:
 *   /pz gate spawn <rank> <size> [sea|ancient]
 *   /pz gate age <days>
 *   /pz gate break
 *   /pz gate leave
 */
public final class GateCommands {
    private GateCommands() {}

    private static final List<String> RANKS = Arrays.stream(GateRank.values())
            .map(r -> r.name().toLowerCase(Locale.ROOT)).toList();
    private static final List<String> SIZES = Arrays.stream(GateSize.values())
            .map(GateSize::key).toList();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("gate")
                        .then(Commands.literal("leave")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    GateService.returnFromDungeon(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Returned from dungeon."), false);
                                    return 1;
                                }))
                        .then(Commands.literal("age")
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> ageLookedAtGate(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("break")
                                .executes(ctx -> breakLookedAtGate(ctx.getSource())))
                        .then(Commands.literal("corruption")
                                .then(Commands.literal("grow")
                                        .executes(ctx -> growCorruption(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> clearCorruption(ctx.getSource()))))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RANKS, b))
                                        .then(Commands.argument("size", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(SIZES, b))
                                                .executes(ctx -> spawnGate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "rank"),
                                                        StringArgumentType.getString(ctx, "size"), false))
                                                .then(Commands.literal("sea")
                                                        .executes(ctx -> spawnGate(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "rank"),
                                                                StringArgumentType.getString(ctx, "size"), true)))
                                                .then(Commands.literal("ancient")
                                                        .executes(ctx -> spawnAncient(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "rank"),
                                                                StringArgumentType.getString(ctx, "size")))))))));
    }

    private static int ageLookedAtGate(CommandSourceStack source, int days) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        GateBlockEntity anchorBe = findLookedAtAnchor(player, level);
        if (anchorBe == null) {
            source.sendFailure(Component.literal("Look at a Gate within 32 blocks."));
            return 0;
        }
        BlockPos anchorPos = anchorBe.anchorPos();
        if (!level.getGameRules().getBoolean(GateRules.RULE_GATE_AGING)) {
            source.sendFailure(Component.literal("Gamerule projectzero:gateAging is currently false."));
            return 0;
        }
        GateService.shiftPortalOpenTime(level, anchorPos, anchorBe, -days * 24000L);
        GateService.evaluateAging(level, anchorPos, level.getBlockState(anchorPos), anchorBe);
        source.sendSuccess(() -> Component.literal("Aged gate at " + anchorPos.toShortString() + " by " + days
                + " day(s) -> " + anchorBe.rank().name() + "-Rank, surges=" + anchorBe.surges()
                + (anchorBe.isBreaking() ? " [BREAKING]" : "") + "."), false);
        return 1;
    }

    private static int breakLookedAtGate(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        GateBlockEntity anchorBe = findLookedAtAnchor(player, level);
        if (anchorBe == null) {
            source.sendFailure(Component.literal("Look at a Gate within 32 blocks."));
            return 0;
        }
        BlockPos anchorPos = anchorBe.anchorPos();
        if (!GateService.startDungeonBreak(level, anchorPos, level.getBlockState(anchorPos), anchorBe)) {
            source.sendFailure(Component.literal("Gate is already breaking or cleared."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Started Dungeon Break at " + anchorPos.toShortString() + "."), false);
        return 1;
    }

    private static int growCorruption(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        GateBlockEntity anchorBe = findLookedAtAnchor(player, level);
        if (anchorBe == null) {
            source.sendFailure(Component.literal("Look at a Gate within 32 blocks."));
            return 0;
        }
        BlockPos anchorPos = anchorBe.anchorPos();
        BlockState st = level.getBlockState(anchorPos);
        boolean sea = st.getBlock() instanceof GateBlock && st.getValue(GateBlock.SEA);
        int slot = Math.max(0, anchorBe.dungeonSlot());
        int converted = CorruptionService.growNow(level, anchorPos, slot, anchorBe.rank(), sea);
        CorruptionSavedData.CorruptionZone zone = CorruptionSavedData.get(level).zone(anchorPos);
        int total = zone != null ? zone.originalBlocks.size() : converted;
        int rad = zone != null ? zone.targetRadius : 0;
        source.sendSuccess(() -> Component.literal("Grew corruption at " + anchorPos.toShortString()
                + " (radius " + rad + "): +" + converted + " blocks (" + total + " total)."), false);
        return 1;
    }

    private static int clearCorruption(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        GateBlockEntity anchorBe = findLookedAtAnchor(player, level);
        BlockPos targetAnchor = anchorBe != null ? anchorBe.anchorPos() : null;
        if (targetAnchor == null) {
            double best = 32.0 * 32.0;
            for (CorruptionSavedData.CorruptionZone z : CorruptionSavedData.get(level).zones()) {
                double d = z.anchor.distToCenterSqr(player.position());
                if (d <= best) {
                    best = d;
                    targetAnchor = z.anchor;
                }
            }
        }
        if (targetAnchor == null) {
            source.sendFailure(Component.literal("Look at a Gate or stand within 32 blocks of active corruption."));
            return 0;
        }
        final BlockPos anchorPos = targetAnchor;
        int restored = CorruptionService.clearImmediate(level, anchorPos);
        source.sendSuccess(() -> Component.literal("Cleared corruption at " + anchorPos.toShortString()
                + " (" + restored + " blocks restored)."), false);
        return 1;
    }

    public static @Nullable GateBlockEntity findLookedAtAnchor(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        BlockPos portalPos = null;

        // Step along the look ray so tall grass/vines in front of a free-floating rift never block targeting.
        for (double d = 0.5; d <= 32.0; d += 0.5) {
            BlockPos p = BlockPos.containing(eye.add(dir.scale(d)));
            BlockState st = level.getBlockState(p);
            if (st.is(ModBlocks.GATE_PORTAL.get())) {
                portalPos = p;
                break;
            }
            if (st.is(ModBlocks.ANCIENT_GATE_STONE.get())) {
                for (BlockPos q : BlockPos.betweenClosed(p.offset(-4, -4, -4), p.offset(4, 4, 4))) {
                    if (level.getBlockState(q).is(ModBlocks.GATE_PORTAL.get())) {
                        portalPos = q.immutable();
                        break;
                    }
                }
                if (portalPos != null) break;
            }
            if (st.isSolidRender(level, p)) break;
        }
        if (portalPos == null) return null;
        if (!(level.getBlockEntity(portalPos) instanceof GateBlockEntity be)) return null;
        BlockPos anchor = be.anchorPos();
        return level.getBlockEntity(anchor) instanceof GateBlockEntity abe ? abe : be;
    }

    /** {@code /pz gate spawn <rank> <size> ancient}: a stone-ringed gate on the ground ahead. */
    private static int spawnAncient(CommandSourceStack source, String rankRaw, String sizeRaw)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GateRank rank = GateRank.byName(rankRaw);
        GateSize size = GateSize.byName(sizeRaw);
        if (rank == null || size == null) {
            source.sendFailure(Component.literal("Unknown gate rank or size: " + rankRaw + " " + sizeRaw));
            return 0;
        }
        Direction facing = player.getDirection();
        boolean alongX = facing.getAxis() == Direction.Axis.Z;
        BlockPos col = player.blockPosition().relative(facing, 6);
        int top = player.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                col.getX(), col.getZ());
        BlockPos ground = new BlockPos(col.getX(), top - 1, col.getZ());
        var rec = GateService.spawnAncientGate(player.serverLevel(), ground, rank, size, alongX);
        if (rec == null) {
            source.sendFailure(Component.literal("Failed to spawn gate (missing projectzero:dungeon dimension)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned ancient " + rank.name() + "-Rank (" + size.name()
                + ") Gate at " + rec.gateAnchor.toShortString() + " [slot " + rec.slot + "]."), false);
        return 1;
    }

    private static int spawnGate(CommandSourceStack source, String rankRaw, String sizeRaw, boolean sea)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GateRank rank = GateRank.byName(rankRaw);
        if (rank == null) {
            source.sendFailure(Component.literal("Unknown gate rank: " + rankRaw));
            return 0;
        }
        GateSize size = GateSize.byName(sizeRaw);
        if (size == null) {
            source.sendFailure(Component.literal("Unknown gate size: " + sizeRaw));
            return 0;
        }

        Direction facing = player.getDirection();
        boolean alongX = (facing.getAxis() == Direction.Axis.Z);
        BlockPos base = player.blockPosition().relative(facing, 4).above(1);

        var rec = GateService.spawnGate(player.serverLevel(), base, rank, size, alongX, sea);
        if (rec == null) {
            source.sendFailure(Component.literal("Failed to spawn gate (missing projectzero:dungeon dimension)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned " + rank.name() + "-Rank (" + size.name()
                + ") " + (sea ? "Sea Gate" : "Gate") + " at " + base.toShortString() + " [slot " + rec.slot + "]."), false);
        return 1;
    }
}
