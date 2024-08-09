/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.gui;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.GuiItemAnnotator;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.utils.mc.LoreUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class SkillPointAnnotator implements GuiItemAnnotator {
    // Test in SkillPointAnnotator_SKILL_POINT_PATTERN
    private static final Pattern SKILL_POINT_PATTERN = Pattern.compile("^§dUpgrade your §[2ebcf][✤✦❉✹❋] (.*)§d skill$");
    // Test in SkillPointAnnotator_LORE_PATTERN
    private static final Pattern LORE_PATTERN = Pattern.compile("^.*§7(-?\\d+) points§r.*§6-?\\d+ points$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
        Matcher matcher = name.getMatcher(SKILL_POINT_PATTERN);
        if (!matcher.matches()) return null;

        String skillName = matcher.group(1);
        Skill skill = Skill.fromString(skillName);

        Matcher m = LoreUtils.matchLoreLine(itemStack, 3, LORE_PATTERN);
        if (!m.matches()) return null;

        int skillPoints = Integer.parseInt(m.group(1));

        return new SkillPointItem(skill, skillPoints);
    }
}
