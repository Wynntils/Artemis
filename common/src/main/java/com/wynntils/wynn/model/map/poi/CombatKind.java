/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.map.poi;

import com.wynntils.gui.render.Texture;
import java.util.Arrays;

public enum CombatKind {
    BOSS_ALTARS("Boss Altars", Texture.BOSS_ALTAR),
    CAVES("Caves", Texture.CAVE),
    DUNGEONS("Dungeons", Texture.DUNGEON_ENTRANCE),
    GRIND_SPOTS("Grind Spots", Texture.GRIND_SPOT),
    RAIDS("Raids", Texture.RAID_ENTRANCE),
    // TODO: We only have sprites for Uth Shrines, so those are the only ones that we have
    // location markers for. Split up the category when more textures are available.
    RUNE_SHRINES("Rune Shrines", Texture.UTH_SHRINE);

    private final String name;
    private final Texture texture;

    CombatKind(String name, Texture texture) {
        this.name = name;
        this.texture = texture;
    }

    public String getName() {
        return name;
    }

    public Texture getIcon() {
        return texture;
    }

    public static CombatKind fromString(String str) {
        return Arrays.stream(values())
                .filter(kind -> kind.getName().equals(str))
                .findFirst()
                .orElse(null);
    }
}
