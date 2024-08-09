/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.impl.identifiable;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.tooltip.type.TooltipIdentificationDecorator;
import com.wynntils.handlers.tooltip.type.TooltipStyle;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.items.properties.IdentifiableItemProperty;
import com.wynntils.models.stats.StatCalculator;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatListDelimiter;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class TooltipIdentifications {
    public static List<Component> buildTooltip(
            IdentifiableItemProperty<?, ?> itemInfo,
            ClassType currentClass,
            TooltipIdentificationDecorator decorator,
            TooltipStyle style) {
        List<Component> identifications = new ArrayList<>();

        List<StatType> listOrdering = Models.Stat.getOrderingList(style.identificationOrdering());
        List<StatType> allStats = new ArrayList<>(itemInfo.getVariableStats());

        // If the item instance contains identifications with stat types not present in the
        // variable stats list, add these as well to the list of stats to be displayed.
        // This should not happen, but might if the info from the API is not up to date with the actual item.
        itemInfo.getIdentifications().stream()
                .map(StatActualValue::statType)
                .filter(stat -> !allStats.contains(stat))
                .forEach(allStats::add);

        if (allStats.isEmpty()) return identifications;

        boolean useDelimiters = style.useDelimiters();

        boolean delimiterNeeded = false;
        // We need to iterate over all possible stats in order, to be able
        // to inject delimiters, instead of just using Models.Stat.getSortedStats
        for (StatType statType : listOrdering) {
            if (useDelimiters && statType instanceof StatListDelimiter) {
                if (delimiterNeeded) {
                    identifications.add(Component.literal(""));
                    delimiterNeeded = false;
                }
            }
            // Most stat types are probably not valid for this gear
            if (!allStats.contains(statType)) continue;

            MutableComponent line = getStatLine(statType, itemInfo, currentClass, decorator, style);
            if (line == null) continue;

            identifications.add(line);
            delimiterNeeded = true;
        }

        if (!identifications.isEmpty() && identifications.getLast().getString().isEmpty()) {
            // Remove last line if it is a delimiter line
            identifications.removeLast();
        }

        return identifications;
    }

    private static MutableComponent getStatLine(
            StatType statType,
            IdentifiableItemProperty<?, ?> itemInfo,
            ClassType currentClass,
            TooltipIdentificationDecorator decorator,
            TooltipStyle style) {
        if (!itemInfo.getIdentifications().isEmpty()) {
            // We have an actual value
            StatActualValue statActualValue = itemInfo.getIdentifications().stream()
                    .filter(stat -> stat.statType() == statType)
                    .findFirst()
                    .orElse(null);
            if (statActualValue == null) {
                WynntilsMod.warn("Missing value in item " + itemInfo.getName() + " for stat: " + statType);
                return null;
            }

            MutableComponent line = buildIdentifiedLine(itemInfo, style, statActualValue, currentClass);

            StatPossibleValues possibleValues = itemInfo.getPossibleValues().stream()
                    .filter(stat -> stat.statType() == statType)
                    .findFirst()
                    .orElse(null);
            // Normally this should not happen, but if our API data does not match the
            // actual gear, it might, so handle it gracefully
            if (possibleValues == null) {
                WynntilsMod.warn("Missing stat type in item " + itemInfo.getName() + " for stat: " + statType
                        + " which has value: " + statActualValue.value());
                return line;
            }

            if (possibleValues.range().isFixed() || decorator == null) return line;

            // Append decorations
            line.append(decorator.getSuffix(statActualValue, possibleValues, style));

            return line;
        } else {
            // Can only show range of possible values
            StatPossibleValues possibleValues = itemInfo.getPossibleValues().stream()
                    .filter(stat -> stat.statType() == statType)
                    .findFirst()
                    .orElse(null);
            if (possibleValues == null) {
                WynntilsMod.warn("Missing possible values for stat type in item " + itemInfo.getName() + " for stat: "
                        + statType);
                return null;
            }

            return buildUnidentifiedLine(itemInfo, style, possibleValues);
        }
    }

    private static MutableComponent buildIdentifiedLine(
            IdentifiableItemProperty itemInfo,
            TooltipStyle style,
            StatActualValue actualValue,
            ClassType currentClass) {
        StatType statType = actualValue.statType();
        int value = actualValue.value();

        int valueToShow = statType.calculateAsInverted() ? -value : value;
        boolean hasPositiveEffect = valueToShow > 0 ^ statType.displayAsInverted();
        String starString = style.showStars() ? "***".substring(3 - actualValue.stars()) : "";

        MutableComponent line = Component.literal(StringUtils.toSignedString(valueToShow)
                        + statType.getUnit().getDisplayName())
                .withStyle(Style.EMPTY.withColor(hasPositiveEffect ? ChatFormatting.GREEN : ChatFormatting.RED));

        if (!starString.isEmpty()) {
            line.append(Component.literal(starString).withStyle(ChatFormatting.DARK_GREEN));
        }

        line.append(Component.literal(" "
                        + Models.Stat.getDisplayName(
                                statType,
                                itemInfo.getRequiredClass(),
                                currentClass,
                                itemInfo.getIdentificationLevelRange()))
                .withStyle(ChatFormatting.GRAY));

        return line;
    }

    private static MutableComponent buildUnidentifiedLine(
            IdentifiableItemProperty itemInfo, TooltipStyle style, StatPossibleValues possibleValues) {
        StatType statType = possibleValues.statType();
        RangedValue valueRange = possibleValues.range();

        // Determine which value to show first and which to show last in the "A to B"
        // range displayed
        Pair<Integer, Integer> displayRange =
                StatCalculator.getDisplayRange(possibleValues, style.showBestValueLastAlways());

        // Use displayRange.a as representative; assume both a and b are either < or > 0.
        boolean hasPositiveEffect = displayRange.a() > 0 ^ statType.displayAsInverted();
        ChatFormatting colorCode = hasPositiveEffect ? ChatFormatting.GREEN : ChatFormatting.RED;
        ChatFormatting colorCodeDark = hasPositiveEffect ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;

        MutableComponent line =
                Component.literal(StringUtils.toSignedString(displayRange.a())).withStyle(colorCode);
        line.append(Component.literal(" to ").withStyle(colorCodeDark));
        line.append(Component.literal(displayRange.b() + statType.getUnit().getDisplayName())
                .withStyle(colorCode));

        line.append(Component.literal(" "
                        + Models.Stat.getDisplayName(
                                statType,
                                itemInfo.getRequiredClass(),
                                Models.Character.getClassType(),
                                itemInfo.getIdentificationLevelRange()))
                .withStyle(ChatFormatting.GRAY));

        return line;
    }
}
