/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.features;

import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapLocation;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.type.Location;

public final class PlaceLocation extends JsonMapLocation {
    private PlaceLocation(String featureId, String categoryId, JsonMapAttributes attributes, Location location) {
        super(featureId, categoryId, attributes, location);
    }

    public enum PlaceType {
        PROVINCE("province"),
        CITY("city"),
        TOWN_OR_PLACE("town-or-place");

        private final String mapDataId;

        PlaceType(String mapDataId) {
            this.mapDataId = mapDataId;
        }

        public String getMapDataId() {
            return mapDataId;
        }

        public String getName() {
            return StringUtils.capitalized(mapDataId);
        }
    }
}
