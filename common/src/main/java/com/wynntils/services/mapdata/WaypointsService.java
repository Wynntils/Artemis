/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.components.Services;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.models.containers.LootChestModel;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.mapdata.attributes.FixedMapVisibility;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.features.WaypointLocation;
import com.wynntils.services.mapdata.providers.builtin.MapIconsProvider;
import com.wynntils.services.mapdata.providers.json.JsonIcon;
import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapAttributesBuilder;
import com.wynntils.services.mapdata.providers.json.JsonMapLocation;
import com.wynntils.services.mapdata.providers.json.JsonMapVisibility;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WaypointsService extends Service {
    @Persisted
    private final Storage<List<WaypointLocation>> waypoints = new Storage<>(new ArrayList<>());

    @Persisted
    private final Storage<List<JsonIcon>> customIcons = new Storage<>(new ArrayList<>());

    public WaypointsService() {
        super(List.of());
    }

    @Override
    public void onStorageLoad(Storage<?> storage) {
        if (storage == waypoints) {
            startPoiMigration();
            Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(waypoints.get());
        }

        if (storage == customIcons) {
            Services.MapData.WAYPOINTS_PROVIDER.updateIcons(customIcons.get());
        }
    }

    public List<WaypointLocation> getWaypoints() {
        return Collections.unmodifiableList(waypoints.get());
    }

    public List<JsonIcon> getCustomIcons() {
        return Collections.unmodifiableList(customIcons.get());
    }

    public void addCustomIcon(JsonIcon iconToAdd) {
        customIcons.get().add(iconToAdd);
        customIcons.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateIcons(customIcons.get());
    }

    public void removeCustomIcon(JsonIcon iconToRemove) {
        customIcons.get().remove(iconToRemove);
        customIcons.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateIcons(customIcons.get());
    }

    public Set<String> getCategories() {
        return getWaypoints().stream().map(JsonMapLocation::getCategoryId).collect(Collectors.toSet());
    }

    public void addWaypoint(WaypointLocation waypoint) {
        waypoints.get().add(waypoint);
        waypoints.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(waypoints.get());
    }

    public void removeWaypoint(WaypointLocation waypoint) {
        waypoints.get().remove(waypoint);
        waypoints.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(waypoints.get());
    }

    // region Poi Migration
    private void startPoiMigration() {
        // The feature instance is not guaranteed to be present, so we have to check
        MainMapFeature featureInstance = Managers.Feature.getFeatureInstance(MainMapFeature.class);
        if (featureInstance == null) return;

        Config<List<CustomPoi>> customPois = featureInstance.customPois;
        if (customPois.get().isEmpty()) return;

        // Try to migrate custom pois to the new mapdata system
        // This is done on storage load, as configs are loaded before storages
        List<CustomPoi> migratedPois = new ArrayList<>();
        for (CustomPoi customPoi : customPois.get()) {
            if (migrateToMapdata(customPoi)) {
                migratedPois.add(customPoi);
            }
        }

        WynntilsMod.info("MapData Migration: Custom Pois: " + customPois.get().size());
        WynntilsMod.info("MapData Migration: User waypoints: " + migratedPois.size());

        customPois.get().removeAll(migratedPois);
        customPois.touched();
    }

    // This feature ports old custom poi data to the new mapdata system
    // This is a one-time migration, but can't be removed in the foreseeable future,
    // so we can keep upfixing old configs
    private boolean migrateToMapdata(CustomPoi customPoi) {
        boolean isLootChest = LootChestModel.isCustomPoiLootChest(customPoi);
        if (isLootChest) return false;

        // This must be a user waypoint, let's migrate it
        Location location = new Location(customPoi.getLocation());
        String label = customPoi.getName();
        String subcategory = ""; // Subcategories did not use to exist

        JsonMapAttributes attributes = new JsonMapAttributesBuilder()
                .setLabel(label)
                .setIcon(MapIconsProvider.getIconIdFromTexture(customPoi.getIcon()))
                .setIconColor(customPoi.getColor())
                .setIconVisibility(new JsonMapVisibility(
                        switch (customPoi.getVisibility()) {
                            case DEFAULT -> MapVisibility.builder().withMin(30f);
                            case ALWAYS -> FixedMapVisibility.ICON_ALWAYS;
                            case HIDDEN -> FixedMapVisibility.ICON_NEVER;
                        }))
                .build();

        WaypointLocation waypointLocation = new WaypointLocation(location, label, subcategory, attributes);
        Services.Waypoints.addWaypoint(waypointLocation);

        return true;
    }

    // endregion
}
