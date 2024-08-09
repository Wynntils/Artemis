/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.npc.label;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelParser;
import com.wynntils.utils.mc.type.Location;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.entity.Entity;

public class NpcLabelParser implements LabelParser<NpcLabelInfo> {
    // Test in NpcLabelParser_NPC_LABEL_PATTERN
    private static final Pattern NPC_LABEL_PATTERN =
            Pattern.compile("^(?:(?<icon>§..+)\n)?§d(?<name>[^§]+)(?:\n(?<description>§..+))?$", Pattern.DOTALL);

    // Special cases
    private static final Pattern TRADE_MARKET_LABEL_PATTERN = Pattern.compile("^§cTrade Market$");
    private static final Pattern HOUSING_LABEL_PATTERN = Pattern.compile("^§fClick §7to go to your housing plot$");

    public NpcLabelInfo getInfo(StyledText label, Location location, Entity entity) {
        Matcher matcher = NPC_LABEL_PATTERN.matcher(label.getString());
        if (matcher.matches()) {
            return new NpcLabelInfo(
                    label,
                    matcher.group("name"),
                    location.offset(0, -1, 0),
                    entity,
                    matcher.group("icon"),
                    matcher.group("description"));
        }

        if (label.matches(TRADE_MARKET_LABEL_PATTERN)) {
            return new NpcLabelInfo(label, "Trade Market", location.offset(0, -1, 0), entity);
        }

        if (label.matches(HOUSING_LABEL_PATTERN)) {
            return new NpcLabelInfo(label, "Housing", location, entity);
        }

        return null;
    }
}
