/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.items.game.TrinketItem;
import com.wynntils.utils.CappedValue;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class TrinketAnnotator implements ItemAnnotator {
    private static final Pattern TRINKET_PATTERN = Pattern.compile("^§[5abcdef]([^\\[]*)( \\[(\\d+)/(\\d+)\\])?$");
    private static final Pattern TRINKET_LORE_PATTERN = Pattern.compile("^§7Right-Click to (use|toggle)$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        Matcher matcher = TRINKET_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        try {
            // Verify by first line of the lore
            Matcher loreMatcher = ItemUtils.matchLoreLine(itemStack, 0, TRINKET_LORE_PATTERN);
            if (!loreMatcher.matches()) return null;

            String trinketName = matcher.group(1);
            GearTier gearTier = GearTier.fromString(name);
            if (matcher.group(3) != null) {
                CappedValue uses =
                        new CappedValue(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
                return new TrinketItem(trinketName, gearTier, uses);
            } else {
                return new TrinketItem(trinketName, gearTier);
            }
        } catch (NoSuchElementException ignored) {
            return null;
        }
    }
}
