/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.google.common.base.CaseFormat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.DynamicOverlay;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.OverlayGroupHolder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
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
                                .map(feature -> Managers.Overlay.getFeatureOverlays(feature).stream()
                                        .map(Overlay::getShortName)
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
                                        .map(Config::getFieldName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_GROUP_FEATURE_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Managers.Feature.getFeatures().stream()
                            .filter(feature -> !Managers.Overlay.getFeatureOverlayGroups(feature)
                                    .isEmpty())
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
                        Optional<Overlay> overlayOptional = Managers.Overlay.getFeatureOverlays(feature).stream()
                                .filter(overlay -> overlay.getShortName().equals(overlayName))
                                .findFirst();

                        return overlayOptional
                                .map(overlay -> overlay.getVisibleConfigOptions().stream()
                                        .map(Config::getFieldName)
                                        .iterator())
                                .orElse(Collections.emptyIterator());
                    },
                    builder);

    private static final SuggestionProvider<CommandSourceStack> CONFIG_VALUE_SUGGESTION_PROVIDER =
            (context, builder) -> {
                String featureName = context.getArgument("feature", String.class);
                String configName = context.getArgument("config", String.class);

                Config<?> config = getConfigFromArguments(context, featureName, configName);

                if (config == null) {
                    return SharedSuggestionProvider.suggest(Stream.of(), builder);
                }

                return SharedSuggestionProvider.suggest(config.getValidLiterals(), builder);
            };

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_CONFIG_VALUE_SUGGESTION_PROVIDER =
            (context, builder) -> {
                String featureName = context.getArgument("feature", String.class);
                String overlayName = context.getArgument("overlay", String.class);
                String configName = context.getArgument("config", String.class);

                Config<?> config = getOverlayConfigFromArguments(context, featureName, overlayName, configName);

                if (config == null) {
                    return SharedSuggestionProvider.suggest(Stream.of(), builder);
                }

                return SharedSuggestionProvider.suggest(config.getValidLiterals(), builder);
            };

    private static final SuggestionProvider<CommandSourceStack> OVERLAY_GROUP_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    () -> {
                        String featureName = context.getArgument("feature", String.class);

                        Optional<Feature> featureOptional = Managers.Feature.getFeatureFromString(featureName);

                        return featureOptional
                                .map(feature -> Managers.Overlay.getFeatureOverlayGroups(feature).stream()
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
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(this.buildGetConfigNode())
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
                                                .suggests(OVERLAY_CONFIG_VALUE_SUGGESTION_PROVIDER)
                                                .executes(this::changeOverlayConfig)))))
                .then(Commands.argument("config", StringArgumentType.word())
                        .suggests(FEATURE_CONFIG_SUGGESTION_PROVIDER)
                        .then(Commands.argument("newValue", StringArgumentType.greedyString())
                                .suggests(CONFIG_VALUE_SUGGESTION_PROVIDER)
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

        int newId = Managers.Overlay.extendOverlayGroup(overlayGroupHolder);

        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();
        Managers.Config.reloadConfiguration();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully added a new %s to %s with the id %d."
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

        Managers.Overlay.removeIdFromOverlayGroup(overlayGroupHolder, id);

        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully removed %s from %s with the id %d."
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
        Managers.Config.reloadConfiguration();
        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reloaded configs from file.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int resetOverlayConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getOverlayConfigFromArguments(context, featureName, overlayName, configName);

        if (config == null) {
            return 0;
        }

        config.reset();

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reset ")
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

        overlay.getConfigOptions().forEach(Config::reset);

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reset ")
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

        for (Config<?> config : overlay.getVisibleConfigOptions()) {
            MutableComponent current = getComponentForConfig(config);

            current.withStyle(style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wynntils config set " + featureName + " overlay " + overlayName + " " + config.getFieldName()
                            + " ")));

            response.append(current);
        }

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int getSpecificOverlayConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getOverlayConfigFromArguments(context, featureName, overlayName, configName);

        if (config == null) {
            return 0;
        }

        String longParentName = Arrays.stream(CaseFormat.UPPER_CAMEL
                        .to(CaseFormat.LOWER_UNDERSCORE, overlayName)
                        .split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));

        MutableComponent response = Component.literal(longParentName + "\n").withStyle(ChatFormatting.AQUA);

        response.append(getSpecificConfigComponent(config));

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int changeOverlayConfig(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String overlayName = context.getArgument("overlay", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getOverlayConfigFromArguments(context, featureName, overlayName, configName);

        return changeConfig(context, config);
    }

    private int getSpecificConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getConfigFromArguments(context, featureName, configName);

        if (config == null) {
            return 0;
        }

        String longParentName = Arrays.stream(CaseFormat.LOWER_CAMEL
                        .to(CaseFormat.LOWER_UNDERSCORE, featureName)
                        .split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));

        MutableComponent response = Component.literal(longParentName + "\n").withStyle(ChatFormatting.YELLOW);

        response.append(getSpecificConfigComponent(config));

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int listAllConfigOptions(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);

        Feature feature = getFeatureFromArguments(context, featureName);
        if (feature == null) return 0;

        MutableComponent response = Component.literal(featureName)
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("'s config options:\n").withStyle(ChatFormatting.WHITE));

        for (Config<?> config : feature.getVisibleConfigOptions()) {
            MutableComponent current = getComponentForConfig(config);

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

        for (Overlay overlay : Managers.Overlay.getFeatureOverlays(feature)) {
            MutableComponent current = getComponentForOverlay(overlay);

            current.withStyle(style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/wynntils config set " + featureName + " overlay " + overlay.getShortName() + " ")));

            response.append(current);
        }

        context.getSource().sendSuccess(() -> response, false);

        return 1;
    }

    private int changeFeatureConfig(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getConfigFromArguments(context, featureName, configName);

        return changeConfig(context, config);
    }

    private static <T> int changeConfig(CommandContext<CommandSourceStack> context, Config<T> config) {
        if (config == null) {
            return 0;
        }

        String newValue = context.getArgument("newValue", String.class);
        T parsedValue = config.tryParseStringValue(newValue);

        if (parsedValue == null) {
            context.getSource()
                    .sendFailure(Component.literal("Failed to parse the inputted value to the correct type!")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        T oldValue = config.get();
        String oldValueString = config.getValueString();

        if (Objects.equals(oldValue, parsedValue)) {
            context.getSource()
                    .sendFailure(Component.literal("The new value is the same as the current setting.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        config.setValue(parsedValue);
        String newValueString = config.getValueString();

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully set ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(config.getDisplayName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(" from ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(oldValueString)
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.RED))
                                .append(Component.literal(" to ").withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(newValueString)
                                        .withStyle(ChatFormatting.BOLD)
                                        .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)),
                        false);

        return 1;
    }

    private int resetFeatureConfigOption(CommandContext<CommandSourceStack> context) {
        String featureName = context.getArgument("feature", String.class);
        String configName = context.getArgument("config", String.class);

        Config<?> config = getConfigFromArguments(context, featureName, configName);

        if (config == null) {
            return 0;
        }

        config.reset();

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reset ")
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
        feature.getVisibleConfigOptions().forEach(Config::reset);

        Managers.Config.saveConfig();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reset ")
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

    private static Config<?> getConfigFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String configName) {
        Feature feature = getFeatureFromArguments(context, featureName);

        if (feature == null) return null;

        Optional<Config<?>> configOptional = feature.getConfigOptionFromString(configName);

        if (configOptional.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Config not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return configOptional.get();
    }

    private static Config<?> getOverlayConfigFromArguments(
            CommandContext<CommandSourceStack> context, String featureName, String overlayName, String configName) {
        Overlay overlay = getOverlayFromArguments(context, featureName, overlayName);

        if (overlay == null) return null;

        Optional<Config<?>> configOptional = overlay.getConfigOptionFromString(configName);

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

        Optional<Overlay> overlayOptional = Managers.Overlay.getFeatureOverlays(feature).stream()
                .filter(overlay -> overlay.getShortName().equals(overlayName))
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

        Optional<OverlayGroupHolder> group = Managers.Overlay.getFeatureOverlayGroups(feature).stream()
                .filter(overlayGroupHolder -> overlayGroupHolder.getFieldName().equalsIgnoreCase(overlayGroupName))
                .findFirst();

        if (group.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("Overlay group not found!").withStyle(ChatFormatting.RED));
            return null;
        }

        return group.get();
    }

    private MutableComponent getComponentForConfig(Config<?> config) {
        String configNameString = config.getDisplayName();
        String configTypeString = " (" + ((Class<?>) config.getType()).getSimpleName() + ")";
        String valueString = config.getValueString();

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

    private MutableComponent getSpecificConfigComponent(Config<?> config) {
        String valueString = config.getValueString();
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
