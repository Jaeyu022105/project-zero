package com.projectzero.titles;

import com.google.gson.JsonObject;
import com.projectzero.ProjectZero;
import com.projectzero.leveling.LevelingService;
import com.projectzero.leveling.Stat;
import com.projectzero.leveling.StatEffects;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SyncTitlesPacket;
import com.projectzero.skills.Skill;
import com.projectzero.skills.SkillRegistry;
import com.projectzero.skills.SkillService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side API for unlocking, equipping, and applying Title effects (DESIGN-DOCUMENT.md §7).
 */
public final class TitleService {
    private TitleService() {}

    public static final String DATA_KEY = ProjectZero.MOD_ID + ":titles";
    private static final String MODIFIER_PREFIX = "title_attr_";
    private static final Set<ResourceLocation> APPLIED_ATTR_KEYS = new HashSet<>();

    public static PlayerTitles get(Player player) {
        return PlayerTitles.load(player.getPersistentData().getCompound(DATA_KEY));
    }

    public static @Nullable Title equippedTitle(Player player) {
        ResourceLocation id = get(player).equipped();
        return id == null ? null : TitleRegistry.get(id);
    }

    public static void set(ServerPlayer player, PlayerTitles titles) {
        player.getPersistentData().put(DATA_KEY, titles.save());
        applyEquippedEffects(player);
        sync(player, titles);
    }

    public static void sync(ServerPlayer player) {
        sync(player, get(player));
    }

    private static void sync(ServerPlayer player, PlayerTitles titles) {
        ModNetwork.sendToPlayer(player, new SyncTitlesPacket(titles));
    }

    public static boolean equip(ServerPlayer player, @Nullable ResourceLocation titleId) {
        PlayerTitles data = get(player);
        if (!data.equip(titleId)) return false;
        set(player, data);
        return true;
    }

    public static @Nullable ResourceLocation cycleEquipped(ServerPlayer player) {
        PlayerTitles data = get(player);
        ResourceLocation next = data.cycleNext();
        set(player, data);
        return next;
    }

    /** Evaluates all loaded titles against the player's milestones and unlocks newly earned ones. */
    public static void checkUnlocks(ServerPlayer player, MilestoneTracker tracker) {
        PlayerTitles data = get(player);
        boolean changed = false;

        for (Title title : TitleRegistry.all()) {
            if (data.hasUnlocked(title.id())) continue;
            if (!title.condition().isMet(tracker)) continue;

            data.unlock(title.id());
            if (data.equipped() == null) {
                data.equip(title.id());
            }
            changed = true;
            announceUnlock(player, title);

            title.grantsSkill().ifPresent(skillId -> {
                Skill skill = SkillRegistry.get(skillId);
                if (skill != null) {
                    SkillService.learn(player, skill, true);
                }
            });
        }

        if (changed) {
            set(player, data);
        }
    }

    /** Applies transient attribute modifiers from the equipped title and refreshes stat effects. */
    public static void applyEquippedEffects(ServerPlayer player) {
        for (Attribute attr : ForgeRegistries.ATTRIBUTES.getValues()) {
            Holder<Attribute> holder = ForgeRegistries.ATTRIBUTES.getHolder(attr).orElse(null);
            if (holder == null) continue;
            AttributeInstance inst = player.getAttribute(holder);
            if (inst == null) continue;
            for (ResourceLocation modId : APPLIED_ATTR_KEYS) {
                inst.removeModifier(modId);
            }
        }

        Title equipped = equippedTitle(player);
        if (equipped != null) {
            int idx = 0;
            for (Title.AttributeBonus bonus : equipped.attributeBonuses()) {
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(bonus.attribute());
                if (attr == null) continue;
                Holder<Attribute> holder = ForgeRegistries.ATTRIBUTES.getHolder(attr).orElse(null);
                if (holder == null) continue;
                AttributeInstance inst = player.getAttribute(holder);
                if (inst == null) continue;
                ResourceLocation modId = ProjectZero.id(MODIFIER_PREFIX + equipped.id().getPath() + "_" + idx++);
                APPLIED_ATTR_KEYS.add(modId);
                inst.removeModifier(modId);
                inst.addTransientModifier(new AttributeModifier(modId, bonus.amount(), bonus.operation()));
            }
        }

        StatEffects.apply(player, LevelingService.get(player));
        SkillService.sync(player);
    }

    /** Flat stat bonus granted by the player's currently equipped title. */
    public static int statBonus(Player player, Stat stat) {
        Title equipped = equippedTitle(player);
        return equipped == null ? 0 : equipped.statBonuses().getOrDefault(stat, 0);
    }

    /** Bonus damage multiplier (e.g. 0.10 for +10%) from the equipped title against {@code target}. */
    public static float damageBonusAgainst(ServerPlayer player, LivingEntity target) {
        Title equipped = equippedTitle(player);
        if (equipped == null || equipped.behavior().isEmpty()) return 0.0F;
        ResourceLocation beh = equipped.behavior().get();
        JsonObject params = equipped.params();

        if (ProjectZero.id("damage_vs_entity").equals(beh) && params.has("entity")) {
            ResourceLocation expected = ResourceLocation.tryParse(GsonHelper.getAsString(params, "entity"));
            if (expected != null && expected.equals(EntityType.getKey(target.getType()))) {
                return GsonHelper.getAsFloat(params, "bonus", 0.10F);
            }
        } else if (ProjectZero.id("damage_vs_tag").equals(beh) && params.has("tag")) {
            String raw = GsonHelper.getAsString(params, "tag");
            if (raw.startsWith("#")) raw = raw.substring(1);
            if ("forge:bosses".equals(raw) || "c:bosses".equals(raw)) {
                if (target.getType().is(Tags.EntityTypes.BOSSES)
                        || target.getPersistentData().getBoolean(ProjectZero.MOD_ID + ":gate_boss")) {
                    return GsonHelper.getAsFloat(params, "bonus", 0.05F);
                }
            }
            ResourceLocation tagId = ResourceLocation.tryParse(raw);
            if (tagId != null) {
                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
                if (target.getType().is(tag)) {
                    return GsonHelper.getAsFloat(params, "bonus", 0.10F);
                }
            }
        }
        return 0.0F;
    }

    /** Bonus XP multiplier (e.g. 0.05 for +5%) from the equipped title. */
    public static double xpBonusMultiplier(ServerPlayer player) {
        Title equipped = equippedTitle(player);
        if (equipped != null && equipped.behavior().filter(ProjectZero.id("xp_bonus")::equals).isPresent()) {
            return GsonHelper.getAsDouble(equipped.params(), "bonus", 0.05);
        }
        return 0.0;
    }

    private static void announceUnlock(ServerPlayer player, Title title) {
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("TITLE UNLOCKED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                title.displayName().copy().withStyle(ChatFormatting.YELLOW)));

        player.sendSystemMessage(Component.literal("[System] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Title acquired: ").withStyle(ChatFormatting.WHITE))
                .append(title.displayName().copy().withStyle(ChatFormatting.GOLD)));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.15F);
    }

    public static void reset(ServerPlayer player) {
        set(player, new PlayerTitles());
        MilestoneTracker.reset(player);
    }

    public static void copy(Player from, Player to) {
        CompoundTag data = from.getPersistentData().getCompound(DATA_KEY);
        to.getPersistentData().put(DATA_KEY, data.copy());
    }
}
