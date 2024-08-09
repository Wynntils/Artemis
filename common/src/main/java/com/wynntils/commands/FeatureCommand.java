/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class FeatureCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> FEATURE_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Managers.Feature.getFeatures().stream().map(Feature::getShortName), builder);

    @Override
    public String getCommandName() {
        return "feature";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("list").executes(this::listFeatures))
                .then(Commands.literal("enable")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTION_PROVIDER)
                                .executes(this::enableFeature)))
                .then(Commands.literal("disable")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTION_PROVIDER)
                                .executes(this::disableFeature)))
                .then(Commands.literal("reload")
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTION_PROVIDER)
                                .executes(this::reloadFeature)))
                .executes(this::syntaxError);
    }

    private int listFeatures(CommandContext<CommandSourceStack> context) {
        List<Feature> features = Managers.Feature.getFeatures().stream()
                .sorted(Feature::compareTo)
                .toList();

        MutableComponent response =
                Component.literal("Currently registered features:").withStyle(ChatFormatting.AQUA);

        Category lastCategory = null;

        for (Feature feature : features) {
            String translatedName = feature.getTranslatedName();
            ChatFormatting color = feature.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED;

            if (lastCategory != feature.getCategory()) {
                lastCategory = feature.getCategory();
                response.append(Component.literal("\n" + lastCategory.toString() + ":")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .withStyle(ChatFormatting.BOLD));
            }

            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(translatedName).withStyle(color));
        }

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int enableFeature(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

        if (featureOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature not found!").withStyle(ChatFormatting.RED));
            return 0;
        }
        Feature feature = featureOptional.get();

        if (feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " is already enabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        feature.setUserEnabled(true);

        if (!feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " could not be enabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(feature.getTranslatedName() + " was enabled successfully.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int disableFeature(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

        if (featureOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature not found!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Feature feature = featureOptional.get();

        if (!feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " is already disabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        feature.setUserEnabled(false);

        if (feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " could not be disabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(feature.getTranslatedName() + " was disabled successfully.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int reloadFeature(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

        if (featureOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature not found!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Feature feature = featureOptional.get();

        if (!feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName()
                                    + " is already disabled, cannot reload a disabled feature!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        Managers.Feature.disableFeature(feature);

        if (feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " could not be disabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        Managers.Feature.enableFeature(feature);

        if (!feature.isEnabled()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature " + feature.getTranslatedName() + " could not be enabled!")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal(feature.getTranslatedName() + " was reloaded successfully.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }
}
