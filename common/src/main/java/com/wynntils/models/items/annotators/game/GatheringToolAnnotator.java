/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GameItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.items.items.game.GatheringToolItem;
import com.wynntils.models.profession.type.ToolProfile;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class GatheringToolAnnotator implements GameItemAnnotator {
    private static final Pattern GATHERING_TOOL_PATTERN =
            Pattern.compile("^§f[ⒸⒷⓀⒿ] Gathering (Axe|Rod|Scythe|Pickaxe) T(\\d+)$");
    private static final Pattern DURABILITY_PATTERN = Pattern.compile("\\[(\\d+)/(\\d+) Durability\\]");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        Matcher matcher = name.getMatcher(GATHERING_TOOL_PATTERN);
        if (!matcher.matches()) return null;

        String toolType = matcher.group(1);
        int tier = Integer.parseInt(matcher.group(2));

        ToolProfile toolProfile = ToolProfile.fromString(toolType, tier);
        if (toolProfile == null) return null;

        CappedValue durability = getDurability(itemStack);

        return new GatheringToolItem(toolProfile, durability);
    }

    private CappedValue getDurability(ItemStack itemStack) {
        List<Component> lore =
                itemStack.getTooltipLines(Item.TooltipContext.of(McUtils.mc().level), null, TooltipFlag.NORMAL);
        for (Component line : lore) {
            Matcher matcher = DURABILITY_PATTERN.matcher(line.getString());
            if (!matcher.find()) continue;

            int currentDurability = Integer.parseInt(matcher.group(1));
            int maxDurability = Integer.parseInt(matcher.group(2));
            return new CappedValue(currentDurability, maxDurability);
        }

        return CappedValue.EMPTY;
    }
}
