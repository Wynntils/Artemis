/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.gui;

import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.items.items.gui.ServerItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class ServerAnnotator implements ItemAnnotator {
    private static final Pattern SERVER_ITEM_PATTERN = Pattern.compile("§[baec]§lWorld (\\d+)(§3 \\(Recommended\\))?");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        Matcher matcher = SERVER_ITEM_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        int serverId = Integer.parseInt(matcher.group(1));

        return new ServerItem(serverId);
    }
}
