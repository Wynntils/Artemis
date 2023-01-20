/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.gui;

import com.wynntils.models.concepts.Skill;
import com.wynntils.models.items.properties.CountedItemProperty;
import com.wynntils.utils.CustomColor;

public class SkillPointItem extends GuiItem implements CountedItemProperty {
    private final Skill skill;
    private final int skillPoints;

    public SkillPointItem(Skill skill, int skillPoints) {
        this.skill = skill;
        this.skillPoints = skillPoints;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public int getCount() {
        return skillPoints;
    }

    @Override
    public CustomColor getCountColor() {
        return CustomColor.fromChatFormatting(skill.getColor());
    }

    @Override
    public String toString() {
        return "SkillPointItem{" + "skill=" + skill + ", skillPoints=" + skillPoints + '}';
    }
}
