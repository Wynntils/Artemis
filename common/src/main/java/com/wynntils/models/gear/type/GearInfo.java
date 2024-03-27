/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear.type;

import com.wynntils.models.stats.type.FixedStats;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GearInfo(
        String name,
        GearType type,
        GearTier tier,
        int powderSlots,
        GearMetaInfo metaInfo,
        GearRequirements requirements,
        FixedStats fixedStats,
        List<Pair<StatType, StatPossibleValues>> variableStats) {
    public StatPossibleValues getPossibleValues(StatType statType) {
        return this.variableStats().stream()
                .filter(p -> p.key().equals(statType))
                .findFirst()
                .map(Pair::value)
                .orElse(null);
    }

    public List<StatType> getVariableStats() {
        return variableStats().stream().map(Pair::key).toList();
    }

    public Map<StatType, StatPossibleValues> getVariableStatsMap() {
        return variableStats().stream().collect(Collectors.toMap(Pair::key, Pair::value));
    }

    public List<StatPossibleValues> getPossibleValueList() {
        return variableStats().stream().map(Pair::value).toList();
    }
}
