/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.components.Models;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.gear.type.CharmProfile;
import com.wynntils.models.gear.type.GearTier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class CharmAnnotator implements ItemAnnotator {
    private static final Pattern CHARM_PATTERN = Pattern.compile("^§[5abcdef](Charm of the (?<Type>\\w+))$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        Matcher matcher = CHARM_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        GearTier tier = GearTier.fromFormattedString(name);
        String type = matcher.group("Type");

        // TODO: replace with API lookup
        CharmProfile charmProfile = new CharmProfile(matcher.group(1), tier, type);

        return Models.GearItem.fromCharmItemStack(itemStack, charmProfile);
    }
}
