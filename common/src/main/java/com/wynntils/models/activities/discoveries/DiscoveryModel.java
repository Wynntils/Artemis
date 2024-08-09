/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.discoveries;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.models.activities.type.DiscoveryType;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.screens.maps.MainMapScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;

public final class DiscoveryModel extends Model {
    // From container query updates
    private final Map<String, DiscoveryStorage> discoveryStorage = new HashMap<>();

    public DiscoveryModel() {
        super(List.of());
    }

    public void openDiscoveryOnMap(DiscoveryInfo discoveryInfo) {
        if (discoveryInfo.type() == DiscoveryType.SECRET) {
            locateSecretDiscovery(discoveryInfo.name(), DiscoveryOpenAction.MAP);
            return;
        }

        TerritoryProfile guildTerritory = Models.Territory.getTerritoryProfile(discoveryInfo.name());
        if (guildTerritory != null) {
            int centerX = (guildTerritory.getEndX() + guildTerritory.getStartX()) / 2;
            int centerZ = (guildTerritory.getEndZ() + guildTerritory.getStartZ()) / 2;

            McUtils.mc().setScreen(MainMapScreen.create(centerX, centerZ));
        }
    }

    public void setDiscoveryCompass(DiscoveryInfo discoveryInfo) {
        if (discoveryInfo.type() == DiscoveryType.SECRET) {
            locateSecretDiscovery(discoveryInfo.name(), DiscoveryOpenAction.COMPASS);
            return;
        }

        TerritoryProfile guildTerritory = Models.Territory.getTerritoryProfile(discoveryInfo.name());
        if (guildTerritory != null) {
            int centerX = (guildTerritory.getEndX() + guildTerritory.getStartX()) / 2;
            int centerZ = (guildTerritory.getEndZ() + guildTerritory.getStartZ()) / 2;

            Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(new Location(centerX, 0, centerZ));
        }
    }

    public void openSecretDiscoveryWiki(DiscoveryInfo discoveryInfo) {
        Managers.Net.openLink(UrlId.LINK_WIKI_LOOKUP, Map.of("title", discoveryInfo.name()));
    }

    private void queryDiscoveries(
            boolean querySecretDiscoveries, boolean queryWorldDiscoveries, boolean queryTerritoryDiscoveries) {
        WynntilsMod.info("Requesting rescan of discoveries in Content Book");

        // This order is a bit arbitrary, but it's the order they appear in the Content Book,
        // so we can use this as a workaround to parse them faster.
        if (querySecretDiscoveries) {
            Models.Activity.scanContentBook(ActivityType.SECRET_DISCOVERY, this::updateSecretDiscoveriesFromQuery);
        }
        if (queryWorldDiscoveries) {
            Models.Activity.scanContentBook(ActivityType.WORLD_DISCOVERY, this::updateWorldDiscoveriesFromQuery);
        }
        if (queryTerritoryDiscoveries) {
            Models.Activity.scanContentBook(
                    ActivityType.TERRITORIAL_DISCOVERY, this::updateTerritoryDiscoveriesFromQuery);
        }
    }

    private void updateTerritoryDiscoveriesFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<DiscoveryInfo> newDiscoveries = new ArrayList<>();
        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.TERRITORIAL_DISCOVERY) {
                WynntilsMod.warn("Incorrect territory discovery activity type recieved: " + activity);
                continue;
            }
            DiscoveryInfo discoveryInfo = getDiscoveryInfoFromActivity(activity);
            newDiscoveries.add(discoveryInfo);
        }

        discoveryStorage.put(
                Models.Character.getId(),
                discoveryStorage
                        .getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY)
                        .with(DiscoveryType.TERRITORY, newDiscoveries, progress));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.TERRITORIAL_DISCOVERY));
    }

    private void updateWorldDiscoveriesFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<DiscoveryInfo> newDiscoveries = new ArrayList<>();
        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.WORLD_DISCOVERY) {
                WynntilsMod.warn("Incorrect discovery activity type recieved: " + activity);
                continue;
            }
            DiscoveryInfo discoveryInfo = getDiscoveryInfoFromActivity(activity);
            newDiscoveries.add(discoveryInfo);
        }

        discoveryStorage.put(
                Models.Character.getId(),
                discoveryStorage
                        .getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY)
                        .with(DiscoveryType.WORLD, newDiscoveries, progress));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.WORLD_DISCOVERY));
    }

    private void updateSecretDiscoveriesFromQuery(List<ActivityInfo> newActivities, List<StyledText> progress) {
        List<DiscoveryInfo> newDiscoveries = new ArrayList<>();
        for (ActivityInfo activity : newActivities) {
            if (activity.type() != ActivityType.SECRET_DISCOVERY) {
                WynntilsMod.warn("Incorrect secret discovery activity type recieved: " + activity);
                continue;
            }
            DiscoveryInfo discoveryInfo = getDiscoveryInfoFromActivity(activity);
            newDiscoveries.add(discoveryInfo);
        }

        discoveryStorage.put(
                Models.Character.getId(),
                discoveryStorage
                        .getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY)
                        .with(DiscoveryType.SECRET, newDiscoveries, progress));
        WynntilsMod.postEvent(new ActivityUpdatedEvent(ActivityType.SECRET_DISCOVERY));
    }

    private DiscoveryInfo getDiscoveryInfoFromActivity(ActivityInfo activity) {
        return DiscoveryInfo.fromActivityInfo(activity);
    }

    public List<Component> getDiscoveriesTooltip() {
        return Stream.concat(
                        getTooltipForType(DiscoveryType.TERRITORY).stream().map(StyledText::getComponent),
                        getTooltipForType(DiscoveryType.WORLD).stream().map(StyledText::getComponent))
                .collect(Collectors.toList());
    }

    public List<Component> getSecretDiscoveriesTooltip() {
        return getTooltipForType(DiscoveryType.SECRET).stream()
                .map(StyledText::getComponent)
                .collect(Collectors.toList());
    }

    public Stream<DiscoveryInfo> getAllDiscoveries(ActivitySortOrder sortOrder) {
        if (sortOrder == ActivitySortOrder.DISTANCE) {
            throw new IllegalArgumentException("Cannot sort discoveries by distance");
        }

        // All discoveries are always sorted by status (available then unavailable), and then
        // the given sort order, and finally a third way if the given sort order is equal.
        Stream<DiscoveryInfo> baseStream = Stream.of();
        for (DiscoveryType type : DiscoveryType.values()) {
            baseStream = Stream.concat(
                    baseStream,
                    discoveryStorage
                            .getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY)
                            .getDiscoveries(type)
                            .stream());
        }

        return switch (sortOrder) {
            case LEVEL -> baseStream.sorted(Comparator.comparing(DiscoveryInfo::discovered)
                    .thenComparing(DiscoveryInfo::minLevel)
                    .thenComparing(DiscoveryInfo::name));
            case ALPHABETIC -> baseStream.sorted(Comparator.comparing(DiscoveryInfo::discovered)
                    .thenComparing(DiscoveryInfo::name)
                    .thenComparing(DiscoveryInfo::minLevel));
            case DISTANCE -> null;
        };
    }

    public Stream<DiscoveryInfo> getAllCompletedDiscoveries(ActivitySortOrder sortOrder) {
        return getAllDiscoveries(sortOrder).filter(DiscoveryInfo::discovered);
    }

    public Stream<DiscoveryInfo> getAllDiscoveriesForType(DiscoveryType type) {
        DiscoveryStorage storage = discoveryStorage.getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY);
        return storage.getDiscoveries(type).stream();
    }

    public List<StyledText> getTooltipForType(DiscoveryType type) {
        DiscoveryStorage storage = discoveryStorage.getOrDefault(Models.Character.getId(), DiscoveryStorage.EMPTY);
        return storage.getTooltip(type);
    }

    private void locateSecretDiscovery(String name, DiscoveryOpenAction action) {
        ApiResponse apiResponse = Managers.Net.callApi(UrlId.API_WIKI_DISCOVERY_QUERY, Map.of("name", name));
        apiResponse.handleJsonObject(json -> {
            if (json.has("error")) { // Returns error if page does not exist
                McUtils.sendErrorToClient("Unable to find discovery coordinates. (Wiki page not found)");
                return;
            }

            String wikiText = json.get("parse")
                    .getAsJsonObject()
                    .get("wikitext")
                    .getAsJsonObject()
                    .get("*")
                    .getAsString()
                    .replace(" ", "")
                    .replace("\n", "");

            String xLocation = wikiText.substring(wikiText.indexOf("xcoordinate="));
            String zLocation = wikiText.substring(wikiText.indexOf("zcoordinate="));

            int xEnd = Math.min(xLocation.indexOf('|'), xLocation.indexOf("}}"));
            int zEnd = Math.min(zLocation.indexOf('|'), zLocation.indexOf("}}"));

            int x;
            int z;

            try {
                x = Integer.parseInt(xLocation.substring(12, xEnd));
                z = Integer.parseInt(zLocation.substring(12, zEnd));
            } catch (NumberFormatException e) {
                McUtils.sendErrorToClient("Unable to find discovery coordinates. (Wiki template not located)");
                return;
            }

            if (x == 0 && z == 0) {
                McUtils.sendErrorToClient("Unable to find discovery coordinates. (Wiki coordinates not located)");
                return;
            }

            switch (action) {
                    // We can't run this is on request thread
                case MAP -> Managers.TickScheduler.scheduleNextTick(
                        () -> McUtils.mc().setScreen(MainMapScreen.create(x, z)));
                case COMPASS -> Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(new Location(x, 0, z));
            }
        });
    }

    public void reloadDiscoveries(
            boolean querySecretDiscoveries, boolean queryWorldDiscoveries, boolean queryTerritoryDiscoveries) {
        queryDiscoveries(querySecretDiscoveries, queryWorldDiscoveries, queryTerritoryDiscoveries);
    }

    private record DiscoveryStorage(Map<DiscoveryType, Pair<List<DiscoveryInfo>, List<StyledText>>> storedInfoPerType) {
        private static final DiscoveryStorage EMPTY = new DiscoveryStorage(Map.of());

        private DiscoveryStorage with(DiscoveryType type, List<DiscoveryInfo> discoveries, List<StyledText> tooltip) {
            Map<DiscoveryType, Pair<List<DiscoveryInfo>, List<StyledText>>> newStoredInfo =
                    new HashMap<>(storedInfoPerType);
            newStoredInfo.put(
                    type, Pair.of(Collections.unmodifiableList(discoveries), Collections.unmodifiableList(tooltip)));
            return new DiscoveryStorage(newStoredInfo);
        }

        private List<DiscoveryInfo> getDiscoveries(DiscoveryType type) {
            return storedInfoPerType
                    .getOrDefault(type, Pair.of(List.of(), List.of()))
                    .key();
        }

        private List<StyledText> getTooltip(DiscoveryType type) {
            return storedInfoPerType
                    .getOrDefault(type, Pair.of(List.of(), List.of()))
                    .value();
        }
    }

    public enum DiscoveryOpenAction {
        MAP,
        COMPASS
    }
}
