/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gearinfo.type;

import com.wynntils.core.WynntilsMod;
import com.wynntils.models.concepts.Powder;
import com.wynntils.models.gearinfo.GearCalculator;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

// FIXME: GearInstance is missing Powder Specials...
public record GearInstance(
        List<StatActualValue> identifications, List<Powder> powders, int rerolls, Optional<Float> overallQuality) {
    public static GearInstance create(
            GearInfo gearInfo, List<StatActualValue> identifications, List<Powder> powders, int rerolls) {
        return new GearInstance(identifications, powders, rerolls, calculateOverallQuality(gearInfo, identifications));
    }

    private static Optional<Float> calculateOverallQuality(GearInfo gearInfo, List<StatActualValue> identifications) {
        DoubleSummaryStatistics percents = identifications.stream()
                .filter(actualValue -> {
                    // We do not include values that cannot possibly change
                    StatPossibleValues possibleValues = gearInfo.getPossibleValues(actualValue.statType());
                    if (possibleValues == null) {
                        WynntilsMod.warn("Error:" + gearInfo.name() + " claims to have identification "
                                + actualValue.statType());
                        return false;
                    }
                    return !possibleValues.range().isFixed();
                })
                .mapToDouble(actualValue -> {
                    StatPossibleValues possibleValues = gearInfo.getPossibleValues(actualValue.statType());
                    return GearCalculator.getPercent(actualValue, possibleValues);
                })
                .summaryStatistics();
        if (percents.getCount() == 0) return Optional.empty();

        return Optional.of((float) percents.getAverage());
    }

    public boolean hasOverallValue() {
        return overallQuality.isPresent();
    }

    public float getOverallPercentage() {
        return overallQuality.orElse(0.0f);
    }

    public boolean isPerfect() {
        return overallQuality.orElse(0.0f) >= 100.0f;
    }

    public boolean isDefective() {
        return overallQuality.orElse(0.0f) <= 0.0f;
    }

    public StatActualValue getActualValue(StatType statType) {
        return identifications.stream()
                .filter(s -> s.statType().equals(statType))
                .findFirst()
                .orElse(null);
    }
}
