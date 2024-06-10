/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.services.mapdata.features.WaypointLocation;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WaypointsProvider extends BuiltInProvider {
    private static final List<MapFeature> PROVIDED_FEATURES = new ArrayList<>();

    @Override
    public String getProviderId() {
        return "waypoints";
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return PROVIDED_FEATURES.stream();
    }

    @Override
    public void reloadData() {}

    public void updateWaypoints(List<WaypointLocation> waypoints) {
        PROVIDED_FEATURES.forEach(this::notifyCallbacks);
        PROVIDED_FEATURES.clear();
        waypoints.forEach(WaypointsProvider::registerFeature);
    }

    private static void registerFeature(WaypointLocation waypoint) {
        PROVIDED_FEATURES.add(waypoint);
    }
}
