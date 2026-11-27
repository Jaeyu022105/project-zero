package com.projectzero.domain;

import com.projectzero.ProjectZero;
import com.projectzero.jobs.JobRarity;
import com.projectzero.jobs.JobService;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.VisionOverlayPacket;
import com.projectzero.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import com.projectzero.fx.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Reawakening vision (DESIGN-DOCUMENT.md §6): what the player "sees" when they reroll their
 * job at an Awakening Stone.
 *
 * Timeline, in ticks from the shard being offered:
 * <ol>
 *   <li>0: eyelids close (client overlay). The player is frozen and cannot be hurt.</li>
 *   <li>{@link #T_TELEPORT}: in the dark, the player is moved onto the mirror sea in the void
 *       and the darkness fades into the vision: a sea of face-down cards drifting over the water.</li>
 *   <li>{@link #T_RISE}..{@link #T_ARRIVE}: one card leaves the sea and glides to the player.</li>
 *   <li>{@link #T_ARRIVE}..{@link #T_REVEAL}: it trembles in front of them (anticipation),
 *       with a heartbeat.</li>
 *   <li>{@link #T_REVEAL}: it flips to show the new job; the other cards burn away.</li>
 *   <li>{@link #T_CLOSE}: eyes close again; {@link #T_HOME}: back where they stood, eyes open.</li>
 *   <li>{@link #T_AURA}: a short burst of aura in the job's rarity color, seen by everyone nearby.</li>
 * </ol>
 *
 * The reroll itself is applied before the vision starts (see
 * {@link JobService#awakeningRerollQuiet}); the vision only shows it. Sessions tick from
 * {@link ReawakeningEvents} on the server tick.
 */
public final class ReawakeningVision {
    private ReawakeningVision() {}

    static final int T_TELEPORT = 22;
    static final int T_TITLE = 45;
    static final int T_RISE = 60;
    static final int T_ARRIVE = 150;
    static final int T_REVEAL = 185;
    static final int T_CLOSE = 250;
    static final int T_HOME = 268;
    static final int T_AURA = 280;
    static final int AURA_TICKS = 6;
    static final int T_END = T_AURA + AURA_TICKS;

    private static final int SEA_CARDS = 64;
    private static final double CHOSEN_START_DIST = 30;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final UUID player;
        final JobService.AwakeningResult result;
        int slot = -1;
        int age;
        @Nullable Vec3 center;
        @Nullable ServerLevel library;
        final List<JobCardEntity> cards = new ArrayList<>();
        /** Per sea card: angle, distance, height above the water, bob phase. */
        final List<double[]> drift = new ArrayList<>();
        @Nullable JobCardEntity chosen;
        Vec3 chosenStart = Vec3.ZERO;

        Session(UUID player, JobService.AwakeningResult result) {
            this.player = player;
            this.result = result;
        }
    }

    public static boolean isInVision(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static boolean owns(JobCardEntity card) {
        for (Session s : SESSIONS.values()) if (s.cards.contains(card)) return true;
        return false;
    }

    /**
     * Starts the vision for a reroll that has already been applied. Returns false if it cannot
     * run (library dimension missing); the caller then announces the result right away.
     */
    public static boolean begin(ServerPlayer player, JobService.AwakeningResult result) {
        if (isInVision(player) || LibraryDomain.isInDomain(player)) return false;
        MinecraftServer server = player.getServer();
        if (server == null || server.getLevel(LibraryDomain.LIBRARY) == null) {
            ProjectZero.LOGGER.error("Library dimension missing; the Reawakening vision cannot open");
            return false;
        }

        SESSIONS.put(player.getUUID(), new Session(player.getUUID(), result));
        LibraryDomain.saveReturnPoint(player);
        LibraryDomain.freeze(player, true);
        overlay(player, 1F, T_TELEPORT - 2, true);     // close the eyes
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    /** Called once per server tick. */
    static void tickAll(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.player);
            if (player == null) {
                cleanup(session);       // logged out; the login check sends them home
                continue;
            }
            session.age++;
            try {
                tick(session, player, server);
            } catch (RuntimeException e) {
                ProjectZero.LOGGER.error("Reawakening vision failed for {}", player.getGameProfile().getName(), e);
                abort(session, player);
            }
        }
    }

    private static void tick(Session session, ServerPlayer player, MinecraftServer server) {
        int age = session.age;

        if (age == T_TELEPORT) {
            enterSea(session, player, server);
            return;
        }
        if (age > T_TELEPORT && age < T_HOME) {
            if (session.library == null || player.level() != session.library) {
                abort(session, player);
                return;
            }
            tickSea(session, player, session.library, age);
        }
        if (age == T_HOME) leaveSea(session, player);
        if (age >= T_AURA && age < T_AURA + AURA_TICKS) {
            auraTick(player, session.result.job().rarity(), age - T_AURA);
        }
        if (age >= T_END) SESSIONS.remove(session.player);
    }

    // ---- the sea ----

    private static void enterSea(Session session, ServerPlayer player, MinecraftServer server) {
        ServerLevel library = server.getLevel(LibraryDomain.LIBRARY);
        if (library == null) {
            abort(session, player);
            return;
        }
        session.slot = freeSlot();
        session.library = library;
        VoidSeaArena.ensureBuilt(library, session.slot);
        BlockPos c = VoidSeaArena.center(session.slot);
        Vec3 center = new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        session.center = center;

        // Face south (+z): the chosen card comes from there.
        player.teleportTo(library, center.x, center.y, center.z, 0.0F, 0.0F);
        overlay(player, 0F, 40, false);     // the darkness opens into the vision

        RandomSource random = player.getRandom();
        float mirror = VoidSeaArena.mirrorY();
        long now = library.getGameTime();
        for (int i = 0; i < SEA_CARDS; i++) {
            JobCardEntity card = ModEntities.JOB_CARD.get().create(library);
            if (card == null) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 5 + Math.sqrt(random.nextDouble()) * 36;
            // Leave a clear lane to the south so the chosen card is seen coming.
            double south = Math.PI / 2;
            if (Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - south))) < 14 && dist < CHOSEN_START_DIST + 4) {
                dist = CHOSEN_START_DIST + 4 + random.nextDouble() * 6;
            }
            double height = 0.3 + random.nextDouble() * 4.5;
            double[] d = {angle, dist, height, random.nextDouble() * Math.PI * 2};
            card.setMirrorY(mirror);
            placeDrifting(card, center, d, 0);
            library.addFreshEntity(card);
            session.cards.add(card);
            session.drift.add(d);
        }

        JobCardEntity chosen = ModEntities.JOB_CARD.get().create(library);
        if (chosen != null) {
            session.chosenStart = center.add(0, 3.0, CHOSEN_START_DIST);
            chosen.setMirrorY(mirror);
            chosen.setPos(session.chosenStart);
            LibraryDomain.faceCenter(chosen, center);
            library.addFreshEntity(chosen);
            session.cards.add(chosen);
            session.chosen = chosen;
        }

        library.playSound(null, c, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.8F);
        library.playSound(null, c, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    private static void tickSea(Session session, ServerPlayer player, ServerLevel level, int age) {
        Vec3 center = session.center;
        if (center == null) return;

        // Stay on the water.
        if (player.position().distanceToSqr(center) > 0.4) {
            player.teleportTo(level, center.x, center.y, center.z, player.getYRot(), player.getXRot());
        }

        if (age == T_TITLE) {
            title(player, Component.literal("REAWAKENING").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.literal("A new path surfaces from the sea").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        // The sea of cards drifts slowly around you.
        for (int i = 0; i < session.drift.size(); i++) {
            JobCardEntity card = session.cards.get(i);
            if (card.isAlive() && !card.isFading()) placeDrifting(card, center, session.drift.get(i), age);
        }

        tickChosen(session, player, level, age, center);
        tickAmbience(player, level, age, center, session.result.shardTier());

        if (age == T_CLOSE) overlay(player, 1F, T_HOME - T_CLOSE - 2, true);
    }

    private static void tickChosen(Session session, ServerPlayer player, ServerLevel level, int age, Vec3 center) {
        JobCardEntity card = session.chosen;
        if (card == null || !card.isAlive()) return;
        Vec3 rest = center.add(0, 0.55, 2.1);     // just in front of the player's eyes

        if (age < T_RISE) {
            double bob = Math.sin(age * 0.05) * 0.2;
            card.setPos(session.chosenStart.add(0, bob, 0));
            LibraryDomain.faceCenter(card, center);
            return;
        }

        if (age <= T_ARRIVE) {
            double t = (age - T_RISE) / (double) (T_ARRIVE - T_RISE);
            double e = t * t * (3 - 2 * t);
            Vec3 pos = session.chosenStart.lerp(rest, e).add(0, Math.sin(Math.PI * t) * 1.4, 0);
            card.setPos(pos);
            LibraryDomain.faceCenter(card, center);
            if (age == T_RISE) {
                level.playSound(null, card.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 0.5F);
            }
            send(player, level, Fx.glow(0xBFEFFF, 0.8F), pos.x, pos.y + 0.6, pos.z, 1, 0.2, 0.3, 0.2, 0.0);
            if (age % 6 == 0) ripple(player, level, new Vec3(pos.x, center.y, pos.z), 1.0 + t,
                    tierTint(0xB8E6FF, session.result.shardTier()));
            return;
        }

        if (age < T_REVEAL) {
            // Anticipation: it trembles harder and harder, the heart beats faster.
            double k = (age - T_ARRIVE) / (double) (T_REVEAL - T_ARRIVE);
            card.setPos(rest.add(0, Math.sin(age * 0.9) * 0.02 * k, 0));
            LibraryDomain.faceCenter(card, center);
            float shake = (float) (Math.sin(age * 1.7) * (2 + 14 * k * k));
            card.setYRot(card.getYRot() + shake);
            card.yRotO = card.getYRot();
            int beat = k < 0.5 ? 14 : (k < 0.8 ? 9 : 6);
            if ((age - T_ARRIVE) % beat == 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                        1.0F + (float) k, 0.9F + (float) k * 0.3F);
            }
            if (age % 2 == 0) send(player, level, Fx.glow(0xBFEFFF, 0.8F), rest.x, rest.y + 0.6, rest.z, 1, 0.3, 0.4, 0.3, 0.01);
            return;
        }

        card.setPos(rest);
        LibraryDomain.faceCenter(card, center);
        if (age == T_REVEAL) reveal(session, player, level, card, rest);
    }

    private static void reveal(Session session, ServerPlayer player, ServerLevel level, JobCardEntity card, Vec3 at) {
        JobRarity rarity = session.result.job().rarity();
        card.reveal(session.result.job().id().toString(), rarity.ordinal());
        for (JobCardEntity other : session.cards) if (other != card) other.startFading();

        ParticleOptions dust = Fx.glow(rarity.argb() & 0xFFFFFF, 1.4F);
        send(player, level, dust, at.x, at.y + 0.6, at.z, 40 + rarity.ordinal() * 30, 0.5, 0.7, 0.5, 0.0);
        send(player, level, Fx.spark(0xFFFFFF, 1.2F), at.x, at.y + 0.6, at.z,
                20 + rarity.ordinal() * 25, 0.3, 0.5, 0.3, 0.05 + rarity.ordinal() * 0.03);
        ripple(player, level, new Vec3(at.x, session.center == null ? at.y : session.center.y, at.z), 2.5, rarity.argb());
        level.playSound(null, card.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, card.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F,
                0.6F + rarity.ordinal() * 0.2F);
        if (rarity.ordinal() >= JobRarity.EPIC.ordinal()) {
            level.playSound(null, card.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        }
    }

    private static void tickAmbience(ServerPlayer player, ServerLevel level, int age, Vec3 center, int shardTier) {
        RandomSource random = level.random;
        // Specks drifting up from the water, far and near.
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 3 + random.nextDouble() * 40;
            send(player, level, Fx.mote(0xBFEFFF, 1.2F), center.x + Math.cos(a) * r, center.y + 0.1 + random.nextDouble() * 10,
                    center.z + Math.sin(a) * r, 0, 0, 0.01, 0, 1.0);
        }
        // Ripples spread from where you stand.
        int beat = (age - T_TELEPORT) % 40;
        if (beat < 14 && beat % 2 == 0) ripple(player, level, center, 0.6 + beat * 0.35, tierTint(0x9FD8FF, shardTier));
        // A charged shard warms the sea: golden motes drift over the water, more for higher tiers.
        if (shardTier > 0) {
            ParticleOptions gold = Fx.glow(tierTint(0x9FD8FF, shardTier), 1.2F);
            for (int i = 0; i < shardTier; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 2 + random.nextDouble() * 30;
                send(player, level, gold, center.x + Math.cos(a) * r, center.y + 0.1 + random.nextDouble() * 4,
                        center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void placeDrifting(JobCardEntity card, Vec3 center, double[] d, int age) {
        double angle = d[0] + age * 0.0015;
        double bob = Math.sin(age * 0.04 + d[3]) * 0.25;
        card.setPos(center.x + Math.cos(angle) * d[1], center.y + d[2] + bob, center.z + Math.sin(angle) * d[1]);
        LibraryDomain.faceCenter(card, center);
    }

    private static void leaveSea(Session session, ServerPlayer player) {
        for (JobCardEntity card : session.cards) card.discard();
        session.cards.clear();
        session.drift.clear();
        session.chosen = null;
        LibraryDomain.freeze(player, false);
        LibraryDomain.sendHome(player, false);
        overlay(player, 0F, 22, true);      // open the eyes
        JobService.announceAwakening(player, session.result);
    }

    // ---- the aura ----

    /** The whole aura burst at once, for when there is no vision (fallback). */
    public static void auraBurst(ServerPlayer player, JobRarity rarity) {
        for (int k = 0; k < AURA_TICKS; k++) auraTick(player, rarity, k);
    }

    /**
     * One tick of the power-up aura around the player, in the rarity color. Visible to everyone
     * nearby. Step 0 is the flash; the next steps let the aura rise and the ring spread.
     */
    static void auraTick(ServerPlayer player, JobRarity rarity, int step) {
        ServerLevel level = player.serverLevel();
        int r = rarity.ordinal();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        ParticleOptions dust = Fx.glow(rarity.argb() & 0xFFFFFF, 1.1F + r * 0.2F);
        RandomSource random = level.random;

        // Flame-like shell around the body, rising a little each step.
        int shell = step == 0 ? 40 + r * 25 : 18 + r * 8;
        double radius = 0.55 + step * 0.08;
        for (int i = 0; i < shell; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double h = random.nextDouble() * (2.0 + r * 0.15) + step * 0.12;
            level.sendParticles(dust, x + Math.cos(a) * radius, y + h, z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }

        // Ring on the ground, spreading out (uncommon and up).
        if (r >= JobRarity.UNCOMMON.ordinal()) {
            double ring = 0.5 + step * (0.4 + r * 0.12);
            int points = 20 + r * 6;
            for (int i = 0; i < points; i++) {
                double a = i * (Math.PI * 2 / points);
                level.sendParticles(dust, x + Math.cos(a) * ring, y + 0.1, z + Math.sin(a) * ring, 1, 0, 0, 0, 0);
            }
        }

        if (step == 0) {
            level.sendParticles(Fx.spark(0xFFFFFF, 1.4F), x, y + 1.0, z, 10 + r * 8, 0.3, 0.6, 0.3, 0.12 + r * 0.03);
            if (r >= JobRarity.RARE.ordinal()) {
                level.sendParticles(Fx.spark(0xBFEFFF, 1.6F), x, y + 1.0, z, 12 + r * 6, 0.5, 0.8, 0.5, 0.3);
            }
            if (r >= JobRarity.EPIC.ordinal()) {
                level.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 1, 0, 0, 0, 0);
            }
            if (r >= JobRarity.LEGENDARY.ordinal()) {
                level.sendParticles(Fx.shard(0xFFD060, 1.4F), x, y + 1.0, z, 60, 0.4, 0.8, 0.4, 0.5);
            }
            sound(level, player, SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.8F + r * 0.15F);
            sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.6F + r * 0.2F);
            if (r >= JobRarity.RARE.ordinal()) sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8F, 1.2F);
            if (r >= JobRarity.LEGENDARY.ordinal()) sound(level, player, SoundEvents.TOTEM_USE, 0.7F, 1.1F);
        }
    }

    // ---- ending early ----

    /** Something went wrong (left the dimension, error): tidy up and tell the player the result. */
    private static void abort(Session session, ServerPlayer player) {
        cleanup(session);
        LibraryDomain.freeze(player, false);
        overlay(player, 0F, 1, false);
        if (player.level().dimension().equals(LibraryDomain.LIBRARY)) {
            LibraryDomain.sendHome(player, false);
        } else {
            LibraryDomain.clearReturnPoint(player);
        }
        if (session.age < T_HOME) JobService.announceAwakening(player, session.result);
    }

    private static void cleanup(Session session) {
        for (JobCardEntity card : session.cards) card.discard();
        session.cards.clear();
        SESSIONS.remove(session.player);
    }

    /** On logout: the reroll is already saved; drop the vision. The login check sends them home. */
    public static void onLogout(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        cleanup(session);
        LibraryDomain.freeze(player, false);
        if (!player.level().dimension().equals(LibraryDomain.LIBRARY)) LibraryDomain.clearReturnPoint(player);
    }

    // ---- helpers ----

    private static int freeSlot() {
        int slot = 0;
        while (true) {
            final int candidate = slot;
            if (SESSIONS.values().stream().noneMatch(s -> s.slot == candidate)) return slot;
            slot++;
        }
    }

    /** An expanding ring of light on the water. */
    private static void ripple(ServerPlayer player, ServerLevel level, Vec3 at, double radius, int rgb) {
        ParticleOptions dust = Fx.glow(rgb, 0.7F);
        int points = Math.max(12, (int) (radius * 10));
        for (int i = 0; i < points; i++) {
            double a = i * (Math.PI * 2 / points);
            send(player, level, dust, at.x + Math.cos(a) * radius, at.y + 0.05, at.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Long-range particles for the one player in the vision. */
    private static void send(ServerPlayer player, ServerLevel level, ParticleOptions type, double x, double y, double z,
                             int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(player, type, true, x, y, z, count, dx, dy, dz, speed);
    }

    private static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Blends a cool sea color toward gold by Shard Vessel charge tier (0 = untouched, 5 = full gold). */
    private static int tierTint(int cool, int shardTier) {
        float t = Math.max(0, Math.min(5, shardTier)) / 5F;
        int gold = 0xFFD060;
        int r = (int) (((cool >> 16) & 0xFF) * (1 - t) + ((gold >> 16) & 0xFF) * t);
        int g = (int) (((cool >> 8) & 0xFF) * (1 - t) + ((gold >> 8) & 0xFF) * t);
        int b = (int) ((cool & 0xFF) * (1 - t) + (gold & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void overlay(ServerPlayer player, float coverage, int ticks, boolean eyelids) {
        ModNetwork.sendToPlayer(player, new VisionOverlayPacket(coverage, ticks, eyelids));
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}
