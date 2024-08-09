/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.commands.Command;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.pois.ServicePoi;
import com.wynntils.services.map.type.ServiceKind;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.wynn.LocationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class CompassCommand extends Command {
    private static final SuggestionProvider<CommandSourceStack> SHARE_TARGET_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Stream.concat(
                            Stream.of("party", "guild"),
                            McUtils.mc().level.players().stream().map(Player::getScoreboardName)),
                    builder);

    public static final SuggestionProvider<CommandSourceStack> TERRITORY_SUGGESTION_PROVIDER =
            (context, builder) -> SharedSuggestionProvider.suggest(Models.Territory.getTerritoryNames(), builder);

    @Override
    public String getCommandName() {
        return "compass";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getCommandBuilder(
            LiteralArgumentBuilder<CommandSourceStack> base, CommandBuildContext context) {
        return base.then(Commands.literal("at")
                        .then(Commands.argument("location", Vec3Argument.vec3()).executes(this::compassAtVec3)))
                .then(Commands.literal("share")
                        .then(Commands.literal("location")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(SHARE_TARGET_SUGGESTION_PROVIDER)
                                        .executes(this::shareLocation)))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(SHARE_TARGET_SUGGESTION_PROVIDER)
                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                        .executes(this::shareCompassIndex))
                                .executes(this::shareCompass)))
                .then(Commands.literal("service")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(LocateCommand.SERVICE_SUGGESTION_PROVIDER)
                                .executes(this::compassService)))
                .then(Commands.literal("place")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(LocateCommand.PLACES_SUGGESTION_PROVIDER)
                                .executes(this::compassPlace)))
                .then(Commands.literal("territory")
                        .then(Commands.argument("territory", StringArgumentType.greedyString())
                                .suggests(TERRITORY_SUGGESTION_PROVIDER)
                                .executes(this::territory)))
                .then(Commands.literal("npc")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(this::notImplemented)))
                .then(Commands.literal("other")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(this::notImplemented)))
                .then(Commands.literal("clear").executes(this::compassClear))
                .then(Commands.argument("location", StringArgumentType.greedyString())
                        .executes(this::compassAtString))
                .executes(this::syntaxError);
    }

    private int shareCompassIndex(CommandContext<CommandSourceStack> context) {
        List<MarkerInfo> markers =
                Models.Marker.USER_WAYPOINTS_PROVIDER.getMarkerInfos().toList();

        if (markers.isEmpty()) {
            context.getSource()
                    .sendFailure(
                            Component.literal("You don't have a compass set!").withStyle(ChatFormatting.RED));
            return 1;
        }

        String target = StringArgumentType.getString(context, "target");
        int index = IntegerArgumentType.getInteger(context, "index");

        LocationUtils.shareCompass(target, markers.get(index).location());

        return 1;
    }

    private int shareCompass(CommandContext<CommandSourceStack> context) {
        List<MarkerInfo> markers =
                Models.Marker.USER_WAYPOINTS_PROVIDER.getMarkerInfos().toList();

        if (markers.isEmpty()) {
            context.getSource()
                    .sendFailure(
                            Component.literal("You don't have a compass set!").withStyle(ChatFormatting.RED));
            return 1;
        }

        String target = StringArgumentType.getString(context, "target");

        LocationUtils.shareCompass(target, markers.getFirst().location());

        return 1;
    }

    private int shareLocation(CommandContext<CommandSourceStack> context) {
        String target = StringArgumentType.getString(context, "target");

        LocationUtils.shareLocation(target);

        return 1;
    }

    private int compassAtVec3(CommandContext<CommandSourceStack> context) {
        Coordinates coordinates = Vec3Argument.getCoordinates(context, "location");
        Location location = new Location(coordinates.getBlockPos(context.getSource()));
        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(location);

        MutableComponent response = Component.literal("Compass set to ").withStyle(ChatFormatting.AQUA);
        response.append(Component.literal(location.toString()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int compassAtString(CommandContext<CommandSourceStack> context) {
        String coordinates = StringArgumentType.getString(context, "location");
        Optional<Location> location = LocationUtils.parseFromString(coordinates);
        if (location.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Incorrect coordinates!"));
            return 0;
        }

        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(location.get());

        MutableComponent response = Component.literal("Compass set to ").withStyle(ChatFormatting.AQUA);
        response.append(Component.literal(location.get().toString()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int compassService(CommandContext<CommandSourceStack> context) {
        String searchedName = context.getArgument("name", String.class);

        ServiceKind selectedKind = LocateCommand.getServiceKind(context, searchedName);
        if (selectedKind == null) return 0;

        Vec3 currentLocation = McUtils.player().position();
        Optional<ServicePoi> closestServiceOptional = Services.Poi.getServicePois()
                .filter(poi -> poi.getKind() == selectedKind)
                .min(Comparator.comparingDouble(poi -> currentLocation.distanceToSqr(
                        poi.getLocation().getX(),
                        poi.getLocation().getY().orElse((int) currentLocation.y),
                        poi.getLocation().getZ())));
        if (closestServiceOptional.isEmpty()) {
            // This really should not happen...
            MutableComponent response = Component.literal("Found no services of kind '" + selectedKind.getName() + "'")
                    .withStyle(ChatFormatting.RED);
            context.getSource().sendFailure(response);
            return 0;
        }
        Poi closestService = closestServiceOptional.get();

        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(
                closestService.getLocation().asLocation(),
                closestServiceOptional.get().getIcon());

        MutableComponent response = Component.literal("Compass set to " + selectedKind.getName() + " at ")
                .withStyle(ChatFormatting.AQUA);
        response.append(
                Component.literal(closestService.getLocation().toString()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int compassPlace(CommandContext<CommandSourceStack> context) {
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

        Poi place;

        if (places.size() > 1) {
            // Try to find one with an exact match, to differentiate e.g. "Detlas" from "Detlas Suburbs"
            Optional<Poi> exactMatch = places.stream()
                    .filter(poi -> poi.getName().equals(searchedName))
                    .findFirst();
            if (exactMatch.isEmpty()) {
                MutableComponent response = Component.literal("Found multiple places matching '" + searchedName
                                + "', but none matched exactly. Matching: ")
                        .withStyle(ChatFormatting.RED);
                response.append(Component.literal(
                        String.join(", ", places.stream().map(Poi::getName).toList())));
                context.getSource().sendFailure(response);
                return 0;
            }
            place = exactMatch.get();
        } else {
            place = places.getFirst();
        }

        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(place.getLocation().asLocation());

        MutableComponent response =
                Component.literal("Compass set to " + place.getName() + " at ").withStyle(ChatFormatting.AQUA);
        response.append(Component.literal(place.getLocation().toString()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    // this is shared by TerritoryCommand
    public int territory(CommandContext<CommandSourceStack> context) {
        String territoryArg = context.getArgument("territory", String.class);

        TerritoryProfile territoryProfile = Models.Territory.getTerritoryProfile(territoryArg);

        if (territoryProfile == null) {
            context.getSource()
                    .sendFailure(Component.literal("Can't find territory '" + territoryArg + "'")
                            .withStyle(ChatFormatting.RED));
            return 1;
        }

        PoiLocation location = territoryProfile.getCenterLocation();

        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();
        Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(location.asLocation());

        MutableComponent response = Component.literal(
                        "Compass set to middle of " + territoryProfile.getFriendlyName() + " at ")
                .withStyle(ChatFormatting.AQUA);
        response.append(Component.literal(location.toString()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int compassClear(CommandContext<CommandSourceStack> context) {
        Models.Marker.USER_WAYPOINTS_PROVIDER.removeAllLocations();

        MutableComponent response = Component.literal("Compass cleared").withStyle(ChatFormatting.AQUA);
        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private int notImplemented(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Not implemented yet").withStyle(ChatFormatting.RED));
        return 0;
    }

    public int syntaxError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("Missing argument").withStyle(ChatFormatting.RED));
        return 0;
    }
}
