/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.items.items.game.MiscItem;
import com.wynntils.utils.mc.LoreUtils;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class MiscAnnotator implements ItemAnnotator {
    private static final StyledText UNTRADABLE = StyledText.fromString("§cUntradable Item");
    private static final StyledText QUEST_ITEM = StyledText.fromString("§cQuest Item");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        List<StyledText> lore = LoreUtils.getLore(itemStack);
        if (lore.isEmpty()) return null;

        boolean untradable = false;
        boolean questItem = false;

        for (StyledText line : lore) {
            if (line.equals(UNTRADABLE)) {
                untradable = true;
            }
            if (line.equals(QUEST_ITEM)) {
                questItem = true;
            }
        }

        // If it is neither Untradable nor a Quest Item, we can't be sure that it is not
        // e.g. a GUI element
        if (!untradable && !questItem) return null;

        return new MiscItem(name, untradable, questItem);
    }
}
