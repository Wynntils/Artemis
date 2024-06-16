/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.features;

import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapLocation;
import com.wynntils.utils.mc.type.Location;
import java.util.Locale;

public final class WaypointLocation extends JsonMapLocation {
    public WaypointLocation(Location location, String label, String subcategory, JsonMapAttributes attributes) {
        super(
                "waypoint" + "-" + label.toLowerCase(Locale.ROOT).replaceAll("\\s", "-") + "-" + location.hashCode(),
                "wynntils:personal:waypoint" + (subcategory.isEmpty() ? "" : ":" + subcategory),
                attributes,
                location);
    }
}
