/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear.type;

import com.wynntils.models.stats.type.StatType;
import java.util.Map;

// wynncraftCount can eventually be removed when Wynncraft fixes their double ring bug...
// Essentially if you have two of the same ring on, it's only counted once
public class SetInstance {
    private final SetInfo setInfo;
    private final Map<String, Boolean> activeItems;
    private int trueCount;

    public SetInstance(SetInfo setInfo, Map<String, Boolean> activeItems, int trueCount) {
        this.setInfo = setInfo;
        this.trueCount = trueCount;
        this.activeItems = activeItems;
    }

    public SetInfo getSetInfo() {
        return setInfo;
    }

    public Map<String, Boolean> getActiveItems() {
        return activeItems;
    }

    public int getTrueCount() {
        return trueCount;
    }

    public void setTrueCount(int trueCount) {
        this.trueCount = trueCount;
    }

    public Map<StatType, Integer> getTrueCountBonuses() {
        return setInfo.getBonusForItems(trueCount);
    }

    @Override
    public String toString() {
        return "SetInstance{" + "setInfo="
                + setInfo + ", activeItems="
                + activeItems + ", trueCount="
                + trueCount + '}';
    }
}
