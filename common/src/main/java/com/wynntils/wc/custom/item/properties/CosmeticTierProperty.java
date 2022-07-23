/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item.properties;

import com.wynntils.features.user.inventory.ItemHighlightFeature;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.type.HighlightProperty;
import net.minecraft.ChatFormatting;

public class CosmeticTierProperty extends ItemProperty implements HighlightProperty {
    private final CustomColor highlightColor;

    public CosmeticTierProperty(WynnItemStack item) {
        super(item);

        // parse color
        ChatFormatting chatColor =
                ChatFormatting.getByCode(item.getHoverName().getString().charAt(1));
        if (chatColor == null) chatColor = ChatFormatting.WHITE;

        highlightColor = CustomColor.fromChatFormatting(chatColor);
    }

    @Override
    public boolean isHighlightEnabled() {
        return ItemHighlightFeature.cosmeticHighlightEnabled;
    }

    @Override
    public CustomColor getHighlightColor() {
        return highlightColor;
    }

    @Override
    public boolean isHotbarHighlight() {
        return false;
    }
}
