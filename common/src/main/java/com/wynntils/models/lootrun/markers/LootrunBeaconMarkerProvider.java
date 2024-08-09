/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun.markers;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.features.combat.CustomLootrunBeaconsFeature;
import com.wynntils.models.beacons.type.LootrunBeaconKind;
import com.wynntils.models.lootrun.type.TaskPrediction;
import com.wynntils.models.marker.type.MarkerInfo;
import com.wynntils.models.marker.type.MarkerProvider;
import com.wynntils.models.marker.type.StaticLocationSupplier;
import com.wynntils.services.map.pois.MarkerPoi;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.type.PoiLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LootrunBeaconMarkerProvider implements MarkerProvider<MarkerPoi> {
    private List<MarkerInfo> taskMarkers = new ArrayList<>();
    private List<MarkerPoi> pois = new ArrayList<>();

    public void reloadTaskMarkers() {
        // Update markers
        List<MarkerInfo> newTaskMarkers = new ArrayList<>();

        for (Map.Entry<LootrunBeaconKind, TaskPrediction> entry :
                Models.Lootrun.getBeacons().entrySet()) {
            newTaskMarkers.add(new MarkerInfo(
                    EnumUtils.toNiceString(entry.getKey()) + " Beacon",
                    new StaticLocationSupplier(entry.getValue().taskLocation().location()),
                    entry.getValue().taskLocation().taskType().getTexture(),
                    entry.getKey().getDisplayColor(),
                    CommonColors.WHITE,
                    entry.getKey().getDisplayColor()));
        }
        taskMarkers = newTaskMarkers;

        // Update POIs
        List<MarkerPoi> newPois = new ArrayList<>();

        for (MarkerInfo entry : taskMarkers) {
            newPois.add(new MarkerPoi(PoiLocation.fromLocation(entry.location()), entry.name(), entry.texture()));
        }
        pois = newPois;
    }

    @Override
    public Stream<MarkerInfo> getMarkerInfos() {
        return taskMarkers.stream();
    }

    @Override
    public Stream<MarkerPoi> getPois() {
        return pois.stream();
    }

    @Override
    public boolean isEnabled() {
        // FIXME: Feature-Model dependency
        return Models.Lootrun.getState().isRunning()
                && Managers.Feature.getFeatureInstance(CustomLootrunBeaconsFeature.class)
                        .isEnabled();
    }
}
