package com.projectzero.gates;

import com.projectzero.ProjectZero;
import com.projectzero.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Code-built placeholder dungeon chamber in {@code projectzero:dungeon}
 * (TODO(claude-art): Claude replaces with room-template dungeon generator).
 */
public final class GateDungeonArena {
    private GateDungeonArena() {}

    public static final String DUNGEON_SLOT_KEY = ProjectZero.MOD_ID + ":dungeon_slot";
    private static final int ROOM_HEIGHT = 9;

    public static BlockPos origin(int slot) {
        return new BlockPos(slot * GateRules.DUNGEON_SLOT_STRIDE, GateRules.DUNGEON_FLOOR_Y, 0);
    }

    public static Vec3 entranceSpawn(int slot) {
        BlockPos o = origin(slot);
        return new Vec3(o.getX() + 0.5, o.getY() + 1.0, o.getZ() + 3.5);
    }

    public static BlockPos exitPortalCenter(int slot, GateSize size) {
        BlockPos o = origin(slot);
        return new BlockPos(o.getX(), o.getY() + 1, o.getZ() + size.roomLength() - 3);
    }

    public static void buildAndPopulate(ServerLevel level, int slot, GateRank rank, GateSize size) {
        BlockPos o = origin(slot);
        int hw = size.roomHalfWidth();
        int len = size.roomLength();

        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState trim = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Carve and enclose stone-brick dungeon hall from z=0..len, x=-hw..hw, y=0..ROOM_HEIGHT
        for (int dx = -hw; dx <= hw; dx++) {
            for (int dz = 0; dz <= len; dz++) {
                for (int dy = 0; dy <= ROOM_HEIGHT; dy++) {
                    BlockPos p = o.offset(dx, dy, dz);
                    boolean boundary = (dx == -hw || dx == hw || dz == 0 || dz == len || dy == 0 || dy == ROOM_HEIGHT);
                    if (!boundary) {
                        level.setBlock(p, air, Block.UPDATE_CLIENTS);
                    } else if (dy == 0) {
                        level.setBlock(p, (Math.abs(dx) <= 1) ? trim : floor, Block.UPDATE_CLIENTS);
                    } else {
                        level.setBlock(p, wall, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // Pillars & soul lanterns along the hall
        for (int dz = 5; dz <= len - 5; dz += 6) {
            for (int side : new int[]{-hw + 2, hw - 2}) {
                for (int dy = 1; dy < ROOM_HEIGHT; dy++) {
                    level.setBlock(o.offset(side, dy, dz), Blocks.DEEPSLATE_TILE_WALL.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(o.offset(side, ROOM_HEIGHT - 1, dz), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        // Sealed exit frame at the far boss altar (filled with EXIT gate portal once boss dies)
        BlockPos exitBase = exitPortalCenter(slot, size);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                boolean frame = (dx == -1 || dx == 1 || dy == 3);
                level.setBlock(exitBase.offset(dx, dy, 0),
                        frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
        }

        // Populate rank-scaled gate monsters
        RandomSource random = level.getRandom();
        EntityType<? extends Mob>[] mobPool = new EntityType[]{
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.VINDICATOR
        };
        for (int i = 0; i < size.mobCount(); i++) {
            EntityType<? extends Mob> chosen = mobPool[random.nextInt(mobPool.length)];
            Mob mob = chosen.create(level);
            if (mob == null) continue;
            double mx = o.getX() + (random.nextDouble() * 2 - 1) * (hw - 3);
            double mz = o.getZ() + 7 + random.nextDouble() * Math.max(4, len - 14);
            mob.moveTo(mx, o.getY() + 1.0, mz, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            mob.setPersistenceRequired();
            mob.getPersistentData().putBoolean(MobScaling.GATE_MONSTER_KEY, true);
            mob.getPersistentData().putInt(DUNGEON_SLOT_KEY, slot);
            MobScaling.apply(mob, rank, rank.randomLevel(random));
            level.addFreshEntity(mob);
        }

        // Spawn Gate Boss near the far altar
        EntityType<? extends Mob> bossType = switch (rank) {
            case E, D -> EntityType.HUSK;
            case C, B -> EntityType.VINDICATOR;
            case A, S -> EntityType.WITHER_SKELETON;
        };
        Mob boss = bossType.create(level);
        if (boss != null) {
            boss.moveTo(o.getX() + 0.5, o.getY() + 1.0, o.getZ() + len - 6.0, 180.0F, 0.0F);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null);
            boss.setPersistenceRequired();
            boss.getPersistentData().putBoolean(MobScaling.GATE_MONSTER_KEY, true);
            boss.getPersistentData().putBoolean(MobScaling.GATE_BOSS_KEY, true);
            boss.getPersistentData().putInt(DUNGEON_SLOT_KEY, slot);
            MobScaling.apply(boss, rank, rank.maxLevel());

            AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.addPermanentModifier(new AttributeModifier(
                        ProjectZero.id("gate_boss_hp"),
                        GateRules.BOSS_EXTRA_HEALTH_MULT - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                boss.setHealth(boss.getMaxHealth());
            }
            boss.setCustomName(Component.literal("[" + rank.name() + " · BOSS · Lv " + rank.maxLevel() + "] ")
                    .withStyle(rank.chatColor(), ChatFormatting.BOLD)
                    .append(boss.getType().getDescription().copy().withStyle(ChatFormatting.GOLD)));
            boss.setCustomNameVisible(true);
            level.addFreshEntity(boss);
        }
    }

    public static void openExitPortal(ServerLevel level, int slot, GateSize size, GateRank rank) {
        BlockPos exitBase = exitPortalCenter(slot, size);
        BlockState portal = ModBlocks.GATE_PORTAL.get().defaultBlockState()
                .setValue(GateBlock.RANK, rank)
                .setValue(GateBlock.EXIT, true);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = exitBase.offset(0, dy, 0);
            level.setBlock(p, portal, Block.UPDATE_ALL);
            if (level.getBlockEntity(p) instanceof GateBlockEntity be) {
                be.configure(rank, size, level.getGameTime(), slot, exitBase);
            }
        }
    }
}
