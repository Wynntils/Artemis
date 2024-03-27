/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.game;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.gear.type.SetInfo;
import com.wynntils.models.gear.type.SetInstance;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.GearTypeItemProperty;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.models.items.properties.LeveledItemProperty;
import com.wynntils.models.items.properties.PowderedItemProperty;
import com.wynntils.models.items.properties.RequirementItemProperty;
import com.wynntils.models.items.properties.RerollableItemProperty;
import com.wynntils.models.items.properties.SetItemProperty;
import com.wynntils.models.items.properties.ShinyItemProperty;
import com.wynntils.models.stats.type.ShinyStat;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.List;
import java.util.Optional;

public class GearItem extends GameItem
        implements GearTierItemProperty,
                GearTypeItemProperty,
                LeveledItemProperty,
                PowderedItemProperty,
                RerollableItemProperty,
                ShinyItemProperty,
                IdentifiableItemProperty<GearInfo, GearInstance>,
                SetItemProperty,
                RequirementItemProperty {
    private final GearInfo gearInfo;
    private final GearInstance gearInstance;
    private final Optional<SetInfo> setInfo;
    private final Optional<SetInstance> setInstance;

    public GearItem(
            GearInfo gearInfo,
            GearInstance gearInstance,
            Optional<SetInfo> setInfo,
            Optional<SetInstance> setInstance) {
        this.gearInfo = gearInfo;
        this.gearInstance = gearInstance;
        this.setInfo = setInfo;
        this.setInstance = setInstance;
    }

    @Override
    public GearInfo getItemInfo() {
        return gearInfo;
    }

    @Override
    public Optional<GearInstance> getItemInstance() {
        return Optional.ofNullable(gearInstance);
    }

    @Override
    public Optional<SetInfo> getSetInfo() {
        return setInfo;
    }

    @Override
    public Optional<SetInstance> getSetInstance() {
        return setInstance;
    }

    public boolean isUnidentified() {
        return gearInstance == null;
    }

    @Override
    public GearTier getGearTier() {
        return gearInfo.tier();
    }

    @Override
    public GearType getGearType() {
        return gearInfo.type();
    }

    @Override
    public int getLevel() {
        return gearInfo.requirements().level();
    }

    @Override
    public String getName() {
        return gearInfo.name();
    }

    @Override
    public ClassType getRequiredClass() {
        return gearInfo.requirements().classType().orElse(null);
    }

    @Override
    public List<StatType> getVariableStats() {
        return gearInfo.variableStats().stream().map(Pair::a).toList();
    }

    @Override
    public List<StatActualValue> getIdentifications() {
        if (gearInstance == null) return List.of();

        return gearInstance.identifications();
    }

    @Override
    public List<StatPossibleValues> getPossibleValues() {
        return gearInfo.variableStats().stream().map(Pair::b).toList();
    }

    @Override
    public RangedValue getIdentificationLevelRange() {
        // Gears do not have level-specific stats
        return RangedValue.NONE;
    }

    @Override
    public boolean hasOverallValue() {
        return gearInstance != null && gearInstance.hasOverallValue();
    }

    @Override
    public boolean isPerfect() {
        return gearInstance != null && gearInstance.isPerfect();
    }

    @Override
    public boolean isDefective() {
        return gearInstance != null && gearInstance.isDefective();
    }

    @Override
    public float getOverallPercentage() {
        return gearInstance != null ? gearInstance.getOverallPercentage() : 0.0f;
    }

    @Override
    public int getPowderSlots() {
        return gearInfo.powderSlots();
    }

    @Override
    public List<Powder> getPowders() {
        return gearInstance != null ? gearInstance.powders() : List.of();
    }

    @Override
    public int getRerollCount() {
        return gearInstance != null ? gearInstance.rerolls() : 0;
    }

    @Override
    public Optional<ShinyStat> getShinyStat() {
        return gearInstance != null ? gearInstance.shinyStat() : Optional.empty();
    }

    @Override
    public boolean meetsActualRequirements() {
        // probably migrate this to GearInstance?
        for (StyledText loreLine : LoreUtils.getLore(this)) {
            if (loreLine.startsWith("§c✖") && loreLine.contains("Min: ")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "GearItem{" + "gearInfo="
                + gearInfo + ", gearInstance="
                + gearInstance + ", setInfo="
                + setInfo + ", setInstance="
                + setInstance + '}';
    }
}
