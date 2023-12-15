/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.skillpoint;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public class SkillPointModel extends Model {
    private static final int TOME_SLOT = 8;
    private static final int[] SKILL_POINT_TOME_SLOTS = {4, 11, 19};

    private final Map<Skill, Integer> totalSkillPoints = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> gearSkillPoints = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> tomeSkillPoints = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> assignedSkillPoints = new EnumMap<>(Skill.class);

    public SkillPointModel() {
        super(List.of());
    }

    public void updateTotals(ItemStack[] skillPointItems) {
        for (int i = 0; i < 5; i++) {
            Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(skillPointItems[i]);
            if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof SkillPointItem skillPoint) {
                totalSkillPoints.put(skillPoint.getSkill(), skillPoint.getSkillPoints());
            } else {
                WynntilsMod.warn("Failed to parse skill point item: " + LoreUtils.getStringLore(skillPointItems[i]));
            }
        }
    }

    public void calculateAssignedSkillPoints() {
        //        McUtils.player().closeContainer();

        Managers.TickScheduler.scheduleNextTick(() -> {
            calculateGearSkillPoints();
            queryTomeSkillPoints();
        });
    }

    private void calculateGearSkillPoints() {
        // fixme: these can be crafted, so we need to check for that
        gearSkillPoints.clear();
        McUtils.inventory().armor.forEach(itemStack -> {
            Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
            if (wynnItemOptional.isEmpty()) return; // Empty slot

            if (wynnItemOptional.get() instanceof GearItem gear) {
                gear.getIdentifications().stream()
                        .filter(x -> x.statType() instanceof SkillStatType)
                        .forEach(x -> gearSkillPoints.merge(
                                ((SkillStatType) x.statType()).getSkill(), x.value(), Integer::sum));
            } else {
                WynntilsMod.warn("Failed to parse armour: " + LoreUtils.getStringLore(itemStack));
            }
        });

        for (int i = 9; i <= 12; i++) {
            Optional<WynnItem> wynnItemOptional =
                    Models.Item.getWynnItem(McUtils.inventory().getItem(i));
            if (wynnItemOptional.isEmpty()) continue; // Empty slot

            if (wynnItemOptional.get() instanceof GearItem gear) {
                gear.getIdentifications().stream()
                        .filter(x -> x.statType() instanceof SkillStatType)
                        .forEach(x -> gearSkillPoints.merge(
                                ((SkillStatType) x.statType()).getSkill(), x.value(), Integer::sum));
            } else {
                WynntilsMod.warn("Failed to parse accessory: "
                        + LoreUtils.getStringLore(McUtils.inventory().getItem(i)));
            }
        }
    }

    private void queryTomeSkillPoints() {
        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Tome Skill Point Query")
                .onError(msg -> WynntilsMod.warn("Failed to query tome skill points: " + msg))
                .then(QueryStep.useItemInHotbar(CharacterModel.CHARACTER_INFO_SLOT - 1)
                        .expectContainerTitle("Character Info"))
                .then(QueryStep.clickOnSlot(TOME_SLOT)
                        .expectContainerTitle("Mastery Tomes")
                        .processIncomingContainer(this::processTomeSkillPoints))
                .build();

        query.executeQuery();
    }

    private void processTomeSkillPoints(ContainerContent content) {
        System.out.println("querying tomes");
        tomeSkillPoints.clear();
        for (Integer slot : SKILL_POINT_TOME_SLOTS) {
            Optional<WynnItem> wynnItemOptional =
                    Models.Item.getWynnItem(content.items().get(slot));
            if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof TomeItem tome) {
                tome.getIdentifications().stream()
                        .filter(x -> x.statType() instanceof SkillStatType)
                        .forEach(x -> tomeSkillPoints.merge(
                                ((SkillStatType) x.statType()).getSkill(), x.value(), Integer::sum));
            } else {
                WynntilsMod.warn("Failed to parse tome: "
                        + LoreUtils.getStringLore(content.items().get(slot)));
            }
        }
    }

    public int getTotalSkillPoints(Skill skill) {
        return totalSkillPoints.getOrDefault(skill, 0);
    }

    public int getGearSkillPoints(Skill skill) {
        return gearSkillPoints.getOrDefault(skill, 0);
    }

    public int getTomeSkillPoints(Skill skill) {
        return tomeSkillPoints.getOrDefault(skill, 0);
    }

    public int getAssignedSkillPoints(Skill skill) {
        return assignedSkillPoints.getOrDefault(skill, 0);
    }
}
