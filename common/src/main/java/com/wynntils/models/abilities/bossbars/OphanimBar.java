/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities.bossbars;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.bossbar.TrackedBar;
import com.wynntils.models.abilities.type.OphanimOrb;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;

public class OphanimBar extends TrackedBar {
    // Test in OphanimBar_OPHANIM_PATTERN
    private static final Pattern OPHANIM_PATTERN =
            Pattern.compile("^§710s Healed: §f(?<healed>\\d+)% §[3468]\\[(?<orbs>((§[bce7])?⏺){0,7})(§[3468])?\\]$");

    private static final Pattern ORB_PATTERN = Pattern.compile("(?<color>§[bce7])?⏺");

    private int healed = 0;
    private final List<OphanimOrb> orbs = new ArrayList<>();

    public OphanimBar() {
        super(OPHANIM_PATTERN);
    }

    public int getHealed() {
        return healed;
    }

    public List<OphanimOrb> getOrbs() {
        return Collections.unmodifiableList(orbs);
    }

    @Override
    public void onUpdateName(Matcher match) {
        healed = Integer.parseInt(match.group("healed"));

        orbs.clear();
        Matcher orbMatcher = ORB_PATTERN.matcher(match.group("orbs"));

        // Due to color code truncation in Component to StyledText conversion, not every orb icon has a color code
        // prefix. In case there is none we assume the orb has the same health state as the previous one.
        OphanimOrb.HealthState healthState = null;
        int start = 0;
        while (orbMatcher.find(start)) {
            String colorCode = orbMatcher.group("color");
            if (colorCode != null) {
                healthState = OphanimOrb.HealthState.fromColor(ChatFormatting.getByCode(colorCode.charAt(1)));
            }

            if (healthState == null) {
                WynntilsMod.warn("Error parsing Ophanim Orbs in string " + match.group("orbs"));
                orbs.clear();
                return;
            }

            orbs.add(new OphanimOrb(healthState));

            start = orbMatcher.end();
        }
    }

    @Override
    public void onUpdateProgress(float progress) {
        updateValue((int) (progress * 100), 100);
    }
}
