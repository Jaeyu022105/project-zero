package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.registry.ModBlocks;
import com.projectzero.registry.ModItems;
import com.projectzero.titles.MilestoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-side manager for spawning Gates, entering/leaving {@code projectzero:dungeon},
 * handling boss kills (opening exit, dropping Awakening Shard, closing Gate), and natural spawning.
 */
public final class GateService {
    private GateService() {}

    public static final ResourceKey<Level> DUNGEON =
            ResourceKey.create(Registries.DIMENSION, ProjectZero.id("dungeon"));

    private static final String RETURN_KEY = ProjectZero.MOD_ID + ":gate_return";
    private static final String COOLDOWN_KEY = ProjectZero.MOD_ID + ":gate_cooldown_until";
    private static final String ACTIVE_SLOT_KEY = ProjectZero.MOD_ID + ":active_dungeon_slot";

    /** Spawns a framed Gate structure at {@code basePos} linked to a fresh slot in {@code projectzero:dungeon}. */
    public static @Nullable DungeonSavedData.DungeonRecord spawnGate(
            ServerLevel originLevel, BlockPos basePos, GateRank rank, GateSize size, boolean alongX) {
        MinecraftServer server = originLevel.getServer();
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) {
            ProjectZero.LOGGER.error("Missing projectzero:dungeon dimension!");
            return null;
        }

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        DungeonSavedData.DungeonRecord record = data.allocate(
                rank, size, originLevel.dimension().location(), basePos, alongX);

        int pw = size.portalWidth();
        int ph = size.portalHeight();
        long now = originLevel.getGameTime();

        BlockState frameState = rank == GateRank.S
                ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : Blocks.OBSIDIAN.defaultBlockState();
        BlockState portalState = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, false);

        for (int w = -1; w <= pw; w++) {
            for (int h = -1; h <= ph; h++) {
                BlockPos p = alongX ? basePos.offset(w, h, 0) : basePos.offset(0, h, w);
                boolean frame = (w == -1 || w == pw || h == -1 || h == ph);
                if (frame) {
                    originLevel.setBlock(p, frameState, Block.UPDATE_ALL);
                } else {
                    originLevel.setBlock(p, portalState, Block.UPDATE_ALL);
                    if (originLevel.getBlockEntity(p) instanceof GateBlockEntity be) {
                        be.configure(rank, size, now, record.slot, basePos);
                    }
                }
            }
        }

        originLevel.playSound(null, basePos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 0.9F);
        return record;
    }

    public static void onEnterPortal(ServerPlayer player, BlockPos pos, BlockState state) {
        long now = player.serverLevel().getGameTime();
        long until = player.getPersistentData().getLong(COOLDOWN_KEY);
        if (now < until) return;
        player.getPersistentData().putLong(COOLDOWN_KEY, now + GateRules.PORTAL_COOLDOWN_TICKS);

        if (state.getValue(GateBlock.EXIT)) {
            returnFromDungeon(player);
            return;
        }

        if (!(player.level().getBlockEntity(pos) instanceof GateBlockEntity be)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) return;

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        int slot = be.dungeonSlot();
        DungeonSavedData.DungeonRecord rec = slot >= 0 ? data.record(slot) : null;
        if (rec == null) {
            rec = data.allocate(be.rank(), be.size(), player.level().dimension().location(), be.anchorPos(), true);
            slot = rec.slot;
            be.configure(be.rank(), be.size(), be.openTime(), slot, be.anchorPos());
        }
        if (rec.cleared) {
            system(player, Component.literal("This gate has already collapsed.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!rec.built) {
            GateDungeonArena.buildAndPopulate(dungeonLevel, slot, rec.rank, rec.size);
            data.markBuilt(slot);
        }

        saveReturnPoint(player);
        player.getPersistentData().putInt(ACTIVE_SLOT_KEY, slot);

        Vec3 target = GateDungeonArena.entranceSpawn(slot);
        player.teleportTo(dungeonLevel, target.x, target.y, target.z, 0.0F, 0.0F);
        player.getPersistentData().putLong(COOLDOWN_KEY, dungeonLevel.getGameTime() + GateRules.PORTAL_COOLDOWN_TICKS);

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(rec.rank.name() + "-RANK GATE").withStyle(rec.rank.chatColor(), ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Slay the dungeon boss to open the exit").withStyle(ChatFormatting.GRAY)));
        system(player, Component.literal("You have entered a ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(rec.rank.name() + "-Rank (" + rec.size.name() + ") Gate")
                        .withStyle(rec.rank.chatColor()))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
    }

    public static void onDungeonBossKilled(ServerLevel level, LivingEntity boss, @Nullable ServerPlayer killer) {
        if (!boss.getPersistentData().getBoolean(MobScaling.GATE_BOSS_KEY)) return;
        if (!boss.getPersistentData().contains(GateDungeonArena.DUNGEON_SLOT_KEY)) return;

        int slot = boss.getPersistentData().getInt(GateDungeonArena.DUNGEON_SLOT_KEY);
        MinecraftServer server = level.getServer();
        ServerLevel dungeonLevel = server.getLevel(DUNGEON);
        if (dungeonLevel == null) return;

        DungeonSavedData data = DungeonSavedData.get(dungeonLevel);
        DungeonSavedData.DungeonRecord rec = data.record(slot);
        if (rec == null || rec.cleared) return;

        data.markCleared(slot);
        GateDungeonArena.openExitPortal(dungeonLevel, slot, rec.size, rec.rank);

        // Guaranteed Awakening Shard drop from Gate Boss (DESIGN-DOCUMENT.md §6)
        ItemEntity shardDrop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(),
                new ItemStack(ModItems.AWAKENING_SHARD.get()));
        shardDrop.setDefaultPickUpDelay();
        level.addFreshEntity(shardDrop);

        // Seal/close the Overworld Gate
        closeOverworldGate(server, rec);

        // Notify and credit all players inside this dungeon slot
        BlockPos origin = GateDungeonArena.origin(slot);
        for (ServerPlayer p : dungeonLevel.players()) {
            if (Math.abs(p.getX() - origin.getX()) < GateRules.DUNGEON_SLOT_STRIDE / 2.0) {
                MilestoneTracker.recordGateCleared(p);
                system(p, Component.literal("Dungeon boss defeated! The exit gate has opened and the outer Gate has sealed.")
                        .withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        if (killer != null && killer.level() != dungeonLevel) {
            MilestoneTracker.recordGateCleared(killer);
        }
    }

    private static void closeOverworldGate(MinecraftServer server, DungeonSavedData.DungeonRecord rec) {
        ServerLevel originLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, rec.originDim));
        if (originLevel == null) return;

        int pw = rec.size.portalWidth();
        int ph = rec.size.portalHeight();
        for (int w = -1; w <= pw; w++) {
            for (int h = -1; h <= ph; h++) {
                BlockPos p = rec.alongX ? rec.gateAnchor.offset(w, h, 0) : rec.gateAnchor.offset(0, h, w);
                BlockState st = originLevel.getBlockState(p);
                if (st.is(ModBlocks.GATE_PORTAL.get()) || st.is(Blocks.OBSIDIAN) || st.is(Blocks.CRYING_OBSIDIAN)) {
                    originLevel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        originLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                rec.gateAnchor.getX() + 1.0, rec.gateAnchor.getY() + 1.5, rec.gateAnchor.getZ() + 0.5,
                40, 1.0, 1.2, 1.0, 0.08);
    }

    public static void returnFromDungeon(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        CompoundTag back = player.getPersistentData().getCompound(RETURN_KEY);
        ServerLevel target = null;
        if (back.contains("dim")) {
            ResourceLocation dim = ResourceLocation.tryParse(back.getString("dim"));
            if (dim != null) target = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        }
        if (target == null) {
            target = server.overworld();
            BlockPos sp = target.getSharedSpawnPos();
            player.teleportTo(target, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0.0F, 0.0F);
        } else {
            player.teleportTo(target, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"),
                    back.getFloat("yaw"), back.getFloat("pitch"));
        }
        player.getPersistentData().remove(RETURN_KEY);
        player.getPersistentData().remove(ACTIVE_SLOT_KEY);
        player.getPersistentData().putLong(COOLDOWN_KEY, target.getGameTime() + GateRules.PORTAL_COOLDOWN_TICKS);
    }

    private static void saveReturnPoint(ServerPlayer player) {
        if (player.level().dimension().equals(DUNGEON)) return;
        Vec3 backPos = player.position().subtract(player.getLookAngle().normalize().scale(1.5));
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension().location().toString());
        back.putDouble("x", backPos.x);
        back.putDouble("y", player.getY());
        back.putDouble("z", backPos.z);
        back.putFloat("yaw", player.getYRot());
        back.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_KEY, back);
    }

    /** Periodic natural Gate spawning near overworld players when {@code projectzero:gateSpawning} is true. */
    public static void tickNaturalSpawning(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % GateRules.SPAWN_CHECK_INTERVAL_TICKS != 0) return;
        if (!level.getGameRules().getBoolean(GateRules.RULE_GATE_SPAWNING)) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        RandomSource random = level.getRandom();
        if (random.nextDouble() >= GateRules.SPAWN_CHANCE_PER_CHECK) return;

        ServerPlayer anchor = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = GateRules.SPAWN_MIN_DISTANCE
                + random.nextDouble() * (GateRules.SPAWN_MAX_DISTANCE - GateRules.SPAWN_MIN_DISTANCE);
        int gx = (int) Math.round(anchor.getX() + Math.cos(angle) * dist);
        int gz = (int) Math.round(anchor.getZ() + Math.sin(angle) * dist);
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) + 1;
        if (gy <= level.getMinBuildHeight() + 2 || gy >= level.getMaxBuildHeight() - 10) return;

        GateRank rank = GateRules.randomRank(random);
        GateSize size = GateRules.randomSize(random);
        BlockPos pos = new BlockPos(gx, gy, gz);
        if (spawnGate(level, pos, rank, size, random.nextBoolean()) != null) {
            system(anchor, Component.literal("A ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(rank.name() + "-Rank Gate").withStyle(rank.chatColor()))
                    .append(Component.literal(" has opened nearby!").withStyle(ChatFormatting.WHITE)));
        }
    }

    private static void system(ServerPlayer player, Component msg) {
        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA).append(msg));
    }
}
