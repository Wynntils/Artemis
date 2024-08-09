/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GameItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.items.game.AspectItem;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.mc.LoreUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

public class AspectAnnotator implements GameItemAnnotator {
    private static final Pattern ASPECT_PATTERN = Pattern.compile("^§(.)(?:(?:(.+) Embodiment)|(?:Aspect)) of .*$");
    private static final Pattern CLASS_PATTERN = Pattern.compile("^§(?:c✖|a✔) §7Class Req: §f(?<name>.+)$");
    private static final Pattern TIER_PATTERN = Pattern.compile("§.Tier ([IV]+).*");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        Matcher matcher = name.getMatcher(ASPECT_PATTERN);
        if (!matcher.matches()) return null;

        ClassType classType = null;
        int tier = 0;

        for (StyledText lore : LoreUtils.getLore(itemStack)) {
            Matcher tierMatcher = lore.getMatcher(TIER_PATTERN);
            if (tierMatcher.find()) {
                tier = MathUtils.integerFromRoman(tierMatcher.group(1));
                continue;
            }

            Matcher classMatcher = lore.getMatcher(CLASS_PATTERN);

            if (classMatcher.matches()) {
                classType = ClassType.fromName(classMatcher.group(1));
                break;
            }
        }

        if (classType == null || tier == 0) {
            WynntilsMod.warn("Failed to get class/tier from aspect: " + LoreUtils.getLore(itemStack));
            return null;
        }

        char colorChar = matcher.group(1).charAt(0);
        GearTier gearTier = GearTier.fromChatFormatting(ChatFormatting.getByCode(colorChar));

        if (gearTier == null) {
            WynntilsMod.warn("Failed to get GearTier from aspect: " + name);
            return null;
        }

        return new AspectItem(classType, gearTier, tier);
    }
}
