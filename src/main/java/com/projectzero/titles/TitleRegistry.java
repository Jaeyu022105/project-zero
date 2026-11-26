package com.projectzero.titles;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.projectzero.ProjectZero;
import com.projectzero.leveling.Stat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reload listener for {@code data/<namespace>/titles/*.json}.
 */
public final class TitleRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final TitleRegistry INSTANCE = new TitleRegistry();

    private Map<ResourceLocation, Title> titles = Map.of();

    private TitleRegistry() {
        super(GSON, "titles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Title> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation id = e.getKey();
            try {
                loaded.put(id, parse(id, GsonHelper.convertToJsonObject(e.getValue(), "title")));
            } catch (RuntimeException ex) {
                ProjectZero.LOGGER.error("Skipping title {}: {}", id, ex.getMessage());
            }
        }
        titles = loaded;
        ProjectZero.LOGGER.info("Loaded {} titles", loaded.size());
    }

    private static Title parse(ResourceLocation id, JsonObject json) {
        JsonObject condObj = GsonHelper.getAsJsonObject(json, "condition");
        String condType = GsonHelper.getAsString(condObj, "type");
        int count = GsonHelper.getAsInt(condObj, "count", 1);
        Optional<ResourceLocation> target = Optional.empty();
        if (condObj.has("entity")) {
            target = Optional.of(parseId(GsonHelper.getAsString(condObj, "entity"), "entity"));
        } else if (condObj.has("tag")) {
            String raw = GsonHelper.getAsString(condObj, "tag");
            if (raw.startsWith("#")) raw = raw.substring(1);
            target = Optional.of(parseId(raw, "tag"));
        }
        Title.Condition condition = new Title.Condition(condType, target, count);

        Map<Stat, Integer> statBonuses = new EnumMap<>(Stat.class);
        List<Title.AttributeBonus> attrBonuses = new ArrayList<>();
        Optional<ResourceLocation> behavior = Optional.empty();
        JsonObject params = new JsonObject();

        if (json.has("effect")) {
            JsonObject eff = GsonHelper.getAsJsonObject(json, "effect");
            if (eff.has("stats")) {
                JsonObject st = GsonHelper.getAsJsonObject(eff, "stats");
                for (Stat stat : Stat.values()) {
                    if (st.has(stat.key())) {
                        statBonuses.put(stat, GsonHelper.getAsInt(st, stat.key()));
                    }
                }
            }
            if (eff.has("attributes")) {
                JsonArray arr = GsonHelper.getAsJsonArray(eff, "attributes");
                for (JsonElement el : arr) {
                    JsonObject ao = el.getAsJsonObject();
                    ResourceLocation attrId = parseId(GsonHelper.getAsString(ao, "attribute"), "attribute");
                    double amount = GsonHelper.getAsDouble(ao, "amount");
                    String opStr = GsonHelper.getAsString(ao, "operation", "add_value").toUpperCase(Locale.ROOT);
                    AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);
                    attrBonuses.add(new Title.AttributeBonus(attrId, amount, op));
                }
            }
            if (eff.has("behavior")) {
                behavior = Optional.of(parseId(GsonHelper.getAsString(eff, "behavior"), "behavior"));
            }
            if (eff.has("params")) {
                params = GsonHelper.getAsJsonObject(eff, "params");
            }
        }

        Optional<ResourceLocation> grantsSkill = json.has("grants_skill")
                ? Optional.of(parseId(GsonHelper.getAsString(json, "grants_skill"), "grants_skill"))
                : Optional.empty();

        return new Title(id, condition, statBonuses, attrBonuses, behavior, params, grantsSkill);
    }

    private static ResourceLocation parseId(String raw, String field) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) throw new JsonParseException("bad id in '" + field + "': " + raw);
        return id;
    }

    public static @Nullable Title get(ResourceLocation id) {
        return INSTANCE.titles.get(id);
    }

    public static Collection<Title> all() {
        return INSTANCE.titles.values();
    }
}
