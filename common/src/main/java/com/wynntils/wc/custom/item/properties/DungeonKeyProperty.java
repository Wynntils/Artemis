/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item.properties;

import com.wynntils.features.user.inventory.ItemTextOverlayFeature;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.type.TextOverlayProperty;
import com.wynntils.wc.utils.WynnItemMatchers;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;

public class DungeonKeyProperty extends ItemProperty implements TextOverlayProperty {
    private static final CustomColor STANDARD_COLOR = CustomColor.fromChatFormatting(ChatFormatting.GOLD);
    private static final CustomColor CORRUPTED_COLOR = CustomColor.fromChatFormatting(ChatFormatting.DARK_RED);

    private final TextOverlay textOverlay;

    public DungeonKeyProperty(WynnItemStack item) {
        super(item);

        // parse dungeon name & type
        CustomColor textColor = STANDARD_COLOR;
        String dungeon = "";

        Matcher keyMatcher = WynnItemMatchers.dungeonKeyNameMatcher(item.getHoverName());
        if (keyMatcher.matches()) {
            String name = keyMatcher.group();
            if (name.contains("Corrupted") || name.contains("Broken")) {
                textColor = CORRUPTED_COLOR;
            }

            dungeon = keyMatcher.group(1).substring(0, 1);
        }

        textOverlay = new TextOverlay(dungeon, textColor, ItemTextOverlayFeature.dungeonKeyShadow, -1, 1, 1f);
    }

    @Override
    public boolean isTextOverlayEnabled() {
        return ItemTextOverlayFeature.dungeonKeyEnabled;
    }

    @Override
    public TextOverlay getTextOverlay() {
        return textOverlay;
    }
}
