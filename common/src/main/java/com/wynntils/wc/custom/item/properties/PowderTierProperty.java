/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item.properties;

import com.wynntils.features.user.inventory.ItemHighlightFeature;
import com.wynntils.features.user.inventory.ItemTextOverlayFeature;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.type.HighlightProperty;
import com.wynntils.wc.custom.item.properties.type.TextOverlayProperty;
import com.wynntils.wc.utils.WynnItemMatchers;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;

public class PowderTierProperty extends ItemProperty implements HighlightProperty, TextOverlayProperty {
    private final CustomColor highlightColor;

    private final TextOverlay textOverlay;

    public PowderTierProperty(WynnItemStack item) {
        super(item);

        // parse color
        ChatFormatting chatColor =
                ChatFormatting.getByCode(item.getHoverName().getString().charAt(1));
        if (chatColor == null) chatColor = ChatFormatting.WHITE;

        highlightColor = CustomColor.fromChatFormatting(chatColor);

        // parse tier
        Matcher powderMatcher = WynnItemMatchers.powderNameMatcher(item.getHoverName());
        String powderNumeral = "I";
        if (powderMatcher.matches()) {
            powderNumeral = powderMatcher.group(2);
        }

        // convert from roman to arabic if necessary
        String text = ItemTextOverlayFeature.powderTierRomanNumerals
                ? powderNumeral
                : "" + MathUtils.integerFromRoman(powderNumeral);

        textOverlay = new TextOverlay(text, highlightColor, ItemTextOverlayFeature.powderTierShadow, -1, 1, 0.75f);
    }

    @Override
    public boolean isHighlightEnabled() {
        return ItemHighlightFeature.powderHighlightEnabled;
    }

    @Override
    public CustomColor getHighlightColor() {
        return highlightColor;
    }

    @Override
    public boolean isTextOverlayEnabled() {
        return ItemTextOverlayFeature.powderTierEnabled;
    }

    @Override
    public TextOverlay getTextOverlay() {
        return textOverlay;
    }
}
