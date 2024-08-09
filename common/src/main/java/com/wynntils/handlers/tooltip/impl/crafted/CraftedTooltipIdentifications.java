/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.impl.crafted;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.tooltip.type.TooltipStyle;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.items.properties.CraftedItemProperty;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatListDelimiter;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class CraftedTooltipIdentifications {
    public static List<Component> buildTooltip(
            CraftedItemProperty craftedItem, ClassType currentClass, TooltipStyle style) {
        List<Component> identifications = new ArrayList<>();

        List<StatType> listOrdering = Models.Stat.getOrderingList(style.identificationOrdering());
        List<StatType> allStats = new ArrayList<>(craftedItem.getStatTypes());

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

            MutableComponent line = getStatLine(statType, craftedItem, currentClass, style);
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
            StatType statType, CraftedItemProperty craftedItem, ClassType currentClass, TooltipStyle style) {
        StatActualValue statActualValue = craftedItem.getIdentifications().stream()
                .filter(stat -> stat.statType() == statType)
                .findFirst()
                .orElse(null);
        if (statActualValue == null) {
            WynntilsMod.warn("Missing value in item " + craftedItem.getName() + " for stat: " + statType);
            return null;
        }

        MutableComponent line = buildIdentifiedLine(craftedItem, style, statActualValue, currentClass);

        return line;
    }

    private static MutableComponent buildIdentifiedLine(
            CraftedItemProperty craftedItem, TooltipStyle style, StatActualValue actualValue, ClassType currentClass) {
        StatType statType = actualValue.statType();
        int value = actualValue.value();

        int valueToShow = statType.calculateAsInverted() ? -value : value;
        boolean hasPositiveEffect = valueToShow > 0 ^ statType.displayAsInverted();

        MutableComponent line = Component.literal(StringUtils.toSignedString(valueToShow)
                        + statType.getUnit().getDisplayName())
                .withStyle(Style.EMPTY.withColor(hasPositiveEffect ? ChatFormatting.GREEN : ChatFormatting.RED));

        // Consumables don't have show max value
        if (style.showMaxValue() && !(craftedItem instanceof CraftedConsumableItem)) {
            craftedItem.getPossibleValues().stream()
                    .filter(possibleValues -> possibleValues.statType() == statType)
                    .findFirst()
                    .ifPresent(possibleValues -> {
                        line.append(Component.literal("/"
                                        + StringUtils.toSignedString(
                                                possibleValues.range().high())
                                        + statType.getUnit().getDisplayName())
                                .withStyle(ChatFormatting.DARK_GRAY));
                    });
        }

        line.append(Component.literal(" "
                        + Models.Stat.getDisplayName(
                                statType, craftedItem.getRequiredClass(), currentClass, RangedValue.NONE))
                .withStyle(ChatFormatting.GRAY));

        return line;
    }
}
