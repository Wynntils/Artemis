/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.mc.type.Location;
import java.util.List;

public class JsonMapLocation implements MapLocation {
    private final String featureId;
    private final String categoryId;
    private final MapAttributes attributes;
    private final Location location;

    public JsonMapLocation(String featureId, String categoryId, MapAttributes attributes, Location location) {
        this.featureId = featureId;
        this.categoryId = categoryId;
        this.attributes = attributes;
        this.location = location;
    }

    @Override
    public String getFeatureId() {
        return featureId;
    }

    @Override
    public String getCategoryId() {
        return categoryId;
    }

    @Override
    public MapAttributes getAttributes() {
        return attributes;
    }

    @Override
    public List<String> getTags() {
        return List.of();
    }

    @Override
    public Location getLocation() {
        return location;
    }
}
