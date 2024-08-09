/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.models.activities.quests.QuestInfo;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

public class QuestCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Models.Quest.getQuests(ActivitySortOrder.ALPHABETIC).stream()
                            .map(QuestInfo::name),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> SORT_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(ActivitySortOrder.values())
                            .map(order -> order.name().toLowerCase(Locale.ROOT)),
                    builder);

    @Override
    public String getCommandName() {
        return "quest";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("list")
                        .executes((ctxt) -> listQuests(ctxt, "distance"))
                        .then(Commands.argument("sort", StringArgumentType.word())
                                .suggests(SORT_SUGGESTION_PROVIDER)
                                .executes((ctxt) -> listQuests(ctxt, ctxt.getArgument("sort", String.class)))))
                .then(Commands.literal("search")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(this::searchQuests)))
                .then(Commands.literal("info")
                        .then(Commands.argument("quest", StringArgumentType.greedyString())
                                .suggests(QUEST_SUGGESTION_PROVIDER)
                                .executes(this::questInfo)))
                .then(Commands.literal("track")
                        .then(Commands.argument("quest", StringArgumentType.greedyString())
                                .suggests(QUEST_SUGGESTION_PROVIDER)
                                .executes(this::trackQuest)))
                .then(Commands.literal("untrack").executes(this::untrackQuest))
                .then(Commands.literal("wiki")
                        .then(Commands.argument("quest", StringArgumentType.greedyString())
                                .suggests(QUEST_SUGGESTION_PROVIDER)
                                .executes(this::lookupOnWiki)))
                .executes(this::syntaxError);
    }

    private int listQuests(CommandContext<CommandSourceStack> context, String sort) {
        ActivitySortOrder order = ActivitySortOrder.fromString(sort);

        Models.Quest.rescanQuestBook(true, false);

        if (Models.Quest.getQuestsRaw().isEmpty()) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.literal("Quest Book was not scanned. You might have to retry this command.")
                                    .withStyle(ChatFormatting.YELLOW),
                            false);
        }

        List<QuestInfo> quests = Models.Quest.getQuests(order).stream()
                .filter(QuestInfo::trackable)
                .toList();

        if (quests.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("No active quests found!").withStyle(ChatFormatting.RED));
            return 1;
        }

        MutableComponent response = Component.literal("Active quests:").withStyle(ChatFormatting.AQUA);
        generateQuestList(quests, response);

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int searchQuests(CommandContext<CommandSourceStack> context) {
        String searchText = context.getArgument("text", String.class);

        Models.Quest.rescanQuestBook(true, false);

        if (Models.Quest.getQuestsRaw().isEmpty()) {
            context.getSource()
                    .sendSuccess(
                            () -> Component.literal("Quest Book was not scanned. You might have to retry this command.")
                                    .withStyle(ChatFormatting.YELLOW),
                            false);
        }

        List<QuestInfo> quests;
        MutableComponent response;

        quests = Models.Quest.getQuestsRaw().stream()
                .filter(quest -> StringUtils.initialMatch(quest.name(), searchText)
                        || StringUtils.initialMatch(quest.nextTask().getStringWithoutFormatting(), searchText))
                .toList();

        if (quests.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("No matching quests found!").withStyle(ChatFormatting.RED));
            return 1;
        }

        response = Component.literal("Matching quests:").withStyle(ChatFormatting.AQUA);

        generateQuestList(quests, response);

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private void generateQuestList(List<QuestInfo> quests, MutableComponent response) {
        Vec3 playerLocation = McUtils.player().position();

        for (QuestInfo quest : quests) {
            double distance = quest.nextLocation().isEmpty()
                    ? 0
                    : playerLocation.distanceTo(quest.nextLocation().get().toVec3());

            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(quest.name())
                            .withStyle(style -> style.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quest info " + quest.name())))
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click for info"))))
                            .withStyle(ChatFormatting.WHITE));
            if (distance > 0) {
                response.append(Component.literal(" (" + (int) Math.round(distance) + " m)")
                        .withStyle(ChatFormatting.DARK_GREEN));
            }

            if (quest.equals(Models.Activity.getTrackedQuestInfo())) {
                response.append(Component.literal(" [Tracked]")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .withStyle(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quest untrack")))
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, Component.literal("Click to stop tracking")))));
            }
        }
    }

    private int questInfo(CommandContext<CommandSourceStack> context) {
        String questName = context.getArgument("quest", String.class);
        QuestInfo quest = getQuestInfo(context, questName);
        if (quest == null) return 0;

        MutableComponent response =
                Component.literal("Info for quest: " + quest.name()).withStyle(ChatFormatting.AQUA);
        response.append(Component.literal("\n - Status: ").withStyle(ChatFormatting.WHITE))
                .append(quest.status().getQuestStateComponent())
                .append(Component.literal("\n - Level: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(Integer.toString(quest.level())).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\n - Length: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(StringUtils.capitalized(quest.length().toString()))
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\n - Next task: ").withStyle(ChatFormatting.WHITE))
                .append(quest.nextTask().getComponent().withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.literal("[Track quest]")
                        .withStyle(style -> style.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quest track " + quest.name()))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT, Component.literal("Click to track quest"))))
                        .withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(" "))
                .append(Component.literal("[Lookup on wiki]")
                        .withStyle(style -> style.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quest wiki " + quest.name()))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to lookup quest on wiki"))))
                        .withStyle(ChatFormatting.DARK_AQUA));

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int trackQuest(CommandContext<CommandSourceStack> context) {
        String questName = context.getArgument("quest", String.class);
        QuestInfo quest = getQuestInfo(context, questName);
        if (quest == null) return 0;

        Models.Quest.startTracking(quest);
        MutableComponent response =
                Component.literal("Now tracking quest " + quest.name()).withStyle(ChatFormatting.AQUA);
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int untrackQuest(CommandContext<CommandSourceStack> context) {
        QuestInfo trackedQuest = Models.Activity.getTrackedQuestInfo();
        if (trackedQuest == null) {
            context.getSource()
                    .sendFailure(Component.literal("No quest currently tracked").withStyle(ChatFormatting.RED));
            return 0;
        }

        Models.Quest.stopTracking();

        MutableComponent response = Component.literal("Stopped tracking quest " + trackedQuest.name())
                .withStyle(ChatFormatting.AQUA);
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int lookupOnWiki(CommandContext<CommandSourceStack> context) {
        String questName = context.getArgument("quest", String.class);
        QuestInfo quest = getQuestInfo(context, questName);
        if (quest == null) return 0;

        Models.Quest.openQuestOnWiki(quest);
        MutableComponent response =
                Component.literal("Quest opened on wiki " + quest.name()).withStyle(ChatFormatting.AQUA);
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }

    private QuestInfo getQuestInfo(CommandContext<CommandSourceStack> context, String questName) {
        String questNameLowerCase = questName.toLowerCase(Locale.ROOT);
        List<QuestInfo> matchingQuests = Models.Quest.getQuestsRaw().stream()
                .filter(questInfo -> questInfo.name().toLowerCase(Locale.ROOT).contains(questNameLowerCase))
                .toList();

        if (matchingQuests.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Quest '" + questName + "' not found")
                            .withStyle(ChatFormatting.RED));
            return null;
        } else if (matchingQuests.size() > 1) {
            MutableComponent error = Component.literal("Quest '" + questName + "' match several quests:")
                    .withStyle(ChatFormatting.RED);
            for (QuestInfo quest : matchingQuests) {
                error.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE));
            }
            context.getSource().sendFailure(error);
            return null;
        }

        return matchingQuests.getFirst();
    }
}
