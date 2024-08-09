/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GameItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.items.items.game.TeleportScrollItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class TeleportScrollAnnotator implements GameItemAnnotator {
    private static final Pattern TELEPORT_SCROLL_PATTERN =
            Pattern.compile("^§#8193ffff(.*) Teleportation Scroll §#f9e79eff\\[(\\d)/(\\d)]$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        Matcher nameMatcher = name.getMatcher(TELEPORT_SCROLL_PATTERN);
        if (!nameMatcher.matches()) return null;

        String scrollName = nameMatcher.group(1);
        int remainingCharges = Integer.parseInt(nameMatcher.group(2));

        if (scrollName.equals("the Forgery")) {
            return new TeleportScrollItem("For", true, remainingCharges);
        }

        String destination = Services.Destination.getAbbreviation(scrollName);
        return new TeleportScrollItem(destination, false, remainingCharges);
    }
}
