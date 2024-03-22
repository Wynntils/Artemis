/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.builtin;

import com.wynntils.services.map.type.CombatKind;
import com.wynntils.services.mapdata.attributes.AbstractMapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CombatListProvider extends BuiltInProvider {
    private static final List<MapFeature> PROVIDED_FEATURES = new ArrayList<>();
    private static int counter;

    @Override
    public String getProviderId() {
        return "combat-list";
    }

    @Override
    public Stream<MapFeature> getFeatures() {
        return PROVIDED_FEATURES.stream();
    }

    public static void registerFeature(Location location, CombatKind kind, String name) {
        PROVIDED_FEATURES.add(new CombatLocation(location, kind, name));
    }

    private static final class CombatLocation implements MapLocation {
        private final Location location;
        private final CombatKind kind;
        private final String name;
        private final int number;

        private CombatLocation(Location location, CombatKind kind, String name) {
            this.location = location;
            this.kind = kind;
            this.name = name;
            this.number = CombatListProvider.counter++;
        }

        @Override
        public String getFeatureId() {
            return kind.getMapDataId() + "-" + number;
        }

        @Override
        public String getCategoryId() {
            return "wynntils:content:" + kind.getMapDataId();
        }

        @Override
        public MapAttributes getAttributes() {
            return new AbstractMapAttributes() {
                @Override
                public String getLabel() {
                    return name;
                }
            };
        }

        @Override
        public List<String> getTags() {
            return List.of();
        }

        @Override
        public Location getLocation() {
            // FIXME: debug
            return location.offset(15, 0, 15);
        }
    }
}
