package com.projectzero.skills;

import com.projectzero.ProjectZero;
import com.projectzero.fx.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Particle shapes for skills (rings, sigils, arcs, domes, columns, lines) and a tiny tick
 * scheduler so an effect can unfold over a few ticks (an expanding shockwave, a travelling
 * wave). Server side; everything is sent as normal particles, so everyone nearby sees it.
 */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillFx {
    private SkillFx() {}

    // Skill colors (match the HUD icons).
    public static final int WIND = 0x7FE8FF;
    public static final int LIFE = 0x6CF0A0;
    public static final int BLOOD = 0xE0303C;
    public static final int EMBER = 0xFFA040;
    public static final int GOLD = 0xFFD860;
    public static final int STEEL = 0xDDE6FF;
    public static final int AEGIS = 0x5CA0FF;
    public static final int HEX = 0xB060FF;
    public static final int SPIRIT = 0x9AD8FF;
    public static final int STORM = 0x88C8FF;
    public static final int SHADOW = 0x5A3A8A;
    public static final int SOUL = 0x70FFC8;
    public static final int SUN = 0xFFC850;

    private record Task(long at, Runnable run) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    /** Runs {@code run} after {@code ticks} server ticks. */
    public static void later(int ticks, Runnable run) {
        TASKS.add(new Task(now + Math.max(1, ticks), run));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.at <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.run.run();
            } catch (RuntimeException e) {
                ProjectZero.LOGGER.warn("Skill effect failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }

    // ---- particles ----

    /** Our soft mana glow in {@code rgb} (textures/particle/mana_glow_*). */
    public static ParticleOptions dust(int rgb, float size) {
        return Fx.glow(rgb, size);
    }

    private static void point(ServerLevel level, ParticleOptions p, double x, double y, double z) {
        level.sendParticles(p, x, y, z, 1, 0, 0, 0, 0);
    }

    /** Flat ring on the ground (or at any height) around {@code c}. */
    public static void ring(ServerLevel level, Vec3 c, double radius, ParticleOptions p) {
        int points = Math.max(10, (int) (radius * 12));
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            point(level, p, c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius);
        }
    }

    /** A ring that grows from {@code from} to {@code to} over {@code ticks}: a shockwave. */
    public static void shockwave(ServerLevel level, Vec3 c, double from, double to, int ticks, ParticleOptions p) {
        for (int t = 0; t <= ticks; t++) {
            double r = from + (to - from) * t / (double) ticks;
            if (t == 0) ring(level, c, r, p);
            else later(t, () -> ring(level, c, r, p));
        }
    }

    /**
     * A System sigil on the ground: outer circle, inner circle and a six-pointed star, drawn
     * a few times so it lingers for about half a second.
     */
    public static void sigil(ServerLevel level, Vec3 c, double radius, int rgb) {
        ParticleOptions d = dust(rgb, 0.9F);
        ParticleOptions glyph = Fx.rune(rgb, 1.3F);
        Runnable draw = () -> {
            Vec3 g = new Vec3(c.x, c.y + 0.05, c.z);
            ring(level, g, radius, d);
            ring(level, g, radius * 0.62, d);
            for (int k = 0; k < 2; k++) {
                for (int i = 0; i < 3; i++) {
                    double a1 = k * Math.PI / 3 + i * Math.PI * 2 / 3;
                    double a2 = a1 + Math.PI * 2 / 3;
                    line(level, g.add(Math.cos(a1) * radius * 0.62, 0, Math.sin(a1) * radius * 0.62),
                            g.add(Math.cos(a2) * radius * 0.62, 0, Math.sin(a2) * radius * 0.62), 0.25, d);
                }
            }
        };
        draw.run();
        later(5, draw);
        later(10, draw);
        // Six glyphs standing on the outer circle, rising as the sigil fades.
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + Math.PI / 6;
            point(level, glyph, c.x + Math.cos(a) * radius, c.y + 0.35, c.z + Math.sin(a) * radius);
        }
    }

    /** Points along a straight line. */
    public static void line(ServerLevel level, Vec3 a, Vec3 b, double step, ParticleOptions p) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-3) return;
        Vec3 u = d.scale(1 / len);
        for (double s = 0; s <= len; s += step) point(level, p, a.x + u.x * s, a.y + u.y * s, a.z + u.z * s);
    }

    /** A cylinder of rising points (a pillar of light / aura). */
    public static void column(ServerLevel level, Vec3 c, double height, double radius, int points, ParticleOptions p) {
        for (int i = 0; i < points; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(level.random.nextDouble());
            point(level, p, c.x + Math.cos(a) * r, c.y + level.random.nextDouble() * height, c.z + Math.sin(a) * r);
        }
    }

    /** Points climbing a helix around {@code c}, drawn over {@code ticks}. */
    public static void spiral(ServerLevel level, Vec3 c, double height, double radius, double turns, int ticks,
                              ParticleOptions p) {
        int steps = Math.max(ticks, 1);
        for (int t = 0; t < steps; t++) {
            double f = t / (double) steps;
            double a = f * turns * Math.PI * 2;
            double y = f * height;
            Runnable draw = () -> {
                for (int arm = 0; arm < 2; arm++) {
                    double aa = a + arm * Math.PI;
                    point(level, p, c.x + Math.cos(aa) * radius, c.y + y, c.z + Math.sin(aa) * radius);
                }
            };
            if (t == 0) draw.run(); else later(t, draw);
        }
    }

    /** A dome / shell of points around {@code c} (shields). */
    public static void dome(ServerLevel level, Vec3 c, double radius, int points, ParticleOptions p) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1));          // 1..0: upper half only
            double r = Math.sqrt(1 - y * y);
            double a = golden * i;
            point(level, p, c.x + Math.cos(a) * r * radius, c.y + y * radius, c.z + Math.sin(a) * r * radius);
        }
    }

    /** A crescent slash facing {@code dir}, centred on {@code c}, spanning about 150 degrees. */
    public static void crescent(ServerLevel level, Vec3 c, Vec3 dir, double radius, ParticleOptions p) {
        Vec3 f = new Vec3(dir.x, 0, dir.z);
        if (f.lengthSqr() < 1.0E-4) f = new Vec3(0, 0, 1);
        f = f.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        int points = Math.max(12, (int) (radius * 14));
        for (int i = 0; i <= points; i++) {
            double t = -1.3 + 2.6 * i / points;
            double tilt = t * 0.25;
            Vec3 q = c.add(side.scale(Math.sin(t) * radius)).add(f.scale(Math.cos(t) * radius * 0.5 - radius * 0.5))
                    .add(0, tilt, 0);
            point(level, p, q.x, q.y, q.z);
        }
    }

    /** A short trail that follows an entity for {@code ticks} (dash afterimage). */
    public static void trail(ServerLevel level, Entity e, int ticks, ParticleOptions p) {
        for (int t = 1; t <= ticks; t++) {
            later(t, () -> {
                if (!e.isAlive()) return;
                column(level, e.position(), e.getBbHeight(), 0.35, 4, p);
            });
        }
    }
}
