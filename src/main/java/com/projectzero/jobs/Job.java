package com.projectzero.jobs;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * One job, loaded from {@code data/<namespace>/jobs/<name>.json}. The file name is the job id.
 *
 * @param upgradesTo the job this one advances into after its trial (job advancement, §5)
 * @param trial      id of the trial gate that unlocks the upgrade; trials are not built yet
 */
public record Job(ResourceLocation id, JobRarity rarity,
                  Optional<ResourceLocation> upgradesTo, Optional<ResourceLocation> trial,
                  boolean noCeiling, boolean companions) {

    public Job(ResourceLocation id, JobRarity rarity,
               Optional<ResourceLocation> upgradesTo, Optional<ResourceLocation> trial) {
        this(id, rarity, upgradesTo, trial, false, false);
    }

    public Job(ResourceLocation id, JobRarity rarity,
               Optional<ResourceLocation> upgradesTo, Optional<ResourceLocation> trial, boolean noCeiling) {
        this(id, rarity, upgradesTo, trial, noCeiling, false);
    }

    /** Lang key {@code job.<namespace>.<path>}, resolved on the client. */
    public static String translationKey(ResourceLocation id) {
        return Util.makeDescriptionId("job", id);
    }

    public static Component displayName(ResourceLocation id) {
        return Component.translatable(translationKey(id));
    }

    public Component displayName() {
        return displayName(id);
    }
}
