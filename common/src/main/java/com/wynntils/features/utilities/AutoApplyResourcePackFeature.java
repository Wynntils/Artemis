/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ServerResourcePackEvent;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UTILITIES)
public class AutoApplyResourcePackFeature extends Feature {
    @Override
    public void onDisable() {
        Services.ResourcePack.setRequestedPreloadHash(null, "");
    }

    @SubscribeEvent
    public void onResourcePackLoad(ServerResourcePackEvent.Load event) {
        if (!Managers.Connection.onServer()) return;
        UUID packId = event.getId();
        String packHash = event.getHash();

        // Use this resource pack as our preloaded pack
        Services.ResourcePack.setRequestedPreloadHash(packId, packHash);
    }
}
