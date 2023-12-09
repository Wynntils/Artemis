/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.rewards.type;

import com.google.common.collect.Streams;
import com.wynntils.models.gear.type.GearMetaInfo;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// A note about baseStats and variableStats:
// baseStats are the stats that are only affected within the CharmRequirement's level range.
// baseStats looks just like variableStats, when displayed in-game.
// variableStats are the stats that are regular identifications.
public record CharmInfo(
        String name,
        GearTier tier,
        GearMetaInfo metaInfo,
        CharmRequirements requirements,
        List<Pair<StatType, StatPossibleValues>> baseStats,
        List<Pair<StatType, StatPossibleValues>> variableStats) {
    public Map<StatType, StatPossibleValues> getVariableStatsMap() {
        // Treat both baseStats and variableStats as item identifications.
        return identificationStream().collect(Collectors.toMap(Pair::key, Pair::value));
    }

    public List<StatPossibleValues> getPossibleValueList() {
        return identificationStream().map(Pair::value).toList();
    }

    public Stream<Pair<StatType, StatPossibleValues>> identificationStream() {
        return Streams.concat(baseStats.stream(), variableStats().stream());
    }
}
