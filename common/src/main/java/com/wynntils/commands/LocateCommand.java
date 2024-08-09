/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.type.ServiceKind;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

public class LocateCommand extends Command {
    public static final SuggestionProvider<CommandSourceStack> SERVICE_SUGGESTION_PROVIDER = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ServiceKind.values()).map(ServiceKind::getName), builder);

    public static final SuggestionProvider<CommandSourceStack> PLACES_SUGGESTION_PROVIDER = (context, builder) ->
            SharedSuggestionProvider.suggest(Services.Poi.getLabelPois().map(Poi::getName), builder);

    @Override
    public String getCommandName() {
        return "locate";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("service")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(LocateCommand.SERVICE_SUGGESTION_PROVIDER)
                                .executes(this::locateService)))
                .then(Commands.literal("place")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(LocateCommand.PLACES_SUGGESTION_PROVIDER)
                                .executes(this::locatePlace)))
                .then(Commands.literal("npc")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(this::notImplemented)))
                .then(Commands.literal("other")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(this::notImplemented)))
                .executes(this::syntaxError);
    }

    // This is shared between /locate and /compass
    public static ServiceKind getServiceKind(CommandContext<CommandSourceStack> context, String searchedName) {
        List<ServiceKind> matchedKinds = Arrays.stream(ServiceKind.values())
                .filter(kind -> StringUtils.partialMatch(kind.getName(), searchedName))
                .toList();

        if (matchedKinds.isEmpty()) {
            MutableComponent response = Component.literal("Found no services matching '" + searchedName + "'")
                    .withStyle(ChatFormatting.RED);
            context.getSource().sendFailure(response);
            return null;
        }

        if (matchedKinds.size() > 1) {
            // Do we have an exact match for any of these?
            Optional<ServiceKind> exactMatch = matchedKinds.stream()
                    .filter(k -> k.getName().equals(searchedName))
                    .findFirst();
            if (exactMatch.isPresent()) {
                return exactMatch.get();
            }

            MutableComponent response = Component.literal("Found multiple services matching '" + searchedName
                            + "'. Pleace specify with more detail. Matching: ")
                    .withStyle(ChatFormatting.RED);
            response.append(Component.literal(String.join(
                    ", ", matchedKinds.stream().map(ServiceKind::getName).toList())));
            context.getSource().sendFailure(response);
            return null;
        }

        // Got exactly one match
        return matchedKinds.getFirst();
    }

    private int locateService(CommandContext<CommandSourceStack> context) {
        String searchedName = context.getArgument("name", String.class);

        ServiceKind selectedKind = LocateCommand.getServiceKind(context, searchedName);
        if (selectedKind == null) return 0;

        List<Poi> services = new ArrayList<>(Services.Poi.getServicePois()
                .filter(poi -> poi.getKind() == selectedKind)
                .toList());

        // Only keep the 4 closest results
        Vec3 currentLocation = McUtils.player().position();
        services.sort(Comparator.comparingDouble(poi -> currentLocation.distanceToSqr(
                poi.getLocation().getX(),
                poi.getLocation().getY().orElse((int) currentLocation.y),
                poi.getLocation().getZ())));
        // Removes from element 4 to the end of the list
        if (services.size() > 4) {
            services.subList(4, services.size()).clear();
        }

        MutableComponent response = Component.literal("Found " + selectedKind.getName() + " services:")
                .withStyle(ChatFormatting.AQUA);

        for (Poi service : services) {
            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(service.getName() + " ")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/compass at " + service.getLocation().asChatCoordinates()))))
                    .append(Component.literal(service.getLocation().toString())
                            .withStyle(ChatFormatting.WHITE)
                            .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/compass at " + service.getLocation().asChatCoordinates()))));
        }

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int locatePlace(CommandContext<CommandSourceStack> context) {
        String searchedName = context.getArgument("name", String.class);

        List<Poi> places = new ArrayList<>(Services.Poi.getLabelPois()
                .filter(poi -> StringUtils.partialMatch(poi.getName(), searchedName))
                .toList());

        if (places.isEmpty()) {
            MutableComponent response = Component.literal("Found no places matching '" + searchedName + "'")
                    .withStyle(ChatFormatting.RED);
            context.getSource().sendFailure(response);
            return 0;
        }

        // Sort in order of closeness to the player
        Vec3 currentLocation = McUtils.player().position();
        places.sort(Comparator.comparingDouble(poi -> currentLocation.distanceToSqr(
                poi.getLocation().getX(),
                poi.getLocation().getY().orElse((int) currentLocation.y),
                poi.getLocation().getZ())));

        MutableComponent response = Component.literal("Found places matching '" + searchedName + "':")
                .withStyle(ChatFormatting.AQUA);

        for (Poi place : places) {
            response.append(Component.literal("\n - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(place.getName() + " ")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND, "/compass place " + place.getName()))))
                    .append(Component.literal(place.getLocation().toString())
                            .withStyle(ChatFormatting.WHITE)
                            .withStyle((style) -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND, "/compass place " + place.getName()))));
        }

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int notImplemented(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Not implemented yet").withStyle(ChatFormatting.RED));
        return 0;
    }

    private int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }
}
