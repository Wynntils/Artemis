/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.services.map.type.CustomPoiProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class MapCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> POI_PROVIDER_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Services.Poi.getCustomPoiProviders().stream()
                            .map(CustomPoiProvider::getName)
                            .toArray(String[]::new),
                    builder);

    @Override
    public String getCommandName() {
        return "map";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return Commands.literal("map")
                .then(Commands.literal("poiProvider")
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("url", StringArgumentType.string())
                                                .executes(this::addPoiProvider))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(POI_PROVIDER_SUGGESTION_PROVIDER)
                                        .executes(this::removePoiProvider)))
                        .then(Commands.literal("list").executes(this::listPoiProviders))
                        .then(Commands.literal("reload").executes(this::reloadPoiProviders))
                        .then(Commands.literal("toggle")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(POI_PROVIDER_SUGGESTION_PROVIDER)
                                        .executes(this::togglePoiProvider))));
    }

    private int reloadPoiProviders(CommandContext<CommandSourceStack> context) {
        Services.Poi.loadCustomPoiProviders();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully reloaded POI providers.")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        return 1;
    }

    private int addPoiProvider(CommandContext<CommandSourceStack> context) {
        String name = context.getArgument("name", String.class);
        String url = context.getArgument("url", String.class);

        try {
            Services.Poi.addCustomPoiProvider(new CustomPoiProvider(name, new URI(url)));
        } catch (URISyntaxException e) {
            context.getSource()
                    .sendFailure(
                            Component.literal("The provided URL is invalid.").withStyle(ChatFormatting.RED));
            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully added POI provider.")
                                .withStyle(ChatFormatting.GREEN),
                        false);
        return 1;
    }

    private int removePoiProvider(CommandContext<CommandSourceStack> context) {
        String name = context.getArgument("name", String.class);

        if (!Services.Poi.removeCustomPoiProvider(name)) {
            context.getSource()
                    .sendFailure(Component.literal("The provided name does not match any POI provider.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully removed POI provider.")
                                .withStyle(ChatFormatting.GREEN),
                        false);
        return 1;
    }

    private int listPoiProviders(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.literal("POI providers: ").withStyle(ChatFormatting.YELLOW);

        for (CustomPoiProvider poiProvider : Services.Poi.getCustomPoiProviders()) {
            message.append(Component.literal("\n"));
            message.append(Component.literal(poiProvider.getName()).withStyle(ChatFormatting.GOLD));
            message.append(Component.literal(poiProvider.isEnabled() ? " (enabled)" : " (disabled)")
                    .withStyle(poiProvider.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED));
            message.append(Component.empty());
            message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(poiProvider.getUrl().toString()).withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        }

        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private int togglePoiProvider(CommandContext<CommandSourceStack> context) {
        String name = context.getArgument("name", String.class);

        Optional<CustomPoiProvider> poiProvider = Services.Poi.getCustomPoiProviders().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();

        if (poiProvider.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.literal("The provided name does not match any POI provider.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        poiProvider.get().setEnabled(!poiProvider.get().isEnabled());

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("Successfully toggled POI provider ")
                                .append(Component.literal(name).withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" to "))
                                .append(Component.literal(poiProvider.get().isEnabled() ? "enabled" : "disabled")
                                        .withStyle(ChatFormatting.UNDERLINE)),
                        false);

        return 1;
    }
}
