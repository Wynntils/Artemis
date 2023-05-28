/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.gui;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.items.items.gui.CosmeticItem;
import com.wynntils.utils.mc.LoreUtils;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CosmeticTierAnnotator implements ItemAnnotator {
    private static final Pattern COSMETIC_PATTERN =
            Pattern.compile("(Common|Rare|Epic|Godly|\\|\\|\\| Black Market \\|\\|\\|) Reward");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        if (!isCosmetic(itemStack)) return null;
        return new CosmeticItem(name.getStyleAt(0).getColor());
    }

    private static boolean isCosmetic(ItemStack itemStack) {
        for (Component c : LoreUtils.getTooltipLines(itemStack)) {
            if (COSMETIC_PATTERN.matcher(c.getString()).matches()) return true;
        }
        return false;
    }
}
