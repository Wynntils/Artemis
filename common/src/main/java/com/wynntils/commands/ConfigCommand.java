/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.google.common.base.CaseFormat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.core.commands.Command;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.OverlayGroupHolder;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.overlays.DynamicOverlay;
import com.wynntils.core.features.overlays.Overlay;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;

public class ConfigCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> FEATURE_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Managers.Feature.getFeatures().stream().map(Feature::getShortName), builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);

                        Optional<Feature> foundFeature = Managers.Feature.getFeatureFromString(featureName);

                        return foundFeature
                                .map(feature -> feature.getOverlays().stream()
                                        .map(Overlay::getConfigJsonName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> FEATURE_CONFIG_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);

                        Optional<Feature> foundFeature = Managers.Feature.getFeatureFromString(featureName);

                        return foundFeature
                                .map(feature -> feature.getVisibleConfigOptions().stream()
                                        .map(ConfigHolder::getFieldName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_GROUP_FEATURE_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Managers.Feature.getFeatures().stream()
                            .filter(feature -> !feature.getOverlayGroups().isEmpty())
                            .map(Feature::getShortName),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_CONFIG_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);
                        String overlayName = context.getArgument("overlay", String.class);

                        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

                        if (featureOptional.isEmpty()) return Collections.emptyIterator();

                        Feature feature = featureOptional.get();
                        Optional<Overlay> overlayOptional = feature.getOverlays().stream()
                                .filter(overlay -> overlay.getConfigJsonName().equals(overlayName))
                                .findFirst();

                        return overlayOptional
                                .map(overlay -> overlay.getVisibleConfigOptions().stream()
                                        .map(ConfigHolder::getFieldName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_GROUP_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);

                        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

                        return featureOptional
                                .map(feature -> feature.getOverlayGroups().stream()
                                        .map(OverlayGroupHolder::getFieldName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_GROUP_REMOVE_ID_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);
                        String groupName = context.getArgument("group", String.class);

                        OverlayGroupHolder overlayGroupHolder =
                                getOverlayGroupHolderFromArguments(context, featureName, groupName);

                        if (overlayGroupHolder == null) return Collections.emptyIterator();

                        return overlayGroupHolder.getOverlays().stream()
                                .map(overlay -> ((DynamicOverlay) overlay).getId())
                                .map(String::valueOf)
                                .iterator();
                    },
                    builder);

    @Override
    public String getCommandName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "Read and manipulate Wynntils settings";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder() {
        return Commands.literal(getCommandName())
                .then(this.buildGetConfigNode())
                .then(this.buildSetConfigNode())
                .then(this.buildResetConfigNode())
                .then(this.buildReloadConfigNode())
                .then(this.buildOverlayGroupNode())
                .executes(this::syntaxError);
    }

    private LiteralCommandNode<CommandSourceStack> buildGetConfigNode() {
        LiteralArgumentBuilder<CommandSourceStack> getConfigArgBuilder = Commands.literal("get");

        // Feature specified, config option is not, print all configs
        // If a feature and config field is specified, print field specific info
        // /wynntils config get <feature>
        // /wynntils config get <feature> <field>
        // /wynntils config get <feature> overlay <overlay>
        // /wynntils config get <feature> overlay <overlay> <field>
        getConfigArgBuilder.then(Commands.argument("feature", StringArgumentType.word())
                .suggests(FEATURE_SUGGESTION_PROVIDER)
                .then(Commands.literal("overlay")
                        .then(Commands.argument("overlay", StringArgumentType.word())
                                .suggests(OVERLAY_SUGGESTION_PROVIDER)
                                .then(Commands.argument("config", StringArgumentType.word())
                                        .suggests(OVERLAY_CONFIG_SUGGESTION_PROVIDER)
                                        .executes(this::getSpecificOverlayConfigOption))
                                .executes(this::listAllOverlayConfigs)))
                .then(Commands.argument("config", StringArgumentType.word())
                        .suggests(FEATURE_CONFIG_SUGGESTION_PROVIDER)
                        .executes(this::getSpecificConfigOption))
                .executes(this::listAllConfigOptions));

        return getConfigArgBuilder.build();
    }

    private LiteralCommandNode<CommandSourceStack> buildSetConfigNode() {
        LiteralArgumentBuilder<CommandSourceStack> setConfigArgBuilder = Commands.literal("set");

        // /wynntils config set <feature> <field> <newValue>
        // /wynntils config set <feature> overlay <overlay> <field> <newValue>
        setConfigArgBuilder.then(Commands.argument("feature", StringArgumentType.word())
                .suggests(FEATURE_SUGGESTION_PROVIDER)
                .then(Commands.literal("overlay")
                        .then(Commands.argument("overlay", StringArgumentType.word())
                                .suggests(OVERLAY_SUGGESTION_PROVIDER)
                                .then(Commands.argument("config", StringArgumentType.word())
                                        .suggests(OVERLAY_CONFIG_SUGGESTION_PROVIDER)
                                        .then(Commands.argument("newValue", StringArgumentType.greedyString())
                                                .executes(this::changeOverlayConfig)))))
                .then(Commands.argument("config", StringArgumentType.word())
                        .suggests(FEATURE_CONFIG_SUGGESTION_PROVIDER)
                        .then(Commands.argument("newValue", StringArgumentType.greedyString())
                                .executes(this::changeFeatureConfig))));

        return setConfigArgBuilder.build();
    }

    private LiteralCommandNode<CommandSourceStack> buildResetConfigNode() {
        LiteralArgumentBuilder<CommandSourceStack> resetConfigArgBuilder = Commands.literal("reset");

        // Feature specified, config option is not, reset all configs
        // If a feature and config field is specified, reset specific field
        // /wynntils config reset <feature>
        // /wynntils config reset <feature> <field>
        // /wynntils config reset <feature> overlay <overlay>
        // /wynntils config reset <feature> overlay <overlay> <field>
        resetConfigArgBuilder.then(Commands.argument("feature", StringArgumentType.word())
                .suggests(FEATURE_SUGGESTION_PROVIDER)
                .then(Commands.literal("overlay")
                        .then(Commands.argument("overlay", StringArgumentType.word())
                                .suggests(OVERLAY_SUGGESTION_PROVIDER)
                                .then(Commands.argument("config", StringArgumentType.word())
                                        .suggests(OVERLAY_CONFIG_SUGGESTION_PROVIDER)
                                        .executes(this::resetOverlayConfigOption))
                                .executes(this::resetAllOverlayConfigOptions)))
                .then(Commands.argument("config", StringArgumentType.word())
                        .suggests(FEATURE_CONFIG_SUGGESTION_PROVIDER)
                        .executes(this::resetFeatureConfigOption))
                .executes(this::resetAllConfigOptions));

        return resetConfigArgBuilder.build();
    }

    private LiteralCommandNode<CommandSourceStack> buildReloadConfigNode() {
        LiteralArgumentBuilder<CommandSourceStack> reloadConfigArgBuilder = Commands.literal("reload");

        // Reload config holder values from config file and then save to "merge".
        // /wynntils config reload
        reloadConfigArgBuilder.executes(this::reloadAllConfigOptions);

        return reloadConfigArgBuilder.build();
    }

    private LiteralCommandNode<CommandSourceStack> buildOverlayGroupNode() {
        LiteralArgumentBuilder<CommandSourceStack> overlayGroupArgBuilder = Commands.literal("overlaygroup");

        overlayGroupArgBuilder.then(Commands.argument("feature", StringArgumentType.word())
                .suggests(OVERLAY_GROUP_FEATURE_SUGGESTION_PROVIDER)
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests(OVERLAY_GROUP_SUGGESTION_PROVIDER)
                        .then(Commands.literal("add").executes(this::addOverlayGroup))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(OVERLAY_GROUP_REMOVE_ID_SUGGESTION_PROVIDER)
                                        .executes(this::removeOverlayGroup)))));

        return overlayGroupArgBuilder.build();
    }

    private int addOverlayGroup(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String groupName = context.getArgument("group", String.class);

        OverlayGroupHolder overlayGroupHolder = getOverlayGroupHolderFromArguments(context, featureName, groupName);

        if (overlayGroupHolder == null) return 0;

        int newId = overlayGroupHolder.extendGroup();

        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully added a new %s to %s with the id %d."
                                        .formatted(
                                                overlayGroupHolder
                                                        .getOverlayClass()
                                                        .getSimpleName(),
                                                overlayGroupHolder.getFieldName(),
                                                newId))
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int removeOverlayGroup(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String groupName = context.getArgument("group", String.class);
        String idName = context.getArgument("id", String.class);

        OverlayGroupHolder overlayGroupHolder = getOverlayGroupHolderFromArguments(context, featureName, groupName);

        if (overlayGroupHolder == null) return 0;

        int id = Integer.parseInt(idName);

        overlayGroupHolder.removeId(id);

        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully removed %s from %s with the id %d."
                                        .formatted(
                                                overlayGroupHolder
                                                        .getOverlayClass()
                                                        .getSimpleName(),
                                                overlayGroupHolder.getFieldName(),
                                                id))
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int reloadAllConfigOptions(CommandContext<CommandSourceStack> context) {
        Managers.Config.loadConfigFile();
        Managers.Config.loadAllConfigOptions();
        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully reloaded configs from file.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int resetOverlayConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder config = getOverlayConfigHolderFromArguments(context, featureName, overlayName, configName);

        if (config == null) {
            return 0;
        }

        config.reset();

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully reset ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(config.getDisplayName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                        false);

        return 1;
    }

    private int resetAllOverlayConfigOptions(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);

        Overlay overlay = getOverlayFromArguments(context, featureName, overlayName);
        if (overlay == null) return 0;

        overlay.getConfigOptions().forEach(ConfigHolder::reset);

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully reset ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(featureName).withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("'s config options.").withStyle(ChatFormatting.GREEN)),
                        false);
        return 1;
    }

    private int listAllOverlayConfigs(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);

        Overlay overlay = getOverlayFromArguments(context, featureName, overlayName);
        if (overlay == null) return 0;

        MutableComponent response = Component.literal(overlayName)
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal("'s config options:\n").withStyle(ChatFormatting.WHITE));

        for (ConfigHolder config : overlay.getVisibleConfigOptions()) {
            MutableComponent current = getComponentForConfigHolder(config);

            current.withStyle(style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wynntils config set " + featureName + " overlay " + overlayName + " " + config.getFieldName()
                            + " ")));

            response.append(current);
        }

        context.getSource().sendSuccess(response, false);

        return 1;
    }

    private int getSpecificOverlayConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder configHolder = getOverlayConfigHolderFromArguments(context, featureName, overlayName, configName);

        if (configHolder == null) {
            return 0;
        }

        String longParentName = Arrays.stream(CaseFormat.UPPER_CAMEL
                        .to(CaseFormat.LOWER_UNDERSCORE, overlayName)
                        .split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));

        MutableComponent response = Component.literal(longParentName + "\n").withStyle(ChatFormatting.AQUA);

        response.append(getSpecificConfigComponent(configHolder));

        context.getSource().sendSuccess(response, false);

        return 1;
    }

    private int changeOverlayConfig(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder config = getOverlayConfigHolderFromArguments(context, featureName, overlayName, configName);

        if (config == null) {
            return 0;
        }

        String newValue = context.getArgument("newValue", String.class);
        Object parsedValue = config.tryParseStringValue(newValue);

        if (parsedValue == null) {
            context.getSource()
                    .sendFailure(Component.literal("Failed to parse the inputted value to the correct type!")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        Object oldValue = config.getValue();

        if (Objects.equals(oldValue, parsedValue)) {
            context.getSource()
                    .sendFailure(Component.literal("The new value is the same as the current setting.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!config.setValue(parsedValue)) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Failed to set config field!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully set ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(config.getDisplayName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(" from ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(oldValue == null ? "null" : oldValue.toString())
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.RED))
                                .append(Component.literal(" to ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(parsedValue.toString())
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                        false);

        return 1;
    }

    private int getSpecificConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder configHolder = getConfigHolderFromArguments(context, featureName, configName);

        if (configHolder == null) {
            return 0;
        }

        String longParentName = Arrays.stream(CaseFormat.LOWER_CAMEL
                        .to(CaseFormat.LOWER_UNDERSCORE, featureName)
                        .split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));

        MutableComponent response = Component.literal(longParentName + "\n").withStyle(ChatFormatting.YELLOW);

        response.append(getSpecificConfigComponent(configHolder));

        context.getSource().sendSuccess(response, false);

        return 1;
    }

    private int listAllConfigOptions(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Feature feature = getFeatureFromArguments(context, featureName);
        if (feature == null) return 0;

        MutableComponent response = Component.literal(featureName)
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("'s config options:\n").withStyle(ChatFormatting.WHITE));

        for (ConfigHolder config : feature.getVisibleConfigOptions()) {
            MutableComponent current = getComponentForConfigHolder(config);

            current.withStyle(style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wynntils config set " + featureName + " " + config.getFieldName() + " ")));

            response.append(current);
        }

        // list overlays
        response.append("\n")
                .append(Component.literal(featureName)
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("'s overlays:\n").withStyle(ChatFormatting.WHITE)));

        for (Overlay overlay : feature.getOverlays()) {
            MutableComponent current = getComponentForOverlay(overlay);

            current.withStyle(style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wynntils config set " + featureName + " overlay " + overlay.getShortName() + " ")));

            response.append(current);
        }

        context.getSource().sendSuccess(response, false);

        return 1;
    }

    private int changeFeatureConfig(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder config = getConfigHolderFromArguments(context, featureName, configName);

        if (config == null) {
            return 0;
        }

        String newValue = context.getArgument("newValue", String.class);
        Object parsedValue = config.tryParseStringValue(newValue);

        if (parsedValue == null) {
            context.getSource()
                    .sendFailure(Component.literal("Failed to parse the inputted value to the correct type!")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        Object oldValue = config.getValue();

        if (Objects.equals(oldValue, parsedValue)) {
            context.getSource()
                    .sendFailure(Component.literal("The new value is the same as the current setting.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!config.setValue(parsedValue)) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Failed to set config field!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully set ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(config.getDisplayName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(" from ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(oldValue == null ? "null" : oldValue.toString())
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.RED))
                                .append(Component.literal(" to ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(parsedValue.toString())
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                        false);

        return 1;
    }

    private int resetFeatureConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        ConfigHolder config = getConfigHolderFromArguments(context, featureName, configName);

        if (config == null) {
            return 0;
        }

        config.reset();

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully reset ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(config.getDisplayName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                        false);
        return 1;
    }

    private int resetAllConfigOptions(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Feature feature = getFeatureFromArguments(context, featureName);
        if (feature == null) return 0;
        feature.getVisibleConfigOptions().forEach(ConfigHolder::reset);

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        Component.literal("Successfully reset ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(featureName).withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("'s config options.").withStyle(ChatFormatting.GREEN)),
                        false);
        return 1;
    }

    private static Feature getFeatureFromArguments(CommandContext<CommandSourceStack> context, String featureName) {
        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

        if (featureOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Feature not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return featureOptional.get();
    }

    private static ConfigHolder getConfigHolderFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String configName) {
        Feature feature = getFeatureFromArguments(context, featureName);

        if (feature == null) return null;

        Optional<ConfigHolder> configOptional = feature.getConfigOptionFromString(configName);

        if (configOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Config not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return configOptional.get();
    }

    private static ConfigHolder getOverlayConfigHolderFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String overlayName, String configName) {
        Overlay overlay = getOverlayFromArguments(context, featureName, overlayName);

        if (overlay == null) return null;

        Optional<ConfigHolder> configOptional = overlay.getConfigOptionFromString(configName);

        if (configOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Config not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return configOptional.get();
    }

    private static Overlay getOverlayFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String overlayName) {
        Feature feature = getFeatureFromArguments(context, featureName);

        if (feature == null) {
            context.getSource()
                    .sendFailure(Component.literal("Feature not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        Optional<Overlay> overlayOptional = feature.getOverlays().stream()
                .filter(overlay -> overlay.getConfigJsonName().equals(overlayName))
                .findFirst();

        if (overlayOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Overlay not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return overlayOptional.get();
    }

    private static OverlayGroupHolder getOverlayGroupHolderFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String overlayGroupName) {
        Feature feature = getFeatureFromArguments(context, featureName);
        if (feature == null) return null;

        Optional<OverlayGroupHolder> group = feature.getOverlayGroups().stream()
                .filter(overlayGroupHolder -> overlayGroupHolder.getFieldName().equalsIgnoreCase(overlayGroupName))
                .findFirst();

        if (group.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Overlay group not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return group.get();
    }

    private MutableComponent getComponentForConfigHolder(ConfigHolder config) {
        Object value = config.getValue();

        String configNameString = config.getDisplayName();
        String configTypeString = " (" + ((Class<?>) config.getType()).getSimpleName() + ")";
        String valueString = value == null ? "Value is null." : value.toString();

        return Component.literal("\n - ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(configNameString)
                        .withStyle(style -> style.withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Description: " + config.getDescription())
                                        .withStyle(ChatFormatting.LIGHT_PURPLE))))
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(configTypeString).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(": "))
                        .append(Component.literal(valueString)
                                .withStyle(ChatFormatting.GREEN)
                                .withStyle(style -> style.withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click here to change this setting."))))));
    }

    private MutableComponent getComponentForOverlay(Overlay overlay) {
        return Component.literal("\n - ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(overlay.getShortName()).withStyle(ChatFormatting.AQUA));
    }

    private MutableComponent getSpecificConfigComponent(ConfigHolder config) {
        Object value = config.getValue();

        String valueString = value == null ? "Value is null." : value.toString();
        String configTypeString = "(" + ((Class<?>) config.getType()).getSimpleName() + ")";

        MutableComponent response = Component.literal("");
        response.append(Component.literal("Config option: ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(config.getDisplayName()).withStyle(ChatFormatting.YELLOW))
                .append("\n"));

        response.append(Component.literal("Value: ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(configTypeString))
                        .append(Component.literal(": "))
                        .append(Component.literal(valueString).withStyle(ChatFormatting.GREEN)))
                .append("\n");
        response.append(Component.literal("Subcategory: ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(config.getMetadata().subcategory())))
                .append("\n");
        response.append(Component.literal("Description: ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(config.getDescription())))
                .append("\n");
        return response;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }
}
