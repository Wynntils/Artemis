/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gearinfo.tooltip;

import com.wynntils.models.concepts.Element;
import com.wynntils.models.concepts.Skill;
import com.wynntils.models.gearinfo.type.GearInfo;
import com.wynntils.models.gearinfo.type.GearInstance;
import com.wynntils.models.gearinfo.type.GearRequirements;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class GearTooltipHeader {
    public static List<Component> buildTooltip(GearInfo gearInfo, GearInstance gearInstance, boolean hideUnidentified) {
        List<Component> header = new ArrayList<>();

        String prefix = gearInstance == null && !hideUnidentified ? "Unidentified " : "";

        header.add(Component.literal(prefix + gearInfo.name())
                .withStyle(gearInfo.tier().getChatFormatting()));

        // attack speed
        if (gearInfo.fixedStats().attackSpeed().isPresent())
            header.add(Component.literal(ChatFormatting.GRAY
                    + gearInfo.fixedStats().attackSpeed().get().getName()));

        header.add(Component.literal(""));

        // elemental damages
        if (!gearInfo.fixedStats().damages().isEmpty()) {
            List<Pair<DamageType, RangedValue>> damages = gearInfo.fixedStats().damages();
            for (Pair<DamageType, RangedValue> damageStat : damages) {
                DamageType type = damageStat.key();
                MutableComponent damage = Component.literal(type.getSymbol() + " " + type.getDisplayName())
                        .withStyle(type.getColorCode());
                damage.append(Component.literal("Damage: " + damageStat.value().asString())
                        .withStyle(
                                type == DamageType.NEUTRAL
                                        ? type.getColorCode()
                                        : ChatFormatting.GRAY)); // neutral is all gold
                header.add(damage);
            }

            header.add(Component.literal(""));
        }

        int health = gearInfo.fixedStats().healthBuff();
        if (health != 0) {
            MutableComponent healthComp = Component.literal("❤ Health: " + StringUtils.toSignedString(health))
                    .withStyle(ChatFormatting.DARK_RED);
            header.add(healthComp);
        }

        // elemental defenses
        if (!gearInfo.fixedStats().defences().isEmpty()) {
            List<Pair<Element, Integer>> defenses = gearInfo.fixedStats().defences();
            for (Pair<Element, Integer> defenceStat : defenses) {
                Element element = defenceStat.key();
                MutableComponent defense = Component.literal(element.getSymbol() + " " + element.getDisplayName())
                        .withStyle(element.getColorCode());
                defense.append(Component.literal(" Defence: " + StringUtils.toSignedString(defenceStat.value()))
                        .withStyle(ChatFormatting.GRAY));
                header.add(defense);
            }

            header.add(Component.literal(""));
        }

        // FIXME: Is requirements in the correct order? Also add checks if the requirements
        // are fulfilled...
        // requirements
        GearRequirements requirements = gearInfo.requirements();
        if (requirements.quest().isPresent()) {
            header.add(getRequirement("Quest Req: " + requirements.quest().get()));
        }
        if (requirements.classType().isPresent()) {
            header.add(getRequirement(
                    "Class Req: " + requirements.classType().get().getFullName()));
        }
        if (requirements.level() != 0) {
            header.add(getRequirement("Combat Lv. Min: " + requirements.level()));
        }
        if (!requirements.skills().isEmpty()) {
            for (Pair<Skill, Integer> skillRequirement : requirements.skills()) {
                header.add(
                        getRequirement(skillRequirement.key().getDisplayName() + " Min: " + skillRequirement.value()));
            }
        }

        // FIXME: Only add if we had requirements
        header.add(Component.literal(""));

        // Add delimiter if variables stats will follow
        if (!gearInfo.variableStats().isEmpty()) {
            header.add(Component.literal(""));
        }

        return header;
    }

    private static MutableComponent getRequirement(String requirementName) {
        MutableComponent requirement;
        requirement = Component.literal("✔ ").withStyle(ChatFormatting.GREEN);
        requirement.append(Component.literal(requirementName).withStyle(ChatFormatting.GRAY));
        return requirement;
    }
}
