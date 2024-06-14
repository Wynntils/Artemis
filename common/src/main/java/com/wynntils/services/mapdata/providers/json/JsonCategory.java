/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapAreaAttributes;
import com.wynntils.services.mapdata.attributes.type.MapLocationAttributes;
import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import com.wynntils.services.mapdata.type.MapCategory;
import java.util.Optional;

public class JsonCategory implements MapCategory {
    private final String id;
    private final String name;
    private final JsonLocationAttributes locationAttributes;
    private final JsonAreaAttributes areaAttributes;
    private final JsonPathAttributes pathAttributes;

    public JsonCategory(
            String id,
            String name,
            JsonLocationAttributes locationAttributes,
            JsonAreaAttributes areaAttributes,
            JsonPathAttributes pathAttributes) {
        this.id = id;
        this.name = name;
        this.locationAttributes = locationAttributes;
        this.areaAttributes = areaAttributes;
        this.pathAttributes = pathAttributes;
    }

    @Override
    public String getCategoryId() {
        return id;
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<MapLocationAttributes> getLocationAttributes() {
        return Optional.ofNullable(locationAttributes);
    }

    @Override
    public Optional<MapPathAttributes> getPathAttributes() {
        return Optional.ofNullable(pathAttributes);
    }

    @Override
    public Optional<MapAreaAttributes> getAreaAttributes() {
        return Optional.ofNullable(areaAttributes);
    }
}
