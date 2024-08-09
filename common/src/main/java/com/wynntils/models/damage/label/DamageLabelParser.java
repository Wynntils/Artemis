/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.damage.label;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.handlers.labels.type.LabelParser;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.utils.mc.type.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.entity.Entity;

public class DamageLabelParser implements LabelParser<DamageLabelInfo> {
    // Test in DamageLabelParser_DAMAGE_LABEL_PATTERN
    private static final Pattern DAMAGE_LABEL_PATTERN = Pattern.compile("(?:§[245bcef](?:§l)?-(\\d+) ([❤✦✤❉❋✹☠]) )+");

    @Override
    public DamageLabelInfo getInfo(StyledText label, Location location, Entity entity) {
        Matcher matcher = label.getMatcher(DAMAGE_LABEL_PATTERN);
        if (!matcher.matches()) return null;

        Map<DamageType, Long> damages = new HashMap<>();

        // Each text part is a different damage type
        for (StyledTextPart part : label) {
            String partString = part.getString(null, PartStyle.StyleType.NONE);

            String[] parts = partString.split(" ");
            if (parts.length != 2) {
                WynntilsMod.warn("Invalid damage label part: " + partString);
                continue;
            }

            long damage = Long.parseLong(parts[0]);

            // Damage is always shown "negative" in the label
            damage = -damage;

            // Sanity check the damage value
            if (damage < 0) {
                WynntilsMod.warn("Player did negative damage: " + damage);
                continue;
            }

            DamageType damageType = DamageType.fromSymbol(parts[1]);
            if (damageType == null) {
                WynntilsMod.warn("Unknown damage type: " + parts[1]);
                continue;
            }

            damages.put(damageType, damage);
        }

        if (damages.isEmpty()) {
            WynntilsMod.warn("No valid damage types found in label: " + label);
            return null;
        }

        return new DamageLabelInfo(label, location, entity, damages);
    }
}
