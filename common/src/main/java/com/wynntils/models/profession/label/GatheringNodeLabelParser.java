/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.label;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelParser;
import com.wynntils.models.profession.type.MaterialProfile;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.Pair;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;

public class GatheringNodeLabelParser implements LabelParser<ProfessionGatheringNodeLabelInfo> {
    // Note: At the moment, only "Dernic" tier does not have the types consistently as a suffix
    private static final Pattern GATHERING_NODE_LABEL = Pattern.compile("^§(.)(.+?)(:?\\s(Fish|Seed|Ore|Wood))?\n$");

    @Override
    public ProfessionGatheringNodeLabelInfo getInfo(StyledText label, Location location, Entity entity) {
        if (label.isEmpty()) return null;

        Matcher matcher = StyledText.fromPart(label.getFirstPart()).getMatcher(GATHERING_NODE_LABEL);
        if (matcher.matches()) {
            Optional<Pair<MaterialProfile.MaterialType, MaterialProfile.SourceMaterial>> materialLookup =
                    MaterialProfile.findByMaterialName(
                            matcher.group(2),
                            ChatFormatting.getByCode(matcher.group(1).charAt(0)));

            return materialLookup
                    .map(materialTypeSourceMaterialPair -> new ProfessionGatheringNodeLabelInfo(
                            label,
                            matcher.group(2) + " Node",
                            location,
                            entity,
                            materialTypeSourceMaterialPair.value(),
                            materialTypeSourceMaterialPair.key()))
                    .orElse(null);
        }

        return null;
    }
}
