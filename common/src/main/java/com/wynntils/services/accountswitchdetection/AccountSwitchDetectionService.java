/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.accountswitchdetection;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Service;
import com.wynntils.mc.event.TickAlwaysEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;

public class AccountSwitchDetectionService extends Service {
    private UUID currentAccount = null;

    public AccountSwitchDetectionService() {
        super(List.of());
    }

    @SubscribeEvent
    public void onTickAlways(TickAlwaysEvent e) {
        if (Models.WorldState.getCurrentState() != WorldState.NOT_CONNECTED)
            return; // account switches only happen when not connected

        if (currentAccount == null) {
            currentAccount = McUtils.mc().getUser().getProfileId();
        } else if (currentAccount != McUtils.mc().getUser().getProfileId()) {
            currentAccount = McUtils.mc().getUser().getProfileId();
            WynntilsMod.info("Account switch detected, reloading configs and storages");
            Managers.Config.reloadAll();
            Managers.Storage.reloadAll();
        }
    }
}
