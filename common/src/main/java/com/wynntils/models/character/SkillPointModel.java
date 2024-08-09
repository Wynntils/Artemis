/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.handlers.container.type.ContainerContentChangeType;
import com.wynntils.models.character.type.SavableSkillPointSet;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.models.items.properties.GearTypeItemProperty;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class SkillPointModel extends Model {
    @Persisted
    private final Storage<Map<String, SavableSkillPointSet>> skillPointLoadouts = new Storage<>(new TreeMap<>());

    private static final int TOME_SLOT = 8;
    private static final int[] SKILL_POINT_TOTAL_SLOTS = {11, 12, 13, 14, 15};
    private static final int SKILL_POINT_TOME_SLOT = 4;
    private static final int CONTENT_BOOK_SLOT = 62;
    private static final int TOME_MENU_CONTENT_BOOK_SLOT = 89;

    private Map<Skill, Integer> totalSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> gearSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> craftedSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> tomeSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> statusEffectSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> setBonusSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> assignedSkillPoints = new EnumMap<>(Skill.class);

    public SkillPointModel() {
        super(List.of());
    }

    public boolean hasLoadout(String name) {
        return skillPointLoadouts.get().containsKey(name);
    }

    public void saveSkillPoints(String name, int[] skillPoints) {
        SavableSkillPointSet assignedSkillPointSet = new SavableSkillPointSet(skillPoints);
        skillPointLoadouts.get().put(name, assignedSkillPointSet);
        WynntilsMod.info("Saved skill point loadout: " + name + " " + assignedSkillPointSet);
    }

    /**
     * Saves only the current assigned skill points to the loadout list.
     */
    public void saveCurrentSkillPoints(String name) {
        saveSkillPoints(name, new int[] {
            getAssignedSkillPoints(Skill.STRENGTH),
            getAssignedSkillPoints(Skill.DEXTERITY),
            getAssignedSkillPoints(Skill.INTELLIGENCE),
            getAssignedSkillPoints(Skill.DEFENCE),
            getAssignedSkillPoints(Skill.AGILITY)
        });
    }

    /**
     * Saves the current equipped gear and provided skill points.
     */
    public void saveBuild(String name, int[] skillPoints) {
        String weapon = null;
        List<String> armourNames = new ArrayList<>();
        List<String> accessoryNames = new ArrayList<>();

        for (ItemStack itemStack : Models.Inventory.getEquippedItems()) {
            Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
            if (wynnItemOptional.isEmpty()) continue;
            WynnItem wynnItem = wynnItemOptional.get();

            if (wynnItem instanceof GearTypeItemProperty gear) {
                if (gear.getGearType().isArmor()) {
                    armourNames.add(
                            StyledText.fromComponent(itemStack.getHoverName()).getString());
                } else if (gear.getGearType().isAccessory()) {
                    accessoryNames.add(
                            StyledText.fromComponent(itemStack.getHoverName()).getString());
                } else if (gear.getGearType().isWeapon()) {
                    weapon = StyledText.fromComponent(itemStack.getHoverName()).getString();
                }
            }
        }

        SavableSkillPointSet assignedSkillPointSet =
                new SavableSkillPointSet(skillPoints, weapon, armourNames, accessoryNames);
        skillPointLoadouts.get().put(name, assignedSkillPointSet);
        WynntilsMod.info("Saved skill point build: " + name + " " + assignedSkillPointSet);
    }

    public void saveCurrentBuild(String name) {
        saveBuild(name, new int[] {
            getAssignedSkillPoints(Skill.STRENGTH),
            getAssignedSkillPoints(Skill.DEXTERITY),
            getAssignedSkillPoints(Skill.INTELLIGENCE),
            getAssignedSkillPoints(Skill.DEFENCE),
            getAssignedSkillPoints(Skill.AGILITY)
        });
    }

    public void deleteLoadout(String name) {
        skillPointLoadouts.get().remove(name);
    }

    public void loadLoadout(String name) {
        ContainerUtils.closeBackgroundContainer();

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Loading Skill Point Loadout Query")
                .onError(msg -> WynntilsMod.warn("Failed to load skill point loadout: " + msg))
                .then(QueryStep.useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer((container) -> loadSkillPointsOnServer(container, name)))
                .build();
        query.executeQuery();
    }

    /**
     * Closes any open containers (but not the screen shown) then queries compass (and tome menus) depending on if
     * tomes have been unlocked.
     */
    public void populateSkillPoints() {
        ContainerUtils.closeBackgroundContainer();

        Managers.TickScheduler.scheduleNextTick(() -> {
            assignedSkillPoints = new EnumMap<>(Skill.class);
            calculateGearSkillPoints();
            calculateStatusEffectSkillPoints();
            queryTotalAndTomeSkillPoints();
        });
    }

    public int getTotalSkillPoints(Skill skill) {
        return totalSkillPoints.getOrDefault(skill, 0);
    }

    public int getTotalSum() {
        return totalSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getGearSkillPoints(Skill skill) {
        return gearSkillPoints.getOrDefault(skill, 0);
    }

    public int getGearSum() {
        return gearSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getCraftedSkillPoints(Skill skill) {
        return craftedSkillPoints.getOrDefault(skill, 0);
    }

    public int getCraftedSum() {
        return craftedSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getTomeSkillPoints(Skill skill) {
        return tomeSkillPoints.getOrDefault(skill, 0);
    }

    public int getTomeSum() {
        return tomeSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getStatusEffectSkillPoints(Skill skill) {
        return statusEffectSkillPoints.getOrDefault(skill, 0);
    }

    public int getStatusEffectsSum() {
        return statusEffectSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getSetBonusSkillPoints(Skill skill) {
        return setBonusSkillPoints.getOrDefault(skill, 0);
    }

    public int getSetBonusSum() {
        return setBonusSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public int getAssignedSkillPoints(Skill skill) {
        return assignedSkillPoints.getOrDefault(skill, 0);
    }

    public int getAssignedSum() {
        return assignedSkillPoints.values().stream().reduce(0, Integer::sum);
    }

    public Map<String, SavableSkillPointSet> getLoadouts() {
        return skillPointLoadouts.get();
    }

    /**
     * @return true if any skills are assigned outside of the 0-100 range.
     */
    public boolean hasIllegalAssigned() {
        for (Skill skill : Skill.values()) {
            if (getAssignedSkillPoints(skill) < 0 || getAssignedSkillPoints(skill) > 100) {
                return true;
            }
        }
        return false;
    }

    private void loadSkillPointsOnServer(ContainerContent containerContent, String name) {
        // we need to figure out which points we can subtract from first to actually allow assigning for positive points
        Map<Skill, Integer> negatives = new EnumMap<>(Skill.class);
        Map<Skill, Integer> positives = new EnumMap<>(Skill.class);
        for (int i = 0; i < Skill.values().length; i++) {
            int buildTarget = skillPointLoadouts.get().get(name).getSkillPointsAsArray()[i];
            int difference = buildTarget - getAssignedSkillPoints(Skill.values()[i]);

            // no difference automatically dropped here
            if (difference > 0) {
                positives.put(Skill.values()[i], difference);
            } else if (difference < 0) {
                negatives.put(Skill.values()[i], difference);
            }
        }

        boolean confirmationCompleted = false;
        for (Map.Entry<Skill, Integer> entry : negatives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // confirmation required, force loop to repeat this iteration
                    i--;
                    confirmationCompleted = true;
                }
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // needs to exist in both loops in case of 1s only
                    i--;
                    confirmationCompleted = true;
                }
            }
        }

        for (Map.Entry<Skill, Integer> entry : positives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
        }

        // Server needs 2 ticks, give a couple extra to be safe
        Managers.TickScheduler.scheduleLater(this::populateSkillPoints, 4);
    }

    private void calculateGearSkillPoints() {
        gearSkillPoints = new EnumMap<>(Skill.class);
        craftedSkillPoints = new EnumMap<>(Skill.class);
        setBonusSkillPoints = new EnumMap<>(Skill.class);

        for (ItemStack itemStack : Models.Inventory.getEquippedItems()) {
            calculateSingleGearSkillPoints(itemStack);
        }

        Models.Set.getUniqueSetNames().forEach(name -> {
            int trueCount = Models.Set.getTrueCount(name);
            Models.Set.getSetInfo(name).getBonusForItems(trueCount).forEach((bonus, value) -> {
                if (bonus instanceof SkillStatType skillStat) {
                    setBonusSkillPoints.merge(skillStat.getSkill(), value, Integer::sum);
                }
            });
        });
    }

    private void calculateSingleGearSkillPoints(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isEmpty()) return; // Empty slot

        if (wynnItemOptional.get() instanceof GearItem gear) {
            gear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    gearSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });

        } else if (wynnItemOptional.get() instanceof CraftedGearItem craftedGear) {
            craftedGear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    craftedSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse gear: " + LoreUtils.getStringLore(itemStack));
        }
    }

    /**
     * Queries the compass (and tome) menu for skill point data.
     */
    private void queryTotalAndTomeSkillPoints() {
        totalSkillPoints = new EnumMap<>(Skill.class);
        tomeSkillPoints = new EnumMap<>(Skill.class);

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Total and Tome Skill Point Query")
                .onError(msg -> WynntilsMod.warn("Failed to query skill points: " + msg))
                .then(QueryStep.useItemInHotbar(CharacterModel.CHARACTER_INFO_SLOT)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer(this::processTotalSkillPoints))
                .conditionalThen(
                        this::checkTomesUnlocked,
                        QueryStep.clickOnSlot(TOME_SLOT)
                                .expectContainerTitle(ContainerModel.MASTERY_TOMES_NAME)
                                .verifyContentChange((container, changes, changeType) ->
                                        verifyChange(container, changes, changeType, TOME_MENU_CONTENT_BOOK_SLOT))
                                .processIncomingContainer(this::processTomeSkillPoints))
                .execute(this::calculateAssignedSkillPoints)
                .build();

        query.executeQuery();
    }

    private boolean checkTomesUnlocked(ContainerContent content) {
        return LoreUtils.getStringLore(content.items().get(TOME_SLOT)).contains("✔");
    }

    private boolean verifyChange(
            ContainerContent content,
            Int2ObjectFunction<ItemStack> changes,
            ContainerContentChangeType changeType,
            int contentBookSlot) {
        // soul points resent last for both containers
        return changeType == ContainerContentChangeType.SET_CONTENT
                && changes.containsKey(contentBookSlot)
                && (content.items().get(contentBookSlot).getItem() == Items.PAPER);
    }

    private void processTotalSkillPoints(ContainerContent content) {
        for (Integer slot : SKILL_POINT_TOTAL_SLOTS) {
            Optional<WynnItem> wynnItemOptional =
                    Models.Item.getWynnItem(content.items().get(slot));
            if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof SkillPointItem skillPoint) {
                totalSkillPoints.merge(skillPoint.getSkill(), skillPoint.getSkillPoints(), Integer::sum);
            } else {
                WynntilsMod.warn("Skill Point Model failed to parse skill point item: "
                        + LoreUtils.getStringLore(content.items().get(slot)));
            }
        }
    }

    private void processTomeSkillPoints(ContainerContent content) {
        ItemStack itemStack = content.items().get(SKILL_POINT_TOME_SLOT);
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof TomeItem tome) {
            tome.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    tomeSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else if (LoreUtils.getStringLore(itemStack).contains("§6Requirements:")) {
            // no-op, this is a tome that has not been unlocked or is not used by the player
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse tome: "
                    + LoreUtils.getStringLore(content.items().get(SKILL_POINT_TOME_SLOT)));
        }
    }

    private void calculateStatusEffectSkillPoints() {
        statusEffectSkillPoints = new EnumMap<>(Skill.class);
        Models.StatusEffect.getStatusEffects().forEach(statusEffect -> {
            for (Skill skill : Skill.values()) {
                if (statusEffect.getName().contains(skill.getDisplayName())) {
                    statusEffectSkillPoints.merge(
                            skill,
                            Integer.parseInt(statusEffect.getModifier().getStringWithoutFormatting()),
                            Integer::sum);
                }
            }
        });
    }

    private void calculateAssignedSkillPoints() {
        for (Skill skill : Skill.values()) {
            assignedSkillPoints.put(
                    skill,
                    getTotalSkillPoints(skill)
                            - getGearSkillPoints(skill)
                            - getSetBonusSkillPoints(skill)
                            - getTomeSkillPoints(skill)
                            - getCraftedSkillPoints(skill)
                            - getStatusEffectSkillPoints(skill));
        }
    }
}
