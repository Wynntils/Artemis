/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.models.emeralds.type.EmeraldSymbols;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.utils.MathUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EmeraldPouchAnnotator implements ItemAnnotator {
    private static final Pattern EMERALD_POUCH_PATTERN = Pattern.compile("^§aEmerald Pouch§2 \\[Tier ([IVX]{1,4})\\]$");
    private static final Pattern EMERALD_POUCH_LORE_PATTERN =
            Pattern.compile("§6§l([\\d\\s]+)" + EmeraldSymbols.E_STRING + ".*");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        // Checks for normal emerald pouch (diamond axe) and emerald pouch pickup texture (gold shovel)
        if (itemStack.getItem() != Items.DIAMOND_AXE && itemStack.getItem() != Items.GOLDEN_SHOVEL) return null;

        Matcher matcher = EMERALD_POUCH_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        int tier = MathUtils.integerFromRoman(matcher.group(1));

        Matcher amountMatcher = ItemUtils.matchLoreLine(itemStack, 0, EMERALD_POUCH_LORE_PATTERN);
        // This can be an emerald pouch on the trade market, it has no amount line
        if (!amountMatcher.matches()) return new EmeraldPouchItem(tier, 0);

        int amount = Integer.parseInt(amountMatcher.group(1).replaceAll("\\s", ""));

        return new EmeraldPouchItem(tier, amount);
    }
}
