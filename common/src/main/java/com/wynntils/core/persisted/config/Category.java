/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.wynntils.utils.render.Texture;
import java.util.Locale;
import net.minecraft.client.resources.language.I18n;

public enum Category {
    UNCATEGORIZED(Texture.UNCATEGORIZED_CONFIG_ICON),
    CHAT(Texture.CHAT_CONFIG_ICON),
    COMBAT(Texture.COMBAT_CONFIG_ICON),
    COMMANDS(Texture.COMMANDS_CONFIG_ICON),
    DEBUG(Texture.DEBUG_CONFIG_ICON),
    EMBELLISHMENTS(Texture.EMBELLISHMENTS_CONFIG_ICON),
    INVENTORY(Texture.INVENTORY_CONFIG_ICON),
    MAP(Texture.MAP_CONFIG_ICON),
    OVERLAYS(Texture.OVERLAYS_CONFIG_ICON),
    PLAYERS(Texture.PLAYERS_CONFIG_ICON),
    REDIRECTS(Texture.REDIRECTS_CONFIG_ICON),
    TOOLTIPS(Texture.TOOLTIPS_CONFIG_ICON),
    TRADEMARKET(Texture.TRADE_MARKET_CONFIG_ICON),
    UI(Texture.UI_CONFIG_ICON),
    UTILITIES(Texture.UTILITIES_CONFIG_ICON),
    WYNNTILS(Texture.WYNNTILS_CONFIG_ICON);

    private final Texture categoryIcon;

    Category(Texture categoryIcon) {
        assert !toString().startsWith("core.wynntils");
        this.categoryIcon = categoryIcon;
    }

    public Texture getCategoryIcon() {
        return categoryIcon;
    }

    @Override
    public String toString() {
        return I18n.get("core.wynntils.category." + this.name().toLowerCase(Locale.ROOT));
    }
}
