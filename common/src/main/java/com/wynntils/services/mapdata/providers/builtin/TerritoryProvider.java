/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.event.TerritoriesUpdatedEvent;
import com.wynntils.services.mapdata.features.TerritoryArea;
import com.wynntils.services.mapdata.type.MapFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TerritoryProvider extends BuiltInProvider {
    private static final List<MapFeature> PROVIDED_FEATURES = new ArrayList<>();

    @SubscribeEvent
    public void onTerritoryApiUpdate(TerritoriesUpdatedEvent.Api event) {
        updateTerritories();
    }

    @SubscribeEvent
    public void onTerritoryAdvancementsUpdate(TerritoriesUpdatedEvent.Advancements event) {
        updateTerritories();
    }

    private void updateTerritories() {
        PROVIDED_FEATURES.forEach(this::notifyCallbacks);
        PROVIDED_FEATURES.clear();
        Models.Territory.getTerritoryProfiles().stream()
                .map(profile -> new TerritoryArea(profile, Models.Territory.getTerritoryInfo(profile.getName())))
                .forEach(TerritoryProvider::registerFeature);
    }

    private static void registerFeature(MapFeature territory) {
        PROVIDED_FEATURES.add(territory);
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return PROVIDED_FEATURES.stream();
    }

    @Override
    public String getProviderId() {
        return "territory";
    }

    @Override
    public void reloadData() {
        updateTerritories();
    }
}
