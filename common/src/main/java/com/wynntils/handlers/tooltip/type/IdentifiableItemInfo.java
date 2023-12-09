/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.type;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.RangedValue;
import java.util.List;

/**
 * Represents an item that can be identified, and provides the information needed to build a tooltip for it.
 */
public interface IdentifiableItemInfo {
    String getName();

    ClassType getRequiredClass();

    List<StatType> getVariableStats();

    List<StatActualValue> getIdentifications();

    List<StatPossibleValues> getPossibleValues();

    RangedValue getIdentificationLevelRange();

    boolean hasOverallValue();

    boolean isPerfect();

    boolean isDefective();

    float getOverallPercentage();
}
