package com.projectzero.client;

import com.projectzero.jobs.JobRarity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client copy of the gauntlets each visible player wears, keyed by entity id, so the
 * gauntlet layer can draw them on other players too. No client-only imports: the sync
 * packet's handler touches this class on both sides.
 */
public final class ClientWornGear {
    private ClientWornGear() {}

    public record Worn(ResourceLocation itemId, @Nullable JobRarity rarity) {}

    private static final Map<Integer, Worn> GAUNTLETS = new HashMap<>();

    public static void setGauntlets(int entityId, @Nullable Worn worn) {
        if (worn == null) GAUNTLETS.remove(entityId);
        else GAUNTLETS.put(entityId, worn);
    }

    public static @Nullable Worn gauntlets(int entityId) {
        return GAUNTLETS.get(entityId);
    }

    public static void clear() {
        GAUNTLETS.clear();
    }
}
