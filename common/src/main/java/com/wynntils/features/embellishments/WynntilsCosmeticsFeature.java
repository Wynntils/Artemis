/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.embellishments;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.PlayerRenderLayerEvent;
import com.wynntils.utils.mc.McUtils;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.EMBELLISHMENTS)
public class WynntilsCosmeticsFeature extends Feature {
    @Persisted
    public final Config<Boolean> renderOwnCape = new Config<>(true);

    @SubscribeEvent
    public void onCapeRender(PlayerRenderLayerEvent.Cape event) {
        if (!isEnabled() || !Managers.Connection.onServer()) return;
        if (McUtils.player().is(event.getPlayer()) && !renderOwnCape.get()) return;

        if (Services.Cosmetics.shouldRenderCape(event.getPlayer(), false)) {
            // Cancel default cape rendering, so ours doesn't cause a double up of capes
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onElytraRender(PlayerRenderLayerEvent.Elytra event) {
        if (!isEnabled() || !Managers.Connection.onServer()) return;
        if (McUtils.player().is(event.getPlayer()) && !renderOwnCape.get()) return;

        if (Services.Cosmetics.shouldRenderCape(event.getPlayer(), true)) {
            // This might not be necessary?
            event.setCanceled(true);
        }
    }
}
