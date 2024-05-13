/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.services.mapdata.attributes.AbstractMapAttributes;
import com.wynntils.services.mapdata.attributes.impl.AlwaysMapVisibility;
import com.wynntils.services.mapdata.attributes.impl.FadingMapVisiblity;
import com.wynntils.services.mapdata.attributes.impl.NeverMapVisibility;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WaypointsProvider extends BuiltInProvider {
    private static final List<MapFeature> PROVIDED_FEATURES = new ArrayList<>();
    private static int counter;

    @Override
    public String getProviderId() {
        return "waypoints";
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return PROVIDED_FEATURES.stream();
    }

    public static void resetFeatures() {
        PROVIDED_FEATURES.clear();
    }

    public static void registerFeature(CustomPoi customPoi) {
        int tier =
                switch (customPoi.getIcon()) {
                    case CHEST_T1 -> 1;
                    case CHEST_T2 -> 2;
                    case CHEST_T3 -> 3;
                    case CHEST_T4 -> 4;
                    default -> 0;
                };
        if (tier == 0) {
            String iconId = MapIconsProvider.getIconIdFromTexture(customPoi.getIcon());
            PROVIDED_FEATURES.add(new WaypointLocation(
                    customPoi.getLocation().asLocation(), customPoi.getName(), iconId, customPoi.getVisibility()));
        } else {
            PROVIDED_FEATURES.add(new FoundChestLocation(customPoi.getLocation().asLocation(), tier));
        }
    }

    private static final class WaypointLocation implements MapLocation {
        public static final FadingMapVisiblity WAYPOINT_VISIBILITY = new FadingMapVisiblity(30, 100, 6);
        private final Location location;
        private final String name;
        private final String iconId;
        private final CustomPoi.Visibility visibility;
        private final int number;

        private WaypointLocation(Location location, String name, String iconId, CustomPoi.Visibility visibility) {
            this.location = location;
            this.name = name;
            this.iconId = iconId;
            this.visibility = visibility;
            this.number = WaypointsProvider.counter++;
        }

        @Override
        public String getFeatureId() {
            return "waypoint" + "-" + number;
        }

        @Override
        public String getCategoryId() {
            return "wynntils:personal:waypoint";
        }

        @Override
        public MapAttributes getAttributes() {
            return new AbstractMapAttributes() {
                @Override
                public String getIconId() {
                    return iconId;
                }

                @Override
                public String getLabel() {
                    return name;
                }

                @Override
                public MapVisibility getIconVisibility() {
                    return switch (visibility) {
                        case DEFAULT -> WAYPOINT_VISIBILITY;
                        case ALWAYS -> new AlwaysMapVisibility();
                        case HIDDEN -> new NeverMapVisibility();
                    };
                }
            };
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

    private static final class FoundChestLocation implements MapLocation {
        private final Location location;
        private final int tier;
        private final int number;

        private FoundChestLocation(Location location, int tier) {
            this.location = location;
            this.tier = tier;
            this.number = WaypointsProvider.counter++;
        }

        @Override
        public String getFeatureId() {
            return "found-chest" + "-" + number;
        }

        @Override
        public String getCategoryId() {
            return "wynntils:personal:found-chest:tier-" + tier;
        }

        @Override
        public MapAttributes getAttributes() {
            return null;
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
}
