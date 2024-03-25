/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map;

import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.RangedValue;

public class Label {
    private final String name;
    private final int x;
    private final int z;
    private final int layer;

    // level is provided as a string for some data sources
    private final String levelString;
    private final transient RangedValue level;

    public Label(String name, int x, int z, int layer, String levelString) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.layer = layer;
        this.levelString = levelString;
        this.level = RangedValue.fromStringSafe(levelString);
    }

    public Label(String name, int x, int z, int layer, RangedValue level) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.layer = layer;
        this.levelString = level.asString();
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * The relative importance of this place
     */
    public LabelLayer getLayer() {
        return LabelLayer.values()[layer - 1];
    }

    /**
     * The recommended minimum combat level for visiting this place
     */
    public String getLevelString() {
        if (levelString == null) return "";

        return levelString;
    }

    public RangedValue getCombatLevel() {
        if (level == null) return RangedValue.NONE;

        return level;
    }

    public Location getLocation() {
        return new Location(x, 0, z);
    }

    public enum LabelLayer {
        PROVINCE("province"),
        CITY("city"),
        TOWN_OR_PLACE("place");

        private final String mapDataId;

        LabelLayer(String mapDataId) {
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
