/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.type;

import com.wynntils.utils.render.Texture;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.Screen;

public enum LootChestTier {
    TIER_1(Texture.CHEST_T1, 1, Pattern.compile("Loot Chest §7\\[§f✫§8✫✫✫§7\\]")),
    TIER_2(Texture.CHEST_T2, 2, Pattern.compile("Loot Chest §e\\[§6✫✫§8✫✫§e\\]")),
    TIER_3(Texture.CHEST_T3, 3, Pattern.compile("Loot Chest §5\\[§d✫✫✫§8✫§5\\]")),
    TIER_4(Texture.CHEST_T4, 4, Pattern.compile("Loot Chest §3\\[§b✫✫✫✫§3\\]"));

    private final Texture waypointTexture;
    private final int waypointTier;
    private final Pattern titlePattern;

    LootChestTier(Texture waypointTexture, int waypointTier, Pattern titlePattern) {
        this.waypointTexture = waypointTexture;
        this.waypointTier = waypointTier;
        this.titlePattern = titlePattern;
    }

    public Texture getWaypointTexture() {
        return waypointTexture;
    }

    public int getWaypointTier() {
        return waypointTier;
    }

    public String getWaypointName() {
        return "Loot Chest " + waypointTier;
    }

    public Pattern getTitlePattern() {
        return titlePattern;
    }

    public static LootChestTier fromTitle(Screen screen) {
        for (LootChestTier chestType : values()) {
            if (chestType
                    .getTitlePattern()
                    .matcher(screen.getTitle().getString())
                    .matches()) {
                return chestType;
            }
        }

        return null;
    }
}
