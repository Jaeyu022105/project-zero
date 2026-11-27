package com.projectzero.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Active skill visuals on entities, ticked on the client. No client-only imports: the packet
 * handler touches this class on both sides. Drawn by {@link SkillVisualRenderer}.
 */
public final class ClientSkillVisuals {
    private ClientSkillVisuals() {}

    /** Visual kinds; the ordinal goes over the network. */
    public enum Kind {
        /** A big shield emblem that pops up in front of the entity and fades. */
        SHIELD_POP,
        /** A hex-lattice aura bubble around the entity. */
        BUBBLE,
        /** Hit reaction: white flash then a short shake in place (our hitstop). */
        HIT_FLASH,
        /** A crescent slash arc swept around the entity, facing its look direction at the start. */
        SLASH,
        /** Camera shake for the local player (heavy hits). */
        SHAKE,
        /** FOV punch-in that snaps back (finishers). */
        PUNCH,
        /** A player body animation; rgb carries the {@code PlayerAnim} ordinal. */
        ANIM,
        /** A lingering skill mark on an enemy's body; rgb top byte = mark style (+0x80 = minimal, bosses). */
        MARK,
        /** A floating WEAK (rgb 1) or RESIST (rgb 0) tag above a boss, for the attacker. */
        TAG;

        public static Kind byId(int id) {
            Kind[] all = values();
            return id >= 0 && id < all.length ? all[id] : BUBBLE;
        }
    }

    public static final class Visual {
        public final int entityId;
        public final Kind kind;
        public final int rgb;
        public final int total;
        public int age;
        /** Facing locked on first draw (slash arcs); NaN until then. */
        public float yaw = Float.NaN;
        /** Slash arcs: 0 forehand (right to left), 1 backhand (left to right), 2 overhead chop. */
        public int variant;

        Visual(int entityId, Kind kind, int rgb, int total) {
            this.entityId = entityId;
            this.kind = kind;
            this.rgb = rgb;
            this.total = Math.max(1, total);
        }

        /** 0..1 through its life, with partial ticks. */
        public float progress(float partialTick) {
            return Math.min(1F, (age + partialTick) / total);
        }
    }

    private static final List<Visual> ACTIVE = new ArrayList<>();

    public static Visual add(int entityId, int kind, int rgb, int ticks) {
        Kind k = Kind.byId(kind);
        // One bubble per entity: a recast refreshes it.
        if (k == Kind.BUBBLE || k == Kind.HIT_FLASH || k == Kind.SHAKE || k == Kind.PUNCH || k == Kind.ANIM || k == Kind.MARK || k == Kind.TAG)
            ACTIVE.removeIf(v -> v.entityId == entityId && v.kind == k);
        Visual v = new Visual(entityId, k, rgb, ticks);
        ACTIVE.add(v);
        return v;
    }

    public static List<Visual> active() {
        return ACTIVE;
    }

    /** The newest visual of this kind on the entity, or null. */
    public static Visual find(int entityId, Kind kind) {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Visual v = ACTIVE.get(i);
            if (v.entityId == entityId && v.kind == kind) return v;
        }
        return null;
    }

    public static void tick() {
        for (Iterator<Visual> it = ACTIVE.iterator(); it.hasNext(); ) {
            Visual v = it.next();
            if (++v.age >= v.total) it.remove();
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
