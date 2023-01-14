/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.handleditems.annotators.game;

import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.wynn.handleditems.items.game.GearBoxItem;
import com.wynntils.wynn.objects.profiles.item.GearTier;
import com.wynntils.wynn.objects.profiles.item.GearType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GearBoxAnnotator implements ItemAnnotator {
    private static final Pattern GEAR_BOX_PATTERN = Pattern.compile("^§[5abcdef]Unidentified (.*)$");
    private static final Pattern LEVEL_RANGE_PATTERN =
            Pattern.compile("^§a- (?:§r)?§7Lv\\. Range: (?:§r)?§f(\\d+)-(\\d+)$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        if (!(itemStack.getItem() == Items.STONE_SHOVEL
                && itemStack.getDamageValue() >= 1
                && itemStack.getDamageValue() <= 6)) return null;
        Matcher matcher = GEAR_BOX_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        GearType gearType = GearType.fromString(matcher.group(1));
        if (gearType == null) return null;

        GearTier gearTier = GearTier.fromFormattedString(name);
        String levelRange = getLevelRange(itemStack);

        if (gearTier == null || levelRange == null) return null;

        return new GearBoxItem(gearType, gearTier, levelRange);
    }

    private static String getLevelRange(ItemStack itemStack) {
        Matcher matcher = ItemUtils.matchLoreLine(itemStack, 6, LEVEL_RANGE_PATTERN);
        if (!matcher.matches()) return null;
        return matcher.group(1) + "-" + matcher.group(2);
    }
}
