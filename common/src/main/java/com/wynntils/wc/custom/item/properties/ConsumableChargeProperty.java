/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item.properties;

import com.wynntils.features.user.inventory.ItemTextOverlayFeature;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.type.TextOverlayProperty;
import com.wynntils.wc.utils.WynnItemMatchers;
import java.util.regex.Matcher;

public class ConsumableChargeProperty extends ItemProperty implements TextOverlayProperty {
    private final TextOverlay textOverlay;

    public ConsumableChargeProperty(WynnItemStack item) {
        super(item);

        // parse charge
        String charges = "";
        Matcher consumableMatcher = WynnItemMatchers.consumableNameMatcher(item.getHoverName());
        if (consumableMatcher.matches()) {
            charges = consumableMatcher.group(2);
        }

        int xOffset = 17 - McUtils.mc().font.width(charges);
        textOverlay = new TextOverlay(
                charges, CommonColors.WHITE, ItemTextOverlayFeature.consumableChargeShadow, xOffset, 9, 1f);
    }

    @Override
    public boolean isTextOverlayEnabled() {
        return ItemTextOverlayFeature.consumableChargeEnabled;
    }

    @Override
    public boolean isHotbarText() {
        return true;
    }

    @Override
    public TextOverlay getTextOverlay() {
        return textOverlay;
    }
}
