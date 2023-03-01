/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.wynnitem.parsing.WynnItemParseResult;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.wynn.WynnItemMatchers;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class CraftedConsumableAnnotator implements ItemAnnotator {
    private static final Pattern CRAFTED_CONSUMABLE_PATTERN = Pattern.compile("^§3(.*)§b \\[(\\d+)/(\\d+)\\]$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        Matcher matcher = CRAFTED_CONSUMABLE_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        String craftedName = matcher.group(1);
        int uses = Integer.parseInt(matcher.group(2));
        int maxUses = Integer.parseInt(matcher.group(3));

        Integer level = WynnItemMatchers.getLevelReq(itemStack, 3);
        if (level == null) return null;

        WynnItemParseResult parseResult = WynnItemParser.parseItemStack(itemStack, null);

        return new CraftedConsumableItem(
                craftedName,
                level,
                parseResult.identifications(),
                parseResult.effects(),
                new CappedValue(uses, maxUses));
    }
}
