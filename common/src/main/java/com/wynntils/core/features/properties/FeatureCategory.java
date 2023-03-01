/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.properties;

import java.util.Locale;
import net.minecraft.client.resources.language.I18n;

public enum FeatureCategory {
    UNCATEGORIZED,
    CHAT,
    COMBAT,
    COMMANDS,
    GLOBAL,
    INVENTORY,
    MAP,
    OVERLAYS,
    PLAYERS,
    REDIRECTS,
    TOOLTIPS,
    UI,
    WYNNTILS;

    FeatureCategory() {
        assert !toString().startsWith("core.wynntils");
    }

    @Override
    public String toString() {
        return I18n.get("core.wynntils.category." + this.name().toLowerCase(Locale.ROOT));
    }
}
