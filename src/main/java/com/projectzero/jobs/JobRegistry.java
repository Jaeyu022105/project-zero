package com.projectzero.jobs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.projectzero.ProjectZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * All jobs, loaded from the {@code jobs/} folder of every datapack. Server side only;
 * the client gets just the player's own job id and rarity through {@code SyncJobPacket}.
 * Reloads with {@code /reload}.
 *
 * JSON format:
 * <pre>
 * { "id": "projectzero:duelist", "rarity": "uncommon",
 *   "upgradesTo": "projectzero:veilstalker", "trial": "projectzero:trial/veilstalker" }
 * </pre>
 * {@code upgradesTo} and {@code trial} are optional. {@code id} is optional and only checked
 * against the file name, which is the real id.
 */
public final class JobRegistry extends SimpleJsonResourceReloadListener {
    public static final JobRegistry INSTANCE = new JobRegistry();

    private static final Gson GSON = new Gson();

    // TreeMap keeps iteration order stable, so command suggestions come out sorted.
    private Map<ResourceLocation, Job> jobs = Map.of();
    private Map<JobRarity, List<Job>> byRarity = Map.of();

    private JobRegistry() {
        super(GSON, "jobs");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Job> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation id = e.getKey();
            try {
                loaded.put(id, parse(id, GsonHelper.convertToJsonObject(e.getValue(), "job")));
            } catch (RuntimeException ex) {
                ProjectZero.LOGGER.error("Skipping job {}: {}", id, ex.getMessage());
            }
        }

        for (Job job : loaded.values()) {
            job.upgradesTo().filter(target -> !loaded.containsKey(target)).ifPresent(target ->
                    ProjectZero.LOGGER.warn("Job {} upgrades to unknown job {}", job.id(), target));
        }

        Map<JobRarity, List<Job>> grouped = new EnumMap<>(JobRarity.class);
        for (Job job : loaded.values()) {
            grouped.computeIfAbsent(job.rarity(), r -> new ArrayList<>()).add(job);
        }

        jobs = loaded;
        byRarity = grouped;
        ProjectZero.LOGGER.info("Loaded {} jobs", loaded.size());
    }

    private static Job parse(ResourceLocation id, JsonObject json) {
        if (json.has("id")) {
            ResourceLocation declared = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (!id.equals(declared)) {
                ProjectZero.LOGGER.warn("Job file {} declares id {}; using the file name", id, declared);
            }
        }
        String rarityName = GsonHelper.getAsString(json, "rarity");
        JobRarity rarity = JobRarity.byName(rarityName);
        if (rarity == null) throw new JsonParseException("unknown rarity '" + rarityName + "'");
        return new Job(id, rarity, optionalId(json, "upgradesTo"), optionalId(json, "trial"));
    }

    private static Optional<ResourceLocation> optionalId(JsonObject json, String field) {
        if (!json.has(field)) return Optional.empty();
        String raw = GsonHelper.getAsString(json, field);
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) throw new JsonParseException("bad id in '" + field + "': " + raw);
        return Optional.of(id);
    }

    // ---- lookups ----

    public static @Nullable Job get(ResourceLocation id) {
        return INSTANCE.jobs.get(id);
    }

    public static Collection<Job> all() {
        return INSTANCE.jobs.values();
    }

    /**
     * Draws a job: first a rarity by weight (only rarities that have jobs take part),
     * then a job of that rarity, uniformly. Returns null if no jobs are loaded.
     *
     * @param exclude a job not to draw (the current one on a reroll); ignored if it is the only job
     */
    public static @Nullable Job random(RandomSource random, @Nullable ResourceLocation exclude) {
        Map<JobRarity, List<Job>> pool = new EnumMap<>(JobRarity.class);
        for (Map.Entry<JobRarity, List<Job>> e : INSTANCE.byRarity.entrySet()) {
            List<Job> candidates = e.getValue().stream().filter(j -> !j.id().equals(exclude)).toList();
            if (!candidates.isEmpty()) pool.put(e.getKey(), candidates);
        }
        if (pool.isEmpty()) {
            return exclude == null ? null : get(exclude);
        }

        int total = pool.keySet().stream().mapToInt(JobRarity::weight).sum();
        int roll = random.nextInt(total);
        for (Map.Entry<JobRarity, List<Job>> e : pool.entrySet()) {
            roll -= e.getKey().weight();
            if (roll < 0) {
                List<Job> list = e.getValue();
                return list.get(random.nextInt(list.size()));
            }
        }
        throw new IllegalStateException("unreachable: weights summed to " + total);
    }
}
