/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.marker;

import com.wynntils.models.marker.type.LocationSupplier;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.models.marker.type.MarkerProvider;
import com.wynntils.models.marker.type.StaticLocationSupplier;
import com.wynntils.services.map.pois.WaypointPoi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.AbstractTexture;
import com.wynntils.utils.type.Pair;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

public class UserWaypointMarkerProvider implements MarkerProvider<WaypointPoi> {
    private final Set<Pair<MarkerInfo, WaypointPoi>> markerInfoSet = new CopyOnWriteArraySet<>();

    public void addLocation(
            Location location, AbstractTexture texture, CustomColor beaconColor, CustomColor textColor) {
        addLocation(new MarkerInfo(
                "Waypoint", new StaticLocationSupplier(location), texture, beaconColor, textColor, CommonColors.WHITE));
    }

    public void addLocation(Location location, Texture texture, CustomColor beaconColor) {
        addLocation(new MarkerInfo(
                "Waypoint",
                new StaticLocationSupplier(location),
                texture,
                beaconColor,
                CommonColors.WHITE,
                CommonColors.WHITE));
    }

    public void addLocation(Location location, AbstractTexture texture) {
        addLocation(new MarkerInfo(
                "Waypoint",
                new StaticLocationSupplier(location),
                texture,
                CustomColor.NONE,
                CommonColors.WHITE,
                CommonColors.WHITE));
    }

    public void addLocation(Location location) {
        addLocation(new MarkerInfo(
                "Waypoint",
                new StaticLocationSupplier(location),
                Texture.WAYPOINT,
                CustomColor.NONE,
                CommonColors.WHITE,
                CommonColors.WHITE));
    }

    public void addLocation(LocationSupplier locationSupplier) {
        addLocation(new MarkerInfo(
                "Waypoint",
                locationSupplier,
                Texture.WAYPOINT,
                CustomColor.NONE,
                CommonColors.WHITE,
                CommonColors.WHITE));
    }

    public void addLocation(MarkerInfo markerInfo) {
        markerInfoSet.add(Pair.of(
                markerInfo,
                new WaypointPoi(
                        () -> PoiLocation.fromLocation(
                                markerInfo.locationSupplier().getLocation()),
                        markerInfo.name())));
    }

    public void removeLocation(Location location) {
        markerInfoSet.removeIf(info -> info.a().location().equals(location));
    }

    public void removeAllLocations() {
        markerInfoSet.clear();
    }

    @Override
    public Stream<WaypointPoi> getPois() {
        return markerInfoSet.stream().map(Pair::b);
    }

    @Override
    public Stream<MarkerInfo> getMarkerInfos() {
        return markerInfoSet.stream().map(Pair::a);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
