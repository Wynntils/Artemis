/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.impl.crafted.components;

import com.wynntils.core.components.Models;
import com.wynntils.handlers.tooltip.impl.crafted.CraftedTooltipComponent;
import com.wynntils.models.activities.quests.QuestInfo;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Element;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CraftedGearTooltipComponent extends CraftedTooltipComponent<CraftedGearItem> {
    @Override
    public List<Component> buildHeaderTooltip(CraftedGearItem craftedItem) {
        List<Component> header = new ArrayList<>();

        // name
        header.add(Component.literal(craftedItem.getName())
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" [" + craftedItem.getEffectStrength() + "%]")
                        .withStyle(ChatFormatting.AQUA)));

        // attack speed
        if (craftedItem.getAttackSpeed().isPresent())
            header.add(Component.literal(
                    ChatFormatting.GRAY + craftedItem.getAttackSpeed().get().getName()));

        header.add(Component.literal(""));

        // elemental damages
        if (!craftedItem.getDamages().isEmpty()) {
            List<Pair<DamageType, RangedValue>> damages = craftedItem.getDamages();
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

        // health
        int health = craftedItem.getHealth();
        if (health != 0) {
            MutableComponent healthComp = Component.literal("❤ Health: " + StringUtils.toSignedString(health))
                    .withStyle(ChatFormatting.DARK_RED);
            header.add(healthComp);
        }

        // elemental defenses
        if (!craftedItem.getDefences().isEmpty()) {
            List<Pair<Element, Integer>> defenses = craftedItem.getDefences();
            for (Pair<Element, Integer> defenceStat : defenses) {
                Element element = defenceStat.key();
                MutableComponent defense = Component.literal(element.getSymbol() + " " + element.getDisplayName())
                        .withStyle(element.getColorCode());
                defense.append(Component.literal(" Defence: " + StringUtils.toSignedString(defenceStat.value()))
                        .withStyle(ChatFormatting.GRAY));
                header.add(defense);
            }
        }

        if (health != 0 || !craftedItem.getDefences().isEmpty()) {
            header.add(Component.literal(""));
        }

        // requirements
        int requirementsCount = 0;
        GearRequirements requirements = craftedItem.getRequirements();
        if (requirements.classType().isPresent()) {
            ClassType classType = requirements.classType().get();
            boolean fulfilled = Models.Character.getClassType() == classType;
            header.add(buildRequirementLine("Class Req: " + classType.getFullName(), fulfilled));
            requirementsCount++;
        }
        if (requirements.quest().isPresent()) {
            String questName = requirements.quest().get();
            Optional<QuestInfo> quest = Models.Quest.getQuestFromName(questName);
            boolean fulfilled = quest.isPresent() && quest.get().status() == ActivityStatus.COMPLETED;
            header.add(buildRequirementLine("Quest Req: " + questName, fulfilled));
            requirementsCount++;
        }
        int level = requirements.level();
        if (level != 0) {
            boolean fulfilled = Models.CombatXp.getCombatLevel().current() >= level;
            header.add(buildRequirementLine("Combat Lv. Min: " + level, fulfilled));
            requirementsCount++;
        }
        if (!requirements.skills().isEmpty()) {
            for (Pair<Skill, Integer> skillRequirement : requirements.skills()) {
                // FIXME: CharacterModel is still missing info about our skill points
                header.add(buildRequirementLine(
                        skillRequirement.key().getDisplayName() + " Min: " + skillRequirement.value(), false));
                requirementsCount++;
            }
        }
        if (requirementsCount > 0) {
            header.add(Component.literal(""));
        }

        return header;
    }

    @Override
    public List<Component> buildFooterTooltip(CraftedGearItem craftedItem) {
        List<Component> footer = new ArrayList<>();

        footer.add(Component.empty());

        // powder slots
        if (!craftedItem.getPowders().isEmpty()) {
            MutableComponent powderLine = Component.literal("["
                            + craftedItem.getPowders().size() + "/" + craftedItem.getPowderSlots() + "] Powder Slots ")
                    .withStyle(ChatFormatting.GRAY);
            if (!craftedItem.getPowders().isEmpty()) {
                MutableComponent powderList = Component.literal("[");
                for (Powder p : craftedItem.getPowders()) {
                    String symbol = p.getColoredSymbol().getString();
                    if (!powderList.getSiblings().isEmpty()) symbol = " " + symbol;
                    powderList.append(Component.literal(symbol));
                }
                powderList.append(Component.literal("]"));
                powderLine.append(powderList);
            }
            footer.add(powderLine);
        }

        // item type + durability
        footer.add(Component.literal("Crafted "
                        + StringUtils.capitalizeFirst(
                                craftedItem.getGearType().name().toLowerCase(Locale.ROOT)))
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" [" + craftedItem.getDurability().current() + "/"
                                + craftedItem.getDurability().max() + " Durability]")
                        .withStyle(ChatFormatting.DARK_GRAY)));

        return footer;
    }
}
