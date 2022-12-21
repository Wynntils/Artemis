/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.bossbar;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.bossbar.TrackedBar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManaBankBar extends TrackedBar {
    public ManaBankBar() {
        super(Pattern.compile("§bMana Bank §3\\[(\\d+)/(\\d+)§3\\]"), BossBarModel.MANABANK);
    }

    @Override
    public void onUpdateName(Matcher match) {
        try {
            current = Integer.parseInt(match.group(1));
            max = Integer.parseInt(match.group(2));
        } catch (NumberFormatException e) {
            WynntilsMod.error(String.format(
                    "Failed to parse current and max for mana bank bar (%s out of %s)",
                    match.group(1), match.group(2)));
        }
    }
}
