/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GameItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TomeAnnotator implements GameItemAnnotator {
    private static final Pattern TOME_PATTERN = Pattern.compile(
            "^§[5abcdef]((?<Variant>[\\w']+)? ?Tome of (?<Type>\\w+))( (?<Subtype>.+)( (?<Tier>[IVX]{1,4}))?)?$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        if (itemStack.getItem() != Items.IRON_HORSE_ARMOR) return null;
        Matcher matcher = name.getMatcher(TOME_PATTERN);
        if (!matcher.matches()) return null;

        return Models.Rewards.fromTomeItemStack(itemStack, name);
    }
}
