/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.impl.identifiable.components;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.tooltip.impl.identifiable.IdentifiableTooltipComponent;
import com.wynntils.models.activities.quests.QuestInfo;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Element;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.gear.type.GearMajorId;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.gear.type.GearRestrictions;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class GearTooltipComponent extends IdentifiableTooltipComponent<GearInfo, GearInstance> {
    private static final int PIXEL_WIDTH = 150;

    @Override
    public List<Component> buildHeaderTooltip(GearInfo gearInfo, GearInstance gearInstance, boolean hideUnidentified) {
        List<Component> header = new ArrayList<>();

        // name
        String unidentifiedPrefix = gearInstance == null && !hideUnidentified ? "Unidentified " : "";
        Component shinyPrefix = gearInstance != null && gearInstance.shinyStat().isPresent()
                ? Component.literal("⬡ ")
                        .withStyle(ChatFormatting.WHITE)
                        .append(Component.literal("Shiny ")
                                .withStyle(gearInfo.tier().getChatFormatting()))
                : Component.empty();
        header.add(Component.empty()
                .withStyle(gearInfo.tier().getChatFormatting())
                .append(Component.literal(unidentifiedPrefix)
                        .append(shinyPrefix)
                        .append(Component.literal(gearInfo.name()))));

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

        // health
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
        }

        if (health != 0 || !gearInfo.fixedStats().defences().isEmpty()) {
            header.add(Component.literal(""));
        }

        // requirements
        int requirementsCount = 0;
        GearRequirements requirements = gearInfo.requirements();
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

        if (gearInstance != null && gearInstance.shinyStat().isPresent()) {
            header.add(Component.literal(
                            "⬡ " + gearInstance.shinyStat().get().statType().displayName() + ": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(
                                    gearInstance.shinyStat().get().value()))
                            .withStyle(ChatFormatting.WHITE)));
            header.add(Component.literal(""));
        }

        return header;
    }

    @Override
    public List<Component> buildFooterTooltip(GearInfo gearInfo, GearInstance gearInstance, boolean showItemType) {
        List<Component> footer = new ArrayList<>();

        // major ids
        if (gearInfo.fixedStats().majorIds().isPresent()) {
            GearMajorId majorId = gearInfo.fixedStats().majorIds().get();

            // The majorId lore contains the name, and colors
            // This dance to and from component is needed to properly recolor all neutral text
            StyledText lore = StyledText.fromComponent(Component.empty()
                    .withStyle(ChatFormatting.DARK_AQUA)
                    .append(majorId.lore().getComponent()));

            Stream.of(RenderedStringUtils.wrapTextBySize(lore, PIXEL_WIDTH)).forEach(c -> footer.add(c.getComponent()));
        }

        footer.add(Component.literal(""));

        // powder slots
        if (gearInfo.powderSlots() > 0) {
            if (gearInstance == null) {
                footer.add(Component.literal("[" + gearInfo.powderSlots() + " Powder Slots]")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent powderLine = Component.literal(
                                "[" + gearInstance.powders().size() + "/" + gearInfo.powderSlots() + "] Powder Slots ")
                        .withStyle(ChatFormatting.GRAY);
                if (!gearInstance.powders().isEmpty()) {
                    MutableComponent powderList = Component.literal("[");
                    for (Powder p : gearInstance.powders()) {
                        String symbol = p.getColoredSymbol().getString();
                        if (!powderList.getSiblings().isEmpty()) symbol = " " + symbol;
                        powderList.append(Component.literal(symbol));
                    }
                    powderList.append(Component.literal("]"));
                    powderLine.append(powderList);
                }
                footer.add(powderLine);
            }
        }

        // tier & rerolls
        GearTier gearTier = gearInfo.tier();
        MutableComponent itemTypeName = showItemType
                ? Component.literal(
                        StringUtils.capitalizeFirst(gearInfo.type().name().toLowerCase(Locale.ROOT)))
                : Component.literal("Item");
        MutableComponent tier = Component.literal(gearTier.getName())
                .withStyle(gearTier.getChatFormatting())
                .append(" ")
                .append(itemTypeName);
        if (gearInstance != null && gearInstance.rerolls() > 1) {
            tier.append(" [" + gearInstance.rerolls() + "]");
        }
        footer.add(tier);

        // restrictions (untradable, quest item)
        if (gearInfo.metaInfo().restrictions() != GearRestrictions.NONE) {
            footer.add(Component.literal(StringUtils.capitalizeFirst(
                            gearInfo.metaInfo().restrictions().getDescription()))
                    .withStyle(ChatFormatting.RED));
        }

        // lore
        Optional<StyledText> lore = gearInfo.metaInfo().lore();
        if (lore.isPresent()) {
            Stream.of(RenderedStringUtils.wrapTextBySize(lore.get(), PIXEL_WIDTH))
                    .forEach(c -> footer.add(c.getComponent().withStyle(ChatFormatting.DARK_GRAY)));
        }

        return footer;
    }
}
