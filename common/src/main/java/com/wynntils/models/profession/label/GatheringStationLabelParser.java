/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.label;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelParser;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.mc.type.Location;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.entity.Entity;

public class GatheringStationLabelParser implements LabelParser<ProfessionCraftingStationLabelInfo> {
    // Note: The lines are multi-line, so we use ^ to match the start of the line (without $ to match the end)
    private static final Pattern GATHERING_STATION_LABEL_PATTERN =
            Pattern.compile("^§f[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] §6§l(.+)§f [ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ]");

    @Override
    public ProfessionCraftingStationLabelInfo getInfo(StyledText label, Location location, Entity entity) {
        Matcher matcher = label.getMatcher(GATHERING_STATION_LABEL_PATTERN);
        if (matcher.find()) {
            ProfessionType professionType = ProfessionType.fromString(matcher.group(1));
            if (professionType == null) return null;

            return new ProfessionCraftingStationLabelInfo(
                    label, matcher.group(1) + " Station", location.offset(0, -2, 0), entity, professionType);
        }

        return null;
    }
}
