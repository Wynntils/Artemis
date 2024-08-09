/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.markers;

import com.wynntils.core.components.Managers;
import com.wynntils.features.combat.ContentTrackerFeature;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.models.marker.type.MarkerProvider;
import com.wynntils.models.marker.type.StaticLocationSupplier;
import com.wynntils.services.map.pois.MarkerPoi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.Texture;
import java.util.Optional;
import java.util.stream.Stream;

public class ActivityMarkerProvider implements MarkerProvider<MarkerPoi> {
    private static final String ACTIVITY_LOCATION_NAME = "Activity Location";

    private ActivityMarkerInfo spawnInfo;
    private ActivityMarkerInfo trackedActivityInfo;

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnInfo = spawnLocation == null
                ? null
                : new ActivityMarkerInfo(
                        spawnLocation,
                        new MarkerInfo(
                                ACTIVITY_LOCATION_NAME,
                                new StaticLocationSupplier(spawnLocation),
                                Texture.QUESTS_SCROLL_ICON,
                                CommonColors.WHITE,
                                CommonColors.WHITE,
                                CommonColors.WHITE),
                        new MarkerPoi(
                                PoiLocation.fromLocation(spawnLocation),
                                ACTIVITY_LOCATION_NAME,
                                Texture.QUESTS_SCROLL_ICON));
    }

    public Optional<Location> getSpawnLocation() {
        return spawnInfo == null ? Optional.empty() : Optional.ofNullable(spawnInfo.location());
    }

    public void setTrackedActivityLocation(Location trackedActivityLocation, CustomColor activityColor) {
        this.trackedActivityInfo = trackedActivityLocation == null
                ? null
                : new ActivityMarkerInfo(
                        trackedActivityLocation,
                        new MarkerInfo(
                                ACTIVITY_LOCATION_NAME,
                                new StaticLocationSupplier(trackedActivityLocation),
                                Texture.QUESTS_SCROLL_ICON,
                                activityColor,
                                CommonColors.WHITE,
                                CommonColors.WHITE),
                        new MarkerPoi(
                                PoiLocation.fromLocation(trackedActivityLocation),
                                ACTIVITY_LOCATION_NAME,
                                Texture.QUESTS_SCROLL_ICON));
    }

    public Location getTrackedActivityLocation() {
        return trackedActivityInfo.location();
    }

    @Override
    public Stream<MarkerInfo> getMarkerInfos() {
        Stream<MarkerInfo> stream = Stream.empty();

        if (spawnInfo != null) {
            stream = Stream.concat(stream, Stream.of(spawnInfo.markerInfo()));
        }
        if (trackedActivityInfo != null) {
            stream = Stream.concat(stream, Stream.of(trackedActivityInfo.markerInfo()));
        }

        return stream;
    }

    @Override
    public Stream<MarkerPoi> getPois() {
        Stream<MarkerPoi> stream = Stream.empty();

        if (spawnInfo != null) {
            stream = Stream.concat(stream, Stream.of(spawnInfo.markerPoi()));
        }
        if (trackedActivityInfo != null) {
            stream = Stream.concat(stream, Stream.of(trackedActivityInfo.markerPoi()));
        }

        return stream;
    }

    @Override
    public boolean isEnabled() {
        // FIXME: Move the config after config refactor
        return Managers.Feature.getFeatureInstance(ContentTrackerFeature.class)
                        .autoTrackCoordinates
                        .get()
                && (spawnInfo != null || trackedActivityInfo != null);
    }

    private record ActivityMarkerInfo(Location location, MarkerInfo markerInfo, MarkerPoi markerPoi) {}
}
