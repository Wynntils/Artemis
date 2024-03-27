/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.game;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Element;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.gear.type.GearAttackSpeed;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.properties.CraftedItemProperty;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.GearTypeItemProperty;
import com.wynntils.models.items.properties.LeveledItemProperty;
import com.wynntils.models.items.properties.PowderedItemProperty;
import com.wynntils.models.items.properties.RequirementItemProperty;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.List;
import java.util.Optional;

public class CraftedGearItem extends GameItem
        implements GearTierItemProperty,
                GearTypeItemProperty,
                DurableItemProperty,
                LeveledItemProperty,
                PowderedItemProperty,
                CraftedItemProperty,
                RequirementItemProperty {
    private final String name;
    private final int effectStrength;
    private final GearType gearType;
    private final GearAttackSpeed attackSpeed;
    private final int health;
    private final List<Pair<DamageType, RangedValue>> damages;
    private final List<Pair<Element, Integer>> defences;
    private final GearRequirements requirements;
    private final List<StatPossibleValues> possibleValues;
    private final List<StatActualValue> identifications;
    private final List<Powder> powders;
    private final int powderSlots;
    private final CappedValue durability;

    public CraftedGearItem(
            String name,
            int effectStrength,
            GearType gearType,
            GearAttackSpeed attackSpeed,
            int health,
            List<Pair<DamageType, RangedValue>> damages,
            List<Pair<Element, Integer>> defences,
            GearRequirements requirements,
            List<StatPossibleValues> possibleValues,
            List<StatActualValue> identifications,
            List<Powder> powders,
            int powderSlots,
            CappedValue durability) {
        this.name = name;
        this.effectStrength = effectStrength;
        this.gearType = gearType;
        this.attackSpeed = attackSpeed;
        this.health = health;
        this.damages = damages;
        this.defences = defences;
        this.requirements = requirements;
        this.possibleValues = possibleValues;
        this.identifications = identifications;
        this.powders = powders;
        this.powderSlots = powderSlots;
        this.durability = durability;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getEffectStrength() {
        return effectStrength;
    }

    @Override
    public GearType getGearType() {
        return gearType;
    }

    public Optional<GearAttackSpeed> getAttackSpeed() {
        return Optional.ofNullable(attackSpeed);
    }

    public int getHealth() {
        return health;
    }

    @Override
    public int getLevel() {
        return requirements.level();
    }

    public List<Pair<DamageType, RangedValue>> getDamages() {
        return damages;
    }

    public List<Pair<Element, Integer>> getDefences() {
        return defences;
    }

    public GearRequirements getRequirements() {
        return requirements;
    }

    @Override
    public List<StatType> getStatTypes() {
        return identifications.stream().map(StatActualValue::statType).toList();
    }

    @Override
    public List<StatActualValue> getIdentifications() {
        return identifications;
    }

    @Override
    public List<StatPossibleValues> getPossibleValues() {
        return possibleValues;
    }

    @Override
    public ClassType getRequiredClass() {
        return requirements.classType().orElse(null);
    }

    @Override
    public List<Powder> getPowders() {
        return powders;
    }

    @Override
    public int getPowderSlots() {
        return powderSlots;
    }

    @Override
    public CappedValue getDurability() {
        return durability;
    }

    @Override
    public GearTier getGearTier() {
        return GearTier.CRAFTED;
    }

    @Override
    public String toString() {
        return "CraftedGearItem{" + "name='"
                + name + '\'' + ", effectStrength="
                + effectStrength + ", gearType="
                + gearType + ", attackSpeed="
                + attackSpeed + ", health="
                + health + ", damages="
                + damages + ", defences="
                + defences + ", requirements="
                + requirements + ", possibleValues="
                + possibleValues + ", identifications="
                + identifications + ", powders="
                + powders + ", powderSlots="
                + powderSlots + ", durability="
                + durability + '}';
    }
}
