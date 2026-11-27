package com.projectzero.domain;

import com.projectzero.ProjectZero;
import com.projectzero.jobs.Job;
import com.projectzero.jobs.JobRegistry;
import com.projectzero.jobs.JobService;
import com.projectzero.registry.ModEntities;
import com.projectzero.ui.NoticeType;
import com.projectzero.ui.SystemNotices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Library domain: the Job Change ceremony (DESIGN-DOCUMENT.md §5).
 *
 * The player is pulled into a circular library in the void "library" dimension, frozen on
 * a dais, and face-down job cards orbit them. Each card's job is drawn with the rarity
 * weights when the domain opens. Right-click a card: it flips, the rest burn away, and the
 * player gets that job and is sent back where they were.
 *
 * Sessions are driven by the cards' own ticks (see {@link #tickCard}), so no global tick
 * hook is needed. Everything is server side; the client only draws the cards.
 */
public final class LibraryDomain {
    private LibraryDomain() {}

    public static final ResourceKey<Level> LIBRARY = ResourceKey.create(Registries.DIMENSION, ProjectZero.id("library"));

    public static final int CARD_COUNT = 7;
    public static final double ORBIT_RADIUS = 3.4;
    /** Ticks before the library picks for an idle player. */
    public static final int TIMEOUT_TICKS = 20 * 90;
    /** Ticks between the pick and returning home (the reveal). */
    public static final int REVEAL_TICKS = 20 * 4;
    private static final double ORBIT_SPEED = 0.012;    // radians per tick

    private static final String RETURN_KEY = ProjectZero.MOD_ID + ":library_return";
    private static final ResourceLocation FREEZE_SPEED = ProjectZero.id("library_freeze_speed");
    private static final ResourceLocation FREEZE_JUMP = ProjectZero.id("library_freeze_jump");

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final UUID player;
        final int slot;
        final Vec3 center;
        final long openedAt;
        final List<JobCardEntity> cards = new ArrayList<>();
        final Map<UUID, Job> jobs = new HashMap<>();     // card uuid -> job
        @Nullable JobCardEntity picked;
        long pickedAt;
        long lastTick = -1;

        Session(UUID player, int slot, Vec3 center, long openedAt) {
            this.player = player;
            this.slot = slot;
            this.center = center;
            this.openedAt = openedAt;
        }
    }

    public static boolean isInDomain(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /**
     * Opens the domain for a player. Returns false if it could not open (no jobs loaded, or
     * the library dimension is missing), in which case the caller should fall back.
     */
    public static boolean open(ServerPlayer player) {
        if (isInDomain(player)) return true;
        MinecraftServer server = player.getServer();
        ServerLevel library = server == null ? null : server.getLevel(LIBRARY);
        if (library == null) {
            ProjectZero.LOGGER.error("Library dimension missing; is data/projectzero/dimension/library.json loaded?");
            return false;
        }
        if (JobRegistry.all().isEmpty()) return false;

        // Remember where to send the player back. If they are somehow already in the library
        // (crash, logout mid-ceremony), keep the return point saved before.
        saveReturnPoint(player);

        int slot = freeSlot();
        LibraryArena.ensureBuilt(library, slot);
        BlockPos c = LibraryArena.center(slot);
        Vec3 center = new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);   // standing on top of the pedestal

        Session session = new Session(player.getUUID(), slot, center, library.getGameTime());
        SESSIONS.put(player.getUUID(), session);

        player.teleportTo(library, center.x, center.y, center.z, 0.0F, 0.0F);
        freeze(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));

        // Cards, evenly spaced on the orbit, each with a job drawn by rarity.
        for (int i = 0; i < CARD_COUNT; i++) {
            Job job = JobRegistry.random(player.getRandom(), null);
            JobCardEntity card = ModEntities.JOB_CARD.get().create(library);
            if (card == null || job == null) continue;
            double angle = i * (Math.PI * 2 / CARD_COUNT);
            placeOnOrbit(card, center, angle, library.getGameTime());
            library.addFreshEntity(card);
            session.cards.add(card);
            session.jobs.put(card.getUUID(), job);
        }

        title(player, Component.literal("DOMAIN EXPANSION").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.literal("The Library of Paths").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        SystemNotices.system(player, NoticeType.QUEST,
                Component.literal("Choose your path. Touch a card.").withStyle(ChatFormatting.WHITE));
        library.playSound(null, c, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.6F);
        library.playSound(null, c, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** Called from a card's server tick. Runs the session once per game tick. */
    static void tickCard(JobCardEntity card) {
        Session session = sessionOf(card);
        if (session == null) {
            card.discard();     // orphan (server restarted, player left)
            return;
        }
        ServerLevel level = (ServerLevel) card.level();
        long now = level.getGameTime();

        // Move this card along the orbit (unless it is the chosen one).
        if (card != session.picked && !card.isFading()) {
            int index = session.cards.indexOf(card);
            double baseAngle = index * (Math.PI * 2 / CARD_COUNT);
            placeOnOrbit(card, session.center, baseAngle + (now - session.openedAt) * ORBIT_SPEED, now);
        }
        if (card.isFading() && card.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, card.getX(), card.getY() + 0.6, card.getZ(),
                    3, 0.2, 0.4, 0.2, 0.01);
        }

        if (session.lastTick == now) return;
        session.lastTick = now;
        tickSession(session, level, now);
    }

    private static void tickSession(Session session, ServerLevel level, long now) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.player);
        if (player == null || player.level() != level) {
            abort(session, level);
            return;
        }

        // Keep the player on the dais.
        if (player.position().distanceToSqr(session.center) > 0.4) {
            player.teleportTo(level, session.center.x, session.center.y, session.center.z, player.getYRot(), player.getXRot());
        }

        // Ambience: glowing specks hanging in the air all through the hall, and glyphs
        // drifting off the shelves. Sent with long range so the far ones show too.
        for (int i = 0; i < 6; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 4 + level.random.nextDouble() * (LibraryArena.RADIUS - 5);
            double y = LibraryArena.ABYSS_Y + 2 + level.random.nextDouble() * (LibraryArena.WALL_TOP - LibraryArena.ABYSS_Y);
            level.sendParticles(player, ParticleTypes.END_ROD, true,
                    session.center.x + Math.cos(a) * r, y, session.center.z + Math.sin(a) * r, 0, 0, 0.02, 0, 1.0);
        }
        if (now % 3 == 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = LibraryArena.RADIUS - 3;
            level.sendParticles(player, ParticleTypes.ENCHANT, true,
                    session.center.x + Math.cos(a) * r, session.center.y - 6 + level.random.nextDouble() * 14,
                    session.center.z + Math.sin(a) * r, 0, -Math.cos(a), 0.1, -Math.sin(a), 1.0);
        }

        if (session.picked == null) {
            if (now - session.openedAt > TIMEOUT_TICKS) {
                List<JobCardEntity> alive = session.cards.stream().filter(c -> c.isAlive()).toList();
                if (!alive.isEmpty()) {
                    SystemNotices.system(player, NoticeType.WARNING,
                            Component.literal("You hesitate. The library chooses for you.").withStyle(ChatFormatting.GRAY));
                    pick(player, alive.get(level.random.nextInt(alive.size())));
                }
            }
            return;
        }

        // Reveal in progress: hold the chosen card in front of the player, then finish.
        JobCardEntity chosen = session.picked;
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 spot = session.center.add(look.scale(1.8)).add(0, 0.6, 0);
        Vec3 pos = chosen.position();
        chosen.setPos(pos.add(spot.subtract(pos).scale(0.15)));
        faceCenter(chosen, session.center);

        long sincePick = now - session.pickedAt;
        if (sincePick == 25) {
            for (JobCardEntity other : session.cards) if (other != chosen) other.discard();
        }
        if (sincePick >= REVEAL_TICKS) {
            finish(session, player, level);
        }
    }

    /** A player right-clicked a card. */
    static void pick(ServerPlayer player, JobCardEntity card) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.picked != null || !session.cards.contains(card)) return;
        Job job = session.jobs.get(card.getUUID());
        if (job == null) return;

        ServerLevel level = (ServerLevel) card.level();
        session.picked = card;
        session.pickedAt = level.getGameTime();
        card.reveal(job.id().toString(), job.rarity().ordinal());
        for (JobCardEntity other : session.cards) if (other != card) other.startFading();

        level.playSound(null, card.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 1.0F);
        level.playSound(null, card.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F,
                0.6F + job.rarity().ordinal() * 0.2F);
        level.sendParticles(ParticleTypes.END_ROD, card.getX(), card.getY() + 0.6, card.getZ(),
                20 + job.rarity().ordinal() * 25, 0.3, 0.5, 0.3, 0.05 + job.rarity().ordinal() * 0.03);
    }

    private static void finish(Session session, ServerPlayer player, ServerLevel level) {
        Job job = session.picked == null ? null : session.jobs.get(session.picked.getUUID());
        cleanup(session, level);
        freeze(player, false);
        sendHome(player);
        if (job != null) {
            JobService.assign(player, job);
        }
    }

    /** Player left or died mid-ceremony: clean up; they get the ceremony again later. */
    private static void abort(Session session, ServerLevel level) {
        cleanup(session, level);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.player);
        if (player != null) freeze(player, false);
    }

    private static void cleanup(Session session, ServerLevel level) {
        for (JobCardEntity card : session.cards) card.discard();
        SESSIONS.remove(session.player);
    }

    /** On logout: end the session. The return point stays saved for the next login. */
    public static void onLogout(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) {
            cleanup(session, level);
        }
        freeze(player, false);
    }

    /** On login: someone stuck in the library without a ceremony is sent home. */
    public static void onLogin(ServerPlayer player) {
        if (player.level().dimension().equals(LIBRARY) && !isInDomain(player) && !ReawakeningVision.isInVision(player)) {
            freeze(player, false);
            sendHome(player);
        }
    }

    /**
     * Remembers where to send the player back. If they are already in the library dimension
     * (crash, logout mid-ceremony), the return point saved before is kept.
     */
    static void saveReturnPoint(ServerPlayer player) {
        if (player.level().dimension().equals(LIBRARY)) return;
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension().location().toString());
        back.putDouble("x", player.getX());
        back.putDouble("y", player.getY());
        back.putDouble("z", player.getZ());
        back.putFloat("yaw", player.getYRot());
        back.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_KEY, back);
    }

    /** Drops a saved return point that will not be used (the player never left). */
    static void clearReturnPoint(ServerPlayer player) {
        player.getPersistentData().remove(RETURN_KEY);
    }

    private static void sendHome(ServerPlayer player) {
        sendHome(player, true);
    }

    /** Teleports the player to their saved return point (or world spawn), optionally with a short blindness. */
    static void sendHome(ServerPlayer player, boolean blind) {
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
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
        } else {
            player.teleportTo(target, back.getDouble("x"), back.getDouble("y"), back.getDouble("z"),
                    back.getFloat("yaw"), back.getFloat("pitch"));
        }
        player.getPersistentData().remove(RETURN_KEY);
        if (blind) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false));
    }

    // ---- helpers ----

    private static @Nullable Session sessionOf(JobCardEntity card) {
        for (Session s : SESSIONS.values()) if (s.cards.contains(card)) return s;
        return null;
    }

    private static int freeSlot() {
        int slot = 0;
        while (true) {
            final int candidate = slot;
            if (SESSIONS.values().stream().noneMatch(s -> s.slot == candidate)) return slot;
            slot++;
        }
    }

    private static void placeOnOrbit(JobCardEntity card, Vec3 center, double angle, long time) {
        double bob = Math.sin(time * 0.08 + angle * 3) * 0.15;
        double x = center.x + Math.cos(angle) * ORBIT_RADIUS;
        double z = center.z + Math.sin(angle) * ORBIT_RADIUS;
        card.setPos(x, center.y + 0.9 + bob, z);
        faceCenter(card, center);
    }

    static void faceCenter(JobCardEntity card, Vec3 center) {
        double dx = center.x - card.getX();
        double dz = center.z - card.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        card.setYRot(yaw);
        card.yRotO = yaw;
    }

    static void freeze(ServerPlayer player, boolean on) {
        toggle(player.getAttribute(Attributes.MOVEMENT_SPEED), FREEZE_SPEED, on);
        toggle(player.getAttribute(Attributes.JUMP_STRENGTH), FREEZE_JUMP, on);
    }

    private static void toggle(@Nullable AttributeInstance attr, ResourceLocation id, boolean on) {
        if (attr == null) return;
        attr.removeModifier(id);
        if (on) attr.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}
