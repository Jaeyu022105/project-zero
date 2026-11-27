package com.projectzero.leveling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Who gets credit for a kill. The damage source's attacker first; otherwise the player who
 * hurt the victim recently (vanilla keeps this for 5 seconds). That covers damage over time
 * (wither, burning), tamed summons like Spirit Hounds, and knockback into lava.
 */
public final class KillCredit {
    private KillCredit() {}

    public static @Nullable ServerPlayer of(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p) return p;
        LivingEntity victim = event.getEntity();
        return victim.getKillCredit() instanceof ServerPlayer p ? p : null;
    }
}
