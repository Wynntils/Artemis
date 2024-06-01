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
import com.wynntils.models.containers.type.LootChestTier;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.mapdata.attributes.FixedMapVisibility;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.providers.builtin.MapIconsProvider;
import com.wynntils.services.mapdata.providers.builtin.WaypointsProvider;
import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapAttributesBuilder;
import com.wynntils.services.mapdata.providers.json.JsonMapVisibility;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UserWaypointService extends Service {
    @Persisted
    private final Storage<List<WaypointsProvider.WaypointLocation>> userWaypoints = new Storage<>(new ArrayList<>());

    public UserWaypointService() {
        super(List.of());
    }

    @Override
    public void onStorageLoad(Storage<?> storage) {
        if (storage == userWaypoints) {
            startPoiMigration();
            Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(userWaypoints.get());
        }
    }

    public List<WaypointsProvider.WaypointLocation> getUserWaypoints() {
        return Collections.unmodifiableList(userWaypoints.get());
    }

    public void addUserWaypoint(WaypointsProvider.WaypointLocation waypoint) {
        userWaypoints.get().add(waypoint);
        userWaypoints.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(userWaypoints.get());
    }

    public void removeUserWaypoint(WaypointsProvider.WaypointLocation waypoint) {
        userWaypoints.get().remove(waypoint);
        userWaypoints.touched();
        Services.MapData.WAYPOINTS_PROVIDER.updateWaypoints(userWaypoints.get());
    }

    // region Poi Migration
    public static boolean isCustomPoiLootChest(CustomPoi customPoi) {
        boolean nameIsLootChest = Arrays.stream(LootChestTier.values())
                .anyMatch(tier -> tier.getWaypointName().equals(customPoi.getName()));
        boolean colorIsLootChest = customPoi.getColor().equals(CommonColors.WHITE);
        boolean iconIsLootChest = Arrays.stream(LootChestTier.values())
                .anyMatch(tier -> tier.getWaypointTexture().equals(customPoi.getIcon()));
        boolean visibilityIsLootChest = customPoi.getVisibility() == CustomPoi.Visibility.DEFAULT;

        return nameIsLootChest && colorIsLootChest && iconIsLootChest && visibilityIsLootChest;
    }

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
        boolean isLootChest = isCustomPoiLootChest(customPoi);
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

        WaypointsProvider.WaypointLocation waypointLocation =
                new WaypointsProvider.WaypointLocation(location, label, subcategory, attributes);
        Services.UserWaypoint.addUserWaypoint(waypointLocation);

        return true;
    }

    // endregion
}
