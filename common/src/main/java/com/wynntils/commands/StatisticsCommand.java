/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.services.statistics.type.StatisticEntry;
import com.wynntils.services.statistics.type.StatisticKind;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StatisticsCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> STATISTIC_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(StatisticKind.values()).map(StatisticKind::getId), builder);

    @Override
    public String getCommandName() {
        return "statistics";
    }

    @Override
    public List<String> getAliases() {
        return List.of("stats", "stat", "st");
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        RequiredArgumentBuilder<CommandSourceStack, String> characterStatisticsBuilder = Commands.argument(
                        "statistic", StringArgumentType.greedyString())
                .suggests(STATISTIC_SUGGESTION_PROVIDER)
                .executes(this::getStatistic);

        RequiredArgumentBuilder<CommandSourceStack, String> overallStatisticsBuilder = Commands.argument(
                        "statistic", StringArgumentType.greedyString())
                .suggests(STATISTIC_SUGGESTION_PROVIDER)
                .executes(this::getStatisticOverall);

        return base.then(Commands.literal("show")
                        .then(Commands.literal("overall").executes(this::showOverallStatistics))
                        .then(Commands.literal("o").executes(this::showOverallStatistics))
                        .executes(this::showCharacterStatistics))
                .then(Commands.literal("list").executes(this::listStatistics))
                .then(Commands.literal("get")
                        .then(Commands.literal("character").then(characterStatisticsBuilder))
                        .then(Commands.literal("c").then(characterStatisticsBuilder))
                        .then(Commands.literal("overall").then(overallStatisticsBuilder))
                        .then(Commands.literal("o").then(overallStatisticsBuilder)))
                .then(Commands.literal("reset")
                        .then(Commands.literal("confirm").executes(this::doResetStatistics))
                        .executes(this::resetStatistics))
                .executes(this::syntaxError);
    }

    private int showCharacterStatistics(CommandContext<CommandSourceStack> context) {
        MutableComponent response = Component.literal("Statistics:").withStyle(ChatFormatting.AQUA);

        for (StatisticKind statistic : Arrays.stream(StatisticKind.values())
                .sorted(Comparator.comparing(StatisticKind::getName))
                .toList()) {
            long value = Services.Statistics.getStatistic(statistic).total();

            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(statistic.getName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(statistic.getFormattedValue(value))
                            .withStyle(ChatFormatting.DARK_GREEN));
        }

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int showOverallStatistics(CommandContext<CommandSourceStack> context) {
        MutableComponent response = Component.literal("Statistics:").withStyle(ChatFormatting.AQUA);

        for (StatisticKind statistic : Arrays.stream(StatisticKind.values())
                .sorted(Comparator.comparing(StatisticKind::getName))
                .toList()) {
            long value = Services.Statistics.getOverallStatistic(statistic).total();

            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(statistic.getName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(statistic.getFormattedValue(value))
                            .withStyle(ChatFormatting.DARK_GREEN));
        }

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int listStatistics(CommandContext<CommandSourceStack> context) {
        MutableComponent response =
                Component.literal("Available kinds of statistics:").withStyle(ChatFormatting.AQUA);

        for (StatisticKind statistic : Arrays.stream(StatisticKind.values())
                .sorted(Comparator.comparing(StatisticKind::getId))
                .toList()) {
            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(statistic.getId()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + statistic.getName() + ")").withStyle(ChatFormatting.DARK_GREEN));
        }

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int getStatistic(CommandContext<CommandSourceStack> context) {
        String statisticId = context.getArgument("statistic", String.class);
        StatisticKind statistic = StatisticKind.from(statisticId);
        if (statistic == null) {
            context.getSource()
                    .sendFailure(Component.literal("No such statistic.").withStyle(ChatFormatting.RED));
            return 0;
        }

        StatisticEntry value = Services.Statistics.getStatistic(statistic);

        MutableComponent response = getStatisticComponent(statistic, value);

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int getStatisticOverall(CommandContext<CommandSourceStack> context) {
        String statisticId = context.getArgument("statistic", String.class);
        StatisticKind statistic = StatisticKind.from(statisticId);
        if (statistic == null) {
            context.getSource()
                    .sendFailure(Component.literal("No such statistic.").withStyle(ChatFormatting.RED));
            return 0;
        }

        StatisticEntry value = Services.Statistics.getOverallStatistic(statistic);

        MutableComponent response = getStatisticComponent(statistic, value);

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private static MutableComponent getStatisticComponent(StatisticKind statistic, StatisticEntry value) {
        MutableComponent response = Component.literal(statistic.getName())
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(":\nTotal: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statistic.getFormattedValue(value.total()))
                        .withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("\nCount: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statistic.getFormattedValue(value.count()))
                        .withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("\nMin: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statistic.getFormattedValue(value.min()))
                        .withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("\nMax: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statistic.getFormattedValue(value.max()))
                        .withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("\nAverage: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(statistic.getFormattedValue(value.average()))
                        .withStyle(ChatFormatting.DARK_GREEN));
        return response;
    }

    private int resetStatistics(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.statistics.warnReset")
                                .withStyle(ChatFormatting.AQUA),
                        false);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatable("command.wynntils.statistics.clickHere")
                                .withStyle(ChatFormatting.RED)
                                .withStyle(ChatFormatting.UNDERLINE)
                                .withStyle(style -> style.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/statistics reset confirm"))),
                        false);

        return 1;
    }

    private int doResetStatistics(CommandContext<CommandSourceStack> context) {
        Services.Statistics.resetStatistics();

        MutableComponent response = Component.literal("All statistics for this character has been reset")
                .withStyle(ChatFormatting.AQUA);
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }
}
