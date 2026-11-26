package com.projectzero.skills;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.projectzero.ProjectZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Datapack reload listener for {@code data/<namespace>/skills/*.json}.
 */
public final class SkillRegistry extends SimpleJsonResourceReloadListener {
    // GSON must precede INSTANCE (static initialization order).
    private static final Gson GSON = new Gson();

    public static final SkillRegistry INSTANCE = new SkillRegistry();

    private Map<ResourceLocation, Skill> skills = Map.of();

    private SkillRegistry() {
        super(GSON, "skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Skill> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation id = e.getKey();
            try {
                loaded.put(id, parse(id, GsonHelper.convertToJsonObject(e.getValue(), "skill")));
            } catch (RuntimeException ex) {
                ProjectZero.LOGGER.error("Skipping skill {}: {}", id, ex.getMessage());
            }
        }
        skills = loaded;
        ProjectZero.LOGGER.info("Loaded {} skills", loaded.size());
    }

    private static Skill parse(ResourceLocation id, JsonObject json) {
        if (json.has("id")) {
            ResourceLocation declared = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (!id.equals(declared)) {
                ProjectZero.LOGGER.warn("Skill file {} declares id {}; using file name", id, declared);
            }
        }
        String typeStr = GsonHelper.getAsString(json, "type");
        SkillType type = SkillType.byName(typeStr);
        if (type == null) throw new JsonParseException("unknown skill type '" + typeStr + "'");

        String sourceStr = GsonHelper.getAsString(json, "source");
        SkillSource source = SkillSource.byName(sourceStr);
        if (source == null) throw new JsonParseException("unknown skill source '" + sourceStr + "'");

        Optional<ResourceLocation> job = Optional.empty();
        if (json.has("job")) {
            ResourceLocation jobId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "job"));
            if (jobId == null) throw new JsonParseException("bad job id");
            job = Optional.of(jobId);
        }

        int manaCost = Math.max(0, GsonHelper.getAsInt(json, "mana_cost", 0));
        int cooldownTicks = Math.max(0, GsonHelper.getAsInt(json, "cooldown_ticks", 0));
        int unlockLevel = Math.max(1, GsonHelper.getAsInt(json, "unlock_level", 1));

        String behaviorStr = GsonHelper.getAsString(json, "behavior");
        ResourceLocation behavior = ResourceLocation.tryParse(behaviorStr);
        if (behavior == null) throw new JsonParseException("bad behavior id '" + behaviorStr + "'");

        JsonObject params = json.has("params")
                ? GsonHelper.getAsJsonObject(json, "params")
                : new JsonObject();

        return new Skill(id, type, source, job, manaCost, cooldownTicks, unlockLevel, behavior, params);
    }

    public static @Nullable Skill get(ResourceLocation id) {
        return INSTANCE.skills.get(id);
    }

    public static Collection<Skill> all() {
        return INSTANCE.skills.values();
    }

    public static List<Skill> basicSkillsForLevel(int level) {
        return INSTANCE.skills.values().stream()
                .filter(s -> s.source() == SkillSource.BASIC && s.unlockLevel() <= level)
                .toList();
    }

    public static List<Skill> jobSkillsFor(ResourceLocation jobId, int jobLevel) {
        return INSTANCE.skills.values().stream()
                .filter(s -> s.source() == SkillSource.JOB
                        && s.job().filter(jobId::equals).isPresent()
                        && s.unlockLevel() <= jobLevel)
                .toList();
    }
}
