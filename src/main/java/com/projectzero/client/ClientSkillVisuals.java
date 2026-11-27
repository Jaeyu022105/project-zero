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
        BUBBLE;

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

    public static void add(int entityId, int kind, int rgb, int ticks) {
        Kind k = Kind.byId(kind);
        // One bubble per entity: a recast refreshes it.
        if (k == Kind.BUBBLE) ACTIVE.removeIf(v -> v.entityId == entityId && v.kind == Kind.BUBBLE);
        ACTIVE.add(new Visual(entityId, k, rgb, ticks));
    }

    public static List<Visual> active() {
        return ACTIVE;
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
