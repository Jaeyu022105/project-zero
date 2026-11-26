package com.projectzero.skills;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.projectzero.ProjectZero;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Operator skill commands merged into {@code /pz}:
 *   /pz skill learn <skill>
 *   /pz skill forget <skill>
 *   /pz skill list
 *   /pz skill slot <1..4> <skill|clear>
 *   /pz skill mana <amount>
 *   /pz skill book <skill>
 */
public final class SkillCommands {
    private SkillCommands() {}

    private static final DynamicCommandExceptionType UNKNOWN_SKILL =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown skill: " + id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("skill")
                        .then(Commands.literal("learn")
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                SkillRegistry.all().stream().map(Skill::id), builder))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Skill skill = find(ResourceLocationArgument.getId(ctx, "skill"));
                                            if (!SkillService.learn(player, skill, true)) {
                                                ctx.getSource().sendFailure(Component.literal("Already learned: " + skill.id()));
                                                return 0;
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                SkillService.get(ctx.getSource().getPlayerOrException()).learned(), builder))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ResourceLocation id = resolveId(ResourceLocationArgument.getId(ctx, "skill"));
                                            if (!SkillService.forget(player, id)) {
                                                ctx.getSource().sendFailure(Component.literal("Skill not learned: " + id));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Forgot skill: " + id), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerSkills data = SkillService.get(player);
                                    if (data.learned().isEmpty()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("No skills learned."), false);
                                        return 1;
                                    }
                                    MutableComponent msg = Component.literal("Learned skills (" + data.learned().size() + "): ");
                                    boolean first = true;
                                    for (ResourceLocation id : data.learned()) {
                                        if (!first) msg.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                                        first = false;
                                        msg.append(Skill.displayName(id).copy().withStyle(ChatFormatting.AQUA));
                                    }
                                    ctx.getSource().sendSuccess(() -> msg, false);
                                    return 1;
                                }))
                        .then(Commands.literal("slot")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, SkillRules.ACTIVE_SLOTS))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                                    SkillService.assignSlot(player, slot, null);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared skill slot " + (slot + 1) + "."), false);
                                                    return 1;
                                                }))
                                        .then(Commands.argument("skill", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        SkillService.get(ctx.getSource().getPlayerOrException()).learned(), builder))
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    int slot = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                                    Skill skill = find(ResourceLocationArgument.getId(ctx, "skill"));
                                                    if (!SkillService.assignSlot(player, slot, skill.id())) {
                                                        ctx.getSource().sendFailure(Component.literal(
                                                                "Skill must be learned and active to slot: " + skill.id()));
                                                        return 0;
                                                    }
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Bound ")
                                                            .append(skill.displayName())
                                                            .append(Component.literal(" to slot " + (slot + 1) + ".")), false);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("mana")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            SkillService.setMana(player, amount);
                                            int current = SkillService.get(player).currentMana();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Mana set to " + current
                                                    + " / " + SkillService.maxMana(player) + "."), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("book")
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                SkillRegistry.all().stream().map(Skill::id), builder))
                                        .executes(ctx -> giveBook(ctx.getSource(),
                                                find(ResourceLocationArgument.getId(ctx, "skill"))))))));
    }

    private static int giveBook(CommandSourceStack source, Skill skill) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack book = SkillbookItem.createFor(skill.id());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        source.sendSuccess(() -> Component.literal("Gave Skillbook: ").append(skill.displayName()), false);
        return 1;
    }

    private static ResourceLocation resolveId(ResourceLocation typed) {
        if (SkillRegistry.get(typed) == null && typed.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            ResourceLocation namespaced = ProjectZero.id(typed.getPath());
            if (SkillRegistry.get(namespaced) != null) return namespaced;
        }
        return typed;
    }

    private static Skill find(ResourceLocation typed) throws CommandSyntaxException {
        Skill s = SkillRegistry.get(resolveId(typed));
        if (s == null) throw UNKNOWN_SKILL.create(typed);
        return s;
    }
}
