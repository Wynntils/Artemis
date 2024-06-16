/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.features;

import com.wynntils.services.mapdata.providers.json.JsonMapAttributes;
import com.wynntils.services.mapdata.providers.json.JsonMapLocation;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.Texture;
import java.util.Arrays;

public final class CombatLocation extends JsonMapLocation {
    public CombatLocation(String featureId, String categoryId, JsonMapAttributes attributes, Location location) {
        super(featureId, categoryId, attributes, location);
    }

    public enum CombatKind {
        BOSS_ALTARS("Boss Altars", Texture.BOSS_ALTAR, "boss-altar"),
        CAVES("Caves", Texture.CAVE, "cave"),
        DUNGEONS("Dungeons", Texture.DUNGEON_ENTRANCE, "dungeon"),
        LOOTRUN_CAMPS("Lootrun Camps", Texture.LOOTRUN_CAMP, "lootrun-camp"),
        GRIND_SPOTS("Grind Spots", Texture.GRIND_SPOT, "grind-spot"),
        RAIDS("Raids", Texture.RAID_ENTRANCE, "raid"),
        RUNE_SHRINES("Rune Shrines", Texture.SHRINE, "shrine");

        private final String name;
        private final Texture texture;
        private final String mapDataId;

        CombatKind(String name, Texture texture, String mapDataId) {
            this.name = name;
            this.texture = texture;
            this.mapDataId = mapDataId;
        }

        public String getName() {
            return name;
        }

        public Texture getIcon() {
            return texture;
        }

        public String getMapDataId() {
            return mapDataId;
        }

        public static CombatKind fromString(String str) {
            return Arrays.stream(values())
                    .filter(kind -> kind.getName().equals(str))
                    .findFirst()
                    .orElse(null);
        }
    }
}
